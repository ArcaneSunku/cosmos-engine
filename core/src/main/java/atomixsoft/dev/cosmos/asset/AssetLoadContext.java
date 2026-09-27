package atomixsoft.dev.cosmos.asset;

public class AssetLoadContext {

    private final AssetManager m_Assets;
    private final AssetSource m_Source;

    AssetLoadContext(AssetManager assets, AssetSource source) {
        if(assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        if(source == null)
            throw new IllegalArgumentException("Asset Source cannot be null!");

        m_Assets = assets;
        m_Source = source;
    }

    public AssetManager getAssets() {
        return m_Assets;
    }

    public AssetSource getSource() {
        return m_Source;
    }

}
