package app.deference.embycl.data.repository.update

import java.io.File
import java.security.MessageDigest

/**
 * Whether [file] hashes to [digest], in the `algorithm:hex` form GitHub gives a release asset
 * (`sha256:…`). Throws [IllegalStateException] for a malformed digest or an unsupported algorithm.
 */
internal fun fileMatchesDigest(
    file: File,
    digest: String,
): Boolean {
    val parts = digest.split(':', limit = 2)
    check(parts.size == 2) { "The release checksum is invalid." }
    val algorithm = when (parts[0].lowercase()) {
        "sha256" -> "SHA-256"
        "sha512" -> "SHA-512"
        else -> error("The release uses an unsupported checksum.")
    }
    val hash = MessageDigest.getInstance(algorithm)
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            hash.update(buffer, 0, count)
        }
    }
    val actual = hash.digest().joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
    return actual.equals(
        other = parts[1],
        ignoreCase = true,
    )
}

private const val BUFFER_SIZE = 64 * 1024
