package app.deference.embycl.data.local.database

import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.FileNotFoundException

/** Read-only provider. Android also enforces the exact, temporary URI grant. */
class OfflineMediaProvider : FileProvider() {
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") throw FileNotFoundException("Offline media is read-only")
        return super.openFile(uri, mode)
    }
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw SecurityException("Offline media is read-only")
}
