package app.deference.embycl.data.local.database

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import app.deference.embycl.domain.repository.*
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException

/** Internal files storage is private, persistent, and compatible with the later FileProvider. */
@Single(binds = [OfflineFileStore::class])
class PlatformOfflineFileStore(context: Context) : OfflineFileStore by JavaOfflineFileStore(
    File(context.applicationContext.filesDir, "offline-downloads"), AndroidOfflineFileOps,
)

internal object AndroidOfflineFileOps : OfflineFileOps {
    override fun stat(file: File): FileEntry? {
        val stat = try { Os.lstat(file.absolutePath) } catch (e: ErrnoException) {
            if (e.errno == OsConstants.ENOENT) return null
            throw IOException(e)
        }
        if (OsConstants.S_ISLNK(stat.st_mode) || (!OsConstants.S_ISREG(stat.st_mode) && !OsConstants.S_ISDIR(stat.st_mode)))
            throw OfflineStorageException(OfflineStorageFailure.INVALID_PATH)
        return FileEntry(OsConstants.S_ISDIR(stat.st_mode), OsConstants.S_ISREG(stat.st_mode), stat.st_size)
    }
    override fun moveAtomic(source: File, destination: File) {
        try { Os.rename(source.absolutePath, destination.absolutePath) } catch (e: ErrnoException) { throw IOException(e) }
    }
    override fun syncDirectory(directory: File) {
        try {
            val fd = Os.open(directory.absolutePath, OsConstants.O_RDONLY, 0)
            try {
                if (!OsConstants.S_ISDIR(Os.fstat(fd).st_mode)) throw IOException("Expected a directory.")
                Os.fsync(fd)
            } finally { Os.close(fd) }
        } catch (e: ErrnoException) { throw IOException(e) }
    }
}
