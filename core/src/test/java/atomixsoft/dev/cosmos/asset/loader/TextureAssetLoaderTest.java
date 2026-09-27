package atomixsoft.dev.cosmos.asset.loader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextureAssetLoaderTest {

    @Test
    void nullPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetLoader(null));
    }

    @Test
    void blankPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TextureAssetLoader(" "));
    }

    @Test
    void validPathIsAccepted() {
        assertDoesNotThrow(() -> new TextureAssetLoader("textures/test.png"));
    }

}