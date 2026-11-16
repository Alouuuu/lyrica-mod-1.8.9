package dev.relaylyrics.platform.mc;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.8.9 stand-in for JOML's {@code Quaternionf} as used by the world lyrics: a chain of axis
 * rotations ({@code new Quaternionf().rotateY(a).rotateX(b)}) replayed as GL rotations in order
 * by {@link MatrixStack#multiply(Quaternionf)}.
 */
public final class Quaternionf {
    final List<float[]> rotations = new ArrayList<>();

    public Quaternionf rotateX(float radians) { rotations.add(new float[]{radians, 1, 0, 0}); return this; }
    public Quaternionf rotateY(float radians) { rotations.add(new float[]{radians, 0, 1, 0}); return this; }
    public Quaternionf rotateZ(float radians) { rotations.add(new float[]{radians, 0, 0, 1}); return this; }
}
