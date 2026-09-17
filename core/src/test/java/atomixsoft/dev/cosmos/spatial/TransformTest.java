package atomixsoft.dev.cosmos.spatial;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformTest {

    private static final float EPSILON = 0.00001f;

    @Test
    void modelMatrixAppliesScaleAndTranslation() {
        final Transform transform = new Transform().setPosition(2.0f, 3.0f, 4.0f).setScale(2.0f);
        final Vector3f point = new Vector3f(1.0f, 1.0f, 1.0f);

        transform.getMatrix(new Matrix4f()).transformPosition(point);

        assertEquals(4.0f, point.x, EPSILON);
        assertEquals(5.0f, point.y, EPSILON);
        assertEquals(6.0f, point.z, EPSILON);
    }

    @Test
    void inverseMatrixReversesTransform() {
        final Transform transform = new Transform().setPosition(2.0f, -1.0f, 3.0f).setRotationEuler(0.25f, 0.5f, 0.1f).setScale(1.5f);

        final Matrix4f model = transform.getMatrix(new Matrix4f());
        final Matrix4f inverse = transform.getInverseMatrix(new Matrix4f());

        final Vector3f original = new Vector3f(0.25f, -0.5f, 1.0f);
        final Vector3f transformed = new Vector3f(original);

        model.transformPosition(transformed);
        inverse.transformPosition(transformed);

        assertEquals(original.x, transformed.x, EPSILON);
        assertEquals(original.y, transformed.y, EPSILON);
        assertEquals(original.z, transformed.z, EPSILON);
    }

    @Test
    void defaultDirectionsMatchCoordinateSystem() {
        final Transform transform = new Transform();

        final Vector3f forward = transform.getForward(new Vector3f());
        final Vector3f right = transform.getRight(new Vector3f());
        final Vector3f up = transform.getUp(new Vector3f());

        assertEquals(0.0f, forward.x, EPSILON);
        assertEquals(0.0f, forward.y, EPSILON);
        assertEquals(-1.0f, forward.z, EPSILON);

        assertEquals(1.0f, right.x, EPSILON);
        assertEquals(0.0f, right.y, EPSILON);
        assertEquals(0.0f, right.z, EPSILON);

        assertEquals(0.0f, up.x, EPSILON);
        assertEquals(1.0f, up.y, EPSILON);
        assertEquals(0.0f, up.z, EPSILON);
    }

    @Test
    void directionsFollowRotation() {
        final Transform transform = new Transform().setRotationEuler(0.0f, (float) Math.toRadians(90.0), 0.0f);
        final Vector3f forward = transform.getForward(new Vector3f());
        final Vector3f right = transform.getRight(new Vector3f());

        assertEquals(-1.0f, forward.x, EPSILON);
        assertEquals(0.0f, forward.y, EPSILON);
        assertEquals(0.0f, forward.z, EPSILON);

        assertEquals(0.0f, right.x, EPSILON);
        assertEquals(0.0f, right.y, EPSILON);
        assertEquals(-1.0f, right.z, EPSILON);
    }

}