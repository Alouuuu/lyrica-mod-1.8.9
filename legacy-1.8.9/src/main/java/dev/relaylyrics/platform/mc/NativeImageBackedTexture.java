package dev.relaylyrics.platform.mc;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

/**
 * 1.8.9 facade for {@code NativeImageBackedTexture}: a {@link NativeImage} backed by a
 * {@link DynamicTexture}. Must be created, uploaded and closed on the client (GL) thread.
 */
public final class NativeImageBackedTexture implements AutoCloseable {
    private NativeImage image;
    private final DynamicTexture texture;
    private boolean blur;
    private boolean closed;

    public NativeImageBackedTexture(NativeImage image) {
        this.image = image;
        GlThread.check("texture creation");   // DynamicTexture allocates GL storage immediately
        this.texture = new DynamicTexture(image.getWidth(), image.getHeight());
        upload();
    }

    public NativeImageBackedTexture(int width, int height, boolean clear) {
        this(new NativeImage(width, height, clear));
    }

    public NativeImage getImage() { return image; }

    public void setImage(NativeImage image) {
        if (image.getWidth() != this.image.getWidth() || image.getHeight() != this.image.getHeight())
            throw new IllegalArgumentException("Texture size cannot change");
        this.image.close();
        this.image = image;
    }

    public void upload() {
        if (closed) return;
        if (!GlThread.check("texture upload")) { GlThread.run(this::upload); return; }
        image.toArgb(texture.getTextureData());
        texture.updateDynamicTexture();   // binds + uploads through TextureUtil
        applyFilter();
    }

    public void setFilter(boolean blur, boolean mipmap) {
        this.blur = blur;
        applyFilter();
    }

    private void applyFilter() {
        if (closed) return;
        GlStateManager.bindTexture(texture.getGlTextureId());
        int filter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
    }

    public void bindTexture() { GlStateManager.bindTexture(texture.getGlTextureId()); }

    public int getGlId() { return texture.getGlTextureId(); }

    DynamicTexture handle() { return texture; }

    @Override public void close() {
        if (closed) return;
        closed = true;
        GlThread.run(texture::deleteGlTexture);
        image.close();
    }
}
