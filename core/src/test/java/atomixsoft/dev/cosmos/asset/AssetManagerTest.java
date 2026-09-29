package atomixsoft.dev.cosmos.asset;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AssetManagerTest {

    private static final AssetKey<TestAsset> TEST_ASSET = AssetKey.of(TestAsset.class, "test/asset");

    @Test
    void registeredAssetCanBeRetrieved() {
        final AssetManager assets = new AssetManager();
        final TestAsset asset = new TestAsset();

        assets.register(TEST_ASSET, asset, null);
        assertSame(asset, assets.get(TEST_ASSET));
    }

    @Test
    void getOrCreateCachesAsset() {
        final AssetManager assets = new AssetManager();
        final AtomicInteger creations = new AtomicInteger();

        final TestAsset first = assets.getOrCreate(TEST_ASSET, () -> {
            creations.incrementAndGet();
            return new TestAsset();
        }, null);

        final TestAsset second = assets.getOrCreate(TEST_ASSET, () -> {
            creations.incrementAndGet();
            return new TestAsset();
        }, null);

        assertSame(first, second);
        assertEquals(1, creations.get());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        final AssetManager assets = new AssetManager();

        assets.register(TEST_ASSET, new TestAsset(), null);
        assertThrows(IllegalStateException.class, () -> assets.register(TEST_ASSET, new TestAsset(), null));
    }

    @Test
    void unloadDisposesAsset() {
        final AssetManager assets = new AssetManager();
        final AtomicInteger disposals = new AtomicInteger();

        assets.register(TEST_ASSET, new TestAsset(), asset -> disposals.incrementAndGet());
        assets.unloadAll();

        assertEquals(1, disposals.get());
        assertFalse(assets.isLoaded(TEST_ASSET));
    }

    @Test
    void clearDisposesInReverseRegistrationOrder() {
        final AssetManager assets = new AssetManager();
        final AssetKey<TestAsset> firstKey = AssetKey.of(TestAsset.class, "test/first");
        final AssetKey<TestAsset> secondKey = AssetKey.of(TestAsset.class, "test/second");
        final List<String> disposed = new ArrayList<>();

        assets.register(firstKey, new TestAsset(), asset -> disposed.add("first"));
        assets.register(secondKey, new TestAsset(), asset -> disposed.add("second"));
        assets.clear();

        assertEquals(List.of("second", "first"), disposed);
        assertEquals(0, assets.getAssetCount());
    }

    @Test
    void loadCachesLoaderResult() {
        final AssetManager assets = new AssetManager();
        final AssetKey<String> key = AssetKey.of(String.class, "test/text");
        final AtomicInteger loads = new AtomicInteger();
        final AssetSource source = _ -> "hello".getBytes(StandardCharsets.UTF_8);
        final AssetLoader<String> loader = assetSource -> {
            loads.incrementAndGet();
            return assetSource.getSource().readString("test.txt");
        };

        final String first = assets.load(key, source, loader);
        final String second = assets.load(key, source, loader);

        assertSame(first, second);
        assertEquals(1, loads.get());
    }

    @Test
    void managedAssetKeyCanBeRecovered() {
        final AssetManager assets = new AssetManager();
        final TestAsset asset = new TestAsset();

        assets.register(TEST_ASSET, asset, null);

        final AssetKey<TestAsset> key = assets.getKey(TestAsset.class, asset);
        assertEquals(TEST_ASSET, key);
    }

    @Test
    void assetInstanceCannotHaveMultipleKeys() {
        final AssetManager assets = new AssetManager();
        final TestAsset asset = new TestAsset();
        final AssetKey<TestAsset> otherKey = AssetKey.of(TestAsset.class, "test/other");

        assets.register(TEST_ASSET, asset, null);

        assertThrows(IllegalStateException.class, () -> assets.register(otherKey, asset, null));
    }

    @Test
    void unloadedAssetNoLongerHasManagedKey() {
        final AssetManager assets = new AssetManager();
        final TestAsset asset = new TestAsset();

        assets.register(TEST_ASSET, asset, null);
        assets.unloadAll();

        assertThrows(IllegalStateException.class, () -> assets.getKey(TestAsset.class, asset));
    }

    @Test
    void clearRemovesManagedAssetIdentity() {
        final AssetManager assets = new AssetManager();
        final TestAsset asset = new TestAsset();

        assets.register(TEST_ASSET, asset, null);
        assets.clear();

        assertThrows(IllegalStateException.class, () -> assets.getKey(TestAsset.class, asset));
    }

    @Test
    void successfulDependencyRemainsLoadedWhenParentFails() {
        final AssetManager assets = new AssetManager();
        final AssetKey<TestAsset> dependencyKey = AssetKey.of(TestAsset.class, "test/dependency");
        final AssetKey<TestAsset> parentKey = AssetKey.of(TestAsset.class, "test/parent");

        assets.getCatalog().registerGenerated(dependencyKey, manager -> new TestAsset(), null);
        assets.getCatalog().registerGenerated(parentKey, manager -> {
            manager.load(dependencyKey);
            throw new AssetLoadException("Expected failure");
        }, null);

        assertThrows(AssetLoadException.class, () -> assets.load(parentKey));
        assertTrue(assets.isLoaded(dependencyKey));
        assertFalse(assets.isLoaded(parentKey));
    }

    @Test
    void unloadAllKeepsCatalogEntries() {
        final AssetManager assets = new AssetManager();
        final AtomicInteger creations = new AtomicInteger();

        assets.getCatalog().registerGenerated(TEST_ASSET, manager -> {
            creations.incrementAndGet();

            return new TestAsset();
        }, null);

        final TestAsset first = assets.load(TEST_ASSET);
        assets.unloadAll();

        assertFalse(assets.isLoaded(TEST_ASSET));
        assertTrue(assets.getCatalog().contains(TEST_ASSET));

        final TestAsset second = assets.load(TEST_ASSET);

        assertNotSame(first, second);
        assertEquals(2, creations.get());
    }

    @Test
    void clearRemovesLoadedAssetsAndCatalog() {
        final AssetManager assets = new AssetManager();

        assets.getCatalog().registerGenerated(TEST_ASSET, manager -> new TestAsset(), null);
        assets.load(TEST_ASSET);
        assets.clear();

        assertFalse(assets.isLoaded(TEST_ASSET));
        assertFalse(assets.getCatalog().contains(TEST_ASSET));
    }

    @Test
    void unloadAllContinuesAfterDisposalFailure() {
        final AssetManager assets = new AssetManager();
        final List<String> disposed = new ArrayList<>();

        final AssetKey<TestAsset> firstKey = AssetKey.of(TestAsset.class, "test/first");
        final AssetKey<TestAsset> secondKey = AssetKey.of(TestAsset.class, "test/second");

        assets.register(firstKey, new TestAsset(), asset -> {
            disposed.add("first");
            throw new IllegalStateException("first failure");
        });

        assets.register(secondKey, new TestAsset(), asset -> {
            disposed.add("second");
            throw new IllegalStateException("second failure");
        });

        assertThrows(IllegalStateException.class, assets::unloadAll);
        assertEquals(List.of("second", "first"), disposed);
        assertEquals(0, assets.getAssetCount());
    }

    private static final class TestAsset {
    }

}