package atomixsoft.dev.cosmos.camera;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class PerspectiveCamera implements Camera{

    private final Matrix4f m_Projection;

    private float m_FOV, m_AspectRatio;
    private float m_Near, m_Far;

    public PerspectiveCamera(float fovRads, float aspectRatio, float near, float far) {
        m_Projection = new Matrix4f();
        setPerspective(fovRads, aspectRatio, near, far);
    }

    @Override
    public void resize(int width, int height) {
        validateDimensions(width, height);
        setAspectRatio((float) width / (float) height);
    }

    public PerspectiveCamera setPerspective(float fovRads, float aspectRatio, float near, float far) {
        validatePerspective(fovRads, aspectRatio, near, far);

        m_FOV = fovRads;
        m_AspectRatio = aspectRatio;
        m_Near = near;
        m_Far = far;

        rebuildProjection();
        return this;
    }

    public PerspectiveCamera setAspectRatio(float aspectRatio) {
        if(aspectRatio <= 0.0f)
            throw new IllegalArgumentException("Camera aspect ratio must be greater than zero!");

        if(Float.compare(m_AspectRatio, aspectRatio) == 0)
            return this;

        m_AspectRatio = aspectRatio;
        rebuildProjection();

        return this;
    }

    public PerspectiveCamera setClipPlanes(float near, float far) {
        validatePerspective(m_FOV, m_AspectRatio, near, far);

        m_Near = near;
        m_Far = far;

        rebuildProjection();
        return this;
    }

    @Override
    public Matrix4fc getProjection() {
        return m_Projection;
    }

    public float getFOV() {
        return m_FOV;
    }

    public float getAspectRatio() {
        return m_AspectRatio;
    }

    public float getNearPlane() {
        return m_Near;
    }

    public float getFarPlane() {
        return m_Far;
    }

    private void rebuildProjection() {
        m_Projection.identity()
                .perspective(m_FOV, m_AspectRatio, m_Near, m_Far);
    }

    private static void validateDimensions(int width, int height) {
        if(width <= 0)
            throw new IllegalArgumentException("Camera viewport width must be greater than zero!");

        if(height <= 0)
            throw new IllegalArgumentException("Camera viewport height must be greater than zero!");
    }

    private static void validatePerspective(float fovRads, float aspectRatio, float near, float far) {
        if(fovRads <= 0.0f || fovRads >= Math.PI)
            throw new IllegalArgumentException("Camera FOV must be between 0 and PI radians!");

        if(aspectRatio <= 0.0f)
            throw new IllegalArgumentException("Camera aspect ratio must be greater than zero!");

        if(near <= 0.0f)
            throw new IllegalArgumentException("Camera near plane must be greater than zero!");

        if(far <= 0.0f || far<= near)
            throw new IllegalArgumentException("Camera far plane must be greater than zero and the near plane!");
    }

}
