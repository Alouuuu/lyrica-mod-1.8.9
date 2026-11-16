package dev.relaylyrics.client.ui;

import dev.relaylyrics.FanDroidMod;
import dev.relaylyrics.platform.mc.DrawContext;
import dev.relaylyrics.platform.mc.MinecraftClient;
import dev.relaylyrics.platform.mc.VertexConsumerProvider;
import dev.relaylyrics.platform.mc.WorldView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * 1.8.9 replacement for upstream's world-render mixin hook: draws {@link WorldLyricsRenderer}
 * from {@code RenderWorldLastEvent}. At that point the modelview is the camera transform with
 * its origin at the view entity's interpolated feet, so the stack is lifted to the eye and the
 * eye position is passed as 1.21's camera position.
 * <p>Any failure disables world lyrics for the session (logged once) so the HUD keeps working.
 */
public final class WorldLyricsHook {
    private static boolean failed;
    private static String lastSummary = "";
    private static long lastSummaryAt;

    private WorldLyricsHook() { }

    public static void tick(MinecraftClient client) {
        if (failed) return;
        try {
            WorldLyricsRenderer.tickPlayerMotion(client);
            WorldTextMesh.tick();
            var preferences = dev.relaylyrics.client.RelayLyricsClient.preferences();
            dev.relaylyrics.client.music.WorldLyricsLights.tick(client.world, System.nanoTime(),
                    preferences.isWorldLyrics() && preferences.isLyricsVisible() && preferences.isWorldLyricsTerrainLight());
        }
        catch (Throwable error) { fail(error); }
    }

    public static void render(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (failed || mc.theWorld == null || mc.thePlayer == null || mc.getRenderViewEntity() == null) return;
        WorldView.beginFrame(partialTicks);
        DrawContext context = DrawContext.begin(partialTicks);
        GlStateManager.disableLighting();
        mc.entityRenderer.disableLightmap();
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        // Fading letters go far below the default 0.1 alpha-test cut.
        GlStateManager.alphaFunc(GL11.GL_GREATER, .003F);
        try {
            GlStateManager.translate(0, WorldView.eyeHeight(), 0);
            WorldLyricsRenderer.render(context.getMatrices(), new VertexConsumerProvider(context), WorldView.cameraPos());
            logSummary();
        } catch (Throwable error) {
            fail(error);
        } finally {
            context.end();
            GlStateManager.alphaFunc(GL11.GL_GREATER, .1F);
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            mc.entityRenderer.enableLightmap();
        }
    }

    private static void fail(Throwable error) {
        failed = true;
        try { dev.relaylyrics.client.music.WorldLyricsLights.tick(null, System.nanoTime(), false); } catch (Throwable ignored) { }
        FanDroidMod.LOGGER.error("[lyrica-world] 3D lyrics disabled for this session after an error", error);
    }

    /** Opt-in diagnostics (-Dlyrica.diag=true): logs the 3D scene only when it changes. */
    private static void logSummary() {
        if (!Boolean.parseBoolean(System.getProperty("lyrica.diag", "false"))) return;
        String summary = WorldLyricsRenderer.probeSummary();
        long now = System.currentTimeMillis();
        if (summary.equals(lastSummary) || now - lastSummaryAt < 2000) return;
        lastSummary = summary; lastSummaryAt = now;
        FanDroidMod.LOGGER.info("[lyrica-world] {}", summary);
    }
}
