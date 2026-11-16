package dev.relaylyrics.client.ui;

import dev.relaylyrics.client.music.PlatformTextures;
import dev.relaylyrics.client.music.WorldLyricsExperience;
import dev.relaylyrics.platform.mc.Identifier;
import dev.relaylyrics.platform.mc.Matrix4f;
import dev.relaylyrics.platform.mc.Text;
import dev.relaylyrics.platform.mc.TtfText;
import dev.relaylyrics.platform.mc.VertexConsumerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.DoubleBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Extruded world-lyric glyphs: captures each glyph's solid pixels on a client tick and builds
 * the closed prism geometry ({@link WorldGlyphMesh}) on one worker.
 * <p>1.8.9 port. Upstream captures shaped quads from 1.21's text pipeline and reads the glyph
 * atlas back from GL. Here the same data comes from the CPU: vanilla glyphs are cut from the font
 * sheets ({@link LyricsGlyphRasterizer#vanillaCells}) and TTF text from its {@link TtfText}
 * raster. Faces are drawn with the Tessellator in the current GL modelview, in upstream's pass
 * order (back, contour, front, rim), and the sung/unsung split uses a clip plane instead of
 * cutting quads on the CPU.
 */
public final class WorldTextMesh {
    private static final int MAX_FACES = 16_384, CACHE_BYTES = 16 * 1024 * 1024;
    private static final double TTF_PIXELS_PER_UNIT = 3;
    private static final LinkedHashMap<Text, Entry> CACHE = new LinkedHashMap<>(32, .75f, true);
    private static final ArrayDeque<Text> QUEUE = new ArrayDeque<>();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "lyrica-world-mesh"); thread.setDaemon(true); return thread;
    });
    private static final DoubleBuffer PLANE = BufferUtils.createDoubleBuffer(4);

    private WorldTextMesh() { }

    public static void clear() {
        for (Entry entry : CACHE.values()) release(entry);
        CACHE.clear(); QUEUE.clear();
    }

    public static void tick() {
        var preferences = dev.relaylyrics.client.RelayLyricsClient.preferences();
        if (!preferences.isWorldLyrics() || Minecraft.getMinecraft().theWorld == null) { clear(); return; }
        for (int i = 0; i < 2 && !QUEUE.isEmpty(); i++) {
            Text text = QUEUE.removeFirst();
            Entry entry = CACHE.get(text);
            if (entry == null) continue;
            try {
                List<Glyph> glyphs = capture(text);
                entry.mesh = CompletableFuture.supplyAsync(() -> build(glyphs), WORKER);
            } catch (RuntimeException exception) {
                entry.mesh = CompletableFuture.completedFuture(null);
            }
        }
        int bytes = 0;
        for (Entry entry : CACHE.values()) {
            Mesh mesh = ready(entry);
            if (mesh != null) bytes += mesh.bytes;
        }
        var iterator = CACHE.entrySet().iterator();
        while ((bytes > CACHE_BYTES || CACHE.size() > 256) && iterator.hasNext()) {
            var old = iterator.next(); Mesh mesh = ready(old.getValue());
            if (mesh != null) bytes -= mesh.bytes;
            release(old.getValue());
            QUEUE.remove(old.getKey()); iterator.remove();
        }
    }

    static boolean drawBudgeted(Text text, Matrix4f matrix, VertexConsumerProvider target, float xOffset,
                                float depth, int faceColor, int sideColor, float alpha, Float boundary,
                                boolean rtl, boolean bevel, WorldLyricsExperience.Budget budget) {
        if (budget.remainingFaces() <= 0) return false;
        Entry entry = CACHE.get(text);
        Mesh mesh = entry == null ? null : ready(entry);
        if (mesh != null && !budget.mesh(mesh.faces * (bevel ? 2 : 1))) return false;
        return draw(text, xOffset, depth, faceColor, sideColor, alpha,
                boundary, rtl, bevel, .30f, mesh == null ? 0 : MAX_FACES);
    }

    private static Mesh ready(Entry entry) {
        return entry.mesh == null || !entry.mesh.isDone() || entry.mesh.isCompletedExceptionally()
                ? null : entry.mesh.getNow(null);
    }

    private static boolean draw(Text text, float xOffset, float depth, int faceColor, int sideColor, float alpha,
                                Float fillBoundary, boolean rtl, boolean bevel, float bevelWidth, int faceLimit) {
        Entry entry = CACHE.get(text);
        if (entry == null) {
            if (QUEUE.size() < 128) { Text key = text.copy(); CACHE.put(key, new Entry()); QUEUE.add(key); }
            return false;
        }
        Mesh mesh = ready(entry);
        if (mesh == null || mesh.faces > faceLimit) return false;
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        if (fillBoundary == null) {
            emit(mesh, xOffset, depth, faceColor, sideColor, alpha, bevel, bevelWidth);
            return true;
        }
        // Upstream splits each quad at the boundary: sung side opaque, unsung side at 46%.
        float boundary = fillBoundary;
        try {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
            plane(-1, boundary); // local x <= boundary
            emit(mesh, xOffset, depth, faceColor, sideColor, alpha * (rtl ? .46f : 1), bevel, bevelWidth);
            plane(1, -boundary); // local x >= boundary
            emit(mesh, xOffset, depth, faceColor, sideColor, alpha * (rtl ? 1 : .46f), bevel, bevelWidth);
        } finally {
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
        }
        return true;
    }

    private static void plane(double a, double d) {
        PLANE.clear();
        PLANE.put(a).put(0).put(0).put(d).flip();
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, PLANE);
    }

    private static void emit(Mesh mesh, float xOffset, float depth, int faceColor, int sideColor, float alpha,
                             boolean bevel, float bevelWidth) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        // Text blends without writing depth, so the back and contour go first and the colored
        // face is composited nearest the viewer (same order as upstream).
        for (int pass = 0; pass < (bevel ? 4 : 3); pass++) for (Part part : mesh.parts) {
            Glyph g = part.glyph;
            if (!g.texture.bind()) continue;
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (WorldGlyphMesh.Face face : part.faces) {
                boolean front = face.side() == WorldGlyphMesh.Side.FRONT;
                boolean rim = pass == 3;
                if (rim ? front || face.side() == WorldGlyphMesh.Side.BACK
                        : pass != (front ? 2 : face.side() == WorldGlyphMesh.Side.BACK ? 0 : 1)) continue;
                boolean cap = !rim && (front || face.side() == WorldGlyphMesh.Side.BACK);
                float shade;
                switch (face.side()) {
                    case FRONT: shade = 1; break;
                    case BACK: shade = .6f; break;
                    case TOP: shade = .90f; break;
                    case BOTTOM: shade = .50f; break;
                    case LEFT: shade = .68f; break;
                    default: shade = .80f; break;
                }
                int rgb = front || rim ? faceColor : sideColor;
                if (rim) shade = face.side() == WorldGlyphMesh.Side.TOP ? .94f : .72f;
                int r = Math.round((rgb >> 16 & 255) * shade), b = Math.round((rgb & 255) * shade);
                int green = Math.round((rgb >> 8 & 255) * shade);
                for (int v = 0; v < 4; v++) {
                    int winding = 3 - v;
                    float px, py, z;
                    switch (face.side()) {
                        case FRONT: case BACK: {
                            int corner = front ? winding : 3 - winding;
                            px = corner == 1 || corner == 2 ? face.x1() : face.x0();
                            py = corner >= 2 ? face.y1() : face.y0(); z = front ? depth : 0;
                            break;
                        }
                        case LEFT: case RIGHT: {
                            int corner = face.side() == WorldGlyphMesh.Side.LEFT ? winding : 3 - winding;
                            px = face.x0(); py = corner >= 2 ? face.y1() : face.y0();
                            boolean inner = corner == 1 || corner == 2;
                            if (rim && inner) px += face.side() == WorldGlyphMesh.Side.LEFT ? bevelWidth : -bevelWidth;
                            z = rim ? depth + (inner ? Math.min(.65f, depth * .2f) : 0) : inner ? depth : 0;
                            break;
                        }
                        default: {
                            int corner = face.side() == WorldGlyphMesh.Side.BOTTOM ? winding : 3 - winding;
                            px = corner >= 2 ? face.x1() : face.x0(); py = face.y0();
                            boolean inner = corner == 1 || corner == 2;
                            if (rim && inner) py += face.side() == WorldGlyphMesh.Side.TOP ? bevelWidth : -bevelWidth;
                            z = rim ? depth + (inner ? Math.min(.65f, depth * .2f) : 0) : inner ? depth : 0;
                        }
                    }
                    double x = g.x0 + (g.x1 - g.x0) * px / g.width + xOffset;
                    double y = g.y0 + (g.y1 - g.y0) * py / g.height;
                    double u = (g.atlasX + (cap ? px : face.sampleX() + .5f)) / g.atlasWidth;
                    double vv = (g.atlasY + (cap ? py : face.sampleY() + .5f)) / g.atlasHeight;
                    buffer.pos(x, y, z).tex(u, vv).color(r, green, b, a).endVertex();
                }
            }
            tessellator.draw(); // an empty batch is skipped by the uploader
        }
    }

    private static Mesh build(List<Glyph> glyphs) {
        if (glyphs == null) return null;
        List<Part> parts = new ArrayList<>(); int count = 0;
        for (Glyph glyph : glyphs) {
            List<WorldGlyphMesh.Face> faces = WorldGlyphMesh.build(glyph.mask);
            count += faces.size(); if (count > MAX_FACES) return null;
            // The mask is needed only on the worker. Retaining it doubles the cached footprint.
            parts.add(new Part(glyph.withoutMask(), faces));
        }
        return new Mesh(Collections.unmodifiableList(parts), count, count * 64 + parts.size() * 192);
    }

    /** Client thread: reads the font sheets / rasterizes TTF text (no GL calls). */
    private static List<Glyph> capture(Text text) {
        Identifier font = text.font();
        TtfText.Face face = font == null ? null : TtfText.face(font.getNamespace() + ":" + font.getPath());
        List<Glyph> glyphs = new ArrayList<>();
        if (face != null) {
            boolean bold = text.getStyle().isBold(), italic = text.getStyle().isItalic();
            for (Text sibling : text.getSiblings()) { bold |= sibling.getStyle().isBold(); italic |= sibling.getStyle().isItalic(); }
            TtfText.Raster raster = TtfText.rasterize(face, text.getString(), bold, italic, TTF_PIXELS_PER_UNIT);
            if (raster == null || raster.width > 512 || raster.height > 512) return glyphs;
            boolean[][] mask = new boolean[raster.height][raster.width];
            for (int y = 0; y < raster.height; y++) for (int x = 0; x < raster.width; x++)
                mask[y][x] = (raster.alpha[y * raster.width + x] & 255) >= 128;
            glyphs.add(new Glyph(new Texture(raster), (float) raster.left, (float) raster.top,
                    (float) (raster.left + raster.width / raster.density), (float) (raster.top + raster.height / raster.density),
                    raster.width, raster.height, 0, 0, raster.width, raster.height, mask));
            return glyphs;
        }
        List<LyricsGlyphRasterizer.Cell> cells = LyricsGlyphRasterizer.vanillaCells(text, LyricsGlyphRasterizer.isUniform(text));
        if (cells == null) return null;
        for (LyricsGlyphRasterizer.Cell cell : cells) {
            if (cell.width > 512 || cell.height > 512) continue;
            boolean[][] mask = new boolean[cell.height][cell.width];
            for (int y = 0; y < cell.height; y++) for (int x = 0; x < cell.width; x++)
                mask[y][x] = (cell.alpha[y * cell.width + x] & 255) >= 128;
            glyphs.add(new Glyph(new Texture(cell.sheet.path), (float) cell.left, (float) cell.top,
                    (float) cell.right, (float) cell.bottom, cell.width, cell.height,
                    cell.x0, cell.y0, cell.sheet.width, cell.sheet.height, mask));
        }
        return glyphs;
    }

    private static void release(Entry entry) {
        Mesh mesh = ready(entry);
        if (mesh != null) for (Part part : mesh.parts) part.glyph.texture.release();
    }

    /** A font sheet (bound by resource location) or a TTF raster uploaded on first draw. */
    private static final class Texture {
        private final ResourceLocation sheet;
        private TtfText.Raster pending;
        private Identifier uploaded;
        Texture(String sheetPath) { this.sheet = new ResourceLocation(sheetPath); }
        Texture(TtfText.Raster raster) { this.sheet = null; this.pending = raster; }

        boolean bind() {
            if (sheet != null) { Minecraft.getMinecraft().getTextureManager().bindTexture(sheet); return true; }
            if (uploaded == null && pending != null) {
                uploaded = PlatformTextures.registerUiMask("world_mesh_" + System.identityHashCode(this),
                        pending.width, pending.height, pending.alpha);
                pending = null;
            }
            if (uploaded == null) return false;
            Minecraft.getMinecraft().getTextureManager().bindTexture(uploaded);
            return true;
        }

        void release() {
            if (uploaded != null) { PlatformTextures.retireEffectMask(uploaded); uploaded = null; }
            pending = null;
        }
    }

    private static final class Glyph {
        final Texture texture;
        final float x0, y0, x1, y1;
        final int width, height, atlasX, atlasY, atlasWidth, atlasHeight;
        final boolean[][] mask;
        Glyph(Texture texture, float x0, float y0, float x1, float y1, int width, int height,
              int atlasX, int atlasY, int atlasWidth, int atlasHeight, boolean[][] mask) {
            this.texture = texture; this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1;
            this.width = width; this.height = height; this.atlasX = atlasX; this.atlasY = atlasY;
            this.atlasWidth = atlasWidth; this.atlasHeight = atlasHeight; this.mask = mask;
        }
        Glyph withoutMask() {
            return new Glyph(texture, x0, y0, x1, y1, width, height, atlasX, atlasY, atlasWidth, atlasHeight, null);
        }
    }

    private static final class Part {
        final Glyph glyph; final List<WorldGlyphMesh.Face> faces;
        Part(Glyph glyph, List<WorldGlyphMesh.Face> faces) { this.glyph = glyph; this.faces = faces; }
    }

    private static final class Mesh {
        final List<Part> parts; final int faces, bytes;
        Mesh(List<Part> parts, int faces, int bytes) { this.parts = parts; this.faces = faces; this.bytes = bytes; }
    }

    private static final class Entry { CompletableFuture<Mesh> mesh; }
}
