package atomixsoft.dev.cosmos.asset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileSystemAssetSource implements AssetSource {

    private final Path m_Root;

    public FileSystemAssetSource(Path root) {
        if (root == null)
            throw new IllegalArgumentException("Filesystem Asset root cannot be null!");

        m_Root = root.toAbsolutePath().normalize();
    }

    @Override
    public byte[] readBytes(String path) {
        final Path resolved = resolve(path);
        if (!Files.isRegularFile(resolved))
            throw new AssetLoadException("Filesystem Asset does not exist: " + path);

        try {
            return Files.readAllBytes(resolved);
        } catch (IOException exception) {
            throw new AssetLoadException("Failed to read Filesystem Asset: " + path, exception);
        }
    }

    public Path getRoot() {
        return m_Root;
    }

    private Path resolve(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("Asset path cannot be empty!");

        final String normalized = path.trim().replace('\\', '/');
        final Path relative = Path.of(normalized).normalize();

        if (relative.isAbsolute())
            throw new IllegalArgumentException("Asset path must be relative to the Asset root!");

        final Path resolved = m_Root.resolve(relative).normalize();
        if (!resolved.startsWith(m_Root))
            throw new IllegalArgumentException("Asset path cannot escape the Asset root!");

        return resolved;
    }

}