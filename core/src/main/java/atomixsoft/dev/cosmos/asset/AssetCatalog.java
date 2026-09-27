package atomixsoft.dev.cosmos.asset;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AssetCatalog {

    private final Map<String, Entry<?>> m_Entries;

    public AssetCatalog() {
        m_Entries = new LinkedHashMap<>();
    }

    public <T> AssetCatalog register(AssetKey<T> key, AssetSource source, AssetLoader<T> loader) {
        validateKey(key);

        if (source == null)
            throw new IllegalArgumentException("Asset Source cannot be null!");

        if (loader == null)
            throw new IllegalArgumentException("Asset Loader cannot be null!");

        return registerEntry(key, assets -> loader.load(new AssetLoadContext(assets, source)), loader::unload);
    }

    public <T> AssetCatalog registerGenerated(AssetKey<T> key, Function<AssetManager, ? extends T> factory, Consumer<? super T> disposer) {
        validateKey(key);

        if (factory == null)
            throw new IllegalArgumentException("Generated Asset factory cannot be null!");

        return registerEntry(key, factory, disposer);
    }

    public boolean contains(AssetKey<?> key) {
        validateKey(key);

        final Entry<?> entry = m_Entries.get(key.getId());
        if (entry == null)
            return false;

        return entry.key().getType().equals(key.getType());
    }

    public int getEntryCount() {
        return m_Entries.size();
    }

    public void clear() {
        m_Entries.clear();
    }

    @SuppressWarnings("unchecked")
    <T> Entry<T> resolve(AssetKey<T> key) {
        validateKey(key);

        final Entry<?> entry = m_Entries.get(key.getId());
        if (entry == null)
            throw new AssetLoadException("Asset is not registered in the Catalog: " + key.getId());

        if (!entry.key().getType().equals(key.getType()))
            throw new AssetLoadException("Catalog Asset " + key.getId() + " is registered as " + entry.key().getType().getName() + ", not " + key.getType().getName() + "!");

        return (Entry<T>) entry;
    }

    private <T> AssetCatalog registerEntry(AssetKey<T> key, Function<AssetManager, ? extends T> factory, Consumer<? super T> disposer) {
        final Entry<?> existing = m_Entries.get(key.getId());
        if (existing != null)
            throw new IllegalStateException("Asset Catalog already contains: " + key.getId());

        m_Entries.put(key.getId(), new Entry<>(key, factory, disposer));
        return this;
    }

    private static void validateKey(AssetKey<?> key) {
        if (key == null)
            throw new IllegalArgumentException("Asset Key cannot be null!");
    }

    static final class Entry<T> {

        private final AssetKey<T> m_Key;
        private final Function<AssetManager, ? extends T> m_Factory;
        private final Consumer<? super T> m_Disposer;

        private Entry(AssetKey<T> key, Function<AssetManager, ? extends T> factory, Consumer<? super T> disposer) {
            m_Key = key;
            m_Factory = factory;
            m_Disposer = disposer;
        }

        AssetKey<T> key() {
            return m_Key;
        }

        T create(AssetManager assets) {
            return m_Factory.apply(assets);
        }

        void dispose(T asset) {
            if (m_Disposer != null) m_Disposer.accept(asset);
        }

    }

}