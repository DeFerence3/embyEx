package app.deference.embycl.ui.player;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.openani.mediamp.mpv.internal.MpvReadbackBackend;
import org.openani.mediamp.mpv.utils.SkiaLayerRedrawer;
import org.openani.mediamp.mpv.utils.SkiaRenderDeviceInterop;

/** Linux CPU frames are independent of Tao's Wayland/X11 graphics context. */
final class LinuxReadbackBackend implements MpvReadbackBackend {
    static final LinuxReadbackBackend INSTANCE = new LinuxReadbackBackend();
    private static boolean loaded;

    private static synchronized void load() {
        if (loaded) return;
        try (InputStream source = LinuxReadbackBackend.class.getResourceAsStream("/libembyex-player.so")) {
            if (source == null) throw new IllegalStateException("The Linux player renderer was not packaged.");
            Path library = Files.createTempFile("embyex-player-", ".so");
            library.toFile().deleteOnExit();
            Files.copy(source, library, StandardCopyOption.REPLACE_EXISTING);
            System.load(library.toAbsolutePath().toString());
            loaded = true;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load the Linux player renderer.", failure);
        }
    }

    @Override public boolean createRenderContext(long id) { load(); return create(id); }
    @Override public boolean destroyRenderContext(long id) { destroy(id); return true; }
    @Override public String getRendererName() { return "libmpv software readback"; }
    @Override public boolean setSurfaceConfig(long id, int width, int height, long device) {
        return resize(id, width, height);
    }
    @Override public long getFrameState(long id) { return state(id); }
    @Override public long copyLatestFrame(long id, long address, int width, int height) {
        return copy(id, address, width, height);
    }
    @Override public boolean hasSurface(long id) { return state(id) != 0; }
    @Override public SkiaRenderDeviceInterop createSkiaInterop(SkiaLayerRedrawer redrawer) {
        throw new UnsupportedOperationException("CPU frames do not use AWT device interop.");
    }
    @Override public int[] readSurfacePixels(long id, int[] dimensions) {
        throw new UnsupportedOperationException("Use the player surface for frame readback.");
    }
    @Override public int[] renderFramePixels(long id, int width, int height) {
        throw new UnsupportedOperationException("Offscreen preview rendering is not supported.");
    }

    private static native boolean create(long id);
    private static native void destroy(long id);
    private static native boolean resize(long id, int width, int height);
    private static native long state(long id);
    private static native long copy(long id, long address, int width, int height);
}
