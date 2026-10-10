package atomixsoft.dev.cosmos.editor.history.command;

import atomixsoft.dev.cosmos.editor.history.EditorCommand;
import atomixsoft.dev.cosmos.editor.history.HierarchyState;

import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

public final class HierarchyChangeCommand implements EditorCommand {

    private final String m_Name;

    private final Scene m_Scene;
    private final EntityId m_EntityId;

    private final HierarchyState m_Before;
    private final HierarchyState m_After;

    public HierarchyChangeCommand(String name, Scene scene, EntityId entityId, HierarchyState before, HierarchyState after) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Command name cannot be empty!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (entityId == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");

        if (before == null || after == null)
            throw new IllegalArgumentException("Hierarchy States cannot be null!");

        m_Name = name;

        m_Scene = scene;
        m_EntityId = entityId;

        m_Before = before;
        m_After = after;
    }

    @Override
    public void execute() {
        m_After.apply(m_Scene, m_EntityId);
    }

    @Override
    public void undo() {
        m_Before.apply(m_Scene, m_EntityId);
    }

    @Override
    public String getName() {
        return m_Name;
    }

    @Override
    public boolean isNoOp() {
        return m_Before.approximatelyEquals(m_After);
    }

}