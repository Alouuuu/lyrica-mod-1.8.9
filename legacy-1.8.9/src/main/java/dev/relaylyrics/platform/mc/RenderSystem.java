package dev.relaylyrics.platform.mc;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** 1.8.9 facade for the subset of {@code RenderSystem} the GUI code uses (fixed-function GL). */
public final class RenderSystem {
    private RenderSystem() { }

    public static void enableBlend() { GlStateManager.enableBlend(); }
    public static void disableBlend() { GlStateManager.disableBlend(); }

    public static void defaultBlendFunc() {
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    public static void setShaderColor(float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void enableDepthTest() { GlStateManager.enableDepth(); }
    public static void disableDepthTest() { GlStateManager.disableDepth(); }
    public static void enableCull() { GlStateManager.enableCull(); }
    public static void disableCull() { GlStateManager.disableCull(); }
}
