package dev.relaylyrics.platform.mc;

import net.minecraft.util.ResourceLocation;

/** 1.8.9 facade for Minecraft 1.21's {@code Identifier}; it is a {@link ResourceLocation}. */
public class Identifier extends ResourceLocation {
    public Identifier(String namespace, String path) { super(namespace, path); }
    public Identifier(String id) { super(id); }

    public static Identifier of(String namespace, String path) { return new Identifier(namespace, path); }
    public static Identifier of(String id) { return new Identifier(id); }
    public static Identifier ofVanilla(String path) { return new Identifier("minecraft", path); }

    public static Identifier wrap(ResourceLocation location) {
        return location instanceof Identifier id ? id : new Identifier(location.getResourceDomain(), location.getResourcePath());
    }

    public String getNamespace() { return getResourceDomain(); }
    public String getPath() { return getResourcePath(); }
}
