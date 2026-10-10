package atomixsoft.dev.cosmos.editor.history;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

import java.util.Objects;

public record HierarchyState(EntityId parentId, int siblingIndex, TransformState transform) {

    public HierarchyState {
        if (siblingIndex < 0)
            throw new IllegalArgumentException("Sibling index cannot be negative!");

        if (transform == null)
            throw new IllegalArgumentException("Transform State cannot be null!");
    }

    public static HierarchyState capture(Entity entity) {
        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null!");

        final Entity parent = entity.getParent();
        return new HierarchyState(parent == null ? null : parent.getId(), entity.getSiblingIndex(), TransformState.capture(entity.getTransform()));
    }

    public void apply(Scene scene, EntityId entityId) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (entityId == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");

        final Entity entity = scene.findEntity(entityId);
        if (entity == null)
            throw new IllegalStateException("Hierarchy Entity no longer exists: " + entityId);

        final Entity parent;
        if (parentId == null) {
            parent = null;
        } else {
            parent = scene.findEntity(parentId);
            if (parent == null)
                throw new IllegalStateException("Hierarchy parent no longer exists: " + parentId);
        }

        entity.setParent(parent, siblingIndex);
        transform.apply(entity.getTransform());
    }

    public boolean approximatelyEquals(HierarchyState other) {
        if (other == null)
            return false;

        return Objects.equals(parentId, other.parentId) && siblingIndex == other.siblingIndex && transform.approximatelyEquals(other.transform);
    }

}