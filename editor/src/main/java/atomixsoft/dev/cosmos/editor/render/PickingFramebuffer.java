package atomixsoft.dev.cosmos.editor.render;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public final class PickingFramebuffer {

    private int m_RenderId;
    private int m_EntityAttachmentId;
    private int m_DepthAttachmentId;

    private int m_Width;
    private int m_Height;

    private boolean m_Initialized;

    public PickingFramebuffer() {
        m_RenderId = 0;
        m_EntityAttachmentId = 0;
        m_DepthAttachmentId = 0;

        m_Width = 0;
        m_Height = 0;

        m_Initialized = false;
    }

    public static void bindDefault() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static void validateCompletion() {
        final int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Picking Framebuffer is incomplete! OpenGL status: 0x" + Integer.toHexString(status));
    }

    private static void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Picking Framebuffer dimensions must be greater than zero!");
    }

    public void create(int width, int height) {
        if (m_Initialized)
            return;

        validateDimensions(width, height);

        int framebufferId = 0;
        int entityAttachmentId = 0;
        int depthAttachmentId = 0;

        try {
            framebufferId = glGenFramebuffers();
            if (framebufferId == 0)
                throw new IllegalStateException("Failed to create Picking Framebuffer!");

            glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
            entityAttachmentId = glGenTextures();

            if (entityAttachmentId == 0)
                throw new IllegalStateException("Failed to create Picking Framebuffer Entity attachment!");

            glBindTexture(GL_TEXTURE_2D, entityAttachmentId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_R32I, width, height, 0, GL_RED_INTEGER, GL_INT, (ByteBuffer) null);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, entityAttachmentId, 0);

            depthAttachmentId = glGenRenderbuffers();

            if (depthAttachmentId == 0)
                throw new IllegalStateException("Failed to create Picking Framebuffer depth attachment!");

            glBindRenderbuffer(GL_RENDERBUFFER, depthAttachmentId);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttachmentId);

            validateCompletion();

            m_RenderId = framebufferId;
            m_EntityAttachmentId = entityAttachmentId;
            m_DepthAttachmentId = depthAttachmentId;

            m_Width = width;
            m_Height = height;

            m_Initialized = true;

            framebufferId = 0;
            entityAttachmentId = 0;
            depthAttachmentId = 0;
        } finally {
            glBindTexture(GL_TEXTURE_2D, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            if (depthAttachmentId != 0)
                glDeleteRenderbuffers(depthAttachmentId);

            if (entityAttachmentId != 0)
                glDeleteTextures(entityAttachmentId);

            if (framebufferId != 0)
                glDeleteFramebuffers(framebufferId);
        }
    }

    public void resize(int width, int height) {
        validate();
        validateDimensions(width, height);

        if (m_Width == width && m_Height == height)
            return;

        glBindTexture(GL_TEXTURE_2D, m_EntityAttachmentId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32I, width, height, 0, GL_RED_INTEGER, GL_INT, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, m_DepthAttachmentId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);

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

    public void bind() {
        validate();
        glBindFramebuffer(GL_FRAMEBUFFER, m_RenderId);
    }

    public void clear() {
        validate();

        glDepthMask(true);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            glClearBufferiv(GL_COLOR, 0, stack.ints(0));
        }

        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public int readEntityId(int x, int y) {
        validate();

        if (x < 0 || x >= m_Width || y < 0 || y >= m_Height)
            return 0;

        bind();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer pixel = stack.mallocInt(1);

            glReadBuffer(GL_COLOR_ATTACHMENT0);
            glReadPixels(x, y, 1, 1, GL_RED_INTEGER, GL_INT, pixel);

            return pixel.get(0);
        } finally {
            bindDefault();
        }
    }

    public void dispose() {
        if (!m_Initialized)
            return;

        glDeleteRenderbuffers(m_DepthAttachmentId);
        glDeleteTextures(m_EntityAttachmentId);
        glDeleteFramebuffers(m_RenderId);

        m_RenderId = 0;
        m_EntityAttachmentId = 0;
        m_DepthAttachmentId = 0;

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
            throw new IllegalStateException("Picking Framebuffer has not been initialized!");
    }

}