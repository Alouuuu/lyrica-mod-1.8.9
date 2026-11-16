package dev.relaylyrics.client;

import dev.relaylyrics.client.music.WorldLyricsLights;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.8.9 port: the terrain-light backend upstream gets from LambDynamicLights. 1.8.9 has no such
 * API, so {@code ChunkCacheMixin} raises the block light that chunk meshes are built with to the
 * sung word's light, and this backend marks only the affected chunk sections for rebuild when a
 * light moves (positions quantized to 1/8 block and luminance to 0.5, as upstream does, and
 * {@link WorldLyricsLights#tick} already limits updates to 10 per second).
 * <p>The light snapshot is read by the chunk-rebuild worker threads, so it is an immutable array
 * published through a volatile field.
 */
public final class WorldLyricsTerrainLight implements WorldLyricsLights.Backend {
    /** Light lost per block of distance; 1 matches vanilla block light. */
    private static final double FALLOFF = 1;
    private static volatile Light[] lights = new Light[0];

    // Opt-in diagnostics (-Dlyrica.diag=true, [lyrica-light] lines): chunk-light calls seen and blocks raised.
    private static final boolean DIAG = Boolean.getBoolean("lyrica.diag");
    private static long applyCalls, raisedBlocks; // racy on purpose: diagnostics only
    private static long lastLogAt;
    private static String lastLog = "";

    public static void install() {
        WorldLyricsLights.install(new WorldLyricsTerrainLight());
        boolean hooked = TerrainLightHooked.class.isAssignableFrom(net.minecraft.world.ChunkCache.class);
        dev.relaylyrics.FanDroidMod.LOGGER.info("[lyrica-light] terrain light backend installed; ChunkCache hook applied = {}", hooked);
    }

    private static void log(String message) {
        long now = System.currentTimeMillis();
        if (message.equals(lastLog) && now - lastLogAt < 10_000) return;
        if (now - lastLogAt < 1_000) return;
        lastLog = message; lastLogAt = now;
        dev.relaylyrics.FanDroidMod.LOGGER.info("[lyrica-light] {}", message);
    }

    @Override public void update(Object world, List<WorldLyricsLights.Point> points) {
        List<Light> next = new ArrayList<>(points.size());
        for (WorldLyricsLights.Point p : points) {
            double luminance = Math.rint(p.luminance() * 2) / 2;
            if (luminance > 0) next.add(new Light(Math.rint(p.x() * 8) / 8, Math.rint(p.y() * 8) / 8,
                    Math.rint(p.z() * 8) / 8, luminance));
        }
        Light[] previous = lights, current = next.toArray(new Light[0]);
        if (DIAG) {
            log("offered=" + points.size() + (current.length > 0 ? " first=" + current[0] : "")
                    + " | since last: chunkLightCalls=" + applyCalls + " raisedBlocks=" + raisedBlocks);
            applyCalls = 0; raisedBlocks = 0;
        }
        if (java.util.Arrays.equals(previous, current)) return;
        lights = current;
        markDirty(previous);
        markDirty(current);
    }

    @Override public void clear() {
        Light[] previous = lights;
        lights = new Light[0];
        markDirty(previous);
    }

    private static void markDirty(Light[] values) {
        Minecraft mc = Minecraft.getMinecraft();
        if (values.length == 0 || mc.renderGlobal == null) return;
        for (Light light : values) {
            int reach = (int) Math.ceil(light.luminance / FALLOFF) + 1;
            int x = (int) Math.floor(light.x), y = (int) Math.floor(light.y), z = (int) Math.floor(light.z);
            mc.renderGlobal.markBlockRangeForRenderUpdate(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach);
        }
    }

    /**
     * Called from chunk rebuilds for every block: returns {@code combined} with its block-light
     * part raised to the words' light. {@code combined} is 1.8.9's packed {@code sky << 20 | block << 4}.
     */
    public static int apply(BlockPos pos, int combined) {
        if (DIAG) applyCalls++;
        Light[] current = lights;
        if (current.length == 0) return combined;
        double cx = pos.getX() + .5, cy = pos.getY() + .5, cz = pos.getZ() + .5, best = 0;
        for (Light light : current) best = Math.max(best, light.at(cx, cy, cz));
        int level = Math.min(15, (int) Math.round(best));
        int block = combined >> 4 & 15;
        if (level <= block) return combined;
        if (DIAG) raisedBlocks++;
        return (combined & ~0xFFFF) | level << 4;
    }

    private static final class Light {
        final double x, y, z, luminance;
        Light(double x, double y, double z, double luminance) { this.x = x; this.y = y; this.z = z; this.luminance = luminance; }
        double at(double px, double py, double pz) {
            double dx = px - x, dy = py - y, dz = pz - z;
            double d2 = dx * dx + dy * dy + dz * dz, reach = luminance / FALLOFF;
            return d2 >= reach * reach ? 0 : luminance - Math.sqrt(d2) * FALLOFF;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Light)) return false;
            Light l = (Light) o;
            return x == l.x && y == l.y && z == l.z && luminance == l.luminance;
        }
        @Override public int hashCode() { return java.util.Arrays.hashCode(new double[]{x, y, z, luminance}); }
        @Override public String toString() { return "(" + x + ", " + y + ", " + z + ") lum=" + luminance; }
    }
}
