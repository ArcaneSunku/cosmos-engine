package atomixsoft.dev.cosmos.editor.history.command;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.editor.history.EditorCommand;
import atomixsoft.dev.cosmos.editor.history.snapshot.EntitySubtreeSnapshot;

import atomixsoft.dev.cosmos.editor.selection.EditorSelection;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

public final class CreateEntityCommand implements EditorCommand {

    private final String m_Name;

    private final Scene m_Scene;
    private final AssetManager m_Assets;

    private final EditorSelection m_Selection;
    private final EntitySubtreeSnapshot m_Snapshot;

    private final EntityId m_PreviousSelectionId;

    public CreateEntityCommand(String name, Scene scene, AssetManager assets, EditorSelection selection, EntitySubtreeSnapshot snapshot, EntityId previousSelectionId) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Command name cannot be empty!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        if (selection == null)
            throw new IllegalArgumentException("Editor Selection cannot be null!");

        if (snapshot == null)
            throw new IllegalArgumentException("Entity Snapshot cannot be null!");

        m_Name = name;

        m_Scene = scene;
        m_Assets = assets;

        m_Selection = selection;
        m_Snapshot = snapshot;

        m_PreviousSelectionId = previousSelectionId;
    }

    @Override
    public void execute() {
        final Entity entity = m_Snapshot.restore(m_Scene, m_Assets);
        m_Selection.select(entity);
    }

    @Override
    public void undo() {
        final Entity selected = m_Selection.getEntity();
        if (selected != null && m_Snapshot.contains(selected.getId()))
            m_Selection.clear();

        m_Snapshot.destroy(m_Scene);

        if (m_PreviousSelectionId == null)
            return;

        final Entity previous = m_Scene.findEntity(m_PreviousSelectionId);
        if (previous != null)
            m_Selection.select(previous);
    }

    @Override
    public String getName() {
        return m_Name;
    }

}