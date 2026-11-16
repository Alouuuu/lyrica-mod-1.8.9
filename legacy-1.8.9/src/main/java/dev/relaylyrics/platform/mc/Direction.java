package dev.relaylyrics.platform.mc;

import net.minecraft.util.EnumFacing;

/** 1.8.9 facade for 1.21's {@code Direction} (block faces). */
public enum Direction {
    DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0);

    private final int x, y, z;
    Direction(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    public int getOffsetX() { return x; }
    public int getOffsetY() { return y; }
    public int getOffsetZ() { return z; }
    public Axis getAxis() { return x != 0 ? Axis.X : y != 0 ? Axis.Y : Axis.Z; }

    public static Direction of(EnumFacing facing) { return values()[facing.getIndex()]; }
    public EnumFacing handle() { return EnumFacing.getFront(ordinal()); }

    public enum Axis {
        X, Y, Z;
        public boolean isHorizontal() { return this != Y; }
        public boolean isVertical() { return this == Y; }
    }
}
