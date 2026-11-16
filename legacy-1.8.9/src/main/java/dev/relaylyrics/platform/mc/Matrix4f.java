package dev.relaylyrics.platform.mc;

/**
 * Minimal JOML {@code Matrix4f} replacement for the 2D GUI transforms Lyrica uses
 * (JOML naming: mCR = column C, row R). Only the affine x/y part is tracked.
 */
public final class Matrix4f {
    // x' = m00*x + m10*y + m30 ; y' = m01*x + m11*y + m31
    float m00 = 1, m01 = 0, m10 = 0, m11 = 1, m30 = 0, m31 = 0;

    public Matrix4f() { }
    Matrix4f(Matrix4f other) { set(other); }

    void set(Matrix4f o) { m00 = o.m00; m01 = o.m01; m10 = o.m10; m11 = o.m11; m30 = o.m30; m31 = o.m31; }

    public float m00() { return m00; }
    public float m01() { return m01; }
    public float m10() { return m10; }
    public float m11() { return m11; }
    public float m30() { return m30; }
    public float m31() { return m31; }

    void translate(float x, float y) {
        m30 += m00 * x + m10 * y;
        m31 += m01 * x + m11 * y;
    }

    void scale(float x, float y) {
        m00 *= x; m01 *= x;
        m10 *= y; m11 *= y;
    }

    public Vector3f transformPosition(Vector3f v) {
        float x = m00 * v.x + m10 * v.y + m30;
        float y = m01 * v.x + m11 * v.y + m31;
        v.x = x; v.y = y;
        return v;
    }
}
