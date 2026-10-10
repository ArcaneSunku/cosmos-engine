package atomixsoft.dev.cosmos.editor.history.command;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.editor.history.EditorCommand;
import atomixsoft.dev.cosmos.editor.history.snapshot.EntitySubtreeSnapshot;

import atomixsoft.dev.cosmos.editor.selection.EditorSelection;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

public final class DeleteEntityCommand implements EditorCommand {

    private final Scene m_Scene;
    private final AssetManager m_Assets;

    private final EditorSelection m_Selection;
    private final EntitySubtreeSnapshot m_Snapshot;

    private final EntityId m_SelectedEntityId;

    public DeleteEntityCommand(Scene scene, AssetManager assets, EditorSelection selection, Entity entity) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        if (selection == null)
            throw new IllegalArgumentException("Editor Selection cannot be null!");

        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null!");

        m_Scene = scene;
        m_Assets = assets;

        m_Selection = selection;
        m_Snapshot = EntitySubtreeSnapshot.capture(scene, entity, assets);

        final Entity selected = selection.getEntity();

        if (selected != null && m_Snapshot.contains(selected.getId()))
            m_SelectedEntityId = selected.getId();
        else
            m_SelectedEntityId = null;
    }

    @Override
    public void execute() {
        final Entity selected = m_Selection.getEntity();
        if (selected != null && m_Snapshot.contains(selected.getId()))
            m_Selection.clear();

        m_Snapshot.destroy(m_Scene);
    }

    @Override
    public void undo() {
        m_Snapshot.restore(m_Scene, m_Assets);

        if (m_SelectedEntityId == null)
            return;

        final Entity selected = m_Scene.findEntity(m_SelectedEntityId);
        if (selected != null)
            m_Selection.select(selected);
    }

    @Override
    public String getName() {
        return "Delete Entity";
    }

}