package atomixsoft.dev.cosmos.render;

import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL11.*;

public class Graphics {

    private int m_ViewportWidth;
    private int m_ViewportHeight;

    private boolean m_Initialized;

    public Graphics() {
        m_Initialized = false;
    }

    public void initialize() {
        if(m_Initialized) return;

        GL.createCapabilities();
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        m_ViewportWidth = m_ViewportHeight = 0;
        m_Initialized = true;
    }

    public void clear() {
        validate();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    public void resize(int width, int height) {
        validate();
        if(width < 0 || height < 0)
            throw new IllegalArgumentException("Viewport dimensions cannot be negative!");

        m_ViewportWidth = width;
        m_ViewportHeight = height;

        glViewport(0, 0, width, height);
    }

    public void drawTriangles(VertexArray vao, int vertexCount) {
        validate();

        if(vao == null)
            throw new IllegalArgumentException("Vertex Array cannot be null!");

        if(vertexCount <= 0)
            throw new IllegalArgumentException("Vertex Count must be greater than zero!");

        vao.bind();

        try {
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        } finally {
            vao.unbind();
        }
    }

    public void drawIndexed(VertexArray vao, IndexBuffer indexBuffer) {
        validate();

        if(vao == null)
            throw new IllegalArgumentException("Vertex Array cannot be null!");

        if(indexBuffer == null)
            throw new IllegalArgumentException("Index Buffer cannot be null!");

        vao.bind();

        try {
            indexBuffer.bind();
            glDrawElements(GL_TRIANGLES, indexBuffer.getCount(), GL_UNSIGNED_INT, 0L);
        } finally {
            vao.unbind();
        }
    }

    public void draw(Mesh mesh) {
        validate();
        if(mesh == null)
            throw new IllegalArgumentException("Mesh cannot be null!");

        mesh.bind();
        glDrawElements(GL_TRIANGLES, mesh.getIndexCount(), GL_UNSIGNED_INT, 0L);
    }

    public void dispose() {
        if(!m_Initialized) return;

        GL.setCapabilities(null);
        m_Initialized = false;
    }

    public void setDepthTestEnabled(boolean enabled) {
        validate();

        if(enabled) glEnable(GL_DEPTH_TEST);
        else glDisable(GL_DEPTH_TEST);
    }

    public int getViewportWidth() {
        validate();
        return m_ViewportWidth;
    }

    public int getViewportHeight() {
        validate();
        return m_ViewportHeight;
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Graphics has not been initialized!");
    }

}
