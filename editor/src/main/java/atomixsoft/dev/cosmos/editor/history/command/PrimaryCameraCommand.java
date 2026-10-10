package atomixsoft.dev.cosmos.editor.history.command;

import atomixsoft.dev.cosmos.editor.history.EditorCommand;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

import java.util.Objects;

public final class PrimaryCameraCommand implements EditorCommand {

    private final Scene m_Scene;

    private final EntityId m_Before;
    private final EntityId m_After;

    public PrimaryCameraCommand(Scene scene, EntityId before, EntityId after) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        m_Scene = scene;

        m_Before = before;
        m_After = after;
    }

    @Override
    public void execute() {
        apply(m_After);
    }

    @Override
    public void undo() {
        apply(m_Before);
    }

    @Override
    public String getName() {
        return "Change Primary Camera";
    }

    @Override
    public boolean isNoOp() {
        return Objects.equals(m_Before, m_After);
    }

    private void apply(EntityId entityId) {
        if (entityId == null) {
            m_Scene.clearPrimaryCamera();
            return;
        }

        final Entity entity = m_Scene.findEntity(entityId);
        if (entity == null)
            throw new IllegalStateException("Primary Camera Entity no longer exists: " + entityId);

        m_Scene.setPrimaryCamera(entity);
    }

}