package dev.relaylyrics.platform.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.8.9 facade for {@code MinecraftClient}. Upstream reads public fields
 * ({@code client.currentScreen}, {@code client.world}, ...); they are refreshed from
 * {@link Minecraft} every time {@link #getInstance()} is called.
 */
public final class MinecraftClient {
    private static final MinecraftClient INSTANCE = new MinecraftClient();

    public GuiScreen currentScreen;
    public WorldClient world;
    public EntityPlayerSP player;
    public GameSettings options;
    public TextRenderer textRenderer;
    public final Keyboard keyboard = new Keyboard();
    public final MouseState mouse = new MouseState();
    private final Window window = new Window();
    private final TextureManager textures = new TextureManager();
    private final SoundManager sounds = new SoundManager();

    private MinecraftClient() { }

    public static MinecraftClient getInstance() {
        INSTANCE.refresh();
        return INSTANCE;
    }

    private void refresh() {
        Minecraft mc = Minecraft.getMinecraft();
        currentScreen = mc.currentScreen;
        world = mc.theWorld;
        player = mc.thePlayer;
        options = mc.gameSettings;
        if (mc.fontRendererObj != null) textRenderer = TextRenderer.get();
    }

    public Minecraft handle() { return Minecraft.getMinecraft(); }
    public Window getWindow() { return window; }
    public TextureManager getTextureManager() { return textures; }
    public SoundManager getSoundManager() { return sounds; }

    /** 1.21 loading overlay (resource reload screen). 1.8.9 reloads synchronously: never present. */
    public Object getOverlay() { return null; }

    public void setScreen(GuiScreen screen) { Minecraft.getMinecraft().displayGuiScreen(screen); }

    /** Runs on the client thread (immediately if already on it). */
    public void execute(Runnable task) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isCallingFromMinecraftThread()) task.run();
        else mc.addScheduledTask(task);
    }

    public boolean isOnThread() { return Minecraft.getMinecraft().isCallingFromMinecraftThread(); }

    /** GUI-scaled window metrics (1.21 {@code Window}). */
    public static final class Window {
        public int getScaledWidth() { return new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth(); }
        public int getScaledHeight() { return new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight(); }
        public double getScaleFactor() { return new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor(); }
        public int getWidth() { return Minecraft.getMinecraft().displayWidth; }
        public int getHeight() { return Minecraft.getMinecraft().displayHeight; }
        public int getFramebufferWidth() { return getWidth(); }
        public int getFramebufferHeight() { return getHeight(); }
        /** There is no GLFW window handle on LWJGL2; GLFW facade calls ignore it. */
        public long getHandle() { return 0L; }
    }

    /** Dynamic texture registry (1.21 {@code TextureManager}). */
    public static final class TextureManager {
        private static final AtomicInteger COUNTER = new AtomicInteger();

        public Identifier registerDynamicTexture(String prefix, NativeImageBackedTexture texture) {
            String clean = prefix.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
            Identifier id = new Identifier("relaylyrics", "dynamic/" + clean + "_" + COUNTER.incrementAndGet());
            Minecraft.getMinecraft().getTextureManager().loadTexture(id, texture.handle());
            return id;
        }

        public void registerTexture(Identifier id, NativeImageBackedTexture texture) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(id, texture.handle());
        }

        public void destroyTexture(ResourceLocation id) {
            // GL frees run on the client thread; off-thread callers are queued (never runs in the shutdown hook).
            if (id != null) GlThread.run(() -> Minecraft.getMinecraft().getTextureManager().deleteTexture(id));
        }

        public void bindTexture(ResourceLocation id) { Minecraft.getMinecraft().getTextureManager().bindTexture(id); }
    }

    /** Clipboard access (1.21 {@code Keyboard}). */
    public static final class Keyboard {
        public void setClipboard(String value) { GuiScreen.setClipboardString(value == null ? "" : value); }
        public String getClipboard() { return GuiScreen.getClipboardString(); }
    }

    /** Raw mouse position in framebuffer pixels, top-left origin (1.21 {@code Mouse}). */
    public static final class MouseState {
        public double getX() { return Mouse.getX(); }
        public double getY() { return Minecraft.getMinecraft().displayHeight - Mouse.getY() - 1; }
    }

    public static final class SoundManager {
        public void playClick() {
            Minecraft.getMinecraft().getSoundHandler().playSound(
                    PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        }
    }
}
