package dev.anthonyhfm.amethyst.core.network

import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectEvent
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectRole
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectSession
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectUser
import dev.anthonyhfm.amethyst.core.network.connect.AmethystConnectContract.ConnectionState
import dev.anthonyhfm.amethyst.core.network.lan.LanConnectProvider
import dev.anthonyhfm.amethyst.core.network.lan.LanDiscoveryService
import dev.anthonyhfm.amethyst.core.network.presence.CollaborationPresence
import dev.anthonyhfm.amethyst.core.network.sync.DeviceSyncBroadcaster
import dev.anthonyhfm.amethyst.core.network.sync.ChainSyncBroadcaster
import dev.anthonyhfm.amethyst.core.network.sync.ChainSyncCoordinator
import dev.anthonyhfm.amethyst.core.network.sync.ChainSyncReceiver
import dev.anthonyhfm.amethyst.core.network.sync.DeviceSyncCoordinator
import dev.anthonyhfm.amethyst.core.network.sync.DeviceSyncReceiver
import dev.anthonyhfm.amethyst.core.network.sync.TimelineSyncBroadcaster
import dev.anthonyhfm.amethyst.core.network.sync.TimelineSyncReceiver
import dev.anthonyhfm.amethyst.core.network.sync.WorkspaceEventBroadcaster
import dev.anthonyhfm.amethyst.core.network.sync.WorkspaceEventReceiver
import dev.anthonyhfm.amethyst.core.network.sync.WorkspaceSyncCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central orchestrator for the LAN collaboration feature.
 */
object CollaborationManager {
    data class InitialSyncProgress(
        val active: Boolean = false,
        val phase: String = "",
        val progress: Float? = null,
        val detail: String = ""
    )

    val provider: LanConnectProvider = LanConnectProvider()

    val connectionState: StateFlow<ConnectionState> = provider.connectionState
    val session: StateFlow<ConnectSession?> = provider.session
    val localUser: StateFlow<ConnectUser?> = provider.localUser

    private val _initialSyncProgress = MutableStateFlow(InitialSyncProgress())
    val initialSyncProgress: StateFlow<InitialSyncProgress> = _initialSyncProgress.asStateFlow()

    val isActive: Boolean
        get() = connectionState.value is ConnectionState.Connected

    val isHosting: Boolean
        get() = localUser.value?.role == ConnectRole.HOST && isActive

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var syncCompletedDeferred: CompletableDeferred<Unit>? = null

    private fun log(message: String) {
        println("[Collaboration ${System.currentTimeMillis()}] $message")
    }

    private fun onFullSyncCompleted() {
        log("onFullSyncCompleted(): completing deferred=${syncCompletedDeferred != null}")
        syncCompletedDeferred?.complete(Unit)
    }

    private fun updateInitialSyncProgress(
        phase: String,
        progress: Float? = null,
        detail: String = "",
        active: Boolean = true
    ) {
        log("initialSyncProgress phase='$phase' progress=$progress detail='$detail' active=$active")
        _initialSyncProgress.value = InitialSyncProgress(
            active = active,
            phase = phase,
            progress = progress,
            detail = detail
        )
    }

    private var workspaceBroadcaster: WorkspaceEventBroadcaster? = null
    private var workspaceReceiver: WorkspaceEventReceiver? = null
    private var deviceBroadcaster: DeviceSyncBroadcaster? = null
    private var deviceReceiver: DeviceSyncReceiver? = null
    private var chainBroadcaster: ChainSyncBroadcaster? = null
    private var chainReceiver: ChainSyncReceiver? = null
    private var timelineBroadcaster: TimelineSyncBroadcaster? = null
    private var timelineReceiver: TimelineSyncReceiver? = null

    suspend fun startHosting(
        sessionName: String,
        localUser: ConnectUser
    ): Result<ConnectSession> {
        log("startHosting(): sessionName='$sessionName' localUser=${localUser.id}/${localUser.name}")
        val result = provider.host(sessionName, localUser)
        if (result.isSuccess) {
            val session = result.getOrThrow()
            log("startHosting(): provider.host success session=${session.id}")
            LanDiscoveryService.startBroadcasting(session)
            startSync(hosting = true)
        } else {
            log("startHosting(): failed ${result.exceptionOrNull()?.message}")
        }
        return result
    }

    suspend fun joinSession(
        hostAddress: String,
        localUser: ConnectUser
    ): Result<ConnectSession> {
        log("joinSession(): start hostAddress=$hostAddress localUser=${localUser.id}/${localUser.name}")
        updateInitialSyncProgress("Connecting", null, hostAddress)
        val deferred = CompletableDeferred<Unit>()
        syncCompletedDeferred = deferred

        // Start sync receivers BEFORE joining so they are subscribed to _events before
        // any incoming events (e.g. FullStateSync) can arrive on the WebSocket.
        startSync(hosting = false)
        log("joinSession(): sync receivers started")
        val result = provider.join(hostAddress, localUser)
        if (!result.isSuccess) {
            log("joinSession(): provider.join failed ${result.exceptionOrNull()?.message}")
            stopSync()
            syncCompletedDeferred = null
            return result
        }
        log("joinSession(): provider.join success session=${result.getOrNull()?.id}; waiting initial sync")
        updateInitialSyncProgress("Waiting for workspace", null, result.getOrNull()?.name.orEmpty())

        return try {
            // Large workspaces can include audio sources; allow enough time for encoding, transfer, and load.
            kotlinx.coroutines.withTimeout(INITIAL_SYNC_TIMEOUT_MS) {
                deferred.await()
            }
            log("joinSession(): initial sync signal received")
            updateInitialSyncProgress("Opening workspace", 1f, "Ready")
            result
        } catch (e: Exception) {
            log("joinSession(): initial sync failed ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            stopSync()
            provider.leave()
            syncCompletedDeferred = null
            updateInitialSyncProgress("Failed to join", null, e.message ?: "", active = false)
            Result.failure(Exception("Failed to complete initial state sync: ${e.message}", e))
        } finally {
            log("joinSession(): clearing syncCompletedDeferred")
            syncCompletedDeferred = null
        }
    }

    suspend fun leaveSession() {
        log("leaveSession()")
        stopSync()
        LanDiscoveryService.stopBroadcasting()
        provider.leave()
    }

    suspend fun sendIfActive(event: ConnectEvent) {
        if (isActive) provider.send(event)
    }

    private fun startSync(hosting: Boolean) {
        log("startSync(hosting=$hosting)")
        stopSync()

        workspaceReceiver = WorkspaceEventReceiver(
            provider = provider,
            scope = scope,
            onFullStateSyncApplied = {
                onFullSyncCompleted()
            },
            onFullStateSyncProgress = { phase, progress, detail ->
                updateInitialSyncProgress(phase, progress, detail)
            }
        ).also { it.start() }
        deviceReceiver = DeviceSyncReceiver(provider, scope).also { it.start() }
        chainReceiver = ChainSyncReceiver(provider, scope).also { it.start() }
        CollaborationPresence.attach(provider, scope)

        workspaceBroadcaster = WorkspaceEventBroadcaster(provider, scope).also {
            it.start()
            WorkspaceSyncCoordinator.attach(it)
        }

        deviceBroadcaster = DeviceSyncBroadcaster(provider, scope).also {
            DeviceSyncCoordinator.attach(it)
        }
        chainBroadcaster = ChainSyncBroadcaster(provider, scope).also {
            it.start()
            ChainSyncCoordinator.attach(it)
        }

        timelineReceiver = TimelineSyncReceiver(provider, scope).also { it.start() }
        timelineBroadcaster = TimelineSyncBroadcaster(provider, scope).also { it.start() }
    }

    private fun stopSync() {
        log("stopSync()")
        workspaceBroadcaster?.stop()
        workspaceBroadcaster?.let { WorkspaceSyncCoordinator.detach(it) }
        workspaceBroadcaster = null

        workspaceReceiver?.stop()
        workspaceReceiver = null

        deviceReceiver?.stop()
        deviceReceiver = null

        deviceBroadcaster?.let { DeviceSyncCoordinator.detach(it) }
        deviceBroadcaster = null

        chainReceiver?.stop()
        chainReceiver = null

        chainBroadcaster?.let {
            it.stop()
            ChainSyncCoordinator.detach(it)
        }
        chainBroadcaster = null

        timelineBroadcaster?.stop()
        timelineBroadcaster = null

        timelineReceiver?.stop()
        timelineReceiver = null

        CollaborationPresence.detach()
    }

    private const val INITIAL_SYNC_TIMEOUT_MS = 5 * 60_000L
}
