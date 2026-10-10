package atomixsoft.dev.cosmos.editor.scene;

import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.camera.PerspectiveCamera;
import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static atomixsoft.dev.cosmos.editor.asset.EditorAssetKeys.*;

public final class SceneAuthoring {

    private static final float DEFAULT_CAMERA_FOV = (float) Math.toRadians(60.0);
    private static final float DEFAULT_CAMERA_ASPECT = 16.0f / 9.0f;
    private static final float DEFAULT_CAMERA_NEAR = 0.1f;
    private static final float DEFAULT_CAMERA_FAR = 100.0f;

    private static final float MIN_INVERTIBLE_DETERMINANT = 0.000001f;

    private final AssetManager m_Assets;

    public SceneAuthoring(AssetManager assets) {
        if(assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        m_Assets = assets;
    }

    public void reparentPreserveWorld(Scene scene, Entity entity, Entity parent, int siblingIndex) {
        validateEntity(scene, entity);

        if (parent != null)
            validateEntity(scene, parent);

        if (entity.getParent() == parent) {
            entity.setSiblingIndex(siblingIndex);
            return;
        }

        final Matrix4f worldMatrix = entity.getWorldMatrix(new Matrix4f());
        final Matrix4f localMatrix = new Matrix4f();

        if (parent == null) {
            localMatrix.set(worldMatrix);
        } else {
            final Matrix4f parentWorld = parent.getWorldMatrix(new Matrix4f());
            final float determinant = parentWorld.determinant();

            if (!Float.isFinite(determinant) || Math.abs(determinant) <= MIN_INVERTIBLE_DETERMINANT)
                throw new IllegalStateException("Cannot preserve world Transform when reparenting under an Entity with a non-invertible world Transform!");

            parentWorld.invert();
            localMatrix.set(parentWorld).mul(worldMatrix);
        }

        final Vector3f position = new Vector3f();
        final Quaternionf rotation = new Quaternionf();
        final Vector3f scale = new Vector3f();

        localMatrix.getTranslation(position);
        localMatrix.getUnnormalizedRotation(rotation).normalize();
        localMatrix.getScale(scale);

        entity.setParent(parent, siblingIndex);
        entity.getTransform().setPosition(position).setRotation(rotation).setScale(scale);
    }

    public Entity createEmpty(Scene scene) {
        validateScene(scene);
        return scene.createEntity(createUniqueName(scene, "Entity"));
    }

    public Entity createCamera(Scene scene) {
        validateScene(scene);

        final Entity entity = scene.createEntity(createUniqueName(scene, "Camera"));
        entity.getTransform().setPosition(0.0f, 0.0f, 5.0f);

        addCameraComponent(scene, entity);
        return entity;
    }

    public Entity createCube(Scene scene) {
        validateScene(scene);

        final Mesh mesh = m_Assets.load(CUBE_MESH);
        final Material material = m_Assets.load(DEFAULT_MATERIAL);
        final Entity entity = scene.createEntity(createUniqueName(scene, "Cube"));

        entity.addComponent(new MeshRenderComponent(mesh, material));

        return entity;
    }

    public CameraComponent addCameraComponent(Scene scene, Entity entity) {
        validateEntity(scene, entity);
        if (entity.hasComponent(CameraComponent.class))
            throw new IllegalStateException("Entity already has a CameraComponent!");

        final CameraComponent component = entity.addComponent(new CameraComponent(new PerspectiveCamera(DEFAULT_CAMERA_FOV, DEFAULT_CAMERA_ASPECT, DEFAULT_CAMERA_NEAR, DEFAULT_CAMERA_FAR)));
        if (!scene.hasPrimaryCamera())
            scene.setPrimaryCamera(entity);

        return component;
    }

    public MeshRenderComponent addMeshRenderComponent(Scene scene, Entity entity) {
        validateEntity(scene, entity);
        if (entity.hasComponent(MeshRenderComponent.class))
            throw new IllegalStateException("Entity already has a MeshRenderComponent!");

        final Mesh mesh = m_Assets.load(CUBE_MESH);
        final Material material = m_Assets.load(DEFAULT_MATERIAL);

        return entity.addComponent(new MeshRenderComponent(mesh, material));
    }

    public CameraComponent removeCameraComponent(Scene scene, Entity entity) {
        validateEntity(scene, entity);
        return entity.removeComponent(CameraComponent.class);
    }

    public MeshRenderComponent removeMeshRenderComponent(Scene scene, Entity entity) {
        validateEntity(scene, entity);
        return entity.removeComponent(MeshRenderComponent.class);
    }

    public void setPrimaryCamera(Scene scene, Entity entity) {
        validateEntity(scene, entity);
        scene.setPrimaryCamera(entity);
    }

    public void clearPrimaryCamera(Scene scene) {
        validateScene(scene);
        scene.clearPrimaryCamera();
    }

    public void deleteEntity(Scene scene, Entity entity) {
        validateEntity(scene, entity);
        scene.destroyEntity(entity);
    }

    public String createUniqueName(Scene scene, String baseName) {
        validateScene(scene);

        if (baseName == null || baseName.isBlank())
            throw new IllegalArgumentException("Base Entity name cannot be empty!");

        if (isNameAvailable(scene, baseName))
            return baseName;

        int suffix = 2;
        while (true) {
            final String candidate = baseName + "(" + suffix + ")";
            if (isNameAvailable(scene, candidate))
                return candidate;

            suffix++;
        }
    }

    private static boolean isNameAvailable(Scene scene, String name) {
        for(Entity entity : scene.getEntities())
            if(entity.getName().equals(name))
                return false;

        return true;
    }

    private static void validateScene(Scene scene) {
        if(scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");
    }

    private static void validateEntity(Scene scene, Entity entity) {
        validateScene(scene);

        if(entity == null)
            throw new IllegalArgumentException("Entity cannot be null!");

        if(!entity.isValid())
            throw new IllegalArgumentException("Entity is no longer valid!");

        if(scene.findEntity(entity.getId()) != entity)
            throw new IllegalArgumentException("Entity must belong to the supplied Scene!");
    }

}
