package atomixsoft.dev.cosmos.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AssetManager {

    private final Map<String, ManagedAsset> m_Assets;

    public AssetManager() {
        m_Assets = new LinkedHashMap<>();
    }

    public <T> T register(AssetKey<T> key, T asset, Consumer<? super T> disposer) {
        validateKey(key);
        if(asset == null)
            throw new IllegalStateException("Asset cannot be null!");

        if(!key.getType().isInstance(asset))
            throw new IllegalArgumentException("Asset does not match Asset Key type: " + key.getType().getName());

        if(m_Assets.containsKey(key.getId()))
            throw new IllegalStateException("Asset is already registered: " + key.getId());

        final Consumer<Object> managedDisposer;

        if(disposer == null) managedDisposer = null;
        else managedDisposer = value -> disposer.accept(key.getType().cast(value));

        m_Assets.put(key.getId(), new ManagedAsset(key.getType(), asset, managedDisposer));

        return asset;
    }

    public <T> T load(AssetKey<T> key, AssetSource source, AssetLoader<T> loader) {
        validateKey(key);
        if(source == null)
            throw new IllegalArgumentException("Asset Source cannot be null!");

        if(loader == null)
            throw new IllegalArgumentException("Asset Loader cannot be null!");

        final ManagedAsset existing = m_Assets.get(key.getId());
        if(existing != null)
            return getExisting(key, existing);

        final T asset;

        try {
            asset = loader.load(source);
        } catch (AssetLoadException e) {
            throw e;
        } catch (RuntimeException | Error e) {
            throw new AssetLoadException("Failed to load asset: " + key.getId(), e);
        }

        if(asset == null)
            throw new AssetLoadException("Asset Loader returned null for: " + key.getId());

        try {
            return register(key, asset, loader::unload);
        } catch (RuntimeException | Error e) {
            try {
                loader.unload(asset);
            } catch (RuntimeException | Error cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }

            throw e;
        }
    }

    public void unload(AssetKey<?> key) {
        validateKey(key);

        final ManagedAsset managed = m_Assets.remove(key.getId());
        if(managed == null)
            return;

        disposeAsset(key.getId(), managed);
    }

    public void clear() {
        if(m_Assets.isEmpty()) return;

        final List<Map.Entry<String, ManagedAsset>> assets = new ArrayList<>(m_Assets.entrySet());
        m_Assets.clear();

        Throwable failure = null;
        for(int i = assets.size() - 1; i >= 0; i--) {
            final Map.Entry<String, ManagedAsset> entry = assets.get(i);
            try {
                disposeAsset(entry.getKey(), entry.getValue());
            } catch(RuntimeException | Error e) {
                if(failure == null)
                    failure = e;
                else
                    failure.addSuppressed(e);
            }
        }

        if(failure == null)
            return;

        if(failure instanceof RuntimeException exception)
            throw exception;

        throw (Error) failure;
    }

    public <T> T getOrCreate(AssetKey<T> key, Supplier<? extends T> factory, Consumer<? super T> disposer) {
        validateKey(key);
        if(factory == null)
            throw new IllegalArgumentException("Asset Factory cannot be null!");

        final ManagedAsset existing = m_Assets.get(key.getId());
        if(existing != null)
            return getExisting(key, existing);

        final T asset = factory.get();
        if(asset == null)
            throw new IllegalStateException("Asset factory returned null for: " + key.getId());

        return register(key, asset, disposer);
    }

    public <T> T get(AssetKey<T> key) {
        validateKey(key);

        final ManagedAsset managed = m_Assets.get(key.getId());
        if(managed == null)
            throw new IllegalStateException("Asset is not loaded: " + key.getId());

        return getExisting(key, managed);
    }

    public boolean contains(AssetKey<?> key) {
        validateKey(key);
        return m_Assets.containsKey(key.getId());
    }

    public int getAssetCount() {
        return m_Assets.size();
    }

    private <T> T getExisting(AssetKey<T> key, ManagedAsset managed) {
        if(!managed.type().equals(key.getType()))
            throw new IllegalStateException("Asset " + key.getId() + "is registered as " + managed.type().getName() + ", not " + key.getType().getName() + "!");

        return key.getType().cast(managed.asset());
    }

    private static void disposeAsset(String id, ManagedAsset managed) {
        if(managed.disposer() == null)
            return;

        try {
            managed.disposer().accept(managed.asset());
        } catch(RuntimeException | Error e) {
            throw new IllegalStateException("Failed to dispose Asset: " + id, e);
        }
    }

    private static void validateKey(AssetKey<?> key) {
        if(key == null)
            throw new IllegalArgumentException("Asset Key cannot be null!");
    }

    private record ManagedAsset(Class<?> type, Object asset, Consumer<Object> disposer) {}

}
