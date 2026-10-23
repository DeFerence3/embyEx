package app.deference.embycl.ui.player;

import org.openani.mediamp.mpv.RenderUpdateListener;

/** A named JNI callback so GraalVM can retain the method explicitly. */
public final class PlayerRenderListener implements RenderUpdateListener {
    private final Runnable callback;

    public PlayerRenderListener(Runnable callback) { this.callback = callback; }

    @Override public void onRenderUpdate() { callback.run(); }
}
