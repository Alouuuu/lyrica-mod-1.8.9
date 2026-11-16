package dev.relaylyrics.platform.mc;

import dev.relaylyrics.client.music.PlatformTextures;
import dev.relaylyrics.client.ui.GuiTransform;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.8.9 port: draws {@link Text} whose font is a registered {@link TtfText} face. Each string is
 * rasterized once into an alpha mask at a fixed density (GUI scale x 2, quantized) and scaled by
 * the GL matrix afterwards, so animated lyric scales don't rebuild textures every frame.
 */
final class TtfFonts {
    private static final int MAX_ENTRIES = 768;
    private static final long MAX_BYTES = 48L << 20;
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static final Map<Key, Cached> CACHE = new LinkedHashMap<>(256, .75f, true);
    private static long bytes;

    static TtfText.Face face(Text text) {
        if (text == null) return null;
        Identifier font = text.font();
        return font == null ? null : TtfText.face(font.getNamespace() + ":" + font.getPath());
    }

    static boolean bold(Text text) { return text.getStyle().isBold() || anySibling(text, true); }
    static boolean italic(Text text) { return text.getStyle().isItalic() || anySibling(text, false); }

    private static boolean anySibling(Text text, boolean bold) {
        for (Text sibling : text.getSiblings()) {
            Style style = sibling.getStyle();
            if (bold ? style.isBold() : style.isItalic()) return true;
        }
        return false;
    }

    static float width(TtfText.Face face, Text text) {
        return TtfText.width(face, text.getString(), bold(text), italic(text));
    }

    static int draw(DrawContext context, TtfText.Face face, Text text, float x, float y, int color, boolean shadow) {
        String value = text.getString();
        float advance = width(face, text);
        if ((color >>> 24) < 4 || value.trim().isEmpty()) return (int) Math.ceil(x + advance);
        boolean bold = bold(text), italic = italic(text);
        double density = Math.round(Math.max(1, Math.min(8, PlatformTextures.guiScale() * 2)) * 2) / 2.0;
        Cached cached = cached(face, value, bold, italic, density);
        if (cached == null) return (int) Math.ceil(x + advance);
        if (shadow) {
            int shadowColor = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
            blit(context, cached, x + 1, y + 1, shadowColor);
        }
        blit(context, cached, x, y, color);
        return (int) Math.ceil(x + advance + (shadow ? 1 : 0));
    }

    private static void blit(DrawContext context, Cached cached, float x, float y, int color) {
        TtfText.Raster raster = cached.raster;
        GuiTransform.push(context);
        GuiTransform.translate(context, (float) (x + raster.left), (float) (y + raster.top));
        GuiTransform.scale(context, (float) (1 / raster.density), (float) (1 / raster.density));
        PlatformTextures.drawTinted(context, cached.id, 0, 0, raster.width, raster.height, 0, 0,
                raster.width, raster.height, raster.width, raster.height, color);
        GuiTransform.pop(context);
    }

    private static Cached cached(TtfText.Face face, String value, boolean bold, boolean italic, double density) {
        Key key = new Key(face.generation, value, bold, italic, density);
        long now = System.nanoTime();
        Cached cached = CACHE.get(key);
        if (cached == null) {
            TtfText.Raster raster = TtfText.rasterize(face, value, bold, italic, density);
            if (raster == null) return null;
            Identifier id = PlatformTextures.registerUiMask("ttf_text_" + GENERATION.incrementAndGet(),
                    raster.width, raster.height, raster.alpha);
            cached = new Cached(id, raster);
            CACHE.put(key, cached);
            bytes += (long) raster.width * raster.height * 4;
        }
        cached.lastUsed = now;
        Iterator<Cached> it = CACHE.values().iterator();
        while ((CACHE.size() > MAX_ENTRIES || bytes > MAX_BYTES) && it.hasNext()) {
            Cached old = it.next();
            if (now - old.lastUsed < 1_000_000_000L) continue;
            it.remove();
            bytes -= (long) old.raster.width * old.raster.height * 4;
            PlatformTextures.retireEffectMask(old.id);
        }
        return cached;
    }

    private static final class Cached {
        final Identifier id; final TtfText.Raster raster; long lastUsed;
        Cached(Identifier id, TtfText.Raster raster) { this.id = id; this.raster = raster; }
    }

    private static final class Key {
        final int generation; final String value; final boolean bold, italic; final double density;
        Key(int generation, String value, boolean bold, boolean italic, double density) {
            this.generation = generation; this.value = value; this.bold = bold; this.italic = italic; this.density = density;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return generation == k.generation && bold == k.bold && italic == k.italic
                    && density == k.density && value.equals(k.value);
        }
        @Override public int hashCode() {
            return (((generation * 31 + value.hashCode()) * 31 + (bold ? 1 : 0)) * 31 + (italic ? 1 : 0)) * 31
                    + Double.hashCode(density);
        }
    }

    private TtfFonts() { }
}
