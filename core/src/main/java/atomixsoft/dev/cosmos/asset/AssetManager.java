package atomixsoft.dev.cosmos.asset;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AssetManager {

    private final Map<String, ManagedAsset> m_Assets;
    private final Map<Object, AssetKey<?>> m_AssetKeys;
    private final Deque<String> m_LoadStack;

    private final AssetCatalog m_Catalog;

    public AssetManager() {
        m_Assets = new LinkedHashMap<>();
        m_AssetKeys = new IdentityHashMap<>();
        m_LoadStack = new ArrayDeque<>();

        m_Catalog = new AssetCatalog();
    }

    public <T> T register(AssetKey<T> key, T asset, Consumer<? super T> disposer) {
        validateKey(key);
        if(asset == null)
            throw new IllegalStateException("Asset cannot be null!");

        if(!key.getType().isInstance(asset))
            throw new IllegalArgumentException("Asset does not match Asset Key type: " + key.getType().getName());

        if(m_Assets.containsKey(key.getId()))
            throw new IllegalStateException("Asset is already registered: " + key.getId());

        final AssetKey<?> existingKey = m_AssetKeys.get(asset);
        if(existingKey != null)
            throw new IllegalStateException("Asset instance is already registered as: " + existingKey.getId());

        final Consumer<Object> managedDisposer;

        if(disposer == null) managedDisposer = null;
        else managedDisposer = value -> disposer.accept(key.getType().cast(value));

        m_Assets.put(key.getId(), new ManagedAsset(key.getType(), asset, managedDisposer));
        m_AssetKeys.put(asset, key);

        return asset;
    }

    public <T> T load(AssetKey<T> key) {
        validateKey(key);

        final ManagedAsset existing = m_Assets.get(key.getId());
        if (existing != null)
            return getExisting(key, existing);

        final AssetCatalog.Entry<T> entry = m_Catalog.resolve(key);
        return loadManaged(key, () -> entry.create(this), entry::dispose);
    }

    public <T> T load(AssetKey<T> key, AssetSource source, AssetLoader<T> loader) {
        validateKey(key);

        if (source == null)
            throw new IllegalArgumentException("Asset Source cannot be null!");

        if (loader == null)
            throw new IllegalArgumentException("Asset Loader cannot be null!");

        return loadManaged(key, () -> loader.load(new AssetLoadContext(this, source)), loader::unload);
    }

    public void unload(AssetKey<?> key) {
        validateKey(key);

        final ManagedAsset managed = m_Assets.remove(key.getId());
        if(managed == null)
            return;

        m_AssetKeys.remove(managed.asset());
        disposeAsset(key.getId(), managed);
    }

    public void clear() {
        if(m_Assets.isEmpty()) return;

        final List<Map.Entry<String, ManagedAsset>> assets = new ArrayList<>(m_Assets.entrySet());

        m_Assets.clear();
        m_AssetKeys.clear();

        m_Catalog.clear();
        m_LoadStack.clear();

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

        if (factory == null)
            throw new IllegalArgumentException("Asset Factory cannot be null!");

        return loadManaged(key, factory, disposer);
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

    public <T> AssetKey<T> getKey(Class<T> type, T asset) {
        if(type == null)
            throw new IllegalArgumentException("Asset Type cannot be null!");

        if(asset == null)
            throw new IllegalArgumentException("Asset cannot be null!");

        final AssetKey<?> key = m_AssetKeys.get(asset);
        if(key == null)
            throw new IllegalStateException("Asset is not managed by this Asset Manager!");

        if(!key.getType().equals(type))
            throw new IllegalStateException("Asset is registered as " + key.getType().getName() + ", not " + type.getName() + "!");

        return AssetKey.of(type, key.getId());
    }

    public int getAssetCount() {
        return m_Assets.size();
    }

    public AssetCatalog getCatalog() {
        return m_Catalog;
    }

    private void beginLoad(AssetKey<?> key) {
        final String id = key.getId();
        if (m_LoadStack.contains(id)) {
            final StringBuilder chain = new StringBuilder();
            for (String loading : m_LoadStack) {
                if (!chain.isEmpty())
                    chain.append(" -> ");

                chain.append(loading);
            }

            if (!chain.isEmpty())
                chain.append(" -> ");

            chain.append(id);
            throw new AssetLoadException("Circular Asset dependency detected: " + chain);
        }

        m_LoadStack.addLast(id);
    }

    private void endLoad() {
        m_LoadStack.removeLast();
    }

    private <T> T loadManaged(AssetKey<T> key, Supplier<? extends T> factory, Consumer<? super T> disposer) {
        final ManagedAsset existing = m_Assets.get(key.getId());
        if (existing != null)
            return getExisting(key, existing);

        beginLoad(key);

        try {
            final T asset;
            try {
                asset = factory.get();
            } catch (AssetLoadException e) {
                throw e;
            } catch (RuntimeException | Error e) {
                throw new AssetLoadException("Failed to load Asset: " + key.getId(), e);
            }

            if (asset == null)
                throw new AssetLoadException("Asset creation returned null for: " + key.getId());

            final boolean alreadyManaged = m_AssetKeys.containsKey(asset);
            try {
                return register(key, asset, disposer);
            } catch (RuntimeException | Error e) {
                if (!alreadyManaged && disposer != null) {
                    try {
                        disposer.accept(asset);
                    } catch (RuntimeException | Error cleanupFailure) {
                        e.addSuppressed(cleanupFailure);
                    }
                }

                throw e;
            }
        } finally {
            endLoad();
        }
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
