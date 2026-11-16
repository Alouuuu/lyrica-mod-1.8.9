package dev.relaylyrics.client;

import dev.relaylyrics.FanDroidMod;
import dev.relaylyrics.client.music.MusicService;
import dev.relaylyrics.client.ui.LyricsConfigScreen;
import dev.relaylyrics.client.ui.LyricsOverlay;
import dev.relaylyrics.platform.mc.DrawContext;
import dev.relaylyrics.platform.mc.MinecraftClient;
import dev.relaylyrics.platform.mc.PlatformEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

/**
 * 1.8.9 port of upstream's standalone client entrypoint ({@code lyrics-popup/.../RelayLyricsClient}).
 * Fabric callbacks are replaced by Forge events; karaoke, presence, Jade and the
 * self-test probes are not part of the 1.8.9 MVP.
 */
public final class RelayLyricsClient {
    private static ClientPreferences preferences;
    private static LyricsOverlay overlay;
    private static MusicService music;
    private static boolean initialized;

    private RelayLyricsClient() { }

    public static ClientPreferences preferences() {
        if (preferences == null) preferences = ClientPreferences.load();
        return preferences;
    }

    public static LyricsOverlay overlay() { return overlay; }
    public static MusicService music() { return music; }
    /** Upstream: true when Relay (FanDroid) provides lyrics instead. Relay does not exist on 1.8.9. */
    public static boolean disabledByRelay() { return false; }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        LyricsOverlay.initializeRenderingBackend();
        CompatibilityManager.initialize();
        overlay = new LyricsOverlay(preferences());
        music = new MusicService(overlay, preferences());
        GoogleFontManager.restore(preferences().getGoogleFontFamily(),
                preferences().getLyricsFontWeight().cssWeight(), overlay::fontChanged);
        PlatformKeyBindings.register();
        WorldLyricsTerrainLight.install(); // 1.8.9: built-in terrain light (upstream: LambDynamicLights)
        MinecraftForge.EVENT_BUS.register(new Events());
        Runtime.getRuntime().addShutdownHook(new Thread(RelayLyricsClient::shutdown, "Lyrica shutdown"));
        FanDroidMod.LOGGER.info("Lyrica (1.8.9) lyrics popup initialized");
    }

    private static synchronized void shutdown() {
        if (!initialized) return;
        initialized = false;
        try { preferences().save(); } catch (RuntimeException ignored) { }
        if (overlay != null) overlay.close();
        if (music != null) music.close();
    }

    public static final class Events {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            MinecraftClient client = MinecraftClient.getInstance();
            PlatformKeyBindings.tick(client);
            overlay.tick();
            if (music != null) {
                music.tickAudioDucking(client.world != null);
                music.tickGeminiShaders();
                if (client.player != null) {
                    String version = music.support().takeNotification();
                    if (!version.isBlank()) Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(
                            new ChatComponentTranslation("config.relaylyrics.updates.notification", version));
                }
            }
            InventoryLyricsController.tick(client);
            dev.relaylyrics.client.ui.WorldLyricsHook.tick(client);
        }

        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.END) dev.relaylyrics.client.ui.PlatformCursor.endFrame();
        }

        @SubscribeEvent
        public void onWorldLast(net.minecraftforge.client.event.RenderWorldLastEvent event) {
            dev.relaylyrics.client.ui.WorldLyricsHook.render(event.partialTicks);
        }

        @SubscribeEvent
        public void onHud(RenderGameOverlayEvent.Post event) {
            if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
            if (Minecraft.getMinecraft().currentScreen != null) return;
            DrawContext context = DrawContext.begin(event.partialTicks);
            try { InventoryLyricsController.renderWorldHud(context); }
            finally { context.end(); }
        }

        @SubscribeEvent
        public void onScreenInit(GuiScreenEvent.InitGuiEvent.Post event) {
            if (handles(event.gui)) InventoryLyricsController.attach(event.gui);
        }

        @SubscribeEvent
        public void onScreenDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
            if (!handles(event.gui)) return;
            DrawContext context = DrawContext.begin(event.renderPartialTicks);
            try { InventoryLyricsController.render(event.gui, context, event.mouseX, event.mouseY); }
            finally { context.end(); }
        }

        /** Mouse handling for vanilla screens (our own screens handle input themselves). */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public void onScreenMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
            GuiScreen screen = event.gui;
            if (!handles(screen)) return;
            double x = PlatformEvents.mouseX(), y = PlatformEvents.mouseY();
            int button = Mouse.getEventButton();
            int wheel = Mouse.getEventDWheel();
            boolean consumed;
            if (wheel != 0) consumed = InventoryLyricsController.mouseScrolled(screen, x, y, 0, Integer.signum(wheel));
            else if (button >= 0 && Mouse.getEventButtonState()) consumed = InventoryLyricsController.mouseClicked(screen, x, y, button);
            else if (button >= 0) consumed = InventoryLyricsController.mouseReleased(screen, x, y, button);
            else if (Mouse.isButtonDown(0)) consumed = InventoryLyricsController.mouseDraggedEvent(screen, x, y, 0);
            else consumed = false;
            if (consumed) event.setCanceled(true);
        }

        private static boolean handles(GuiScreen screen) {
            return screen != null && !(screen instanceof LyricsConfigScreen)
                    && InventoryLyricsController.supports(screen);
        }
    }
}
