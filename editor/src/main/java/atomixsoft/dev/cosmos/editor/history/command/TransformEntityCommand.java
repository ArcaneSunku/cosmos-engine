package atomixsoft.dev.cosmos.editor.history.command;

import atomixsoft.dev.cosmos.editor.history.EditorCommand;
import atomixsoft.dev.cosmos.editor.history.TransformState;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

public final class TransformEntityCommand implements EditorCommand {

    private final Scene m_Scene;
    private final EntityId m_EntityId;

    private final TransformState m_Before;
    private final TransformState m_After;

    private final String m_Name;

    public TransformEntityCommand(String name, Scene scene, EntityId entityId, TransformState before, TransformState after) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Command name cannot be empty!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (entityId == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");

        if (before == null)
            throw new IllegalArgumentException("Before Transform State cannot be null!");

        if (after == null)
            throw new IllegalArgumentException("After Transform State cannot be null!");

        m_Name = name;

        m_Scene = scene;
        m_EntityId = entityId;

        m_Before = before;
        m_After = after;
    }

    @Override
    public String getName() {
        return m_Name;
    }

    @Override
    public void execute() {
        m_After.apply(resolveEntity().getTransform());
    }

    @Override
    public void undo() {
        m_Before.apply(resolveEntity().getTransform());
    }

    @Override
    public boolean isNoOp() {
        return m_Before.approximatelyEquals(m_After);
    }

    private Entity resolveEntity() {
        final Entity entity = m_Scene.findEntity(m_EntityId);
        if (entity == null)
            throw new IllegalStateException("Cannot apply Transform history because Entity no longer exists: " + m_EntityId);

        return entity;
    }

}