package dev.relaylyrics.mixin;

import dev.relaylyrics.client.TerrainLightHooked;
import dev.relaylyrics.client.WorldLyricsTerrainLight;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Terrain light from the sung world-lyric word (upstream: LambDynamicLights). Chunk meshes are
 * built through {@code RegionRenderCache}, which extends ChunkCache in vanilla and with OptiFine,
 * so raising the packed light here lights blocks, smooth lighting and AO alike.
 */
@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin implements TerrainLightHooked {
    @Inject(method = "getCombinedLight", at = @At("RETURN"), cancellable = true)
    private void lyrica$wordLight(BlockPos pos, int lightValue, CallbackInfoReturnable<Integer> cir) {
        try {
            int value = cir.getReturnValue();
            int lit = WorldLyricsTerrainLight.apply(pos, value);
            if (lit != value) cir.setReturnValue(lit);
        } catch (Throwable ignored) {
            // Never break a chunk rebuild worker.
        }
    }
}
