package atomixsoft.dev.cosmos.render;

import atomixsoft.dev.cosmos.camera.PerspectiveCamera;
import atomixsoft.dev.cosmos.scene.Scene;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SceneRendererTest {

    private static PerspectiveCamera createCamera() {
        return new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f);
    }

    @Test
    void nullGraphicsIsRejected() {
        final SceneRenderer renderer = new SceneRenderer();
        assertThrows(IllegalArgumentException.class, () -> renderer.render(null, new Scene("Test"), createCamera(), new Matrix4f()));
    }

    @Test
    void nullSceneIsRejected() {
        final SceneRenderer renderer = new SceneRenderer();
        assertThrows(IllegalArgumentException.class, () -> renderer.render(new Graphics(), null, createCamera(), new Matrix4f()));
    }

    @Test
    void nullCameraIsRejected() {
        final SceneRenderer renderer = new SceneRenderer();
        assertThrows(IllegalArgumentException.class, () -> renderer.render(new Graphics(), new Scene("Test"), null, new Matrix4f()));
    }

    @Test
    void nullViewMatrixIsRejected() {
        final SceneRenderer renderer = new SceneRenderer();
        assertThrows(IllegalArgumentException.class, () -> renderer.render(new Graphics(), new Scene("Test"), createCamera(), null));
    }

}