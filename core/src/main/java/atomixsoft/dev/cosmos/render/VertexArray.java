package atomixsoft.dev.cosmos.render;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class VertexArray {

    private int m_RenderId;
    private boolean m_Initialized;

    public VertexArray() {
        m_RenderId = 0;
        m_Initialized = false;
    }

    public void create() {
        if(m_Initialized) return;

        m_RenderId = glGenVertexArrays();
        if(m_RenderId == 0)
            throw new IllegalStateException("Failed to create Vertex Array!");

        m_Initialized = true;
    }

    public void configure(VertexBuffer buffer, BufferLayout layout) {
        validate();

        if(buffer == null)
            throw new IllegalArgumentException("Vertex Buffer cannot be null!");

        if(layout == null)
            throw new IllegalArgumentException("Buffer Layout cannot be null!");

        if(layout.getAttribCount() == 0)
            throw new IllegalArgumentException("Buffer Layout cannot be empty!");

        bind();

        try {
            buffer.bind();

            for(int index = 0; index < layout.getAttribCount(); index++) {
                glEnableVertexAttribArray(index);
                glVertexAttribPointer(index, layout.getComponentCount(index), GL_FLOAT, false, layout.getStride(), layout.getOffset(index));
            }
        } finally {
            buffer.unbind();
            unbind();
        }
    }

    public void bind() {
        validate();
        glBindVertexArray(m_RenderId);
    }

    public void unbind() {
        validate();
        glBindVertexArray(0);
    }

    public void dispose() {
        if(!m_Initialized) return;

        glDeleteVertexArrays(m_RenderId);

        m_RenderId = 0;
        m_Initialized = false;
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Vertex Array has not been initialized!");
    }

}
