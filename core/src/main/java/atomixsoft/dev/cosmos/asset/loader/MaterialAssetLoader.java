package atomixsoft.dev.cosmos.asset.loader;

import atomixsoft.dev.cosmos.asset.AssetLoadContext;
import atomixsoft.dev.cosmos.asset.AssetLoadException;
import atomixsoft.dev.cosmos.asset.AssetLoader;
import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.asset.definition.MaterialDefinition;
import atomixsoft.dev.cosmos.asset.definition.MaterialDefinitionParser;
import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Shader;
import atomixsoft.dev.cosmos.render.Texture2D;

import java.util.Map;

public final class MaterialAssetLoader implements AssetLoader<Material> {

    private final String m_Path;

    public MaterialAssetLoader(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("Material Asset path cannot be empty!");

        m_Path = path;
    }

    @Override
    public Material load(AssetLoadContext context) {
        final MaterialDefinition definition = MaterialDefinitionParser.parse(context.getSource().readString(m_Path));
        final AssetManager assets = context.getAssets();
        final Shader shader;

        try {
            shader = assets.load(definition.getShader());
        } catch (IllegalStateException e) {
            throw new AssetLoadException("Material " + m_Path + " requires unloaded Shader Asset: " + definition.getShader().getId(), e);
        }

        final Material material = new Material(shader).setRenderState(definition.getRenderState());
        for (Map.Entry<String, Float> entry : definition.getFloats().entrySet())
            material.setFloat(entry.getKey(), entry.getValue());

        for (Map.Entry<String, MaterialDefinition.Float4Value> entry : definition.getFloat4s().entrySet()) {
            final MaterialDefinition.Float4Value value = entry.getValue();
            material.setFloat4(entry.getKey(), value.x(), value.y(), value.z(), value.w());
        }

        for (Map.Entry<String, MaterialDefinition.TextureReference> entry : definition.getTextures().entrySet()) {
            final MaterialDefinition.TextureReference reference = entry.getValue();

            final Texture2D texture;
            try {
                texture = assets.load(reference.asset());
            } catch (IllegalStateException e) {
                throw new AssetLoadException("Material " + m_Path + " requires unloaded Texture Asset: " + reference.asset().getId(), e);
            }

            material.setTexture(entry.getKey(), texture, reference.slot());
        }

        return material;
    }

}