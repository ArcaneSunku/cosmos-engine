package atomixsoft.dev.cosmos.editor.history;

import atomixsoft.dev.cosmos.spatial.Transform;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public record TransformState(float positionX, float positionY, float positionZ,
                             float rotationX, float rotationY, float rotationZ, float rotationW,
                             float scaleX, float scaleY, float scaleZ) {

    private static final float EPSILON = 0.000001f;

    public static TransformState capture(Transform transform) {
        if (transform == null)
            throw new IllegalArgumentException("Transform cannot be null!");

        final Vector3fc position = transform.getPosition();
        final Quaternionfc rotation = transform.getRotation();
        final Vector3fc scale = transform.getScale();

        return new TransformState(position.x(), position.y(), position.z(),
                rotation.x(), rotation.y(), rotation.z(), rotation.w(),
                scale.x(), scale.y(), scale.z());
    }

    public static TransformState identity() {
        return new TransformState(0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 1.0f);
    }

    private static boolean approximatelyEqual(float a, float b) {
        return Math.abs(a - b) <= EPSILON;
    }

    public void apply(Transform transform) {
        if (transform == null)
            throw new IllegalArgumentException("Transform cannot be null!");

        transform.setPosition(positionX, positionY, positionZ).setRotation(new org.joml.Quaternionf(rotationX, rotationY, rotationZ, rotationW)).setScale(scaleX, scaleY, scaleZ);
    }

    public boolean approximatelyEquals(TransformState other) {
        if (other == null)
            return false;

        if (!approximatelyEqual(positionX, other.positionX) || !approximatelyEqual(positionY, other.positionY) || !approximatelyEqual(positionZ, other.positionZ))
            return false;

        if (!approximatelyEqual(scaleX, other.scaleX) || !approximatelyEqual(scaleY, other.scaleY) || !approximatelyEqual(scaleZ, other.scaleZ))
            return false;

        final float rotationDot = rotationX * other.rotationX + rotationY * other.rotationY + rotationZ * other.rotationZ + rotationW * other.rotationW;
        return Math.abs(1.0f - Math.abs(rotationDot)) <= EPSILON;
    }

}