package dev.relaylyrics.platform.mc;

/**
 * 1.8.9 facade for 1.21's {@code BlockHitResult}. Unlike 1.8.9's {@code rayTraceBlocks}, which
 * returns null on a miss, a miss here is a {@link HitResult.Type#MISS} at the ray's end point.
 */
public final class BlockHitResult extends HitResult {
    private final Type type;
    private final Vec3d pos;
    private final BlockPos blockPos;
    private final Direction side;

    private BlockHitResult(Type type, Vec3d pos, BlockPos blockPos, Direction side) {
        this.type = type; this.pos = pos; this.blockPos = blockPos; this.side = side;
    }

    public static BlockHitResult miss(Vec3d end, Direction side) {
        return new BlockHitResult(Type.MISS, end, BlockPos.ofFloored(end), side);
    }

    public static BlockHitResult block(Vec3d pos, BlockPos blockPos, Direction side) {
        return new BlockHitResult(Type.BLOCK, pos, blockPos, side);
    }

    @Override public Type getType() { return type; }
    @Override public Vec3d getPos() { return pos; }
    public BlockPos getBlockPos() { return blockPos; }
    public Direction getSide() { return side; }
}
