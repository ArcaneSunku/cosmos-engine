package atomixsoft.dev.cosmos.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class Texture2DTest {

    @Test
    void zeroWidthIsRejected() {
        final Texture2D texture = new Texture2D();
        assertThrows(IllegalArgumentException.class, () -> texture.create(0, 1, new byte[4]));
    }

    @Test
    void zeroHeightIsRejected() {
        final Texture2D texture = new Texture2D();
        assertThrows(IllegalArgumentException.class, () -> texture.create(1, 0, new byte[4]));
    }

    @Test
    void nullPixelDataIsRejected() {
        final Texture2D texture = new Texture2D();
        assertThrows(IllegalArgumentException.class, () -> texture.create(1, 1, null));
    }

    @Test
    void incorrectRgbaPixelCountIsRejected() {
        final Texture2D texture = new Texture2D();
        assertThrows(IllegalArgumentException.class, () -> texture.create(2, 2, new byte[12]));
    }

}