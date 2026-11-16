package dev.relaylyrics.client.ui;

import dev.relaylyrics.client.music.LyricsEffectRaster;
import dev.relaylyrics.platform.mc.Identifier;
import dev.relaylyrics.platform.mc.Text;
import dev.relaylyrics.platform.mc.TtfText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.8.9 port of the glyph capture behind glow/shadow masks. Upstream captures the shaped quads
 * from 1.21's text pipeline; 1.8.9's FontRenderer draws in immediate mode, so the same quads are
 * rebuilt on the CPU:
 * <ul>
 *   <li>TTF faces (Inter, Google Fonts): the {@link TtfText} raster as a single quad;</li>
 *   <li>vanilla text: per-glyph coverage cut from {@code ascii.png} / {@code unicode_page_XX.png},
 *       using 1.8.9's FontRenderer metrics (resource packs included).</li>
 * </ul>
 * Runs on the client thread from {@code LyricsEffectCache.tick()}.
 */
final class LyricsGlyphRasterizer {
    private static final String ASCII_CHARS = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000";
    private static final Identifier UNIFORM = Identifier.ofVanilla("uniform");
    private static final double PIXELS_PER_UNIT = 4;

    // Font textures read back for coverage; cleared when the FontRenderer is recreated (resource reload).
    private static final Map<String, Sheet> SHEETS = new HashMap<>();
    private static byte[] glyphSizes;
    private static FontRenderer sheetsOwner;

    private LyricsGlyphRasterizer() { }

    static LyricsEffectRaster.Mask capture(Text text) {
        if (text == null) return null;
        Identifier font = text.font();
        TtfText.Face face = font == null ? null : TtfText.face(font.getNamespace() + ":" + font.getPath());
        if (face != null) return captureTtf(face, text);
        return captureVanilla(text, font != null && UNIFORM.equals(font));
    }

    private static LyricsEffectRaster.Mask captureTtf(TtfText.Face face, Text text) {
        boolean bold = text.getStyle().isBold(), italic = text.getStyle().isItalic();
        for (Text sibling : text.getSiblings()) {
            bold |= sibling.getStyle().isBold();
            italic |= sibling.getStyle().isItalic();
        }
        TtfText.Raster raster = TtfText.rasterize(face, text.getString(), bold, italic, PIXELS_PER_UNIT);
        if (raster == null) return null;
        List<LyricsEffectRaster.Quad> quads = new ArrayList<>(1);
        quads.add(new LyricsEffectRaster.Quad(raster.left, raster.top,
                raster.left + raster.width / raster.density, raster.top + raster.height / raster.density,
                raster.width, raster.height, raster.alpha));
        return LyricsEffectRaster.compose(quads, PIXELS_PER_UNIT);
    }

    private static LyricsEffectRaster.Mask captureVanilla(Text text, boolean uniform) {
        List<Cell> cells = vanillaCells(text, uniform);
        if (cells == null || cells.isEmpty()) return null;
        List<LyricsEffectRaster.Quad> quads = new ArrayList<>(cells.size());
        for (Cell cell : cells) quads.add(new LyricsEffectRaster.Quad(cell.left, cell.top, cell.right, cell.bottom,
                cell.width, cell.height, cell.alpha));
        return LyricsEffectRaster.compose(quads, PIXELS_PER_UNIT);
    }

    static boolean isUniform(Text text) {
        Identifier font = text.font();
        return font != null && UNIFORM.equals(font);
    }

    /**
     * One drawn glyph of vanilla text: its coverage cut from the font sheet ({@code sheet},
     * source rectangle {@code x0,y0,width,height} in sheet pixels) and the rectangle it is drawn
     * at in text units. Shared by the glow masks and the extruded world mesh. Client thread only.
     */
    static List<Cell> vanillaCells(Text text, boolean uniform) {
        FontRenderer renderer = Minecraft.getMinecraft().fontRendererObj;
        if (renderer == null) return null;
        if (renderer != sheetsOwner) { SHEETS.clear(); glyphSizes = null; sheetsOwner = renderer; }
        boolean unicode = uniform || renderer.getUnicodeFlag();
        String value = text.toLegacyString();
        List<Cell> quads = new ArrayList<>();
        float x = 0;
        boolean bold = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '§' && i + 1 < value.length()) {
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(value.charAt(++i)));
                if (code < 16 || code == 21) bold = false;
                else if (code == 17) bold = true;
                continue;
            }
            float advance;
            if (c == ' ') {
                advance = 4;
            } else {
                int index = ASCII_CHARS.indexOf(c);
                if (index != -1 && c > 0 && !unicode) {
                    advance = renderer.getCharWidth(c);
                    addAscii(quads, index, advance, x);
                    if (bold) addAscii(quads, index, advance, x + 1);
                } else {
                    advance = addUnicode(quads, c, x);
                    if (bold && advance > 0) addUnicode(quads, c, x + .5f);
                }
                if (bold) advance += 1;
            }
            x += advance;
        }
        return quads;
    }

    /** Mirrors FontRenderer.renderDefaultChar: cell (index%16, index/16) of a 128-unit sheet. */
    private static void addAscii(List<Cell> quads, int index, float charWidth, float x) {
        Sheet sheet = sheet("textures/font/ascii.png");
        if (sheet == null) return;
        float drawn = charWidth - .01f - 1;
        if (drawn <= 0) return;
        double k = sheet.width / 128.0;
        addQuad(quads, sheet, (index % 16) * 8 * k, (index / 16) * 8 * k, drawn * k, 7.99 * k,
                x, 0, x + drawn, 7.99);
    }

    /** Mirrors FontRenderer.renderUnicodeChar (16px cells drawn at half size). */
    private static float addUnicode(List<Cell> quads, char c, float x) {
        byte[] sizes = glyphSizes();
        if (sizes == null || sizes[c] == 0) return 0;
        int start = (sizes[c] & 0xFF) >>> 4, end = (sizes[c] & 15) + 1;
        float drawn = end - start - .02f;
        Sheet sheet = sheet(String.format("textures/font/unicode_page_%02x.png", c / 256));
        if (sheet != null && drawn > 0) {
            double k = sheet.width / 256.0;
            addQuad(quads, sheet, ((c % 16) * 16 + start) * k, ((c & 255) / 16 * 16) * k, drawn * k, 15.98 * k,
                    x, 0, x + drawn / 2, 7.99);
        }
        return (end - start) / 2f + 1;
    }

    private static void addQuad(List<Cell> quads, Sheet sheet, double sx, double sy,
                                double sw, double sh, double left, double top, double right, double bottom) {
        int x0 = (int) Math.round(sx), y0 = (int) Math.round(sy);
        int w = Math.min(sheet.width - x0, Math.max(1, (int) Math.round(sw)));
        int h = Math.min(sheet.height - y0, Math.max(1, (int) Math.round(sh)));
        if (w <= 0 || h <= 0) return;
        byte[] alpha = new byte[w * h];
        boolean any = false;
        for (int y = 0; y < h; y++) for (int xx = 0; xx < w; xx++) {
            byte a = sheet.alpha[(y0 + y) * sheet.width + x0 + xx];
            alpha[y * w + xx] = a;
            any |= a != 0;
        }
        if (any) quads.add(new Cell(sheet, x0, y0, w, h, alpha, left, top, right, bottom));
    }

    private static byte[] glyphSizes() {
        if (glyphSizes == null) {
            byte[] sizes = new byte[65536];
            try (InputStream in = resource("font/glyph_sizes.bin")) {
                int read = 0, n;
                while (read < sizes.length && (n = in.read(sizes, read, sizes.length - read)) > 0) read += n;
                glyphSizes = sizes;
            } catch (Exception ignored) {
                return null;
            }
        }
        return glyphSizes;
    }

    private static Sheet sheet(String path) {
        if (SHEETS.containsKey(path)) return SHEETS.get(path);
        Sheet sheet = null;
        try (InputStream in = resource(path)) {
            BufferedImage image = ImageIO.read(in);
            if (image != null) {
                int w = image.getWidth(), h = image.getHeight();
                int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
                byte[] alpha = new byte[w * h];
                for (int i = 0; i < alpha.length; i++) alpha[i] = (byte) (argb[i] >>> 24);
                sheet = new Sheet(path, w, h, alpha);
            }
        } catch (Exception ignored) { }
        SHEETS.put(path, sheet);
        return sheet;
    }

    private static InputStream resource(String path) throws java.io.IOException {
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(path));
        return resource.getInputStream();
    }

    /** A font texture read back as alpha; {@code path} is its resource location (for binding). */
    static final class Sheet {
        final String path; final int width, height; final byte[] alpha;
        Sheet(String path, int width, int height, byte[] alpha) {
            this.path = path; this.width = width; this.height = height; this.alpha = alpha;
        }
    }

    static final class Cell {
        final Sheet sheet;
        final int x0, y0, width, height;
        final byte[] alpha;
        final double left, top, right, bottom;
        Cell(Sheet sheet, int x0, int y0, int width, int height, byte[] alpha,
             double left, double top, double right, double bottom) {
            this.sheet = sheet; this.x0 = x0; this.y0 = y0; this.width = width; this.height = height; this.alpha = alpha;
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
    }
}
