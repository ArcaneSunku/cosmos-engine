package atomixsoft.dev.cosmos.render;

import static org.lwjgl.opengl.GL15.*;

public class VertexBuffer {

    private int m_RenderId;
    private boolean m_Initialized;

    public VertexBuffer() {
        m_RenderId = 0;
        m_Initialized = false;
    }

    public void create(float[] data) {
        if(m_Initialized) return;

        if(data == null || data.length == 0)
            throw new IllegalArgumentException("Vertex Buffer Data cannot be empty!");

        m_RenderId = glGenBuffers();
        if(m_RenderId == 0)
            throw new IllegalStateException("Failed to create Vertex Buffer!");

        glBindBuffer(GL_ARRAY_BUFFER, m_RenderId);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        m_Initialized = true;
    }

    public void bind() {
        validate();
        glBindBuffer(GL_ARRAY_BUFFER, m_RenderId);
    }

    public void unbind() {
        validate();
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void dispose() {
        if(!m_Initialized) return;

        glDeleteBuffers(m_RenderId);

        m_RenderId = 0;
        m_Initialized = false;
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Vertex Buffer has not been initialized!");
    }

}
