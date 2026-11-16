package dev.relaylyrics.platform.mc;

/** 1.8.9 facade for 1.21's {@code HitResult.Type}. */
public abstract class HitResult {
    public enum Type { MISS, BLOCK, ENTITY }
    public abstract Type getType();
    public abstract Vec3d getPos();
}
