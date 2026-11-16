package dev.relaylyrics.platform.mc;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

import java.nio.file.Path;

/** 1.8.9 facade for {@code FabricLoader}, backed by Forge's {@link Loader}. */
public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() { }

    public static FabricLoader getInstance() { return INSTANCE; }

    public Path getConfigDir() {
        java.io.File config = Loader.instance().getConfigDir();
        return (config != null ? config : new java.io.File(Minecraft.getMinecraft().mcDataDir, "config")).toPath();
    }

    public Path getGameDir() { return Minecraft.getMinecraft().mcDataDir.toPath(); }

    public boolean isModLoaded(String id) { return Loader.isModLoaded(id); }

    public java.util.Optional<ModContainer> getModContainer(String id) {
        if ("minecraft".equals(id)) return java.util.Optional.of(new ModContainer(id, Loader.MC_VERSION));
        net.minecraftforge.fml.common.ModContainer mod = Loader.instance().getIndexedModList().get(
                "relaylyrics".equals(id) ? "lyrica" : id);
        return mod == null ? java.util.Optional.empty() : java.util.Optional.of(new ModContainer(id, mod.getVersion()));
    }

    public boolean isDevelopmentEnvironment() {
        Object deobf = net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment");
        return Boolean.TRUE.equals(deobf);
    }
}
