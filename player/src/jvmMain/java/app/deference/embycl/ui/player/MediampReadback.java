package app.deference.embycl.ui.player;

import org.jetbrains.skia.Image;
import org.openani.mediamp.mpv.MPVHandle;
import org.openani.mediamp.mpv.internal.D3D11ReadbackSurfaceBackend;
import org.openani.mediamp.mpv.internal.MacosReadbackSurfaceBackend;
import org.openani.mediamp.mpv.internal.MpvSurfaceBackend;
import org.openani.mediamp.mpv.internal.MpvSurfaceConsumer;
import java.util.function.Consumer;

/**
 * Version-pinned MediaMP 0.5.2 adapter. Its JVM-visible internal readback API is
 * isolated here because the published Compose surface requires an AWT window.
 * Direct calls deliberately fail compilation when upstream changes this ABI;
 * no reflection or access-suppression flags are used.
 */
public final class MediampReadback implements AutoCloseable {
    private final MPVHandle handle;
    private final long id;
    private final MpvSurfaceBackend backend;
    private final MpvSurfaceConsumer consumer;
    private boolean closed;

    public MediampReadback(MPVHandle handle, Runnable onFrame) {
        this.handle = handle;
        this.id = handle.getPtr$mediamp_mpv();
        String os = System.getProperty("os.name");
        this.backend = os.startsWith("Mac") ? MacosReadbackSurfaceBackend.INSTANCE
            : os.startsWith("Windows") ? D3D11ReadbackSurfaceBackend.INSTANCE
            : LinuxReadbackBackend.INSTANCE;
        if (!backend.createRenderContext(id)) {
            throw new IllegalStateException("Could not create the video renderer.");
        }
        consumer = backend.createSurfaceConsumer(id);
        handle.setRenderUpdateListener$mediamp_mpv(new PlayerRenderListener(onFrame));
    }

    public Surface surface() {
        return new Surface();
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        handle.setRenderUpdateListener$mediamp_mpv(null);
        backend.setSurfaceConfig(id, 0, 0, 0L);
        backend.destroyRenderContext(id);
        consumer.invalidateForRenderEnvironmentChange();
    }

    public final class Surface implements AutoCloseable {
        private int width;
        private int height;
        private boolean retired;

        public void resize(int width, int height) {
            synchronized (MediampReadback.this) {
                this.width = width;
                this.height = height;
            }
        }

        public void draw(Consumer<Image> drawFrame) {
            synchronized (MediampReadback.this) {
                if (closed || retired || width <= 0 || height <= 0) return;
                consumer.requestSurface(width, height, 0L);
                Image image = consumer.currentFrameImage(null);
                // Serialize cache replacement with drawing on the two Tao threads.
                // The shared raster cache retains the last frame through resizing.
                if (image != null) drawFrame.accept(image);
            }
        }

        @Override
        public void close() {
            synchronized (MediampReadback.this) { retired = true; }
        }
    }
}
