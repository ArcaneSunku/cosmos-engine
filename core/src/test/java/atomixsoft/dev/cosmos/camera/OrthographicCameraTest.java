package atomixsoft.dev.cosmos.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrthographicCameraTest {

    private static final float EPSILON = 0.00001f;

    @Test
    void resizeUpdatesAspectRatio() {
        final OrthographicCamera camera = new OrthographicCamera(10.0f, 1.0f, -100.0f, 100.0f);

        camera.resize(1920, 1080);
        assertEquals(1920.0f / 1080.0f, camera.getAspectRatio(), EPSILON);
    }

    @Test
    void resizeChangesHorizontalProjectionOnly() {
        final OrthographicCamera camera = new OrthographicCamera(10.0f, 1.0f, -100.0f, 100.0f);
        final float originalHorizontalScale = camera.getProjection().m00();
        final float originalVerticalScale = camera.getProjection().m11();

        camera.setAspectRatio(2.0f);

        assertNotEquals(originalHorizontalScale, camera.getProjection().m00());
        assertEquals(originalVerticalScale, camera.getProjection().m11(), EPSILON);
    }

    @Test
    void invalidVerticalSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrthographicCamera(0.0f, 1.0f, -1.0f, 1.0f));
    }

    @Test
    void invalidClipPlanesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrthographicCamera(10.0f, 1.0f, 10.0f, 5.0f));
    }

}