package atomixsoft.dev.cosmos.asset;

import java.io.IOException;
import java.io.InputStream;

public final class ClassPathAssetSource implements AssetSource {

    private final Class<?> m_Anchor;

    public ClassPathAssetSource(Class<?> anchor) {
        if (anchor == null)
            throw new IllegalArgumentException("Classpath anchor cannot be null!");

        m_Anchor = anchor;
    }

    @Override
    public byte[] readBytes(String path) {
        final String normalizedPath = normalizePath(path);
        try (InputStream stream = m_Anchor.getResourceAsStream("/" + normalizedPath)) {
            if (stream == null)
                throw new AssetLoadException("Classpath Asset does not exist: " + normalizedPath);

            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new AssetLoadException("Failed to read Classpath Asset: " + normalizedPath, exception);
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("Asset path cannot be empty!");

        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/"))
            normalized = normalized.substring(1);

        if (normalized.isBlank())
            throw new IllegalArgumentException("Asset path cannot be empty!");

        return normalized;
    }

}