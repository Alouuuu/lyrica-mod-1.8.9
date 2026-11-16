package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for 1.21's immutable {@code Vec3d} (the subset the world lyrics use). */
public final class Vec3d {
    public static final Vec3d ZERO = new Vec3d(0, 0, 0);
    public final double x, y, z;

    public Vec3d(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    public static Vec3d of(net.minecraft.util.Vec3 value) { return new Vec3d(value.xCoord, value.yCoord, value.zCoord); }
    public net.minecraft.util.Vec3 handle() { return new net.minecraft.util.Vec3(x, y, z); }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public Vec3d add(Vec3d o) { return new Vec3d(x + o.x, y + o.y, z + o.z); }
    public Vec3d add(double dx, double dy, double dz) { return new Vec3d(x + dx, y + dy, z + dz); }
    public Vec3d subtract(Vec3d o) { return new Vec3d(x - o.x, y - o.y, z - o.z); }
    public Vec3d subtract(double dx, double dy, double dz) { return new Vec3d(x - dx, y - dy, z - dz); }
    public Vec3d multiply(double f) { return new Vec3d(x * f, y * f, z * f); }
    public Vec3d multiply(double fx, double fy, double fz) { return new Vec3d(x * fx, y * fy, z * fz); }
    public Vec3d negate() { return multiply(-1); }

    public Vec3d normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return length < 1.0E-5 ? ZERO : new Vec3d(x / length, y / length, z / length);
    }

    public double dotProduct(Vec3d o) { return x * o.x + y * o.y + z * o.z; }
    public Vec3d crossProduct(Vec3d o) { return new Vec3d(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
    public double length() { return Math.sqrt(lengthSquared()); }
    public double lengthSquared() { return x * x + y * y + z * z; }
    public double horizontalLength() { return Math.sqrt(x * x + z * z); }
    public double distanceTo(Vec3d o) { return Math.sqrt(squaredDistanceTo(o)); }
    public double squaredDistanceTo(Vec3d o) { double dx = o.x - x, dy = o.y - y, dz = o.z - z; return dx * dx + dy * dy + dz * dz; }

    /** Same convention as 1.21's {@code Vec3d.rotateY} (radians). */
    public Vec3d rotateY(float angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3d(x * cos + z * sin, y, z * cos - x * sin);
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Vec3d)) return false;
        Vec3d v = (Vec3d) o;
        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0 && Double.compare(z, v.z) == 0;
    }
    @Override public int hashCode() { return java.util.Arrays.hashCode(new double[]{x, y, z}); }
    @Override public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
}
