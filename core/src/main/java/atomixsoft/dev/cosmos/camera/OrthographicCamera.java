package atomixsoft.dev.cosmos.camera;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class OrthographicCamera implements Camera {

    private final Matrix4f m_Projection;

    private float m_VerticalSize;
    private float m_AspectRatio;

    private float m_Near;
    private float m_Far;

    public OrthographicCamera(float verticalSize, float aspectRatio, float near, float far) {
        m_Projection = new Matrix4f();
        setOrthographic(verticalSize, aspectRatio, near, far);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0) throw new IllegalArgumentException("Camera viewport width must be greater than zero!");
        if (height <= 0) throw new IllegalArgumentException("Camera viewport height must be greater than zero!");

        setAspectRatio((float) width / (float) height);
    }

    public OrthographicCamera setOrthographic(float verticalSize, float aspectRatio, float near, float far) {
        validateOrthographic(verticalSize, aspectRatio, near, far);

        m_VerticalSize = verticalSize;
        m_AspectRatio = aspectRatio;

        m_Near = near;
        m_Far = far;

        rebuildProjection();
        return this;
    }

    public OrthographicCamera setClipPlanes(float near, float far) {
        if (far <= near) throw new IllegalArgumentException("Camera far plane must be greater than its near plane!");

        m_Near = near;
        m_Far = far;

        rebuildProjection();
        return this;
    }

    public OrthographicCamera setVerticalSize(float verticalSize) {
        if (verticalSize <= 0.0f)
            throw new IllegalArgumentException("Orthographic vertical size must be greater than zero!");

        if (Float.compare(m_VerticalSize, verticalSize) == 0)
            return this;

        m_VerticalSize = verticalSize;
        rebuildProjection();

        return this;
    }

    public OrthographicCamera setAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0.0f) throw new IllegalArgumentException("Camera aspect ratio must be greater than zero!");

        if (Float.compare(m_AspectRatio, aspectRatio) == 0)
            return this;

        m_AspectRatio = aspectRatio;
        rebuildProjection();

        return this;
    }

    @Override
    public Matrix4fc getProjection() {
        return m_Projection;
    }

    public float getVerticalSize() {
        return m_VerticalSize;
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
        final float halfHeight = m_VerticalSize * 0.5f;
        final float halfWidth = halfHeight * m_AspectRatio;

        m_Projection.identity().ortho(-halfWidth, halfWidth, -halfHeight, halfHeight, m_Near, m_Far);
    }

    private static void validateOrthographic(float verticalSize, float aspectRatio, float near, float far) {
        if (verticalSize <= 0.0f)
            throw new IllegalArgumentException("Orthographic vertical size must be greater than zero!");

        if (aspectRatio <= 0.0f) throw new IllegalArgumentException("Camera aspect ratio must be greater than zero!");

        if (far <= near) throw new IllegalArgumentException("Camera far plane must be greater than its near plane!");
    }

}