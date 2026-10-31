package app.deference.embycl.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

/** The view borrows the player; losing a surface never destroys the playback session. */

@SuppressLint("ViewConstructor")
internal class AndroidMpvSurfaceView(
    context: Context,
    private val player: AndroidMpvPlayer,
) : SurfaceView(context), SurfaceHolder.Callback {
    private var attached: Surface? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(
        holder: SurfaceHolder,
    ) {
        attach(holder = holder)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        attach(holder = holder)
    }

    override fun surfaceDestroyed(
        holder: SurfaceHolder,
    ) {
        detach()
    }

    fun release() {
        holder.removeCallback(this)
        detach()
    }

    private fun attach(
        holder: SurfaceHolder,
    ) {
        attached = holder.surface
        player.attachSurface(
            value = holder.surface,
            width = holder.surfaceFrame.width(),
            height = holder.surfaceFrame.height(),
        )
    }

    private fun detach() {
        attached?.let { player.detachSurface(value = it) }
        attached = null
    }
}
