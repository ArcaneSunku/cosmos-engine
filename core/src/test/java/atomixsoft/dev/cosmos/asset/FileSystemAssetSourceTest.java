package atomixsoft.dev.cosmos.asset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemAssetSourceTest {

    @TempDir
    Path m_TempDirectory;

    @Test
    void readsTextFromAssetRoot() throws Exception {
        final Path assets = m_TempDirectory.resolve("assets");

        Files.createDirectories(assets.resolve("shaders"));
        Files.writeString(assets.resolve("shaders/test.vert"), "test shader", StandardCharsets.UTF_8);

        final FileSystemAssetSource source = new FileSystemAssetSource(assets);
        assertEquals("test shader", source.readString("shaders/test.vert"));
    }

    @Test
    void parentTraversalIsRejected() {
        final FileSystemAssetSource source = new FileSystemAssetSource(m_TempDirectory);
        assertThrows(IllegalArgumentException.class, () -> source.readBytes("../outside.txt"));
    }

    @Test
    void missingAssetThrowsLoadException() {
        final FileSystemAssetSource source = new FileSystemAssetSource(m_TempDirectory);
        assertThrows(AssetLoadException.class, () -> source.readBytes("missing.txt"));
    }

}