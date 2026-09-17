package atomixsoft.dev.cosmos.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityHierarchyTest {

    private static final float EPSILON = 0.00001f;

    @Test
    void parentAndChildRelationshipsAreSynchronized() {
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");
        final Entity child = scene.createEntity("Child");

        child.setParent(parent);

        assertSame(parent, child.getParent());
        assertEquals(1, parent.getChildCount());
        assertSame(child, parent.getChildren().getFirst());
    }

    @Test
    void childWorldTransformIncludesParentTransform() {
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");
        final Entity child = scene.createEntity("Child");

        parent.getTransform().setPosition(10.0f, 0.0f, 0.0f);
        child.getTransform().setPosition(2.0f, 3.0f, 0.0f);
        child.setParent(parent);

        final Vector3f worldPosition = child.getWorldMatrix(new Matrix4f()).getTranslation(new Vector3f());

        assertEquals(12.0f, worldPosition.x, EPSILON);
        assertEquals(3.0f, worldPosition.y, EPSILON);
        assertEquals(0.0f, worldPosition.z, EPSILON);
    }

    @Test
    void reparentingPreservesLocalTransform() {
        final Scene scene = new Scene("Test");
        final Entity firstParent = scene.createEntity("First Parent");
        final Entity secondParent = scene.createEntity("Second Parent");
        final Entity child = scene.createEntity("Child");

        child.getTransform().setPosition(2.0f, 3.0f, 4.0f);

        child.setParent(firstParent);
        child.setParent(secondParent);

        assertEquals(2.0f, child.getTransform().getPosition().x(), EPSILON);
        assertEquals(3.0f, child.getTransform().getPosition().y(), EPSILON);
        assertEquals(4.0f, child.getTransform().getPosition().z(), EPSILON);
    }

    @Test
    void hierarchyCyclesAreRejected() {
        final Scene scene = new Scene("Test");
        final Entity root = scene.createEntity("Root");
        final Entity child = scene.createEntity("Child");
        final Entity grandchild = scene.createEntity("Grandchild");

        child.setParent(root);
        grandchild.setParent(child);

        assertThrows(IllegalArgumentException.class, () -> root.setParent(grandchild));
    }

    @Test
    void entitiesFromDifferentScenesCannotBeRelated() {
        final Scene firstScene = new Scene("First");
        final Scene secondScene = new Scene("Second");
        final Entity first = firstScene.createEntity("First");
        final Entity second = secondScene.createEntity("Second");

        assertThrows(IllegalArgumentException.class, () -> first.setParent(second));
    }

    @Test
    void destroyingParentDestroysDescendants() {
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");
        final Entity child = scene.createEntity("Child");
        final Entity grandchild = scene.createEntity("Grandchild");

        child.setParent(parent);
        grandchild.setParent(child);

        scene.destroyEntity(parent);

        assertFalse(parent.isValid());
        assertFalse(child.isValid());
        assertFalse(grandchild.isValid());

        assertEquals(0, scene.getEntityCount());
    }

}