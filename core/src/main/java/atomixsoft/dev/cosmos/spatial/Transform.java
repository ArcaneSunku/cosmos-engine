package atomixsoft.dev.cosmos.spatial;

import org.joml.*;

public final class Transform {

    private final Vector3f m_Position;
    private final Quaternionf m_Rotation;
    private final Vector3f m_Scale;

    public Transform() {
        m_Position = new Vector3f();
        m_Rotation = new Quaternionf();
        m_Scale = new Vector3f(1f);
    }

    public Transform translate(float x, float y, float z) {
        m_Position.add(x, y, z);
        return this;
    }

    public Transform translate(Vector3fc position) {
        if(position == null)
            throw new IllegalArgumentException("Position cannot be null!");

        m_Position.add(position);
        return this;
    }

    public Transform rotateEuler(float xRadians, float yRadians, float zRadians) {
        m_Rotation.rotateXYZ(xRadians, yRadians, zRadians);
        return this;
    }

    public Matrix4f applyTo(Matrix4f destination) {
        if(destination == null)
            throw new IllegalArgumentException("Destination cannot be null!");

        return destination
                .translate(m_Position)
                .rotate(m_Rotation)
                .scale(m_Scale);
    }

    public Transform setPosition(float x, float y, float z) {
        m_Position.set(x, y, z);
        return this;
    }

    public Transform setPosition(Vector3fc position) {
        if(position == null)
            throw new IllegalArgumentException("Position cannot be null!");

        m_Position.set(position);
        return this;
    }

    public Transform setRotation(Quaternionfc rotation) {
        if(rotation == null)
            throw new IllegalArgumentException("Rotation cannot be null!");

        m_Rotation.set(rotation);
        return this;
    }

    public Transform setRotationEuler(float xRadians, float yRadians, float zRadians) {
        m_Rotation.identity()
                .rotateXYZ(xRadians, yRadians, zRadians);

        return this;
    }

    public Transform setScale(float scale) {
        m_Scale.set(scale);
        return this;
    }

    public Transform setScale(float x, float y, float z) {
        m_Scale.set(x, y, z);
        return this;
    }

    public Transform setScale(Vector3fc scale) {
        if(scale == null)
            throw new IllegalArgumentException("Scale cannot be null!");

        m_Scale.set(scale);
        return this;
    }

    public Vector3f getForward(Vector3f destination) {
        if(destination == null)
            throw new IllegalArgumentException("Destination cannot be null!");

        destination.set(0.0f, 0.0f, -1.0f);
        m_Rotation.transform(destination);

        return destination;
    }

    public Vector3f getRight(Vector3f destination) {
        if(destination == null)
            throw new IllegalArgumentException("Destination cannot be null!");

        destination.set(1.0f, 0.0f, 0.0f);
        m_Rotation.transform(destination);

        return destination;
    }

    public Vector3f getUp(Vector3f destination) {
        if(destination == null)
            throw new IllegalArgumentException("Destination cannot be null!");

        destination.set(0.0f, 1.0f, 0.0f);
        m_Rotation.transform(destination);

        return destination;
    }

    public Vector3fc getPosition() {
        return m_Position;
    }

    public Quaternionfc getRotation() {
        return m_Rotation;
    }

    public Vector3fc getScale() {
        return  m_Scale;
    }

    public Matrix4f getMatrix(Matrix4f destination) {
        if(destination == null)
            throw new IllegalArgumentException("Destination Matrix cannot be null!");

        destination.identity();
        return applyTo(destination);
    }

    public Matrix4f getInverseMatrix(Matrix4f destination) {
        return getMatrix(destination).invert();
    }

}
