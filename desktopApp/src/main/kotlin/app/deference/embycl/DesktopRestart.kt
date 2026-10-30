package app.deference.embycl

import java.io.File
import java.util.concurrent.TimeUnit

private const val RESTART_PARENT_ARGUMENT = "--embyex-restart-after="

/**
 * Starts the same JVM/native executable in a waiting mode, then lets the caller
 * exit. The replacement waits until this process has ended before entering
 * nucleusApplication, avoiding Nucleus' single-instance lock race.
 *
 * This requires the current process command line to be available from ProcessHandle.
 */
fun restartDesktopProcess() {
    val current = ProcessHandle.current()
    val command = current.info().command().orElseThrow {
        IllegalStateException("Unable to determine current executable for restart.")
    }
    val arguments = current.info().arguments().orElseThrow {
        IllegalStateException("Unable to read current process arguments for restart.")
    }
    val childCommand = buildList {
        add(command)
        addAll(arguments)
        add("$RESTART_PARENT_ARGUMENT${current.pid()}")
    }

    ProcessBuilder(childCommand)
        .directory(File(System.getProperty("user.dir", ".")))
        .redirectInput(ProcessBuilder.Redirect.DISCARD)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
}

/** Call before nucleusApplication so the waiting instance doesn't acquire its single-instance lock. */
fun waitForPreviousProcessIfRequested(args: Array<String>): Array<String> {
    val marker = args.firstOrNull { it.startsWith(RESTART_PARENT_ARGUMENT) } ?: return args
    val parentPid = marker.removePrefix(RESTART_PARENT_ARGUMENT).toLongOrNull() ?: return args
    val remainingArgs = args.filterNot { it.startsWith(RESTART_PARENT_ARGUMENT) }.toTypedArray()
    val parent = ProcessHandle.of(parentPid).orElse(null) ?: return remainingArgs

    while (parent.isAlive) {
        try {
            Thread.sleep(TimeUnit.MILLISECONDS.toMillis(100))
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            return remainingArgs
        }
    }
    return remainingArgs
}
