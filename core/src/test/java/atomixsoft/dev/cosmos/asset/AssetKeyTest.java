package atomixsoft.dev.cosmos.asset;

import atomixsoft.dev.cosmos.render.Shader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetKeyTest {

    @Test
    void equivalentKeysAreEqual() {
        final AssetKey<Shader> first = AssetKey.of(Shader.class, "editor/shaders/basic");
        final AssetKey<Shader> second = AssetKey.of(Shader.class, "editor/shaders/basic");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void backslashesAreNormalized() {
        final AssetKey<Shader> key = AssetKey.of(Shader.class, "editor\\shaders\\basic");
        assertEquals("editor/shaders/basic", key.getId());
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AssetKey.of(Shader.class, " "));
    }

}