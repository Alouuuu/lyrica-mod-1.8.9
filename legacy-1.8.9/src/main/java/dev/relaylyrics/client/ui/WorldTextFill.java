package dev.relaylyrics.client.ui;

import dev.relaylyrics.platform.mc.LightmapTextureManager;
import dev.relaylyrics.platform.mc.Matrix4f;
import dev.relaylyrics.platform.mc.Text;
import dev.relaylyrics.platform.mc.TextRenderer;
import dev.relaylyrics.platform.mc.VertexConsumerProvider;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.DoubleBuffer;

/**
 * Splits a word at a local-space fill boundary: sung side opaque, unsung side at 46%.
 * <p>1.8.9 port: upstream clips the font quads on the CPU through a VertexConsumer. 1.8.9's
 * FontRenderer draws in immediate mode, so the same split is two draws with a fixed-function
 * clip plane given in the text's local space (GL transforms it by the current modelview).
 */
final class WorldTextFill {
    private static final DoubleBuffer PLANE = BufferUtils.createDoubleBuffer(4);

    private WorldTextFill() { }

    static void draw(TextRenderer renderer, Text text, float x, int color, boolean shadow,
                     Matrix4f matrix, VertexConsumerProvider target, float boundary, boolean rtl) {
        int alpha = color >>> 24, rgb = color & 0xFFFFFF;
        int full = alpha << 24 | rgb, dim = Math.round(alpha * .46F) << 24 | rgb;
        try {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
            plane(-1, boundary); // keeps local x <= boundary
            renderer.draw(text, x, 0, rtl ? dim : full, shadow, matrix, target,
                    TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            plane(1, -boundary); // keeps local x >= boundary
            renderer.draw(text, x, 0, rtl ? full : dim, shadow, matrix, target,
                    TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        } finally {
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
        }
    }

    private static void plane(double a, double d) {
        PLANE.clear();
        PLANE.put(a).put(0).put(0).put(d).flip();
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, PLANE);
    }
}
