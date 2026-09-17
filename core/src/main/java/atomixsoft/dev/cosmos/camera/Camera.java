package atomixsoft.dev.cosmos.camera;

import org.joml.Matrix4fc;

public interface Camera {

    Matrix4fc getProjection();
    void resize(int width, int height);

}
