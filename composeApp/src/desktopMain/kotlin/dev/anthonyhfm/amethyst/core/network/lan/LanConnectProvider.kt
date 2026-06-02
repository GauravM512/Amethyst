package dev.anthonyhfm.amethyst.core.network.lan

import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectEvent
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectRole
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectSession
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectUser
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectionState
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectProvider
import dev.anthonyhfm.amethyst.core.network.connect.decodeToConnectEvent
import dev.anthonyhfm.amethyst.core.network.connect.encodeToBytes
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * [AmethystConnectProvider] that communicates over a local-area network
 * using Ktor WebSockets (ProtoBuf binary wire format).
 */
class LanConnectProvider : AmethystConnectProvider() {

    companion object {
        const val SERVER_PORT = 7842
        const val WS_PATH = "/session"

        /** Backoff delays (ms) for each successive reconnect attempt (max 5 attempts). */
        private val RECONNECT_DELAYS = longArrayOf(1_000, 2_000, 4_000, 8_000, 16_000)
        private const val CONNECTION_HANDSHAKE_TIMEOUT_MS = 10_000L
        private const val READY_FOR_SYNC_TIMEOUT_MS = 1_000L
        private const val MAX_WORKSPACE_SYNC_FRAME_SIZE = 256L * 1024L * 1024L
        private const val FULL_STATE_SYNC_CHUNK_SIZE = 256 * 1024
    }

    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null

    private val connectedClients = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val sessionMutex = Mutex()

    /** Maps a joining client's userId to a deferred that completes when ReadyForSync arrives. */
    private val pendingStateSyncMap = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    private val httpClient = HttpClient(CIO) {
        install(WebSockets) {
            maxFrameSize = MAX_WORKSPACE_SYNC_FRAME_SIZE
        }
    }

    private var clientSession: DefaultWebSocketSession? = null

    /** Set to false by [leave] to distinguish intentional disconnects from accidental drops. */
    @Volatile private var shouldReconnect = false

    private fun log(message: String) {
        println("[LAN ${System.currentTimeMillis()}] $message")
    }

    private fun eventName(event: ConnectEvent): String =
        event::class.simpleName ?: event::class.toString()

    private fun bytesLabel(bytes: Int): String =
        "${bytes}B (${bytes / 1024.0 / 1024.0}MiB)"

    override suspend fun host(sessionName: String, localUser: ConnectUser): Result<ConnectSession> {
        return runCatching {
            log("host(): starting session name='$sessionName' localUser=${localUser.id}/${localUser.name}")
            updateConnectionState(ConnectionState.Connecting)

            val hostUser = localUser.copy(role = ConnectRole.HOST)
            val sessionId = generateId()
            val session = ConnectSession(
                id = sessionId,
                name = sessionName,
                host = hostUser,
                participants = listOf(hostUser)
            )

            updateLocalUser(hostUser)
            updateSession(session)

            startServer(session)

            updateConnectionState(ConnectionState.Connected(session))
            log("host(): connected session=${session.id} participants=${session.participants.map { it.id }}")
            session
        }.onFailure { e ->
            log("host(): failed ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            updateConnectionState(ConnectionState.Error(e))
        }
    }

    override suspend fun join(address: String, localUser: ConnectUser): Result<ConnectSession> {
        return try {
            log("join(): start address=$address localUser=${localUser.id}/${localUser.name}")
            updateConnectionState(ConnectionState.Connecting)

            val guestUser = localUser.copy(role = ConnectRole.GUEST)
            updateLocalUser(guestUser)

            // Placeholder until the host sends SessionSnapshot with the real metadata.
            val placeholderSession = ConnectSession(
                id = "pending",
                name = address,
                host = ConnectUser("host", "Host", 0, ConnectRole.HOST),
                participants = listOf(guestUser)
            )
            updateSession(placeholderSession)
            log("join(): placeholder session set, waiting for SessionSnapshot timeout=${CONNECTION_HANDSHAKE_TIMEOUT_MS}ms")

            val handshake = CompletableDeferred<ConnectSession>()
            shouldReconnect = false
            connectToHost(address, guestUser, attempt = 0, initialHandshake = handshake)

            val establishedSession = withTimeout(CONNECTION_HANDSHAKE_TIMEOUT_MS) {
                handshake.await()
            }
            shouldReconnect = true
            log("join(): handshake established session=${establishedSession.id} participants=${establishedSession.participants.map { it.id }}")
            Result.success(establishedSession)
        } catch (e: Exception) {
            log("join(): failed ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            shouldReconnect = false
            runCatching {
                clientSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Join failed"))
            }
            clientSession = null
            updateSession(null)
            updateLocalUser(null)
            updateConnectionState(ConnectionState.Error(e))
            Result.failure(e)
        }
    }

    override suspend fun leave() {
        log("leave(): role=${localUser.value?.role} localId=${localUser.value?.id}")
        shouldReconnect = false
        val localId = localUser.value?.id
        val isHost = localUser.value?.role == ConnectRole.HOST

        if (isHost) {
            broadcastToClients(ConnectEvent.SessionEnded.encodeToBytes(), excludeId = null)
            server?.stop(500, 1000)
            server = null
            connectedClients.clear()
        } else {
            if (localId != null) {
                sendRaw(ConnectEvent.UserLeft(localId).encodeToBytes())
            }
            clientSession?.close(CloseReason(CloseReason.Codes.NORMAL, "User left"))
            clientSession = null
        }

        updateSession(null)
        updateLocalUser(null)
        updateConnectionState(ConnectionState.Disconnected())
    }

    override suspend fun send(event: ConnectEvent) {
        log("send(): ${eventName(event)}")
        sendRaw(event.encodeToBytes())
    }

    override suspend fun sendToUser(userId: String, event: ConnectEvent) {
        val bytes = event.encodeToBytes()
        log("sendToUser(): userId=$userId event=${eventName(event)} wireBytes=${bytesLabel(bytes.size)}")
        when (localUser.value?.role) {
            ConnectRole.HOST -> sessionMutex.withLock {
                connectedClients[userId]?.let { ws ->
                    runCatching { ws.send(Frame.Binary(fin = true, data = bytes)) }
                        .onFailure { log("sendToUser(): failed userId=$userId ${it::class.simpleName}: ${it.message}") }
                }
            }
            ConnectRole.GUEST -> sendRaw(bytes)
            null -> Unit
        }
    }

    private suspend fun sendRaw(bytes: ByteArray) {
        log("sendRaw(): role=${localUser.value?.role} wireBytes=${bytesLabel(bytes.size)}")
        when (localUser.value?.role) {
            ConnectRole.HOST -> broadcastToClients(bytes, excludeId = null)
            ConnectRole.GUEST -> clientSession?.send(Frame.Binary(fin = true, data = bytes))
            null -> Unit
        }
    }

    private fun startServer(hostSession: ConnectSession) {
        log("startServer(): port=$SERVER_PORT path=$WS_PATH maxFrameSize=$MAX_WORKSPACE_SYNC_FRAME_SIZE session=${hostSession.id}")
        server = embeddedServer(ServerCIO, port = SERVER_PORT) {
            install(ServerWebSockets) {
                maxFrameSize = MAX_WORKSPACE_SYNC_FRAME_SIZE
            }

            routing {
                webSocket(WS_PATH) {
                    handleIncomingClient(this, hostSession)
                }
            }
        }.start(wait = false)
    }

    private suspend fun handleIncomingClient(
        wsSession: DefaultWebSocketSession,
        hostSession: ConnectSession
    ) {
        var clientUserId: String? = null

        log("host: incoming websocket connected")
        try {
            for (frame in wsSession.incoming) {
                if (frame !is Frame.Binary) {
                    log("host: ignored non-binary frame ${frame::class.simpleName}")
                    continue
                }
                val bytes = frame.data
                log("host: received frame wireBytes=${bytesLabel(bytes.size)}")
                val event = runCatching { bytes.decodeToConnectEvent() }
                    .onFailure {
                        log("host: decode failed ${it::class.simpleName}: ${it.message}")
                        it.printStackTrace()
                    }
                    .getOrNull() ?: continue
                log("host: decoded ${eventName(event)}")

                when {
                    event is ConnectEvent.UserJoined && clientUserId == null -> {
                        clientUserId = event.user.id
                        log("host: UserJoined id=${event.user.id} name=${event.user.name}")
                        sessionMutex.withLock { connectedClients[clientUserId] = wsSession }

                        handleEvent(event)
                        broadcastToClients(bytes, excludeId = clientUserId)

                        val readyDeferred = CompletableDeferred<Unit>()
                        pendingStateSyncMap[clientUserId] = readyDeferred

                        scope.launch(CoroutineName("full-state-sync-$clientUserId")) {
                            try {
                                log("host: full-state pipeline start userId=$clientUserId")
                                sendParticipantSnapshotTo(wsSession)
                                sendSessionSnapshotTo(wsSession)
                                log("host: waiting ReadyForSync userId=$clientUserId timeout=${READY_FOR_SYNC_TIMEOUT_MS}ms")
                                withTimeoutOrNull(READY_FOR_SYNC_TIMEOUT_MS) {
                                    readyDeferred.await()
                                }
                                log("host: sending FullStateSync userId=$clientUserId")
                                sendFullStateSyncTo(wsSession)
                            } finally {
                                log("host: full-state pipeline cleanup userId=$clientUserId")
                                pendingStateSyncMap.remove(clientUserId)
                            }
                        }
                    }

                    event is ConnectEvent.ReadyForSync -> {
                        log("host: ReadyForSync userId=${event.userId}")
                        pendingStateSyncMap[event.userId]?.complete(Unit)
                    }

                    else -> {
                        log("host: forwarding ${eventName(event)} from=$clientUserId")
                        handleEvent(event)
                        broadcastToClients(bytes, excludeId = clientUserId)
                    }
                }
            }
        } finally {
            log("host: websocket finally clientUserId=$clientUserId")
            val id = clientUserId ?: return
            pendingStateSyncMap.remove(id)?.cancel()
            sessionMutex.withLock { connectedClients.remove(id) }
            val leftEvent = ConnectEvent.UserLeft(id)
            handleEvent(leftEvent)
            broadcastToClients(leftEvent.encodeToBytes(), excludeId = id)
        }
    }

    private suspend fun sendParticipantSnapshotTo(wsSession: DefaultWebSocketSession) {
        session.value?.participants.orEmpty().forEach { user ->
            log("host: send participant snapshot user=${user.id}/${user.name}")
            runCatching {
                wsSession.send(Frame.Binary(fin = true, data = ConnectEvent.UserJoined(user).encodeToBytes()))
            }.onFailure { log("host: participant snapshot send failed ${it::class.simpleName}: ${it.message}") }
        }
    }

    /** Sends the real session metadata so the guest can replace its placeholder session. */
    private suspend fun sendSessionSnapshotTo(wsSession: DefaultWebSocketSession) {
        val currentSession = session.value ?: return
        log("host: send SessionSnapshot session=${currentSession.id} participants=${currentSession.participants.map { it.id }}")
        runCatching {
            wsSession.send(Frame.Binary(fin = true, data = ConnectEvent.SessionSnapshot(currentSession).encodeToBytes()))
        }.onFailure { log("host: SessionSnapshot send failed ${it::class.simpleName}: ${it.message}") }
    }

    private suspend fun broadcastToClients(bytes: ByteArray, excludeId: String?) {
        sessionMutex.withLock {
            log("host: broadcast clients=${connectedClients.keys} exclude=$excludeId wireBytes=${bytesLabel(bytes.size)}")
            connectedClients.entries
                .filter { it.key != excludeId }
                .forEach { (_, ws) ->
                    runCatching { ws.send(Frame.Binary(fin = true, data = bytes)) }
                        .onFailure { log("host: broadcast failed ${it::class.simpleName}: ${it.message}") }
                }
        }
    }

    private suspend fun sendFullStateSyncTo(wsSession: DefaultWebSocketSession) {
        runCatching {
            log("host: build FullStateSync start")
            val syncEvent = buildFullStateSyncEvent()
            if (syncEvent.workspaceData.size <= FULL_STATE_SYNC_CHUNK_SIZE) {
                val bytes = syncEvent.encodeToBytes()
                log("host: send FullStateSync workspaceBytes=${bytesLabel(syncEvent.workspaceData.size)} wireBytes=${bytesLabel(bytes.size)}")
                wsSession.send(Frame.Binary(fin = true, data = bytes))
                log("host: FullStateSync send complete")
            } else {
                sendChunkedFullStateSync(wsSession, syncEvent)
            }
        }.onFailure { error ->
            println("LanConnectProvider: failed to send full state sync: ${error.message}")
            error.printStackTrace()
        }
    }

    private suspend fun sendChunkedFullStateSync(
        wsSession: DefaultWebSocketSession,
        syncEvent: ConnectEvent.FullStateSync
    ) {
        val transferId = generateId()
        val chunkCount = (syncEvent.workspaceData.size + FULL_STATE_SYNC_CHUNK_SIZE - 1) / FULL_STATE_SYNC_CHUNK_SIZE
        log(
            "host: send chunked FullStateSync transfer=$transferId workspaceBytes=${bytesLabel(syncEvent.workspaceData.size)} chunks=$chunkCount chunkSize=$FULL_STATE_SYNC_CHUNK_SIZE"
        )

        for (chunkIndex in 0 until chunkCount) {
            val start = chunkIndex * FULL_STATE_SYNC_CHUNK_SIZE
            val end = minOf(start + FULL_STATE_SYNC_CHUNK_SIZE, syncEvent.workspaceData.size)
            val chunk = syncEvent.workspaceData.copyOfRange(start, end)
            val chunkEvent = ConnectEvent.FullStateSyncChunk(
                transferId = transferId,
                chunkIndex = chunkIndex,
                chunkCount = chunkCount,
                workspaceDataChunk = chunk,
                bpm = syncEvent.bpm,
                projectName = syncEvent.projectName,
                macros = syncEvent.macros
            )
            val bytes = chunkEvent.encodeToBytes()
            log("host: send FullStateSyncChunk transfer=$transferId chunk=${chunkIndex + 1}/$chunkCount payloadBytes=${bytesLabel(chunk.size)} wireBytes=${bytesLabel(bytes.size)}")
            wsSession.send(Frame.Binary(fin = true, data = bytes))
        }

        log("host: chunked FullStateSync send complete transfer=$transferId chunks=$chunkCount")
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private fun buildFullStateSyncEvent(): ConnectEvent.FullStateSync {
        val workspaceRepo = dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
        log("host: saveWorkspace() start")
        val data = workspaceRepo.saveWorkspace()
        log("host: saveWorkspace() done tracks=${data.timelineData.size} audioSources=${data.audioSources.size} macros=${data.macros.size}")
        val bytes = dev.anthonyhfm.amethyst.core.util.AmethystProtoBuf.encodeToByteArray(
            dev.anthonyhfm.amethyst.workspace.data.SavableWorkspaceData.serializer(),
            data.copy(
                macros = data.macros.map {
                    dev.anthonyhfm.amethyst.workspace.data.Macro(0)
                }
            )
        )
        log("host: protobuf encode done ${bytesLabel(bytes.size)}")
        return ConnectEvent.FullStateSync(
            workspaceData = bytes,
            bpm = workspaceRepo.bpm.value,
            projectName = workspaceRepo.projectName.value ?: "",
            macros = List(workspaceRepo.macros.value.size) {
                dev.anthonyhfm.amethyst.workspace.data.Macro(0)
            }
        )
    }

    private fun connectToHost(
        address: String,
        guestUser: ConnectUser,
        attempt: Int,
        initialHandshake: CompletableDeferred<ConnectSession>? = null
    ) {
        log("client: connectToHost start address=$address attempt=$attempt initialHandshake=${initialHandshake != null}")
        scope.launch(CoroutineName("lan-client-attempt-$attempt")) {
            try {
                httpClient.webSocket(host = address, port = SERVER_PORT, path = WS_PATH) {
                    log("client: websocket opened address=$address attempt=$attempt")
                    clientSession = this

                    val joinBytes = ConnectEvent.UserJoined(guestUser).encodeToBytes()
                    log("client: sending UserJoined id=${guestUser.id} wireBytes=${bytesLabel(joinBytes.size)}")
                    send(Frame.Binary(fin = true, data = joinBytes))

                    var handshakeDone = false

                    try {
                        for (frame in incoming) {
                            if (frame !is Frame.Binary) {
                                log("client: ignored non-binary frame ${frame::class.simpleName}")
                                continue
                            }
                            val bytes = frame.data
                            log("client: received frame wireBytes=${bytesLabel(bytes.size)}")
                            val event = runCatching { bytes.decodeToConnectEvent() }
                                .onFailure {
                                    log("client: decode failed ${it::class.simpleName}: ${it.message}")
                                    it.printStackTrace()
                                }
                                .getOrNull()
                                ?: continue
                            log("client: decoded ${eventName(event)}")

                            if (!handshakeDone && event is ConnectEvent.SessionSnapshot) {
                                handshakeDone = true
                                log("client: SessionSnapshot handshake session=${event.session.id} participants=${event.session.participants.map { it.id }}")
                                handleEvent(event)
                                updateConnectionState(ConnectionState.Connected(event.session))
                                initialHandshake?.complete(event.session)
                                val readyBytes = ConnectEvent.ReadyForSync(guestUser.id).encodeToBytes()
                                log("client: sending ReadyForSync wireBytes=${bytesLabel(readyBytes.size)}")
                                send(Frame.Binary(fin = true, data = readyBytes))
                            } else {
                                handleEvent(event)
                            }
                        }
                    } finally {
                        log("client: websocket finally handshakeDone=$handshakeDone state=${connectionState.value}")
                        if (!handshakeDone) {
                            initialHandshake?.completeExceptionally(
                                IllegalStateException("Connection closed before session snapshot was received")
                            )
                        }
                        clientSession = null
                        if (connectionState.value is ConnectionState.Connected) {
                            scheduleReconnect(address, guestUser, attempt)
                        }
                    }
                }
            } catch (e: Exception) {
                log("client: websocket failed ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()
                initialHandshake?.completeExceptionally(e)
                clientSession = null
                if (shouldReconnect) {
                    scheduleReconnect(address, guestUser, attempt)
                } else {
                    updateConnectionState(ConnectionState.Error(e))
                }
            }
        }
    }

    private fun scheduleReconnect(address: String, guestUser: ConnectUser, previousAttempt: Int) {
        if (!shouldReconnect) return
        val nextAttempt = previousAttempt + 1
        log("client: scheduleReconnect previous=$previousAttempt next=$nextAttempt")
        if (nextAttempt > RECONNECT_DELAYS.size) {
            updateConnectionState(ConnectionState.Disconnected("Could not reconnect after ${RECONNECT_DELAYS.size} attempts"))
            log("client: reconnect exhausted")
            return
        }
        updateConnectionState(ConnectionState.Reconnecting(nextAttempt))
        scope.launch(CoroutineName("lan-reconnect-$nextAttempt")) {
            log("client: reconnect delay=${RECONNECT_DELAYS[nextAttempt - 1]}ms")
            delay(RECONNECT_DELAYS[nextAttempt - 1])
            if (shouldReconnect) {
                connectToHost(address, guestUser, nextAttempt)
            }
        }
    }

    private fun generateId(): String {
        val chars = ('a'..'f') + ('0'..'9')
        fun seg(len: Int) = (1..len).map { chars.random() }.joinToString("")
        return "${seg(8)}-${seg(4)}-${seg(4)}-${seg(4)}-${seg(12)}"
    }
}
