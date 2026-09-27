package atomixsoft.dev.cosmos.asset.loader;

import atomixsoft.dev.cosmos.asset.AssetLoadContext;
import atomixsoft.dev.cosmos.asset.AssetLoadException;
import atomixsoft.dev.cosmos.asset.AssetLoader;
import atomixsoft.dev.cosmos.asset.AssetSource;
import atomixsoft.dev.cosmos.render.Texture2D;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBImage.*;

public class TextureAssetLoader implements AssetLoader<Texture2D> {

    private static final Object s_DecodeLock = new Object();

    private final String m_Path;
    private final boolean m_FlipVertically;

    public TextureAssetLoader(String path, boolean flipVertically) {
        if(path == null || path.isBlank())
            throw new IllegalArgumentException("Texture Asset path cannot be empty!");

        m_Path = path;
        m_FlipVertically = flipVertically;
    }

    public TextureAssetLoader(String path) {
        this(path, true);
    }

    @Override
    public Texture2D load(AssetLoadContext context) {
        final AssetSource source = context.getSource();
        final byte[] encodedBytes = source.readBytes(m_Path);
        if(encodedBytes.length == 0)
            throw new AssetLoadException("Texture Asset is empty: " + m_Path);

        final ByteBuffer encoded = MemoryUtil.memAlloc(encodedBytes.length);
        ByteBuffer decoded = null;

        try {
            encoded.put(encodedBytes).flip();

            final int width, height;

            String failureReason = null;
            try(MemoryStack stack = MemoryStack.stackPush()) {
                final IntBuffer w = stack.mallocInt(1);
                final IntBuffer h = stack.mallocInt(1);
                final IntBuffer channels = stack.mallocInt(1);

                synchronized(s_DecodeLock) {
                    stbi_set_flip_vertically_on_load(m_FlipVertically);

                    try {
                        decoded = stbi_load_from_memory(encoded, w, h, channels, STBI_rgb_alpha);
                        if(decoded == null)
                            failureReason = stbi_failure_reason();
                    } finally {
                        stbi_set_flip_vertically_on_load(false);
                    }
                }

                if(decoded == null)
                    throw new AssetLoadException("Failed to decode Texture Asset " + m_Path + ": " + (failureReason == null ? "Unknown STB error" : failureReason));

                width = w.get(0);
                height = h.get(0);
            }

            final int pixelByteCount;

            try {
                pixelByteCount = Math.multiplyExact(Math.multiplyExact(width, height), 4);
            } catch (ArithmeticException e) {
                throw new AssetLoadException("Decoded Texture dimensions are too large: " + width + "x" + height, e);
            }

            final byte[] rgbaPixels = new byte[pixelByteCount];
            decoded.duplicate().get(rgbaPixels);

            final Texture2D texture = new Texture2D();
            try {
                texture.create(width, height, rgbaPixels);
                return texture;
            } catch (RuntimeException | Error e) {
                texture.dispose();
                throw new AssetLoadException("Failed to create GPU Texture for Asset: " + m_Path, e);
            }
        } finally {
            if(decoded != null)
                stbi_image_free(decoded);

            MemoryUtil.memFree(encoded);
        }
    }

    @Override
    public void unload(Texture2D texture) {
        texture.dispose();
    }
}
