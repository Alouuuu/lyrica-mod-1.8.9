package dev.relaylyrics.platform.mc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * 1.8.9 facade for Minecraft 1.21's {@code NativeImage}: a CPU-side RGBA image.
 * Pixels use the 1.21 ABGR int layout for {@code getColor/setColor}; callers convert explicitly.
 */
public final class NativeImage implements AutoCloseable {
    private final int width;
    private final int height;
    private int[] abgr;

    public NativeImage(int width, int height, boolean clear) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid image size " + width + "x" + height);
        this.width = width;
        this.height = height;
        this.abgr = new int[width * height];
    }

    public static NativeImage read(InputStream stream) throws IOException {
        BufferedImage buffered = ImageIO.read(stream);
        if (buffered == null) throw new IOException("Unsupported image format");
        NativeImage image = new NativeImage(buffered.getWidth(), buffered.getHeight(), false);
        int[] argb = buffered.getRGB(0, 0, image.width, image.height, null, 0, image.width);
        for (int i = 0; i < argb.length; i++) image.abgr[i] = swapRedBlue(argb[i]);
        return image;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getColor(int x, int y) { return abgr[index(x, y)]; }
    public void setColor(int x, int y, int color) { abgr[index(x, y)] = color; }

    public byte getOpacity(int x, int y) { return (byte) (abgr[index(x, y)] >>> 24); }
    public byte getRed(int x, int y) { return (byte) abgr[index(x, y)]; }
    public byte getGreen(int x, int y) { return (byte) (abgr[index(x, y)] >>> 8); }
    public byte getBlue(int x, int y) { return (byte) (abgr[index(x, y)] >>> 16); }

    /** ARGB pixels in the layout {@code DynamicTexture} expects. */
    int[] toArgb(int[] target) {
        int[] out = target != null && target.length == abgr.length ? target : new int[abgr.length];
        for (int i = 0; i < abgr.length; i++) out[i] = swapRedBlue(abgr[i]);
        return out;
    }

    private int index(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height)
            throw new IllegalArgumentException("(" + x + ", " + y + ") outside of image bounds (" + width + ", " + height + ")");
        return y * width + x;
    }

    private static int swapRedBlue(int color) {
        return color & 0xFF00FF00 | (color & 0xFF) << 16 | color >>> 16 & 0xFF;
    }

    @Override public void close() { abgr = new int[0]; }
}
