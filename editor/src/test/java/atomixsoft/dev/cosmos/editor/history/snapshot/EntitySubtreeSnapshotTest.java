package atomixsoft.dev.cosmos.editor.history.snapshot;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.camera.PerspectiveCamera;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

import atomixsoft.dev.cosmos.scene.component.CameraComponent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntitySubtreeSnapshotTest {

    @Test
    void restorePreservesHierarchyIdsAndPrimaryCamera() {
        final AssetManager assets = new AssetManager();
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");

        parent.getTransform().setPosition(2.0f, 3.0f, 4.0f);

        final Entity camera = scene.createEntity("Camera");

        camera.setParent(parent);
        camera.addComponent(new CameraComponent(new PerspectiveCamera((float) Math.toRadians(70.0), 16.0f / 9.0f, 0.1f, 500.0f)));
        scene.setPrimaryCamera(camera);

        final EntityId parentId = parent.getId();
        final EntityId cameraId = camera.getId();
        final EntitySubtreeSnapshot snapshot = EntitySubtreeSnapshot.capture(scene, parent, assets);

        scene.destroyEntity(parent);

        assertEquals(0, scene.getEntityCount());
        assertFalse(scene.hasPrimaryCamera());

        final Entity restoredParent = snapshot.restore(scene, assets);
        final Entity restoredCamera = scene.findEntity(cameraId);

        assertNotNull(restoredParent);
        assertNotNull(restoredCamera);
        assertEquals(parentId, restoredParent.getId());
        assertEquals(cameraId, restoredCamera.getId());
        assertSame(restoredParent, restoredCamera.getParent());
        assertNotNull(restoredCamera.getComponent(CameraComponent.class));
        assertSame(restoredCamera, scene.getPrimaryCameraEntity());
        assertEquals(2.0f, restoredParent.getTransform().getPosition().x());
        assertEquals(3.0f, restoredParent.getTransform().getPosition().y());
        assertEquals(4.0f, restoredParent.getTransform().getPosition().z());
    }

    @Test
    void duplicateUsesFreshEntityIds() {
        final AssetManager assets = new AssetManager();
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");
        final Entity child = scene.createEntity("Child");

        child.setParent(parent);

        final EntitySubtreeSnapshot source = EntitySubtreeSnapshot.capture(scene, parent, assets);
        final EntitySubtreeSnapshot duplicate = source.duplicate("Parent Copy");

        assertNotEquals(source.root().id(), duplicate.root().id());
        assertNotEquals(source.root().children().getFirst().id(), duplicate.root().children().getFirst().id());
        assertEquals("Parent Copy", duplicate.root().name());
        assertNull(duplicate.primaryCameraId());
    }

}