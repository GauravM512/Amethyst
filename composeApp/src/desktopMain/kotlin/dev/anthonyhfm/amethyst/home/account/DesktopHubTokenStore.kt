package dev.anthonyhfm.amethyst.home.account

import dev.anthonyhfm.amethyst.hub.data.HubSessionStore
import dev.anthonyhfm.amethyst.hub.data.HubSessionTokens
import java.io.File
import java.util.Base64

/** Uses the user's OS credential store. If unavailable, the session stays in memory. */
internal class DesktopHubTokenStore : HubSessionStore {
    private var volatileTokens: HubSessionTokens? = null
    private val os = System.getProperty("os.name").lowercase()
    private val service = "dev.anthonyhfm.amethyst.hub"
    private val account = System.getProperty("user.name")

    override fun load(): HubSessionTokens? {
        val payload = try {
            when {
                os.contains("mac") -> command(listOf("/usr/bin/security", "find-generic-password", "-a", account, "-s", service, "-w"))
                os.contains("win") -> windowsRead()
                else -> command(listOf("secret-tool", "lookup", "application", service, "account", account))
            }
        } catch (_: Exception) { null }
        return payload?.trim()?.let(::decode) ?: volatileTokens
    }

    override fun save(tokens: HubSessionTokens?) {
        volatileTokens = tokens
        try {
            when {
                os.contains("mac") -> {
                    if (tokens == null) command(listOf("/usr/bin/security", "delete-generic-password", "-a", account, "-s", service))
                    else command(listOf("/usr/bin/security", "add-generic-password", "-U", "-a", account, "-s", service, "-w"), encode(tokens))
                }
                os.contains("win") -> windowsWrite(tokens)
                else -> {
                    if (tokens == null) command(listOf("secret-tool", "clear", "application", service, "account", account))
                    else command(listOf("secret-tool", "store", "--label=Amethyst Hub session", "application", service, "account", account), encode(tokens))
                }
            }
        } catch (_: Exception) {
            // Credential services are optional on Linux. Never write plaintext credentials.
        }
    }

    private fun encode(tokens: HubSessionTokens): String = Base64.getEncoder().encodeToString(
        "${tokens.accessToken}\n${tokens.refreshToken}".toByteArray(Charsets.UTF_8)
    )

    private fun decode(value: String): HubSessionTokens? = runCatching {
        val pieces = String(Base64.getDecoder().decode(value), Charsets.UTF_8).split('\n', limit = 2)
        if (pieces.size != 2 || pieces.any { it.isBlank() }) null
        else HubSessionTokens(pieces[0], pieces[1], 300)
    }.getOrNull()

    private fun command(args: List<String>, input: String? = null): String? {
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        process.outputStream.bufferedWriter().use { writer -> if (input != null) writer.write(input + "\n") }
        val result = process.inputStream.bufferedReader().readText()
        return if (process.waitFor() == 0) result else null
    }

    private fun windowsFile(): File {
        val base = System.getenv("APPDATA") ?: System.getProperty("user.home")
        return File(File(base, "Amethyst"), "hub-session.dpapi")
    }

    private fun windowsWrite(tokens: HubSessionTokens?) {
        val file = windowsFile()
        if (tokens == null) { file.delete(); return }
        file.parentFile.mkdirs()
        val script = """
            Add-Type -AssemblyName System.Security
            ${'$'}raw = [Console]::In.ReadToEnd().Trim()
            ${'$'}plain = [Text.Encoding]::UTF8.GetBytes(${'$'}raw)
            ${'$'}cipher = [Security.Cryptography.ProtectedData]::Protect(${'$'}plain,${'$'}null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
            [IO.File]::WriteAllBytes(${'$'}args[0],${'$'}cipher)
        """.trimIndent()
        command(listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script, file.absolutePath), encode(tokens))
    }

    private fun windowsRead(): String? {
        val file = windowsFile()
        if (!file.isFile) return null
        val script = """
            Add-Type -AssemblyName System.Security
            ${'$'}cipher = [IO.File]::ReadAllBytes(${'$'}args[0])
            ${'$'}plain = [Security.Cryptography.ProtectedData]::Unprotect(${'$'}cipher,${'$'}null,[Security.Cryptography.DataProtectionScope]::CurrentUser)
            [Console]::Out.Write([Text.Encoding]::UTF8.GetString(${'$'}plain))
        """.trimIndent()
        return command(listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script, file.absolutePath))
    }
}
