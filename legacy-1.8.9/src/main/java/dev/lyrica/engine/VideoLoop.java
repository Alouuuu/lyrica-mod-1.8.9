package dev.lyrica.engine;

/**
 * 1.8.9 port: upstream decodes Spotify Canvas / animated covers with JavaCV + FFmpeg, which is
 * not bundled. This stub keeps the same API and always reports the video as unavailable, so
 * callers fall back to the static cover.
 */
public final class VideoLoop implements AutoCloseable {
    public static final int MAX_EDGE = 512;
    private volatile boolean closed;

    public VideoLoop() { this(false); }
    public VideoLoop(boolean allowLocalFiles) { }

    public record PixelFrame(int width, int height, int[] argb, long sequence) { }

    public void setSource(String value) { }
    public void setPlaying(boolean value, boolean pauseWhenStopped) { }
    public void touchVisible() { }
    public void touchVisible(boolean autoplay) { }
    public PixelFrame latest() { return null; }
    public String status() { return "Video unavailable on Minecraft 1.8.9"; }
    public boolean failed() { return true; }
    public Throwable failure() { return null; }
    public boolean closed() { return closed; }
    @Override public void close() { closed = true; }
}
