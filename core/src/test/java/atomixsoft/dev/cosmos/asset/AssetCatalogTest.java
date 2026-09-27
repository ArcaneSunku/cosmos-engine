package atomixsoft.dev.cosmos.asset;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AssetCatalogTest {

    private static final AssetKey<TestAsset> TEST_ASSET = AssetKey.of(TestAsset.class, "test/asset");

    @Test
    void generatedAssetLoadsThroughCatalog() {
        final AssetManager assets = new AssetManager();
        assets.getCatalog().registerGenerated(TEST_ASSET, manager -> new TestAsset(), null);

        final TestAsset asset = assets.load(TEST_ASSET);

        assertNotNull(asset);
        assertSame(asset, assets.get(TEST_ASSET));
    }

    @Test
    void catalogAssetIsCached() {
        final AssetManager assets = new AssetManager();
        final AtomicInteger creations = new AtomicInteger();

        assets.getCatalog().registerGenerated(TEST_ASSET, manager -> {
            creations.incrementAndGet();

            return new TestAsset();
        }, null);

        final TestAsset first = assets.load(TEST_ASSET);
        final TestAsset second = assets.load(TEST_ASSET);

        assertSame(first, second);
        assertEquals(1, creations.get());
    }

    @Test
    void missingCatalogEntryIsRejected() {
        final AssetManager assets = new AssetManager();
        assertThrows(AssetLoadException.class, () -> assets.load(TEST_ASSET));
    }

    @Test
    void duplicateCatalogRegistrationIsRejected() {
        final AssetManager assets = new AssetManager();
        assets.getCatalog().registerGenerated(TEST_ASSET, manager -> new TestAsset(), null);

        assertThrows(IllegalStateException.class, () -> assets.getCatalog().registerGenerated(TEST_ASSET, manager -> new TestAsset(), null));
    }

    @Test
    void generatedAssetCanAutomaticallyLoadDependency() {
        final AssetManager assets = new AssetManager();
        final AssetKey<TestAsset> dependencyKey = AssetKey.of(TestAsset.class, "test/dependency");
        final AssetKey<DependentAsset> dependentKey = AssetKey.of(DependentAsset.class, "test/dependent");

        assets.getCatalog().registerGenerated(dependencyKey, manager -> new TestAsset(), null);
        assets.getCatalog().registerGenerated(dependentKey, manager -> new DependentAsset(manager.load(dependencyKey)), null);

        final DependentAsset dependent = assets.load(dependentKey);

        assertSame(assets.get(dependencyKey), dependent.dependency());
    }

    @Test
    void circularDependenciesAreRejected() {
        final AssetManager assets = new AssetManager();
        final AssetKey<TestAsset> firstKey = AssetKey.of(TestAsset.class, "test/first");
        final AssetKey<TestAsset> secondKey = AssetKey.of(TestAsset.class, "test/second");

        assets.getCatalog().registerGenerated(firstKey, manager -> {
            manager.load(secondKey);
            return new TestAsset();
        }, null);

        assets.getCatalog().registerGenerated(secondKey, manager -> {
            manager.load(firstKey);
            return new TestAsset();
        }, null);

        final AssetLoadException exception = assertThrows(AssetLoadException.class, () -> assets.load(firstKey));

        assertTrue(exception.getMessage().contains("test/first -> test/second -> test/first"));
    }

    private static final class TestAsset { }

    private record DependentAsset(TestAsset dependency) { }

}