package dev.relaylyrics.platform.mc;

/**
 * 1.8.9 stand-in for 1.21's {@code VertexConsumerProvider}. 1.8.9 draws immediately with the
 * current GL modelview, so this only carries the {@link DrawContext} used for textured text.
 */
public final class VertexConsumerProvider {
    private final DrawContext context;
    public VertexConsumerProvider(DrawContext context) { this.context = context; }
    public DrawContext context() { return context; }
}
