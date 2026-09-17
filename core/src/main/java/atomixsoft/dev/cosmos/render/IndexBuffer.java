package atomixsoft.dev.cosmos.render;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.GL_COPY_WRITE_BUFFER;

public class IndexBuffer {

    private int m_RenderId;
    private int m_Count;

    private boolean m_Initialized;

    public IndexBuffer() {
        m_RenderId = 0;
        m_Count = 0;

        m_Initialized = false;
    }

    public void create(int[] indices) {
        if(m_Initialized) return;

        if(indices == null || indices.length == 0)
            throw new IllegalArgumentException("Index Buffer data cannot be empty!");

        m_RenderId = glGenBuffers();
        if(m_RenderId == 0)
            throw new IllegalStateException("Failed to create Index Buffer!");

        glBindBuffer(GL_COPY_WRITE_BUFFER, m_RenderId);
        glBufferData(GL_COPY_WRITE_BUFFER, indices, GL_STATIC_DRAW);
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);

        m_Count = indices.length;
        m_Initialized = true;
    }

    public void bind() {
        validate();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_RenderId);
    }

    public void unbind() {
        validate();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void dispose() {
        if(!m_Initialized) return;

        glDeleteBuffers(m_RenderId);

        m_RenderId = 0;
        m_Count = 0;
        m_Initialized = false;
    }

    public int getCount() {
        return m_Count;
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Index Buffer has not been initialized");
    }

}
