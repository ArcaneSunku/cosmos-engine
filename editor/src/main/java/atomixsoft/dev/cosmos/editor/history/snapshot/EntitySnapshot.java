package atomixsoft.dev.cosmos.editor.history.snapshot;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.editor.history.TransformState;

import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

import java.util.ArrayList;
import java.util.List;

public record EntitySnapshot(EntityId id, String name, TransformState transform, List<ComponentSnapshot> components,
                             List<EntitySnapshot> children) {

    public EntitySnapshot {
        if (id == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Entity name cannot be empty!");

        if (transform == null)
            throw new IllegalArgumentException("Transform State cannot be null!");

        if (components == null)
            throw new IllegalArgumentException("Component Snapshots cannot be null!");

        if (children == null)
            throw new IllegalArgumentException("Child Entity Snapshots cannot be null!");

        components = List.copyOf(components);
        children = List.copyOf(children);
    }

    public static EntitySnapshot capture(Entity entity, AssetManager assets) {
        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        final List<ComponentSnapshot> componentSnapshots = new ArrayList<>();
        for (Component component : entity.getComponents())
            componentSnapshots.add(ComponentSnapshot.capture(component, assets));

        final List<EntitySnapshot> childSnapshots = new ArrayList<>();
        for (Entity child : entity.getChildren())
            childSnapshots.add(capture(child, assets));

        return new EntitySnapshot(entity.getId(), entity.getName(), TransformState.capture(entity.getTransform()), componentSnapshots, childSnapshots);
    }

    public Entity restore(Scene scene, AssetManager assets, Entity parent, int siblingIndex) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        final Entity entity = scene.createEntity(id, name);

        try {
            transform.apply(entity.getTransform());
            entity.setParent(parent, siblingIndex);

            for (ComponentSnapshot component : components)
                component.restore(entity, assets);

            for (int i = 0; i < children.size(); i++)
                children.get(i).restore(scene, assets, entity, i);

            return entity;
        } catch (RuntimeException | Error exception) {
            if (entity.isValid()) {
                try {
                    scene.destroyEntity(entity);
                } catch (RuntimeException | Error cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }

            throw exception;
        }
    }

    public void validateCanRestore(Scene scene) {
        if (scene.containsEntity(id))
            throw new IllegalStateException("Cannot restore Entity because its ID already exists: " + id);

        for (EntitySnapshot child : children)
            child.validateCanRestore(scene);
    }

    public boolean contains(EntityId entityId) {
        if (id.equals(entityId))
            return true;


        for (EntitySnapshot child : children)
            if (child.contains(entityId))
                return true;

        return false;
    }

    public EntitySnapshot duplicate(String rootName) {
        if (rootName == null || rootName.isBlank())
            throw new IllegalArgumentException("Duplicate Entity name cannot be empty!");

        return duplicateInternal(rootName, true);
    }

    private EntitySnapshot duplicateInternal(String rootName, boolean root) {
        final List<EntitySnapshot> duplicatedChildren = new ArrayList<>();
        for (EntitySnapshot child : children)
            duplicatedChildren.add(child.duplicateInternal(child.name, false));

        return new EntitySnapshot(EntityId.random(), root ? rootName : name, transform, components, duplicatedChildren);
    }

}