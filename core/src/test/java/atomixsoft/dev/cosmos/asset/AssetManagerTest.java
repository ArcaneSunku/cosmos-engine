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
        assets.unload(TEST_ASSET);

        assertEquals(1, disposals.get());
        assertFalse(assets.contains(TEST_ASSET));
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
            return assetSource.readString("test.txt");
        };

        final String first = assets.load(key, source, loader);
        final String second = assets.load(key, source, loader);

        assertSame(first, second);
        assertEquals(1, loads.get());
    }

    private static final class TestAsset {
    }

}