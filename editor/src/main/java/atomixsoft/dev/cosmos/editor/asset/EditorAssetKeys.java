package atomixsoft.dev.cosmos.editor.asset;

import atomixsoft.dev.cosmos.asset.AssetKey;
import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.render.Shader;
import atomixsoft.dev.cosmos.render.Texture2D;

public final class EditorAssetKeys {

    public static final AssetKey<Shader> BASIC_SHADER = AssetKey.of(Shader.class, "editor/shaders/basic");
    public static final AssetKey<Mesh> CUBE_MESH = AssetKey.of(Mesh.class, "editor/meshes/cube");
    public static final AssetKey<Texture2D> CHECKER_TEXTURE = AssetKey.of(Texture2D.class, "editor/textures/checkers");

    public static final AssetKey<Material> DEFAULT_MATERIAL = AssetKey.of(Material.class, "editor/materials/default");

    private EditorAssetKeys() {

    }

}
