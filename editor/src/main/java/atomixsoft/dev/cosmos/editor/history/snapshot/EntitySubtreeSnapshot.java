package atomixsoft.dev.cosmos.editor.history.snapshot;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

public record EntitySubtreeSnapshot(EntitySnapshot root, EntityId parentId, int siblingIndex,
                                    EntityId primaryCameraId) {

    public EntitySubtreeSnapshot {
        if (root == null)
            throw new IllegalArgumentException("Root Entity Snapshot cannot be null!");

        if (primaryCameraId != null && !root.contains(primaryCameraId))
            throw new IllegalArgumentException("Primary Camera ID must belong to the captured subtree!");

        if (siblingIndex < 0)
            throw new IllegalArgumentException("Sibling index cannot be negative!");
    }

    public static EntitySubtreeSnapshot capture(Scene scene, Entity rootEntity, AssetManager assets) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (rootEntity == null)
            throw new IllegalArgumentException("Root Entity cannot be null!");

        if (scene.findEntity(rootEntity.getId()) != rootEntity)
            throw new IllegalArgumentException("Entity must belong to the supplied Scene!");

        final EntitySnapshot root = EntitySnapshot.capture(rootEntity, assets);
        final Entity parent = rootEntity.getParent();

        final Entity primaryCamera = scene.getPrimaryCameraEntity();
        final EntityId primaryCameraId;

        if (primaryCamera != null && root.contains(primaryCamera.getId()))
            primaryCameraId = primaryCamera.getId();
        else
            primaryCameraId = null;

        return new EntitySubtreeSnapshot(root, parent == null ? null : parent.getId(), rootEntity.getSiblingIndex(), primaryCameraId);
    }

    public Entity restore(Scene scene, AssetManager assets) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        root.validateCanRestore(scene);

        final Entity parent;
        if (parentId == null) {
            parent = null;
        } else {
            parent = scene.findEntity(parentId);

            if (parent == null)
                throw new IllegalStateException("Cannot restore Entity subtree because its parent no longer exists: " + parentId);
        }

        final Entity restored = root.restore(scene, assets, parent, siblingIndex);

        try {
            if (primaryCameraId != null) {
                final Entity primary = scene.findEntity(primaryCameraId);

                if (primary == null)
                    throw new IllegalStateException("Restored Primary Camera Entity could not be found!");

                scene.setPrimaryCamera(primary);
            }

            return restored;
        } catch (RuntimeException | Error exception) {
            try {
                scene.destroyEntity(restored);
            } catch (RuntimeException | Error cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }

            throw exception;
        }
    }

    public void destroy(Scene scene) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        final Entity entity = scene.findEntity(root.id());
        if (entity == null)
            throw new IllegalStateException("Cannot destroy Entity subtree because its root does not exist: " + root.id());

        scene.destroyEntity(entity);
    }

    public EntitySubtreeSnapshot duplicate(String rootName) {
        return new EntitySubtreeSnapshot(root.duplicate(rootName), parentId, siblingIndex + 1, null);
    }

    public boolean contains(EntityId entityId) {
        return root.contains(entityId);
    }

}