package atomixsoft.dev.cosmos.scene;

import atomixsoft.dev.cosmos.camera.PerspectiveCamera;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneCameraTest {

    private CameraComponent createCameraComponent() {
        return new CameraComponent(new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f));
    }

    @Test
    void cameraEntityCanBecomePrimary() {
        final Scene scene = new Scene("Test");
        final Entity cameraEntity = scene.createEntity("Camera");
        final CameraComponent component = cameraEntity.addComponent(createCameraComponent());

        scene.setPrimaryCamera(cameraEntity);

        assertSame(cameraEntity, scene.getPrimaryCameraEntity());
        assertSame(component, scene.getPrimaryCameraComponent());
    }

    @Test
    void entityWithoutCameraComponentCannotBecomePrimary() {
        final Scene scene = new Scene("Test");
        final Entity entity = scene.createEntity("Not A Camera");

        assertThrows(IllegalArgumentException.class, () -> scene.setPrimaryCamera(entity));
    }

    @Test
    void entityFromAnotherSceneCannotBecomePrimary() {
        final Scene first = new Scene("First");
        final Scene second = new Scene("Second");
        final Entity camera = second.createEntity("Camera");

        camera.addComponent(createCameraComponent());

        assertThrows(IllegalArgumentException.class, () -> first.setPrimaryCamera(camera));
    }

    @Test
    void destroyingPrimaryCameraClearsSelection() {
        final Scene scene = new Scene("Test");
        final Entity camera = scene.createEntity("Camera");

        camera.addComponent(createCameraComponent());
        scene.setPrimaryCamera(camera);
        scene.destroyEntity(camera);

        assertNull(scene.getPrimaryCameraEntity());
        assertNull(scene.getPrimaryCameraComponent());
    }

    @Test
    void removingCameraComponentInvalidatesPrimaryCamera() {
        final Scene scene = new Scene("Test");
        final Entity camera = scene.createEntity("Camera");

        camera.addComponent(createCameraComponent());
        scene.setPrimaryCamera(camera);
        camera.removeComponent(CameraComponent.class);

        assertNull(scene.getPrimaryCameraEntity());
        assertNull(scene.getPrimaryCameraComponent());
    }

    @Test
    void removingCameraComponentClearsPrimaryCameraImmediately() {
        final Scene scene = new Scene("Test");
        final Entity camera = scene.createEntity("Camera");

        camera.addComponent(createCameraComponent());
        scene.setPrimaryCamera(camera);

        assertTrue(scene.hasPrimaryCamera());

        camera.removeComponent(CameraComponent.class);

        assertFalse(scene.hasPrimaryCamera());
        assertNull(scene.getPrimaryCameraEntity());
    }

    @Test
    void destroyingPrimaryCameraClearsPrimaryState() {
        final Scene scene = new Scene("Test");
        final Entity camera = scene.createEntity("Camera");

        camera.addComponent(createCameraComponent());
        scene.setPrimaryCamera(camera);
        scene.destroyEntity(camera);

        assertFalse(scene.hasPrimaryCamera());
    }

    @Test
    void sceneCameraCanInheritParentTransform() {
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");

        parent.getTransform().setPosition(10.0f, 0.0f, 0.0f);

        final Entity camera = scene.createEntity("Camera");

        camera.getTransform().setPosition(0.0f, 2.0f, 5.0f);
        camera.setParent(parent);
        camera.addComponent(createCameraComponent());
        scene.setPrimaryCamera(camera);

        final Matrix4f world = camera.getWorldMatrix(new Matrix4f());
        final Vector3f position = world.getTranslation(new Vector3f());

        assertEquals(10.0f, position.x, 0.00001f);
        assertEquals(2.0f, position.y, 0.00001f);
        assertEquals(5.0f, position.z, 0.00001f);
    }

}