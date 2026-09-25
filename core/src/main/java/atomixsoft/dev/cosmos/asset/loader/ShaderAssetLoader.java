package atomixsoft.dev.cosmos.asset.loader;

import atomixsoft.dev.cosmos.asset.AssetLoadException;
import atomixsoft.dev.cosmos.asset.AssetLoader;
import atomixsoft.dev.cosmos.asset.AssetSource;
import atomixsoft.dev.cosmos.render.Shader;

public final class ShaderAssetLoader implements AssetLoader<Shader> {

    private final String m_VertexPath;
    private final String m_FragmentPath;

    public ShaderAssetLoader(String vertexPath, String fragmentPath) {
        validatePath(vertexPath, "Vertex Shader");
        validatePath(fragmentPath, "Fragment Shader");

        m_VertexPath = vertexPath;
        m_FragmentPath = fragmentPath;
    }

    @Override
    public Shader load(AssetSource source) {
        final String vertexSource = source.readString(m_VertexPath);
        final String fragmentSource = source.readString(m_FragmentPath);
        final Shader shader = new Shader();

        try {
            shader.initialize(vertexSource, fragmentSource);
            return shader;
        } catch (RuntimeException | Error exception) {
            shader.dispose();
            throw new AssetLoadException("Failed to create Shader from " + m_VertexPath + " and " + m_FragmentPath, exception);
        }
    }

    @Override
    public void unload(Shader shader) {
        shader.dispose();
    }

    private static void validatePath(String path, String name) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException(name + " path cannot be empty!");
    }

}