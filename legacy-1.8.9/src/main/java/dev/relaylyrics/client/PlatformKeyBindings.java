package dev.relaylyrics.client;

import dev.relaylyrics.client.ui.LyricsConfigScreen;
import dev.relaylyrics.platform.mc.MinecraftClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * 1.8.9 keybindings. Upstream opens settings through Mod Menu and the popup buttons; 1.8.9 has
 * no Mod Menu, so settings and visibility get dedicated keys (rebindable in Controls).
 */
public final class PlatformKeyBindings {
    private static final String CATEGORY = "key.categories.lyrica";
    private static KeyBinding openSettings;
    private static KeyBinding toggleLyrics;

    private PlatformKeyBindings() { }

    public static void register() {
        openSettings = new KeyBinding("key.lyrica.settings", Keyboard.KEY_L, CATEGORY);
        toggleLyrics = new KeyBinding("key.lyrica.toggle", Keyboard.KEY_NONE, CATEGORY);
        ClientRegistry.registerKeyBinding(openSettings);
        ClientRegistry.registerKeyBinding(toggleLyrics);
    }

    public static void tick(MinecraftClient client) {
        if (openSettings == null) return;
        while (openSettings.isPressed())
            if (client.currentScreen == null) client.setScreen(new LyricsConfigScreen(null));
        while (toggleLyrics.isPressed()) {
            ClientPreferences preferences = RelayLyricsClient.preferences();
            preferences.setLyricsVisible(!preferences.isLyricsVisible());
            preferences.save();
        }
    }
}
