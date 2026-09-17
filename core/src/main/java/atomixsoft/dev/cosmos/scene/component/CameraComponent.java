package atomixsoft.dev.cosmos.scene.component;

import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.scene.Component;

public final class CameraComponent implements Component {

    private Camera m_Camera;

    public CameraComponent(Camera camera) {
        setCamera(camera);
    }

    public CameraComponent setCamera(Camera camera) {
        if (camera == null)
            throw new IllegalArgumentException("Camera cannot be null!");

        m_Camera = camera;
        return this;
    }

    public Camera getCamera() {
        return m_Camera;
    }

}