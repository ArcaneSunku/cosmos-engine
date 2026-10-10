package atomixsoft.dev.cosmos.editor.history.command;

import atomixsoft.dev.cosmos.editor.history.EditorCommand;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

public final class RenameEntityCommand implements EditorCommand {

    private final Scene m_Scene;
    private final EntityId m_EntityId;

    private final String m_Before;
    private final String m_After;

    public RenameEntityCommand(Scene scene, EntityId entityId, String before, String after) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (entityId == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");

        validateName(before);

        validateName(after);

        m_Scene = scene;
        m_EntityId = entityId;

        m_Before = before;
        m_After = after;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Entity name cannot be empty!");
    }

    @Override
    public String getName() {
        return "Rename Entity";
    }

    @Override
    public void execute() {
        resolveEntity().setName(m_After);
    }

    @Override
    public void undo() {
        resolveEntity().setName(m_Before);
    }

    @Override
    public boolean isNoOp() {
        return m_Before.equals(m_After);
    }

    private Entity resolveEntity() {
        final Entity entity = m_Scene.findEntity(m_EntityId);
        if (entity == null)
            throw new IllegalStateException("Cannot apply Rename history because Entity no longer exists: " + m_EntityId);

        return entity;
    }

}