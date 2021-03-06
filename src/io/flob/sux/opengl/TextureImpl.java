package io.flob.sux.opengl;

import io.flob.sux.opengl.renderer.Renderer;
import io.flob.sux.opengl.renderer.SGL;
import io.flob.sux.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

/**
 * A texture to be bound within JOGL. This object is responsible for keeping
 * track of a given OpenGL texture and for calculating the texturing mapping
 * coordinates of the full image.
 *
 * Since textures need to be powers of 2 the actual texture may be considerably
 * bigged that the source image and hence the texture mapping coordinates need
 * to be adjusted to match up drawing the sprite against the texture.
 *
 * @author Kevin Glass
 * @author Brian Matzon
 */
public class TextureImpl implements Texture {

    /**
     * The renderer to use for all GL operations
     */
    protected static SGL GL = Renderer.get();

    /**
     * The last texture that was bound to
     */
    static Texture lastBind;

    /**
     * Retrieve the last texture bound through the texture interface
     *
     * @return The last texture bound
     */
    public static Texture getLastBind() {
        return lastBind;
    }

    /**
     * The GL target type
     */
    private int target;
    /**
     * The GL texture ID
     */
    private int textureID;
    /**
     * The height of the image
     */
    private int height;
    /**
     * The width of the image
     */
    private int width;
    /**
     * The width of the texture
     */
    private int texWidth;
    /**
     * The height of the texture
     */
    private int texHeight;
    /**
     * The ratio of the width of the image to the texture
     */
    private float widthRatio;
    /**
     * The ratio of the height of the image to the texture
     */
    private float heightRatio;
    /**
     * If this texture has alpha
     */
    private boolean alpha;
    /**
     * The reference this texture was loaded from
     */
    private String ref;
    /**
     * The name the texture has in the cache
     */
    private String cacheName;

    /**
     * Data used to reload this texture
     */
    private ReloadData reloadData;

    /**
     * For subclasses to utilise
     */
    protected TextureImpl() {
    }

    /**
     * Create a new texture
     *
     * @param ref The reference this texture was loaded from
     * @param target The GL target
     * @param textureID The GL texture ID
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public TextureImpl(String ref, int target, int textureID) {
        this.target = target;
        this.ref = ref;
        this.textureID = textureID;
        lastBind = this;
    }

    /**
     * Set the name this texture is stored against in the cache
     *
     * @param cacheName The name the texture is stored against in the cache
     */
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public boolean hasAlpha() {
        return alpha;
    }

    @Override
    public String getTextureRef() {
        return ref;
    }

    /**
     * If this texture has alpha
     *
     * @param alpha True, If this texture has alpha
     */
    public void setAlpha(boolean alpha) {
        this.alpha = alpha;
    }

    /**
     * Clear the binding of the texture
     */
    public static void bindNone() {
        lastBind = null;
        GL.glDisable(SGL.GL_TEXTURE_2D);
    }

    /**
     * Clear SUX caching of the last bound texture so that an external texture
     * binder can play with the context before returning control to SUX.
     */
    public static void unbind() {
        lastBind = null;
    }

    @Override
    public void bind() {
        if (lastBind != this) {
            lastBind = this;
            GL.glEnable(SGL.GL_TEXTURE_2D);
            GL.glBindTexture(target, textureID);
        }
    }

    /**
     * Set the height of the image
     *
     * @param height The height of the image
     */
    public void setHeight(int height) {
        this.height = height;
        setHeight();
    }

    /**
     * Set the width of the image
     *
     * @param width The width of the image
     */
    public void setWidth(int width) {
        this.width = width;
        setWidth();
    }

    @Override
    public int getImageHeight() {
        return height;
    }

    @Override
    public int getImageWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return heightRatio;
    }

    @Override
    public float getWidth() {
        return widthRatio;
    }

    @Override
    public int getTextureHeight() {
        return texHeight;
    }

    @Override
    public int getTextureWidth() {
        return texWidth;
    }

    /**
     * Set the height of this texture
     *
     * @param texHeight The height of the texture
     */
    public void setTextureHeight(int texHeight) {
        this.texHeight = texHeight;
        setHeight();
    }

    /**
     * Set the width of this texture
     *
     * @param texWidth The width of the texture
     */
    public void setTextureWidth(int texWidth) {
        this.texWidth = texWidth;
        setWidth();
    }

    /**
     * Set the height of the texture. This will update the ratio also.
     */
    private void setHeight() {
        if (texHeight != 0) {
            heightRatio = ((float) height) / texHeight;
        }
    }

    /**
     * Set the width of the texture. This will update the ratio also.
     */
    private void setWidth() {
        if (texWidth != 0) {
            widthRatio = ((float) width) / texWidth;
        }
    }

    @Override
    public void release() {
        IntBuffer texBuf = createIntBuffer(1);
        texBuf.put(textureID);
        texBuf.flip();

        GL.glDeleteTextures(texBuf);

        if (lastBind == this) {
            bindNone();
        }

        if (cacheName != null) {
            InternalTextureLoader.get().clear(cacheName);
        } else {
            InternalTextureLoader.get().clear(ref);
        }
    }

    @Override
    public int getTextureID() {
        return textureID;
    }

    /**
     * Set the OpenGL texture ID for this texture
     *
     * @param textureID The OpenGL texture ID
     */
    public void setTextureID(int textureID) {
        this.textureID = textureID;
    }

    /**
     * Creates an integer buffer to hold specified ints - strictly a utility
     * method
     *
     * @param size how many int to contain
     * @return created IntBuffer
     */
    protected IntBuffer createIntBuffer(int size) {
        ByteBuffer temp = ByteBuffer.allocateDirect(4 * size);
        temp.order(ByteOrder.nativeOrder());

        return temp.asIntBuffer();
    }

    @Override
    public byte[] getTextureData() {
        ByteBuffer buffer = BufferUtils.createByteBuffer((hasAlpha() ? 4 : 3) * texWidth * texHeight);
        bind();
        GL.glGetTexImage(SGL.GL_TEXTURE_2D, 0, hasAlpha() ? SGL.GL_RGBA : SGL.GL_RGB, SGL.GL_UNSIGNED_BYTE,
                buffer);
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);
        buffer.clear();

        return data;
    }

    @Override
    public void setTextureFilter(int textureFilter) {
        bind();
        GL.glTexParameteri(target, SGL.GL_TEXTURE_MIN_FILTER, textureFilter);
        GL.glTexParameteri(target, SGL.GL_TEXTURE_MAG_FILTER, textureFilter);
    }

    /**
     * Set the texture data that this texture can be reloaded from
     *
     * @param srcPixelFormat The pixel format
     * @param componentCount The component count
     * @param minFilter The OpenGL minification filter
     * @param magFilter The OpenGL magnification filter
     * @param textureBuffer The texture buffer containing the data for the
     * texture
     */
    public void setTextureData(int srcPixelFormat, int componentCount,
            int minFilter, int magFilter, ByteBuffer textureBuffer) {
        reloadData = new ReloadData();
        reloadData.srcPixelFormat = srcPixelFormat;
        reloadData.componentCount = componentCount;
        reloadData.minFilter = minFilter;
        reloadData.magFilter = magFilter;
        reloadData.textureBuffer = textureBuffer;
    }

    /**
     * Reload this texture
     */
    public void reload() {
        if (reloadData != null) {
            textureID = reloadData.reload();
        }
    }

    /**
     * Reload this texture from it's original source data
     */
    private class ReloadData {

        /**
         * The src pixel format
         */
        private int srcPixelFormat;
        /**
         * The component count
         */
        private int componentCount;
        /**
         * The OpenGL minification filter
         */
        private int minFilter;
        /**
         * The OpenGL magnification filter
         */
        private int magFilter;
        /**
         * The texture buffer of pixel data
         */
        private ByteBuffer textureBuffer;

        /**
         * Reload this texture
         *
         * @return The new texture ID assigned to this texture
         */
        public int reload() {
            Log.error("Reloading texture: " + ref);
            return InternalTextureLoader.get().reload(TextureImpl.this, srcPixelFormat, componentCount, minFilter, magFilter, textureBuffer);
        }
    }
}
