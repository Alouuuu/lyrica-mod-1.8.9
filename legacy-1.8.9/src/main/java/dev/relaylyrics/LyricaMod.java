package dev.relaylyrics;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = LyricaMod.MOD_ID, useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public final class LyricaMod {
    public static final String MOD_ID = "lyrica";
    public static final Logger LOGGER = LogManager.getLogger("Lyrica");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Java 8u51 (the 1.8.9 launcher runtime) lacks current root CAs; see LegacyTls.
        LOGGER.info("Lyrica: modern TLS roots installed = {}", dev.relaylyrics.compat.LegacyTls.installJvmDefault());
        if (Boolean.getBoolean("lyrica.smoke")) {
            Thread probe = new Thread(() -> {
                try { dev.relaylyrics.compat.Java8Probe.run("all"); }
                catch (Throwable failure) { LOGGER.error("Lyrica smoke test failed", failure); }
            }, "Lyrica smoke test");
            probe.setDaemon(true);
            probe.start();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Reflective so the -PengineOnly build (no client sources) still compiles.
        try {
            Class.forName("dev.relaylyrics.client.RelayLyricsClient").getMethod("initialize").invoke(null);
        } catch (ClassNotFoundException engineOnly) {
            LOGGER.info("Lyrica: engine-only build, client not present");
        } catch (ReflectiveOperationException failure) {
            throw new RuntimeException("Lyrica client failed to initialize", failure.getCause() != null ? failure.getCause() : failure);
        }
    }
}
