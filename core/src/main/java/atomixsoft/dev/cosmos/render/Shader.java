package atomixsoft.dev.cosmos.render;

import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class Shader {

    private final Map<String, Integer> m_UniformLocations;

    private int m_RenderId;
    private boolean m_Initialized;

    public Shader() {
        m_UniformLocations = new HashMap<>();

        m_RenderId = 0;
        m_Initialized = false;
    }

    public void initialize(String vertSrc, String fragSrc) {
        if(m_Initialized) return;

        if(vertSrc == null || vertSrc.isBlank())
            throw new IllegalStateException("Vertex Source cannot be empty!");

        if(fragSrc == null || fragSrc.isBlank())
            throw new IllegalStateException("Fragment Source cannot be empty!");

        int vertShader = 0;
        int fragShader = 0;
        int program = 0;

        try {
            vertShader = compileShader(GL_VERTEX_SHADER, vertSrc);
            fragShader = compileShader(GL_FRAGMENT_SHADER, fragSrc);

            program = glCreateProgram();
            if(program == 0)
                throw new IllegalStateException("Failed to create Shader Program!");

            glAttachShader(program, vertShader);
            glAttachShader(program, fragShader);

            glLinkProgram(program);
            if(glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                final String infoLog = glGetProgramInfoLog(program);
                throw new IllegalStateException("Failed to link Shader Program:\n" + infoLog);
            }

            m_RenderId = program;
            m_Initialized = true;
            program = 0;
        } finally {
            if(vertShader != 0)
                glDeleteShader(vertShader);

            if(fragShader != 0)
                glDeleteShader(fragShader);

            if(program != 0)
                glDeleteProgram(program);
        }
    }

    public void bind() {
        validate();
        glUseProgram(m_RenderId);
    }

    public void unbind() {
        validate();
        glUseProgram(0);
    }

    public void dispose() {
        if(!m_Initialized) return;

        glDeleteProgram(m_RenderId);

        m_UniformLocations.clear();
        m_RenderId = 0;
        m_Initialized = false;
    }

    public void setInt(String name, int value) {
        validate();
        glUniform1i(getUniformLocation(name), value);
    }

    public void setFloat(String name, float value) {
        validate();
        glUniform1f(getUniformLocation(name), value);
    }

    public void setFloat2(String name, float x, float y) {
        validate();
        glUniform2f(getUniformLocation(name), x, y);
    }

    public void setFloat2(String name, Vector2f value) {
        setFloat2(name, value.x, value.y);
    }

    public void setFloat3(String name, float x, float y, float z) {
        validate();
        glUniform3f(getUniformLocation(name), x, y, z);
    }

    public void setFloat3(String name, Vector3f value) {
        setFloat3(name, value.x, value.y, value.z);
    }

    public void setFloat4(String name, float x, float y, float z, float w) {
        validate();
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    public void setFloat4(String name, Vector4f value) {
        setFloat4(name, value.x, value.y, value.z, value.w);
    }

    public void setMatrix4(String name, Matrix4fc matrix) {
        validate();
        if(matrix == null)
            throw new IllegalArgumentException("Matrix cannot be null!");

        try(MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(getUniformLocation(name), false, buffer);
        }
    }

    private int compileShader(int type, String src) {
        final int shader = glCreateShader(type);
        if(shader == 0)
            throw new IllegalStateException("Failed to create Shader!");

        glShaderSource(shader, src);
        glCompileShader(shader);

        if(glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            final String infoLog = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile Shader:\n" + infoLog);
        }

        return shader;
    }

    private int getUniformLocation(String name) {
        validate();
        if(name == null || name.isBlank())
            throw new IllegalArgumentException(("Uniform name cannot be empty!"));

        final Integer cachedLocation = m_UniformLocations.get(name);
        if(cachedLocation != null)
            return cachedLocation;

        final int location = glGetUniformLocation(m_RenderId, name);
        if(location == -1)
            throw new IllegalArgumentException(("Shader Uniform does not exist or is inactive: " + name));

        m_UniformLocations.put(name, location);
        return location;
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Shader has not been initialized");
    }

}
