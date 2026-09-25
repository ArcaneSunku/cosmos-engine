package atomixsoft.dev.cosmos.render;

import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Material {

    private final Shader m_Shader;

    private final Map<String, Float> m_FloatUniforms;
    private final Map<String, Vector4f> m_Vector4Uniforms;
    private final Map<String, TextureBinding> m_Textures;

    private RenderState m_RenderState;

    public Material(Shader shader) {
        if (shader == null)
            throw new IllegalArgumentException("Material Shader cannot be null!");

        m_Shader = shader;

        m_FloatUniforms = new LinkedHashMap<>();
        m_Vector4Uniforms = new LinkedHashMap<>();
        m_Textures = new LinkedHashMap<>();

        m_RenderState = RenderState.OPAQUE;
    }

    public void bind() {
        m_Shader.bind();

        try {
            for (Map.Entry<String, Float> entry : m_FloatUniforms.entrySet())
                m_Shader.setFloat(entry.getKey(), entry.getValue());

            for (Map.Entry<String, Vector4f> entry : m_Vector4Uniforms.entrySet()) {
                final Vector4f value = entry.getValue();
                m_Shader.setFloat4(entry.getKey(), value.x, value.y, value.z, value.w);
            }

            for (Map.Entry<String, TextureBinding> entry : m_Textures.entrySet()) {
                final TextureBinding binding = entry.getValue();
                binding.texture().bind(binding.slot());
                m_Shader.setInt(entry.getKey(), binding.slot());
            }
        } catch (RuntimeException | Error exception) {
            m_Shader.unbind();
            throw exception;
        }
    }

    public void unbind() {
        m_Shader.unbind();
    }

    public Material setFloat(String name, float value) {
        validateUniformName(name);
        m_FloatUniforms.put(name, value);

        return this;
    }

    public Material setFloat4(String name, float x, float y, float z, float w) {
        validateUniformName(name);
        m_Vector4Uniforms.put(name, new Vector4f(x, y, z, w));

        return this;
    }

    public Material setFloat4(String name, Vector4fc value) {
        validateUniformName(name);
        if (value == null)
            throw new IllegalArgumentException("Material Vector4 value cannot be null!");

        m_Vector4Uniforms.put(name, new Vector4f(value));
        return this;
    }

    public Material setTexture(String name, Texture2D texture, int slot) {
        validateUniformName(name);

        if (texture == null) throw new IllegalArgumentException("Material Texture cannot be null!");
        if (slot < 0) throw new IllegalArgumentException("Texture slot cannot be negative!");

        for (Map.Entry<String, TextureBinding> entry : m_Textures.entrySet()) {
            if (entry.getKey().equals(name)) continue;

            if (entry.getValue().slot() == slot)
                throw new IllegalArgumentException("Texture slot " + slot + " is already used by uniform " + entry.getKey() + "!");
        }

        m_Textures.put(name, new TextureBinding(texture, slot));
        return this;
    }

    public Material setRenderState(RenderState renderState) {
        if(renderState == null)
            throw new IllegalArgumentException("RenderState cannot be null!");

        m_RenderState = renderState;
        return this;
    }

    public Shader getShader() {
        return m_Shader;
    }

    public RenderState getRenderState() {
        return m_RenderState;
    }

    private static void validateUniformName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Material uniform name cannot be empty!");
    }

    private record TextureBinding(Texture2D texture, int slot) {
    }

}