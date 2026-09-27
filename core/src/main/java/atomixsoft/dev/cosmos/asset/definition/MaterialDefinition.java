package atomixsoft.dev.cosmos.asset.definition;

import atomixsoft.dev.cosmos.asset.AssetKey;
import atomixsoft.dev.cosmos.render.RenderState;
import atomixsoft.dev.cosmos.render.Shader;
import atomixsoft.dev.cosmos.render.Texture2D;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MaterialDefinition {

    private final AssetKey<Shader> m_Shader;
    private final RenderState m_RenderState;

    private final Map<String, Float> m_Floats;
    private final Map<String, Float4Value> m_Float4s;
    private final Map<String, TextureReference> m_Textures;

    public MaterialDefinition(AssetKey<Shader> shader, RenderState renderState, Map<String, Float> floats, Map<String, Float4Value> float4s, Map<String, TextureReference> textures) {
        if (shader == null)
            throw new IllegalArgumentException("Material Shader reference cannot be null!");

        if (renderState == null)
            throw new IllegalArgumentException("Material Render State cannot be null!");

        if (floats == null)
            throw new IllegalArgumentException("Material float parameters cannot be null!");

        if (float4s == null)
            throw new IllegalArgumentException("Material float4 parameters cannot be null!");

        if (textures == null)
            throw new IllegalArgumentException("Material texture parameters cannot be null!");

        m_Shader = shader;
        m_RenderState = renderState;

        m_Floats = Collections.unmodifiableMap(new LinkedHashMap<>(floats));

        m_Float4s = Collections.unmodifiableMap(new LinkedHashMap<>(float4s));

        m_Textures = Collections.unmodifiableMap(new LinkedHashMap<>(textures));
    }

    public AssetKey<Shader> getShader() {
        return m_Shader;
    }

    public RenderState getRenderState() {
        return m_RenderState;
    }

    public Map<String, Float> getFloats() {
        return m_Floats;
    }

    public Map<String, Float4Value> getFloat4s() {
        return m_Float4s;
    }

    public Map<String, TextureReference> getTextures() {
        return m_Textures;
    }

    public record Float4Value(float x, float y, float z, float w) { }

    public record TextureReference(AssetKey<Texture2D> asset, int slot) {
        public TextureReference {
            if (asset == null) throw new IllegalArgumentException("Texture Asset reference cannot be null!");
            if (slot < 0) throw new IllegalArgumentException("Texture slot cannot be negative!");
        }
    }

}