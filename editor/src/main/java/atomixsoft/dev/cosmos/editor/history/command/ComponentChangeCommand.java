package atomixsoft.dev.cosmos.editor.history.command;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.editor.history.EditorCommand;
import atomixsoft.dev.cosmos.editor.history.snapshot.ComponentSnapshot;

import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

import java.util.Objects;

public final class ComponentChangeCommand implements EditorCommand {

    private final String m_Name;

    private final Scene m_Scene;
    private final AssetManager m_Assets;

    private final EntityId m_EntityId;

    private final Class<? extends Component> m_ComponentType;

    private final ComponentSnapshot m_Before;
    private final ComponentSnapshot m_After;

    private final EntityId m_PrimaryCameraBefore;
    private final EntityId m_PrimaryCameraAfter;

    public ComponentChangeCommand(String name, Scene scene, AssetManager assets, EntityId entityId, Class<? extends Component> componentType, ComponentSnapshot before, ComponentSnapshot after, EntityId primaryCameraBefore, EntityId primaryCameraAfter) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Command name cannot be empty!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        if (entityId == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");

        if (componentType == null)
            throw new IllegalArgumentException("Component Type cannot be null!");

        if (before == null && after == null)
            throw new IllegalArgumentException("Component Change must contain a before or after state!");

        validateSnapshotType(componentType, before);
        validateSnapshotType(componentType, after);

        m_Name = name;

        m_Scene = scene;
        m_Assets = assets;

        m_EntityId = entityId;
        m_ComponentType = componentType;

        m_Before = before;
        m_After = after;

        m_PrimaryCameraBefore = primaryCameraBefore;
        m_PrimaryCameraAfter = primaryCameraAfter;
    }

    private static void validateSnapshotType(Class<? extends Component> type, ComponentSnapshot snapshot) {
        if (snapshot == null)
            return;

        if (!snapshot.getComponentType().equals(type))
            throw new IllegalArgumentException("Component Snapshot type does not match command Component type!");
    }

    @Override
    public void execute() {
        apply(m_After, m_PrimaryCameraAfter);
    }

    @Override
    public void undo() {
        apply(m_Before, m_PrimaryCameraBefore);
    }

    @Override
    public String getName() {
        return m_Name;
    }

    @Override
    public boolean isNoOp() {
        return Objects.equals(m_Before, m_After) && Objects.equals(m_PrimaryCameraBefore, m_PrimaryCameraAfter);
    }

    private void apply(ComponentSnapshot snapshot, EntityId primaryCameraId) {
        final Entity entity = resolveEntity();

        if (entity.hasComponent(m_ComponentType))
            entity.removeComponent(m_ComponentType);

        if (snapshot != null)
            snapshot.restore(entity, m_Assets);

        restorePrimaryCamera(primaryCameraId);
    }

    private void restorePrimaryCamera(EntityId entityId) {
        if (entityId == null) {
            m_Scene.clearPrimaryCamera();
            return;
        }

        final Entity entity = m_Scene.findEntity(entityId);
        if (entity == null)
            throw new IllegalStateException("Primary Camera Entity no longer exists: " + entityId);

        m_Scene.setPrimaryCamera(entity);
    }

    private Entity resolveEntity() {
        final Entity entity = m_Scene.findEntity(m_EntityId);
        if (entity == null)
            throw new IllegalStateException("Cannot apply Component history because Entity no longer exists: " + m_EntityId);

        return entity;
    }

}