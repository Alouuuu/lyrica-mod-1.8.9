package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for Fabric's {@code ModContainer}: just enough for {@code getMetadata().getVersion().getFriendlyString()}. */
public final class ModContainer {
    private final String id;
    private final String version;

    ModContainer(String id, String version) { this.id = id; this.version = version; }

    public Metadata getMetadata() { return new Metadata(); }

    public final class Metadata {
        public String getId() { return id; }
        public Version getVersion() { return new Version(); }
    }

    public final class Version {
        public String getFriendlyString() { return version; }
        @Override public String toString() { return version; }
    }
}
