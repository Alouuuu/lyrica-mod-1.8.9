package dev.relaylyrics.platform.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 1.8.9 facade for Minecraft 1.21's {@code DrawContext} (GUI drawing), implemented with
 * fixed-function GL. One instance per frame/pass: {@link #begin(float)} ... {@link #end()}.
 */
public final class DrawContext {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ScaledResolution resolution = new ScaledResolution(mc);
    private final MatrixStack matrices = new MatrixStack();
    private final Deque<int[]> scissors = new ArrayDeque<>();
    private final float tickDelta;

    private DrawContext(float tickDelta) { this.tickDelta = tickDelta; }

    public static DrawContext begin(float tickDelta) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        RenderSystem.defaultBlendFunc();
        GlStateManager.color(1, 1, 1, 1);
        return new DrawContext(tickDelta);
    }

    /** Restores GL state changed by this pass. */
    public void end() {
        matrices.unwind();
        while (!scissors.isEmpty()) disableScissor();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public float tickDelta() { return tickDelta; }
    public MatrixStack getMatrices() { return matrices; }
    public int getScaledWindowWidth() { return resolution.getScaledWidth(); }
    public int getScaledWindowHeight() { return resolution.getScaledHeight(); }

    /** 1.21 batches GUI draws; 1.8.9 draws immediately, so flushing is a no-op. */
    public void draw() { }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if ((color >>> 24) == 0) return;
        Gui.drawRect(x1, y1, x2, y2, color);
        GlStateManager.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public void fill(int x1, int y1, int x2, int y2, int z, int color) { fill(x1, y1, x2, y2, color); }

    public void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        RenderSystem.defaultBlendFunc();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x2, y1, 0).color(r(top), g(top), b(top), a(top)).endVertex();
        buffer.pos(x1, y1, 0).color(r(top), g(top), b(top), a(top)).endVertex();
        buffer.pos(x1, y2, 0).color(r(bottom), g(bottom), b(bottom), a(bottom)).endVertex();
        buffer.pos(x2, y2, 0).color(r(bottom), g(bottom), b(bottom), a(bottom)).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    /** Same parameter order as 1.21.1 {@code drawTexture(Identifier, x, y, w, h, u, v, regionW, regionH, texW, texH)}. */
    public void drawTexture(Identifier texture, int x, int y, int width, int height, float u, float v,
                            int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();   // keep anti-aliased edges (alpha test would cut them)
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, regionWidth, regionHeight, width, height,
                textureWidth, textureHeight);
        GlStateManager.enableAlpha();
    }

    public void drawTexture(Identifier texture, int x, int y, float u, float v, int width, int height,
                            int textureWidth, int textureHeight) {
        drawTexture(texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    public int drawText(TextRenderer renderer, String text, int x, int y, int color, boolean shadow) {
        return renderer.draw(text, x, y, color, shadow);
    }

    public int drawText(TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        return renderer.draw(this, text, x, y, color, shadow);
    }

    public int drawText(TextRenderer renderer, OrderedText text, int x, int y, int color, boolean shadow) {
        return renderer.draw(this, text.text(), x, y, color, shadow);
    }

    public int drawTextWithShadow(TextRenderer renderer, Text text, int x, int y, int color) {
        return drawText(renderer, text, x, y, color, true);
    }

    public int drawTextWithShadow(TextRenderer renderer, String text, int x, int y, int color) {
        return drawText(renderer, text, x, y, color, true);
    }

    public int drawTextWithShadow(TextRenderer renderer, OrderedText text, int x, int y, int color) {
        return drawText(renderer, text, x, y, color, true);
    }

    public void drawCenteredTextWithShadow(TextRenderer renderer, Text text, int centerX, int y, int color) {
        drawText(renderer, text, centerX - renderer.getWidth(text) / 2, y, color, true);
    }

    /** GUI-space rectangle (already transformed by the caller, like 1.21.1). Nested calls intersect. */
    public void enableScissor(int left, int top, int right, int bottom) {
        int[] rect = {left, top, right, bottom};
        int[] parent = scissors.peek();
        if (parent != null) rect = new int[]{Math.max(left, parent[0]), Math.max(top, parent[1]),
                Math.min(right, parent[2]), Math.min(bottom, parent[3])};
        scissors.push(rect);
        apply(rect);
    }

    public void disableScissor() {
        if (scissors.isEmpty()) return;
        scissors.pop();
        int[] parent = scissors.peek();
        if (parent == null) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        else apply(parent);
    }

    private void apply(int[] rect) {
        int scale = resolution.getScaleFactor();
        int width = Math.max(0, rect[2] - rect[0]), height = Math.max(0, rect[3] - rect[1]);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        // glScissor uses framebuffer pixels with a bottom-left origin.
        GL11.glScissor(rect[0] * scale, mc.displayHeight - (rect[1] + height) * scale, width * scale, height * scale);
    }

    private static int a(int c) { return c >>> 24; }
    private static int r(int c) { return c >>> 16 & 255; }
    private static int g(int c) { return c >>> 8 & 255; }
    private static int b(int c) { return c & 255; }
}
