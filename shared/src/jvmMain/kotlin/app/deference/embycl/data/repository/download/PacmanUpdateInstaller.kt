package app.deference.embycl.data.repository.download

import java.io.File
import kotlin.system.exitProcess

/**
 * Updates an install that pacman owns (the Arch Linux package). Nucleus can only install a
 * `.deb`, `.rpm` or AppImage, and on Linux it runs `pkexec dpkg -i`, which Arch does not have.
 *
 * Unlike Nucleus, the package is installed while the app is still running, so a cancelled
 * authentication or a pacman error leaves the app open and is reported to the user. pacman
 * replaces files by unlinking them, so this process keeps the files it already mapped.
 *
 * Every call blocks; call it off the main thread.
 */
internal class PacmanUpdateInstaller private constructor(
    private val launcher: File,
) {
    /**
     * Installs [packageFile] through `pkexec pacman -U`, which asks for the administrator
     * password with the desktop's polkit agent. Throws [IllegalStateException] carrying a
     * user-facing message when it does not install.
     */
    fun install(packageFile: File) {
        val process = ProcessBuilder(
            PKEXEC,
            PACMAN,
            "-U",
            "--noconfirm",
            packageFile.absolutePath,
        )
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            failureMessage(
                exitCode = exitCode,
                output = output,
            )
        }
        packageFile.delete()
    }

    /** Starts the installed version once this process has exited, then exits. */
    fun restart(): Nothing {
        // The PID and launcher are positional parameters, so no path is parsed by the shell.
        ProcessBuilder(
            "setsid",
            "sh",
            "-c",
            "while kill -0 \"$1\" 2>/dev/null; do sleep 0.5; done; exec \"$2\"",
            "embyex-relaunch",
            ProcessHandle.current().pid().toString(),
            launcher.path,
        )
            .directory(File(System.getProperty("user.home")))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        exitProcess(0)
    }

    private fun failureMessage(
        exitCode: Int,
        output: String,
    ): String = when {
        exitCode == PKEXEC_DISMISSED -> "Authentication was cancelled. Your download is saved."
        output.contains(other = "authentication agent", ignoreCase = true) ->
            "No polkit authentication agent is running, so the administrator password cannot be requested. " +
                "Start one and retry, or install the package from GitHub."
        exitCode == PKEXEC_NOT_AUTHORIZED -> "Authentication failed. Your download is saved."
        else -> output.lineSequence().lastOrNull { it.isNotBlank() }?.trim()
            ?.let { "pacman could not install the update: $it" }
            ?: "pacman could not install the update (exit code $exitCode)."
    }

    companion object {
        private const val PACMAN = "/usr/bin/pacman"
        private const val PKEXEC = "/usr/bin/pkexec"
        private const val PACKAGE_NAME = "embyex"
        private const val PKEXEC_DISMISSED = 126
        private const val PKEXEC_NOT_AUTHORIZED = 127
        const val PACKAGE_SUFFIX = ".pkg.tar.zst"
        fun forCurrentProcess(): PacmanUpdateInstaller? {
            if (!File(PACMAN).canExecute() || !File(PKEXEC).canExecute()) return null
            val launcher = ProcessHandle.current().info().command()
                .map { File(it).canonicalFile }
                .orElse(null)
                ?: return null
            val query = ProcessBuilder(
                PACMAN,
                "-Qqo",
                launcher.path,
            )
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            val owner = query.inputStream.bufferedReader().use { it.readText() }.trim()
            val ownedByApp = query.waitFor() == 0 && owner == PACKAGE_NAME
            return if (ownedByApp) PacmanUpdateInstaller(launcher = launcher) else null
        }
    }
}
