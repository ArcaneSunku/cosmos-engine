package atomixsoft.dev.cosmos.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public final class Texture2D {

    private int m_RenderId;

    private int m_Width;
    private int m_Height;

    private boolean m_Initialized;

    public Texture2D() {
        m_RenderId = 0;

        m_Width = 0;
        m_Height = 0;

        m_Initialized = false;
    }

    public void create(int width, int height, byte[] rgbaPixels) {
        if (m_Initialized) return;

        if (width <= 0)
            throw new IllegalArgumentException("Texture width must be greater than zero!");

        if (height <= 0)
            throw new IllegalArgumentException("Texture height must be greater than zero!");

        if (rgbaPixels == null)
            throw new IllegalArgumentException("Texture pixel data cannot be null!");

        final int requiredSize = width * height * 4;

        if (rgbaPixels.length != requiredSize)
            throw new IllegalArgumentException("RGBA Texture requires exactly " + requiredSize + " bytes, but received " + rgbaPixels.length + "!");

        int renderId = 0;
        final ByteBuffer pixels = MemoryUtil.memAlloc(rgbaPixels.length);

        try {
            pixels.put(rgbaPixels).flip();

            renderId = glGenTextures();
            if (renderId == 0)
                throw new IllegalStateException("Failed to create Texture!");

            glBindTexture(GL_TEXTURE_2D, renderId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, 0);

            m_RenderId = renderId;

            m_Width = width;
            m_Height = height;

            m_Initialized = true;

            renderId = 0;
        } finally {
            MemoryUtil.memFree(pixels);

            if (renderId != 0)
                glDeleteTextures(renderId);
        }
    }

    public void bind(int slot) {
        validate();
        if (slot < 0)
            throw new IllegalArgumentException("Texture slot cannot be negative!");

        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, m_RenderId);
    }

    public void dispose() {
        if (!m_Initialized) return;

        glDeleteTextures(m_RenderId);

        m_RenderId = 0;

        m_Width = 0;
        m_Height = 0;

        m_Initialized = false;
    }

    public int getWidth() {
        validate();
        return m_Width;
    }

    public int getHeight() {
        validate();
        return m_Height;
    }

    private void validate() {
        if (!m_Initialized)
            throw new IllegalStateException("Texture has not been initialized!");
    }

}