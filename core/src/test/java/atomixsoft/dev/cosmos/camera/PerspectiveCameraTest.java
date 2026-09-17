package atomixsoft.dev.cosmos.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerspectiveCameraTest {

    @Test
    void aspectRatioChangesProjection() {
        final PerspectiveCamera camera = new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f);
        final float originalM00 = camera.getProjection().m00();

        camera.setAspectRatio(4.0f / 3.0f);
        assertNotEquals(originalM00, camera.getProjection().m00());
    }

    @Test
    void invalidClipPlanesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 1.0f, 0.5f));
    }

    @Test
    void resizeUpdatesAspectRatio() {
        final PerspectiveCamera camera = new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 100.0f);
        camera.resize(1920, 1080);
        assertEquals(1920.0f / 1080.0f, camera.getAspectRatio(), 0.00001f);
    }

    @Test
    void invalidViewportDimensionsAreRejected() {
        final PerspectiveCamera camera = new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 100.0f);
        assertThrows(IllegalArgumentException.class, () -> camera.resize(1280, 0));
    }

}