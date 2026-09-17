package atomixsoft.dev.cosmos.render;

public class Mesh {

    private final VertexArray m_VAO;
    private final VertexBuffer m_VBO;
    private final IndexBuffer m_EBO;

    private boolean m_Initialized;

    public Mesh() {
        m_VAO = new VertexArray();
        m_VBO = new VertexBuffer();
        m_EBO = new IndexBuffer();

        m_Initialized = false;
    }

    public void create(float[] vertices, int[] indices, BufferLayout layout) {
        if(m_Initialized) return;

        if(vertices == null || vertices.length == 0)
            throw new IllegalArgumentException("Mesh Vertex Data cannot be empty!");

        if(indices == null || indices.length == 0)
            throw new IllegalArgumentException("Mesh Index Data cannot be empty!");

        if(layout == null || layout.isEmpty())
            throw new IllegalArgumentException("Mesh Layout cannot be null or empty!");

        try {
            m_VAO.create();
            m_VBO.create(vertices);
            m_EBO.create(indices);

            m_VAO.configure(m_VBO, layout);

            m_Initialized = true;
        } catch(RuntimeException | Error e) {
            dispose();
            throw e;
        }
    }

    public void bind() {
        validate();

        m_VAO.bind();
        m_EBO.bind();
    }

    public void dispose() {
        if(!m_Initialized) {
            m_VAO.dispose();
            m_EBO.dispose();
            m_VBO.dispose();

            return;
        }

        try {
            m_VAO.dispose();
        } finally {
            try {
                m_EBO.dispose();
            } finally {
                m_VBO.dispose();
            }
        }

        m_Initialized = false;
    }

    public int getIndexCount() {
        validate();
        return m_EBO.getCount();
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Mesh has not been initialized!");
    }

}
