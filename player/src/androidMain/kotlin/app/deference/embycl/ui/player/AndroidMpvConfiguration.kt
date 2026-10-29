package app.deference.embycl.ui.player

import android.content.Context
import java.io.File

/** Called on the IO dispatcher before native initialization. */
internal fun androidMpvOptions(
    context: Context,
): Map<String, String> {
    val directory = File(context.filesDir, "mpv")
    check(value = directory.isDirectory || directory.mkdirs())
    val certificate = File(directory, "cacert.pem")
    context.assets.open("cacert.pem").use { input ->
        certificate.outputStream().use { output -> input.copyTo(out = output) }
    }
    // libass/fontconfig needs Android's font directories; otherwise unembedded subtitles can be blank.
    File(directory, "fonts.conf").writeText(
        text = """
            <?xml version="1.0"?>
            <fontconfig>
                <dir>/system/fonts</dir>
                <dir>/product/fonts</dir>
                <cachedir>${context.cacheDir.absolutePath}</cachedir>
                <alias>
                    <family>sans-serif</family>
                    <prefer><family>Roboto</family><family>Noto Sans</family></prefer>
                </alias>
                <alias>
                    <family>serif</family>
                    <prefer><family>Noto Serif</family></prefer>
                </alias>
                <alias>
                    <family>monospace</family>
                    <prefer><family>Droid Sans Mono</family></prefer>
                </alias>
            </fontconfig>
        """.trimIndent(),
    )
    return mapOf(
        "config" to "no",
        "config-dir" to directory.absolutePath,
        "load-scripts" to "no",
        "ytdl" to "no",
        "terminal" to "no",
        "msg-level" to "all=no",
        "profile" to "fast",
        // OpenGL works on older phones; mpv falls back to software decoding for unsupported codecs.
        "vo" to "gpu",
        "gpu-context" to "android",
        "gpu-api" to "opengl",
        "opengl-es" to "yes",
        "hwdec" to "auto-safe",
        "ao" to "audiotrack,opensles",
        "idle" to "yes",
        "keep-open" to "always",
        "force-window" to "no",
        "save-position-on-quit" to "no",
        "osd-level" to "0",
        "input-default-bindings" to "no",
        "demuxer-max-bytes" to (32 * 1024 * 1024).toString(),
        "demuxer-max-back-bytes" to (8 * 1024 * 1024).toString(),
        "tls-verify" to "yes",
        "tls-ca-file" to certificate.absolutePath,
    )
}
