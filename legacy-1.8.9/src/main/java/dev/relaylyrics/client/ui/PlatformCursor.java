package dev.relaylyrics.client.ui;

import dev.relaylyrics.FanDroidMod;
import dev.relaylyrics.platform.mc.DrawContext;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.EnumMap;

/**
 * 1.8.9 port: upstream changes the cursor only through the optional "Minecraft Cursor" mod, which
 * has no 1.8.9 build. Here the cursors are native LWJGL2 cursors drawn by the port (hand, closed
 * hand, diagonal resize), so this is a port addition rather than a 1:1 copy.
 * <p>Requests are single-cycle like upstream's {@code setSingleCycleCursor}: a style holds only for
 * the frame that asked for it, and {@link #endFrame()} (client thread, end of each render tick)
 * goes back to the system cursor when nothing asked.
 */
public final class PlatformCursor {
    public enum Style { POINTER, GRABBING, RESIZE }

    private static final String[] HAND = {
            "     XX          ",
            "    X..X         ",
            "    X..X         ",
            "    X..X         ",
            "    X..X         ",
            "    X..XXX       ",
            "    X..X..XXX    ",
            "    X..X..X..XXX ",
            " XX X..X..X..X..X",
            "X..XX..X..X..X..X",
            "X...X...........X",
            "X...............X",
            " X..............X",
            " X..............X",
            "  X.............X",
            "  X............X ",
            "   X...........X ",
            "   X..........X  ",
            "    X.........X  ",
            "    X.........X  ",
            "    XXXXXXXXXXX  ",
    };
    private static final String[] FIST = {
            "     XX XX XX    ",
            "    X..X..X..XX  ",
            " XX X..X..X..X.X ",
            "X..XX..X..X..X..X",
            "X...............X",
            " X..............X",
            " X..............X",
            "  X.............X",
            "  X............X ",
            "   X...........X ",
            "   X..........X  ",
            "    X.........X  ",
            "    XXXXXXXXXXX  ",
    };

    private static final EnumMap<Style, Cursor> CURSORS = new EnumMap<>(Style.class);
    private static volatile Style requested;
    private static Style applied;
    private static boolean initialized, unavailable;

    private PlatformCursor() { }

    public static void request(DrawContext context, Style style) {
        requested = style;
    }

    /** Applies this frame's request (or the system cursor) and clears it. Client thread only. */
    public static void endFrame() {
        Style want = requested;
        requested = null;
        if (want == applied || unavailable) return;
        try {
            if (want != null && !initialize()) return;
            Mouse.setNativeCursor(want == null ? null : CURSORS.get(want));
            applied = want;
        } catch (Throwable failure) {
            unavailable = true;
            try { Mouse.setNativeCursor(null); } catch (Throwable ignored) { }
            applied = null;
            FanDroidMod.LOGGER.warn("Lyrica cursors disabled: {}", failure.toString());
        }
    }

    private static boolean initialize() throws Exception {
        if (initialized) return !unavailable;
        initialized = true;
        int capabilities = Cursor.getCapabilities();
        if ((capabilities & (Cursor.CURSOR_8_BIT_ALPHA | Cursor.CURSOR_ONE_BIT_TRANSPARENCY)) == 0) {
            unavailable = true;
            return false;
        }
        boolean alpha = (capabilities & Cursor.CURSOR_8_BIT_ALPHA) != 0;
        int size = Math.max(Cursor.getMinCursorSize(), 32);
        if (Cursor.getMaxCursorSize() > 0) size = Math.min(size, Cursor.getMaxCursorSize());
        if (size < 21) { unavailable = true; return false; }
        CURSORS.put(Style.POINTER, create(bitmap(HAND, size), 5, 0, size, alpha));
        CURSORS.put(Style.GRABBING, create(bitmap(FIST, size), 8, 6, size, alpha));
        CURSORS.put(Style.RESIZE, create(resize(size), 9, 9, size, alpha));
        return true;
    }

    private static BufferedImage bitmap(String[] rows, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < rows.length && y < size; y++)
            for (int x = 0; x < rows[y].length() && x < size; x++) {
                char c = rows[y].charAt(x);
                if (c == 'X') image.setRGB(x, y, 0xFF000000);
                else if (c == '.') image.setRGB(x, y, 0xFFFFFFFF);
            }
        return image;
    }

    /** NW-SE double arrow centred on (9, 9). */
    private static BufferedImage resize(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            // Crisp pixels (antialiasing turns a cursor this small grey).
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            Path2D.Double arrow = new Path2D.Double();
            double[][] points = {{-10, 0}, {-5, -5}, {-5, -1.6}, {5, -1.6}, {5, -5}, {10, 0},
                    {5, 5}, {5, 1.6}, {-5, 1.6}, {-5, 5}};
            arrow.moveTo(points[0][0], points[0][1]);
            for (int i = 1; i < points.length; i++) arrow.lineTo(points[i][0], points[i][1]);
            arrow.closePath();
            AffineTransform transform = new AffineTransform();
            transform.translate(9, 9);
            transform.rotate(Math.PI / 4);
            java.awt.Shape shape = transform.createTransformedShape(arrow);
            g.setColor(Color.WHITE);
            g.fill(shape);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.draw(shape);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** LWJGL2 cursors are ARGB with the origin and hotspot at the lower left. */
    private static Cursor create(BufferedImage image, int hotX, int hotY, int size, boolean alpha) throws Exception {
        IntBuffer pixels = BufferUtils.createIntBuffer(size * size);
        for (int y = size - 1; y >= 0; y--)
            for (int x = 0; x < size; x++) {
                int argb = image.getRGB(x, y);
                if (!alpha) argb = (argb >>> 24) >= 128 ? argb | 0xFF000000 : 0;
                pixels.put(argb);
            }
        pixels.flip();
        return new Cursor(size, size, hotX, size - 1 - hotY, 1, pixels, null);
    }
}
