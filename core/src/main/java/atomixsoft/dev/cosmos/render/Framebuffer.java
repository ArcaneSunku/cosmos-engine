package atomixsoft.dev.cosmos.render;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;

public class Framebuffer {

    private int m_RenderId;
    private int m_ColorAttachmentId;
    private int m_DepthAttachmentId;

    private int m_Width, m_Height;

    private boolean m_Initialized;

    public Framebuffer() {
        m_RenderId = 0;
        m_ColorAttachmentId = 0;
        m_DepthAttachmentId = 0;

        m_Width = 0;
        m_Height = 0;

        m_Initialized = false;
    }

    public void create(int width, int height) {
        if(m_Initialized)
            return;

        validateDimensions(width, height);

        int framebufferId = 0;
        int colorAttachmentId = 0;
        int depthAttachmentId = 0;

        try {
            framebufferId = glGenFramebuffers();
            if(framebufferId == 0)
                throw new IllegalStateException("Failed to create Framebuffer!");

            glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);

            colorAttachmentId = glGenTextures();
            if(colorAttachmentId == 0)
                throw new IllegalStateException("Failed to create Framebuffer color attachment!");

            glBindTexture(GL_TEXTURE_2D, colorAttachmentId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorAttachmentId, 0);

            depthAttachmentId = glGenRenderbuffers();
            if(depthAttachmentId == 0)
                throw new IllegalStateException("Failed to create Framebuffer depth/stencil attachment!");

            glBindRenderbuffer(GL_RENDERBUFFER, depthAttachmentId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthAttachmentId);

            validateCompletion();

            m_RenderId = framebufferId;
            m_ColorAttachmentId = colorAttachmentId;
            m_DepthAttachmentId = depthAttachmentId;

            m_Width = width;
            m_Height = height;

            m_Initialized = true;

            framebufferId = 0;
            colorAttachmentId = 0;
            depthAttachmentId = 0;
        } finally {
            glBindTexture(GL_TEXTURE_2D, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            if(depthAttachmentId != 0)
                glDeleteRenderbuffers(depthAttachmentId);

            if(colorAttachmentId != 0)
                glDeleteTextures(colorAttachmentId);

            if(framebufferId != 0)
                glDeleteFramebuffers(framebufferId);
        }
    }

    public void bind() {
        validate();
        glBindFramebuffer(GL_FRAMEBUFFER, m_RenderId);
    }

    public static void bindDefault() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void dispose() {
        if(!m_Initialized)
            return;

        glDeleteRenderbuffers(m_DepthAttachmentId);
        glDeleteTextures(m_ColorAttachmentId);
        glDeleteFramebuffers(m_RenderId);

        m_RenderId = 0;
        m_ColorAttachmentId = 0;
        m_DepthAttachmentId = 0;

        m_Width = 0;
        m_Height = 0;

        m_Initialized = false;
    }

    public void resize(int width, int height) {
        validate();
        validateDimensions(width, height);

        if(m_Width == width && m_Height == height)
            return;

        glBindTexture(GL_TEXTURE_2D, m_ColorAttachmentId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_2D, 0);

        glBindRenderbuffer(GL_RENDERBUFFER, m_DepthAttachmentId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        glBindFramebuffer(GL_FRAMEBUFFER, m_RenderId);

        try {
            validateCompletion();
        } finally {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        m_Width = width;
        m_Height = height;
    }

    public long getColorAttachmentId() {
        validate();
        return Integer.toUnsignedLong(m_ColorAttachmentId);
    }

    public int getWidth() {
        validate();
        return m_Width;
    }

    public int getHeight() {
        validate();
        return m_Height;
    }

    private static void validateCompletion() {
        final int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if(status != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalArgumentException("Framebuffer is incomplete! OpenGL status: 0x" + Integer.toHexString(status));
    }

    private static void validateDimensions(int width, int height) {
        if(width <= 0 || height <= 0)
            throw new IllegalArgumentException("Framebuffer size must be greater than zero!");
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Framebuffer has not been initialized!");
    }

}
