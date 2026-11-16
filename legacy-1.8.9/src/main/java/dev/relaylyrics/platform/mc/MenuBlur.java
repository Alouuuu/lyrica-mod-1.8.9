package dev.relaylyrics.platform.mc;

import dev.relaylyrics.FanDroidMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 1.8.9 stand-in for 1.21's menu background blur ({@code Screen.applyBlur}). Runs its own
 * {@link ShaderGroup} (vanilla {@code blur} program, two separable box passes at radius 5, like the
 * 1.21 default blurriness) over the main framebuffer at the moment the screen asks for it, so
 * everything drawn before the call is blurred and everything after stays sharp. It never touches
 * {@code EntityRenderer}'s own shader group.
 * <p>Needs framebuffers: OptiFine's Fast Render turns them off, and then this is a no-op.
 */
public final class MenuBlur {
    private static final ResourceLocation SHADER = new ResourceLocation("relaylyrics", "shaders/post/menu_blur.json");
    private static ShaderGroup group;
    private static int width = -1, height = -1;
    private static boolean failed;

    private MenuBlur() { }

    public static boolean available() {
        return !failed && OpenGlHelper.shadersSupported && OpenGlHelper.isFramebufferEnabled()
                && Minecraft.getMinecraft().getFramebuffer() != null;
    }

    public static void apply(float partialTicks) {
        if (!available()) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (group == null) {
                group = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), SHADER);
                width = height = -1;
            }
            if (width != mc.displayWidth || height != mc.displayHeight) {
                width = mc.displayWidth; height = mc.displayHeight;
                group.createBindFramebuffers(width, height);
            }
            // The post passes (Shader.preLoadShader) switch blend/alpha/depth off and move the
            // viewport; the GL matrices are untouched (projection is a uniform), so the screen's own
            // transforms survive and only this state needs restoring.
            boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            GlStateManager.matrixMode(GL11.GL_TEXTURE); // same bracket as EntityRenderer
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            group.loadShaderGroup(partialTicks);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            mc.getFramebuffer().bindFramebuffer(true);
            GlStateManager.enableBlend(); // GUI default (the caller turns it off before the passes)
            if (alpha) GlStateManager.enableAlpha();
            if (depth) GlStateManager.enableDepth();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1, 1, 1, 1);
        } catch (Throwable error) {
            failed = true;
            release();
            try { GlStateManager.matrixMode(GL11.GL_MODELVIEW); mc.getFramebuffer().bindFramebuffer(true); } catch (Throwable ignored) { }
            FanDroidMod.LOGGER.warn("Lyrica menu blur disabled: {}", error.toString());
        }
    }

    /** Frees the blur framebuffers; called when a screen closes. */
    public static void release() {
        if (group != null) {
            try { group.deleteShaderGroup(); } catch (Throwable ignored) { }
            group = null;
        }
        width = height = -1;
    }
}
