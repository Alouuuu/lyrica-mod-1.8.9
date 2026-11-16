package dev.relaylyrics.platform.mc;

import net.minecraft.client.renderer.GlStateManager;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 1.8.9 facade for {@code MatrixStack}: mirrors every operation into the fixed-function GL
 * modelview stack and keeps a CPU copy so callers can transform points (scissor bands etc.).
 */
public final class MatrixStack {
    public final class Entry {
        private final Matrix4f matrix;
        Entry(Matrix4f matrix) { this.matrix = matrix; }
        public Matrix4f getPositionMatrix() { return matrix; }
    }

    private final Deque<Matrix4f> stack = new ArrayDeque<>();

    MatrixStack() { stack.push(new Matrix4f()); }

    public void push() {
        stack.push(new Matrix4f(stack.peek()));
        GlStateManager.pushMatrix();
    }

    public void pop() {
        if (stack.size() <= 1) throw new IllegalStateException("MatrixStack underflow");
        stack.pop();
        GlStateManager.popMatrix();
    }

    public void translate(float x, float y, float z) {
        stack.peek().translate(x, y);
        GlStateManager.translate(x, y, z);
    }

    public void translate(double x, double y, double z) { translate((float) x, (float) y, (float) z); }

    public void scale(float x, float y, float z) {
        stack.peek().scale(x, y);
        GlStateManager.scale(x, y, z);
    }

    /** World rendering only: GL-side rotation (the CPU copy is 2D and doesn't track it). */
    public void multiply(Quaternionf rotation) {
        for (float[] r : rotation.rotations)
            GlStateManager.rotate((float) Math.toDegrees(r[0]), r[1], r[2], r[3]);
    }

    public void scale(double x, double y, double z) { scale((float) x, (float) y, (float) z); }

    public Entry peek() { return new Entry(stack.peek()); }

    int depth() { return stack.size(); }

    /** Pops anything a caller left pushed (keeps GL and CPU stacks balanced across frames). */
    void unwind() { while (stack.size() > 1) pop(); }
}
