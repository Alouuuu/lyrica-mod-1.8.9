package dev.relaylyrics.platform.mc;

/** 1.8.9 facade adding 1.21's {@code BlockPos.ofFloored}. */
public final class BlockPos extends net.minecraft.util.BlockPos {
    public BlockPos(int x, int y, int z) { super(x, y, z); }
    public static BlockPos ofFloored(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
    public static BlockPos ofFloored(Vec3d pos) { return ofFloored(pos.x, pos.y, pos.z); }
    public static BlockPos of(net.minecraft.util.BlockPos pos) { return new BlockPos(pos.getX(), pos.getY(), pos.getZ()); }
}
