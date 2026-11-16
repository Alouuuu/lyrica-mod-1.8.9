package dev.relaylyrics.platform.mc;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.8.9 port: stands in for Minecraft 1.21's {@code ttf} font providers, which 1.8.9 lacks.
 * Each face is an AWT font chain plus the provider {@code size} in GUI pixels. Measurement and
 * rasterization use the same calibration as upstream's {@code SettingsTextRaster}: the AWT point
 * size equals the provider size, and the baseline sits 7 GUI pixels below the draw origin.
 * This class has no Minecraft dependencies, so it can be tested headless.
 */
public final class TtfText {
    static final double BASELINE = 7;
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static final Map<String, Face> FACES = new ConcurrentHashMap<>();
    private static final Map<WidthKey, Float> WIDTHS = Collections.synchronizedMap(
            new LinkedHashMap<WidthKey, Float>(256, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<WidthKey, Float> eldest) { return size() > 4096; }
            });

    /** A registered font: fallback chain (first face that can display a code point wins) and size. */
    public static final class Face {
        final String id;
        final Font[] chain;
        final float size;
        final int generation;
        private static int generations;

        Face(String id, Font[] chain, float size) {
            this.id = id; this.chain = chain; this.size = size;
            synchronized (Face.class) { generation = ++generations; }
        }
    }

    static {
        builtin("relaylyrics:settings", "settings_regular.ttf", 14);
        builtin("relaylyrics:settings_semibold", "settings_semibold.ttf", 14);
        builtin("relaylyrics:settings_compact", "settings_regular.ttf", 9);
        builtin("relaylyrics:settings_compact_semibold", "settings_semibold.ttf", 9);
        builtin("relaylyrics:world", "settings_semibold.ttf", 10);
    }

    private static void builtin(String id, String file, float size) {
        try (InputStream in = TtfText.class.getResourceAsStream("/assets/relaylyrics/font/" + file)) {
            if (in != null) FACES.put(id, new Face(id, new Font[]{Font.createFont(Font.TRUETYPE_FONT, in)}, size));
        } catch (Exception ignored) { }
    }

    /** Registers (or replaces) a face from TTF files; returns false when none could be loaded. */
    public static boolean register(String id, List<Path> files, float size) {
        List<Font> fonts = new ArrayList<>();
        for (Path file : files) {
            try (InputStream in = Files.newInputStream(file)) { fonts.add(Font.createFont(Font.TRUETYPE_FONT, in)); }
            catch (Exception ignored) { }
        }
        if (fonts.isEmpty()) return false;
        FACES.put(id, new Face(id, fonts.toArray(new Font[0]), size));
        WIDTHS.clear();
        return true;
    }

    public static Face face(String id) { return id == null ? null : FACES.get(id); }

    private static TextLayout layout(Face face, String value, boolean bold, boolean italic, float size) {
        int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
        AttributedString text = new AttributedString(value);
        Font fallback = null;
        for (int at = 0; at < value.length();) {
            int cp = value.codePointAt(at), end = at + Character.charCount(cp);
            Font chosen = null;
            for (Font font : face.chain) if (font.canDisplay(cp)) { chosen = font; break; }
            if (chosen == null) {
                if (fallback == null) fallback = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
                chosen = fallback;
            }
            text.addAttribute(TextAttribute.FONT, chosen.deriveFont(style, size), at, end);
            at = end;
        }
        return new TextLayout(text.getIterator(), FRC);
    }

    /** Advance in GUI pixels (fractional). */
    public static float width(Face face, String value, boolean bold, boolean italic) {
        if (value == null || value.isEmpty()) return 0;
        WidthKey key = new WidthKey(face.generation, value, bold, italic);
        Float cached = WIDTHS.get(key);
        if (cached != null) return cached;
        float width = rawWidth(face, value, bold, italic);
        WIDTHS.put(key, width);
        return width;
    }

    private static float rawWidth(Face face, String value, boolean bold, boolean italic) {
        if (value.isEmpty()) return 0;
        // Measured at 8x so hinting at tiny point sizes doesn't skew advances.
        return layout(face, value, bold, italic, face.size * 8).getAdvance() / 8f;
    }

    /** Longest prefix (on code point boundaries) that fits in {@code maxWidth}. */
    public static String trim(Face face, String value, double maxWidth, boolean bold, boolean italic) {
        if (value == null || value.isEmpty() || maxWidth <= 0) return "";
        if (width(face, value, bold, italic) <= maxWidth) return value;
        int[] cuts = value.codePoints().toArray();
        int lo = 0, hi = cuts.length; // number of code points kept
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (rawWidth(face, new String(cuts, 0, mid), bold, italic) <= maxWidth) lo = mid; else hi = mid - 1;
        }
        return new String(cuts, 0, lo);
    }

    /** Greedy word wrap honouring explicit newlines; words wider than a line are broken. */
    public static List<String> wrap(Face face, String value, double maxWidth, boolean bold, boolean italic) {
        List<String> lines = new ArrayList<>();
        if (value == null) return lines;
        for (String paragraph : value.split("\n", -1)) {
            String line = "";
            for (String word : paragraph.split(" ", -1)) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (line.isEmpty() || width(face, candidate, bold, italic) <= maxWidth) { line = candidate; }
                else { lines.add(line); line = word; }
                while (!line.isEmpty() && width(face, line, bold, italic) > maxWidth) {
                    String head = trim(face, line, maxWidth, bold, italic);
                    if (head.isEmpty()) head = new String(Character.toChars(line.codePointAt(0)));
                    lines.add(head);
                    line = line.substring(head.length());
                }
            }
            lines.add(line);
        }
        return lines;
    }

    /** Alpha coverage mask; {@code left/top} are GUI-pixel offsets from the draw origin. */
    public static final class Raster {
        public final int width, height;
        public final double left, top, density;
        public final byte[] alpha;
        Raster(int width, int height, double left, double top, double density, byte[] alpha) {
            this.width = width; this.height = height; this.left = left; this.top = top; this.density = density; this.alpha = alpha;
        }
    }

    public static Raster rasterize(Face face, String value, boolean bold, boolean italic, double density) {
        if (value == null || value.trim().isEmpty()) return null;
        double width = width(face, value, bold, italic);
        // Texture limits used by PlatformTextures.registerUiMask (4096 x 1024).
        double ppu = Math.max(.5, Math.min(density, Math.min(4000 / Math.max(1, width + 4), 1000 / (face.size * 2 + 4))));
        TextLayout layout = layout(face, value, bold, italic, (float) (face.size * ppu));
        Shape outline = layout.getOutline(null);
        double baseline = BASELINE * ppu;
        Rectangle2D bounds = outline.getBounds2D();
        if (bounds.isEmpty()) return null;
        int left = (int) Math.floor(bounds.getX()) - 1, top = (int) Math.floor(bounds.getY() + baseline) - 1;
        int w = Math.max(1, (int) Math.ceil(bounds.getMaxX()) - left + 1);
        int h = Math.max(1, (int) Math.ceil(bounds.getMaxY() + baseline) - top + 1);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(-left, baseline - top);
            g.setColor(java.awt.Color.WHITE);
            g.fill(outline);
        } finally { g.dispose(); }
        int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
        byte[] alpha = new byte[w * h];
        for (int i = 0; i < alpha.length; i++) alpha[i] = (byte) (argb[i] >>> 24);
        return new Raster(w, h, left / ppu, top / ppu, ppu, alpha);
    }

    private static final class WidthKey {
        final int generation; final String value; final boolean bold, italic;
        WidthKey(int generation, String value, boolean bold, boolean italic) {
            this.generation = generation; this.value = value; this.bold = bold; this.italic = italic;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof WidthKey)) return false;
            WidthKey k = (WidthKey) o;
            return generation == k.generation && bold == k.bold && italic == k.italic && value.equals(k.value);
        }
        @Override public int hashCode() { return ((generation * 31 + value.hashCode()) * 31 + (bold ? 1 : 0)) * 31 + (italic ? 1 : 0); }
    }

    private TtfText() { }
}
