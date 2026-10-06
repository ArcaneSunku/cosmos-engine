package atomixsoft.dev.cosmos.editor.scene;

import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.render.Shader;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;

import org.junit.jupiter.api.Test;

import static atomixsoft.dev.cosmos.editor.asset.EditorAssetKeys.CUBE_MESH;
import static atomixsoft.dev.cosmos.editor.asset.EditorAssetKeys.DEFAULT_MATERIAL;

import static org.junit.jupiter.api.Assertions.*;

class SceneAuthoringTest {

    @Test
    void generatedEntityNamesAreUnique() {
        final Scene scene = new Scene("Test");
        final SceneAuthoring authoring = new SceneAuthoring(new AssetManager());
        final Entity first = authoring.createEmpty(scene);
        final Entity second = authoring.createEmpty(scene);

        assertEquals("Entity", first.getName());
        assertEquals("Entity(2)", second.getName());
    }

    @Test
    void firstCameraBecomesPrimary() {
        final Scene scene = new Scene("Test");
        final SceneAuthoring authoring = new SceneAuthoring(new AssetManager());
        final Entity first = authoring.createCamera(scene);
        final Entity second = authoring.createCamera(scene);

        assertTrue(first.hasComponent(CameraComponent.class));
        assertTrue(second.hasComponent(CameraComponent.class));
        assertSame(first, scene.getPrimaryCameraEntity());
    }

    @Test
    void removingPrimaryCameraClearsPrimaryCamera() {
        final Scene scene = new Scene("Test");
        final SceneAuthoring authoring = new SceneAuthoring(new AssetManager());
        final Entity camera = authoring.createCamera(scene);

        authoring.removeCameraComponent(scene, camera);

        assertFalse(scene.hasPrimaryCamera());
        assertFalse(camera.hasComponent(CameraComponent.class));
    }

    @Test
    void cubeUsesManagedEditorAssets() {
        final AssetManager assets = new AssetManager();
        final Mesh mesh = new Mesh();
        final Material material = new Material(new Shader());

        assets.register(CUBE_MESH, mesh, null);
        assets.register(DEFAULT_MATERIAL, material, null);

        final SceneAuthoring authoring = new SceneAuthoring(assets);
        final Scene scene = new Scene("Test");
        final Entity cube = authoring.createCube(scene);
        final MeshRenderComponent renderer = cube.getComponent(MeshRenderComponent.class);

        assertNotNull(renderer);
        assertSame(mesh, renderer.getMesh());
        assertSame(material, renderer.getMaterial());
    }

    @Test
    void deletingParentDeletesSubtree() {
        final Scene scene = new Scene("Test");
        final SceneAuthoring authoring = new SceneAuthoring(new AssetManager());
        final Entity parent = authoring.createEmpty(scene);
        final Entity child = authoring.createEmpty(scene);

        child.setParent(parent);
        authoring.deleteEntity(scene, parent);

        assertEquals(0, scene.getEntityCount());
        assertFalse(parent.isValid());
        assertFalse(child.isValid());
    }

}