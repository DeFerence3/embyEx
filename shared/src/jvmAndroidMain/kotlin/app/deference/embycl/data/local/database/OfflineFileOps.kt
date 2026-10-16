package app.deference.embycl.data.local.database

import java.io.File
import java.io.IOException

/** Platform calls isolate Android API 21 file primitives from desktop NIO APIs. */
internal interface OfflineFileOps {
	/** Null only for ENOENT; permissions and unavailable volumes must throw. */
	fun stat(file: File): FileEntry?
	fun moveAtomic(source: File, destination: File)
	fun delete(file: File) {
		if (!file.delete()) throw IOException("Cannot delete managed entry.")
	}
	/** Windows does not expose portable directory fsync; see the storage review notes. */
	fun syncDirectory(directory: File)
}