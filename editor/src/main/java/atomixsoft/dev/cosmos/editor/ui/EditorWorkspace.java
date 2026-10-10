package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.editor.history.EditorHistory;
import atomixsoft.dev.cosmos.editor.history.HierarchyState;
import atomixsoft.dev.cosmos.editor.history.command.CreateEntityCommand;
import atomixsoft.dev.cosmos.editor.history.command.DeleteEntityCommand;
import atomixsoft.dev.cosmos.editor.history.command.HierarchyChangeCommand;
import atomixsoft.dev.cosmos.editor.history.snapshot.EntitySubtreeSnapshot;
import atomixsoft.dev.cosmos.editor.scene.SceneAuthoring;
import atomixsoft.dev.cosmos.editor.selection.EditorSelection;
import atomixsoft.dev.cosmos.editor.selection.ScenePickRequest;
import atomixsoft.dev.cosmos.editor.settings.EditorSettings;
import atomixsoft.dev.cosmos.editor.tool.EditorToolState;
import atomixsoft.dev.cosmos.editor.tool.TransformTool;
import atomixsoft.dev.cosmos.render.Framebuffer;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;

import atomixsoft.dev.cosmos.spatial.Transform;
import imgui.*;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiTreeNodeFlags;

import imgui.internal.flag.ImGuiDockNodeFlags;
import imgui.type.ImInt;

import java.util.List;

public final class EditorWorkspace {

    private static final int WORKSPACE_LAYOUT_VERSION = 1;

    private static final String HIERARCHY_ENTITY_PAYLOAD = "COSMOS_HIERARCHY_ENTITY";
    private static final float HIERARCHY_DROP_ZONE_HEIGHT = 5.0f;

    private final SceneViewportPanel m_SceneViewport;
    private final GameViewportPanel m_GameViewport;
    private final InspectorPanel m_Inspector;

    private final EditorSelection m_Selection;
    private final EditorToolState m_Tools;
    private final EditorHistory m_History;

    private AssetManager m_Assets;
    private HierarchyAction m_PendingHierarchyAction;

    private SceneAuthoring m_Authoring;
    private EditorSettings m_Settings;

    private HierarchyMove m_PendingHierarchyMove;

    private boolean m_DefaultLayoutBuilt;
    private boolean m_ResetLayoutRequested;

    private boolean m_ShowHierarchy;
    private boolean m_ShowInspector;
    private boolean m_ShowGame;
    private boolean m_ShowAssets;
    private boolean m_ShowScene;

    public EditorWorkspace() {
        m_SceneViewport = new SceneViewportPanel();
        m_GameViewport = new GameViewportPanel();
        m_Inspector = new InspectorPanel();

        m_Selection = new EditorSelection();
        m_Tools = new EditorToolState();
        m_History = new EditorHistory();

        m_DefaultLayoutBuilt = false;
        m_ResetLayoutRequested = false;

        m_ShowHierarchy = true;
        m_ShowInspector = true;
        m_ShowGame = true;
        m_ShowAssets = true;
        m_ShowScene = true;
    }

    public void initialize(AssetManager assets, EditorSettings settings, boolean savedLayoutExists) {
        if(assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        if(settings == null)
            throw new IllegalArgumentException("Editor Settings cannot be null!");

        m_Assets = assets;
        m_PendingHierarchyAction = null;
        m_PendingHierarchyMove = null;

        m_History.clear();

        m_Authoring = new SceneAuthoring(assets);
        m_Settings = settings;

        m_ShowHierarchy = settings.isShowHierarchy();
        m_ShowInspector = settings.isShowInspector();
        m_ShowAssets = settings.isShowAssets();
        m_ShowScene = settings.isShowScene();
        m_ShowGame = settings.isShowGame();

        m_DefaultLayoutBuilt = savedLayoutExists && settings.getWorkspaceLayoutVersion() == WORKSPACE_LAYOUT_VERSION;
        m_ResetLayoutRequested = false;

        m_SceneViewport.initialize();

        try {
            m_GameViewport.initialize();
        } catch(RuntimeException | Error exception) {
            m_SceneViewport.dispose();

            m_Authoring = null;
            m_Settings = null;

            throw exception;
        }
    }

    public void draw(Engine engine, Scene scene, Camera editorCamera, Transform editorCamTransform) {
        if(m_Authoring == null)
            throw new IllegalStateException("Editor Workspace has not been initialized!");

        if (engine == null)
            throw new IllegalArgumentException("Engine cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if(editorCamera == null)
            throw new IllegalArgumentException("Editor Camera cannot be null!");

        if(editorCamTransform == null)
            throw new IllegalArgumentException("Camera Transform cannot be null!");

        drawMainMenu(engine, scene);

        final ImGuiViewport viewport = ImGui.getMainViewport();
        final int dockSpaceId = ImGui.dockSpaceOverViewport(0, viewport, 0);

        if(!m_DefaultLayoutBuilt || m_ResetLayoutRequested) {
            buildDefaultLayout(dockSpaceId, viewport);

            m_Settings.setWorkspaceLayoutVersion(WORKSPACE_LAYOUT_VERSION);
            m_DefaultLayoutBuilt = true;
            m_ResetLayoutRequested = false;
        }

        if(m_ShowScene) m_SceneViewport.draw(scene, editorCamera, editorCamTransform, m_Selection, m_Tools, m_History, engine.getInput().isControlDown());
        else m_SceneViewport.hide(scene, m_History);

        if(m_ShowGame) m_GameViewport.draw(scene.hasPrimaryCamera());
        else m_GameViewport.hide();

        if (m_ShowHierarchy) drawHierarchy(scene);
        if (m_ShowInspector) m_Inspector.draw(engine, scene, m_Authoring, m_Selection, m_History);
        if (m_ShowAssets) drawAssets(engine);
    }

    public void dispose() {
        clearSelection();
        m_History.clear();

        Throwable failure = null;

        try {
            m_GameViewport.dispose();
        } catch (RuntimeException | Error exception) {
            failure = exception;
        }

        try {
            m_SceneViewport.dispose();
        } catch (RuntimeException | Error exception) {
            if(failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }

        if(m_Settings != null) {
            syncPanelSettings();

            try {
                m_Settings.save();
            } catch (RuntimeException | Error exception) {
                if(failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }

        m_Assets = null;

        m_PendingHierarchyAction = null;
        m_PendingHierarchyMove = null;

        m_Authoring = null;
        m_Settings = null;

        if(failure == null)
            return;

        if(failure instanceof RuntimeException exception)
            throw exception;

        throw (Error) failure;
    }

    public void clearSelection() {
        m_Selection.clear();
    }

    public void selectEntity(Entity entity) {
        if(entity == null) {
            m_Selection.clear();
            return;
        }

        m_Selection.select(entity);
    }

    public void setTransformTool(TransformTool tool) {
        m_Tools.setTransformTool(tool);
    }

    public void toggleTransformSpace() {
        m_Tools.toggleTransformSpace();
    }

    public void deleteSelected(Scene scene) {
        deleteEntity(scene, m_Selection.getEntity());
    }

    public void duplicateSelected(Scene scene) {
        duplicateEntity(scene, m_Selection.getEntity());
    }

    public Framebuffer getSceneFramebuffer() {
        return m_SceneViewport.getFramebuffer();
    }

    public Framebuffer getGameFramebuffer() {
        return m_GameViewport.getFramebuffer();
    }

    public ScenePickRequest consumeScenePickRequest() {
        return m_SceneViewport.consumePickRequest();
    }

    public Entity getSelectedEntity() {
        return m_Selection.getEntity();
    }

    public boolean undo(Scene scene) {
        commitPendingEdits(scene);
        return m_History.undo();
    }

    public boolean redo(Scene scene) {
        commitPendingEdits(scene);
        return m_History.redo();
    }

    public boolean canUndo() {
        return m_History.canUndo();
    }

    public boolean canRedo() {
        return m_History.canRedo();
    }

    public boolean isSceneViewportVisible() {
        return m_SceneViewport.isVisible();
    }

    public boolean isSceneViewportHovered() {
        return m_SceneViewport.isHovered();
    }

    public boolean isSceneViewportFocused() {
        return m_SceneViewport.isFocused();
    }

    public boolean isGameViewportVisible() {
        return m_GameViewport.isVisible();
    }

    public boolean isSceneGizmoInteracting() {
        return m_SceneViewport.isGizmoInteracting();
    }

    private void buildDefaultLayout(int dockSpaceId, ImGuiViewport viewport) {
        imgui.internal.ImGui.dockBuilderRemoveNode(dockSpaceId);
        imgui.internal.ImGui.dockBuilderAddNode(dockSpaceId, ImGuiDockNodeFlags.DockSpace);

        imgui.internal.ImGui.dockBuilderSetNodePos(dockSpaceId, viewport.getWorkPosX(), viewport.getWorkPosY());
        imgui.internal.ImGui.dockBuilderSetNodeSize(dockSpaceId, viewport.getWorkSizeX(), viewport.getWorkSizeY());

        // Hierarchy & Inspector
        final ImInt hierarchyNode = new ImInt();
        final ImInt remainderAfterHierarchy = new ImInt();

        imgui.internal.ImGui.dockBuilderSplitNode(dockSpaceId, ImGuiDir.Left, 0.20f, hierarchyNode, remainderAfterHierarchy);

        final ImInt inspectorNode = new ImInt();
        final ImInt centerNode = new ImInt();

        imgui.internal.ImGui.dockBuilderSplitNode(remainderAfterHierarchy.get(), ImGuiDir.Right, 0.25f, inspectorNode, centerNode);

        // Assets & Scene
        final ImInt assetsNode = new ImInt();
        final ImInt sceneNode = new ImInt();

        imgui.internal.ImGui.dockBuilderSplitNode(centerNode.get(), ImGuiDir.Down, 0.28f, assetsNode, sceneNode);

        imgui.internal.ImGui.dockBuilderDockWindow("Hierarchy", hierarchyNode.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Inspector", inspectorNode.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Assets", assetsNode.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Scene", sceneNode.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Game", sceneNode.get());
        imgui.internal.ImGui.dockBuilderFinish(dockSpaceId);
    }

    private void resetPanelVisibility() {
        m_ShowHierarchy = true;
        m_ShowInspector = true;
        m_ShowAssets = true;
        m_ShowScene = true;
        m_ShowGame = true;

        syncPanelSettings();
    }

    private void syncPanelSettings() {
        if (m_Settings == null)
            return;

        m_Settings.setShowHierarchy(m_ShowHierarchy);
        m_Settings.setShowInspector(m_ShowInspector);
        m_Settings.setShowAssets(m_ShowAssets);
        m_Settings.setShowScene(m_ShowScene);
        m_Settings.setShowGame(m_ShowGame);
    }

    private void recordCreatedEntity(Scene scene, Entity entity, String commandName) {
        final Entity previouslySelected = m_Selection.getEntity();
        final EntityId previousSelectionId = previouslySelected == null ? null : previouslySelected.getId();
        final EntitySubtreeSnapshot snapshot;

        try {
            snapshot = EntitySubtreeSnapshot.capture(scene, entity, m_Assets);
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

        m_Selection.select(entity);
        m_History.recordExecuted(new CreateEntityCommand(commandName, scene, m_Assets, m_Selection, snapshot, previousSelectionId));
    }

    private void deleteEntity(Scene scene, Entity entity) {
        if (entity == null)
            return;

        commitPendingEdits(scene);
        m_History.execute(new DeleteEntityCommand(scene, m_Assets, m_Selection, entity));
    }

    private void duplicateEntity(Scene scene, Entity entity) {
        if (entity == null)
            return;

        commitPendingEdits(scene);

        final EntitySubtreeSnapshot source = EntitySubtreeSnapshot.capture(scene, entity, m_Assets);
        final String duplicateName = m_Authoring.createUniqueName(scene, entity.getName() + " Copy");
        final EntitySubtreeSnapshot duplicate = source.duplicate(duplicateName);

        final EntityId previousSelectionId = entity.getId();

        m_History.execute(new CreateEntityCommand("Duplicate Entity", scene, m_Assets, m_Selection, duplicate, previousSelectionId));
    }

    private void drawMainMenu(Engine engine, Scene scene) {
        if (!ImGui.beginMainMenuBar())
            return;

        try {
            if (ImGui.beginMenu("File")) {
                try {
                    if (ImGui.menuItem("Exit")) {
                        engine.stop();
                    }
                } finally {
                    ImGui.endMenu();
                }
            }

            if (ImGui.beginMenu("Edit")) {
                try {
                    if (m_History.canUndo()) {
                        final String name = m_History.getUndoName();
                        if (ImGui.menuItem("Undo " + name))
                            undo(scene);
                    } else {
                        ImGui.textDisabled("Undo");
                    }

                    if (m_History.canRedo()) {
                        final String name = m_History.getRedoName();
                        if (ImGui.menuItem("Redo " + name))
                            redo(scene);
                    } else {
                        ImGui.textDisabled("Redo");
                    }
                } finally {
                    ImGui.endMenu();
                }
            }

            if(ImGui.beginMenu("Entity")) {
                try {
                    if(ImGui.menuItem("Create Empty")) {
                        commitPendingEdits(scene);
                        recordCreatedEntity(scene, m_Authoring.createEmpty(scene), "Create Entity");
                    }

                    if(ImGui.menuItem("Create Cube")) {
                        commitPendingEdits(scene);
                        recordCreatedEntity(scene, m_Authoring.createCube(scene), "Create Cube");
                    }

                    if(ImGui.menuItem("Create Camera")) {
                        commitPendingEdits(scene);
                        recordCreatedEntity(scene, m_Authoring.createCamera(scene), "Create Camera");
                    }

                    final Entity selected = m_Selection.getEntity();
                    if(selected != null) {
                        ImGui.separator();

                        if(ImGui.menuItem("Duplicate Selected"))
                            duplicateEntity(scene, selected);

                        if(ImGui.menuItem("Delete Selected"))
                            deleteEntity(scene, selected);
                    }
                } finally {
                    ImGui.endMenu();
                }
            }

            if (ImGui.beginMenu("View")) {
                try {
                    if(ImGui.menuItem("Scene", m_ShowScene)) {
                        m_ShowScene = !m_ShowScene;
                        m_Settings.setShowScene(m_ShowScene);
                    }

                    if(ImGui.menuItem("Game", m_ShowGame)) {
                        m_ShowGame = !m_ShowGame;
                        m_Settings.setShowGame(m_ShowGame);
                    }

                    if (ImGui.menuItem("Hierarchy", m_ShowHierarchy)) {
                        m_ShowHierarchy = !m_ShowHierarchy;
                        m_Settings.setShowHierarchy(m_ShowHierarchy);
                    }

                    if (ImGui.menuItem("Inspector", m_ShowInspector)) {
                        if(m_ShowInspector)
                            commitPendingEdits(scene);

                        m_ShowInspector = !m_ShowInspector;
                        m_Settings.setShowInspector(m_ShowInspector);
                    }

                    if (ImGui.menuItem("Assets", m_ShowAssets)) {
                        m_ShowAssets = !m_ShowAssets;
                        m_Settings.setShowAssets(m_ShowAssets);
                    }

                    if(ImGui.menuItem("Reset Layout")) {
                        resetPanelVisibility();
                        m_ResetLayoutRequested = true;
                    }

                } finally {
                    ImGui.endMenu();
                }
            }
        } finally {
            ImGui.endMainMenuBar();
        }
    }

    private void drawHierarchy(Scene scene) {
        final boolean visible = ImGui.begin("Hierarchy");

        m_PendingHierarchyAction = null;
        m_PendingHierarchyMove = null;

        try {
            if (!visible)
                return;

            if(scene.getEntityCount() == 0) {
                ImGui.textDisabled("Scene is Empty.");
                return;
            }

            ImGui.text(scene.getName());
            ImGui.separator();

            drawSiblingList(scene, null, scene.getRootEntities());

            applyPendingHierarchyAction(scene);
            applyPendingHierarchyMove(scene);
        } finally {
            ImGui.end();
        }
    }

    private void drawSiblingList(Scene scene, Entity parent, List<Entity> siblings) {
        for (int i = 0; i < siblings.size(); i++) {
            drawHierarchyDropZone(scene, parent, i);
            drawEntityNode(scene, siblings.get(i));
        }

        drawHierarchyDropZone(scene, parent, siblings.size());
    }

    private void drawEntityNode(Scene scene, Entity entity) {
        int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.SpanAvailWidth;

        if (m_Selection.isSelected(entity))
            flags |= ImGuiTreeNodeFlags.Selected;

        final boolean leaf = entity.getChildCount() == 0;

        if (leaf)
            flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;

        final boolean open = ImGui.treeNodeEx(entity.getId().toString(), flags, entity.getName());

        if (ImGui.beginDragDropSource()) {
            try {
                ImGui.setDragDropPayload(HIERARCHY_ENTITY_PAYLOAD, entity.getId());
                ImGui.text(entity.getName());
            } finally {
                ImGui.endDragDropSource();
            }
        }

        drawEntityDropTarget(scene, entity);

        if (ImGui.isItemClicked())
            m_Selection.select(entity);

        if (ImGui.beginPopupContextItem("EntityContext##" + entity.getId())) {
            try {
                m_Selection.select(entity);

                if(ImGui.menuItem("Create Empty Child"))
                    m_PendingHierarchyAction = new HierarchyAction(HierarchyActionType.CREATE_CHILD, entity.getId());

                ImGui.separator();

                if (ImGui.menuItem("Duplicate"))
                    m_PendingHierarchyAction = new HierarchyAction(HierarchyActionType.DUPLICATE, entity.getId());

                if(entity.getParent() != null)
                    if(ImGui.menuItem("Unparent"))
                        m_PendingHierarchyAction = new HierarchyAction(HierarchyActionType.UNPARENT, entity.getId());

                if (ImGui.menuItem("Delete"))
                    m_PendingHierarchyAction = new HierarchyAction(HierarchyActionType.DELETE, entity.getId());
            } finally {
                ImGui.endPopup();
            }
        }

        if (!leaf && open) {
            drawSiblingList(scene, entity, entity.getChildren());
            ImGui.treePop();
        }
    }

    private void drawEntityDropTarget(Scene scene, Entity targetParent) {
        final EntityId draggedId = ImGui.getDragDropPayload(HIERARCHY_ENTITY_PAYLOAD);
        if (draggedId == null)
            return;

        final Entity dragged = scene.findEntity(draggedId);
        if (dragged == null)
            return;

        if (!canReparent(dragged, targetParent))
            return;

        if (!ImGui.beginDragDropTarget())
            return;

        try {
            final EntityId accepted = ImGui.acceptDragDropPayload(HIERARCHY_ENTITY_PAYLOAD, EntityId.class);
            if (accepted == null)
                return;

            m_PendingHierarchyMove = new HierarchyMove(accepted, targetParent.getId(), targetParent.getChildCount());
        } finally {
            ImGui.endDragDropTarget();
        }
    }

    private void drawHierarchyDropZone(Scene scene, Entity parent, int insertionIndex) {
        final String parentKey = parent == null ? "ROOT" : parent.getId().toString();

        ImGui.invisibleButton("##HierarchyDrop_" + parentKey + "_" + insertionIndex, ImGui.getContentRegionAvailX(), HIERARCHY_DROP_ZONE_HEIGHT);

        final EntityId draggedId = ImGui.getDragDropPayload(HIERARCHY_ENTITY_PAYLOAD);
        if (draggedId == null)
            return;

        final Entity dragged = scene.findEntity(draggedId);
        if (dragged == null)
            return;

        if (!canReparent(dragged, parent))
            return;

        if (ImGui.isItemHovered()) {
            final ImVec2 min = ImGui.getItemRectMin();
            final ImVec2 max = ImGui.getItemRectMax();

            final float y = (min.y + max.y) * 0.5f;

            final ImDrawList drawList = ImGui.getWindowDrawList();
            drawList.addLine(min.x, y, max.x, y, ImColor.rgba(90, 160, 255, 255), 2.0f);
        }

        if (!ImGui.beginDragDropTarget())
            return;

        try {
            final EntityId accepted = ImGui.acceptDragDropPayload(HIERARCHY_ENTITY_PAYLOAD, EntityId.class);
            if (accepted == null)
                return;

            m_PendingHierarchyMove = new HierarchyMove(accepted, parent == null ? null : parent.getId(), insertionIndex);
        } finally {
            ImGui.endDragDropTarget();
        }
    }

    private void commitPendingEdits(Scene scene) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        m_SceneViewport.commitPendingEdit(scene, m_History);
        m_Inspector.commitPendingEdits(m_Assets, scene, m_History);
    }

    private void applyPendingHierarchyAction(Scene scene) {
        final HierarchyAction action = m_PendingHierarchyAction;
        m_PendingHierarchyAction = null;

        if (action == null)
            return;

        final Entity entity = scene.findEntity(action.entityId());
        if (entity == null)
            return;

        switch (action.type()) {
            case CREATE_CHILD -> {
                commitPendingEdits(scene);

                final Entity child = m_Authoring.createEmpty(scene);
                child.setParent(entity);

                recordCreatedEntity(scene, child, "Create Child");
            }

            case DUPLICATE -> duplicateEntity(scene, entity);
            case UNPARENT -> m_PendingHierarchyMove = new HierarchyMove(entity.getId(), null, scene.getRootEntityCount());
            case DELETE -> deleteEntity(scene, entity);
        }
    }

    private void applyPendingHierarchyMove(Scene scene) {
        final HierarchyMove move = m_PendingHierarchyMove;
        m_PendingHierarchyMove = null;

        if (move == null)
            return;

        commitPendingEdits(scene);

        final Entity entity = scene.findEntity(move.entityId());
        if (entity == null)
            return;

        final Entity parent;
        if (move.parentId() == null) {
            parent = null;
        } else {
            parent = scene.findEntity(move.parentId());
            if (parent == null)
                return;
        }

        if (!canReparent(entity, parent))
            return;

        int insertionIndex = move.insertionIndex();
        if (entity.getParent() == parent && entity.getSiblingIndex() < insertionIndex)
            insertionIndex--;

        final HierarchyState before = HierarchyState.capture(entity);
        final boolean changingParent = entity.getParent() != parent;

        if (changingParent)
            m_Authoring.reparentPreserveWorld(scene, entity, parent, insertionIndex);
        else
            entity.setSiblingIndex(insertionIndex);

        final HierarchyState after = HierarchyState.capture(entity);
        m_History.recordExecuted(new HierarchyChangeCommand(changingParent ? "Reparent Entity" : "Reorder Entity", scene, entity.getId(), before, after));

        m_Selection.select(entity);
    }

    private void drawAssets(Engine engine) {
        final boolean visible = ImGui.begin("Assets");

        try {
            if (!visible)
                return;

            ImGui.text("Loaded Assets: " + engine.getAssets().getAssetCount());
            ImGui.text("Catalog Entries: " + engine.getAssets().getCatalog().getEntryCount());
        } finally {
            ImGui.end();
        }
    }

    private static boolean canReparent(Entity entity, Entity parent) {
        if (parent == null)
            return true;

        if (entity == parent)
            return false;

        Entity ancestor = parent;
        while (ancestor != null) {
            if (ancestor == entity)
                return false;

            ancestor = ancestor.getParent();
        }

        return true;
    }

    private enum HierarchyActionType {
        CREATE_CHILD,
        DUPLICATE,
        UNPARENT,
        DELETE
    }

    private record HierarchyAction(HierarchyActionType type, EntityId entityId) {
        private HierarchyAction {
            if (type == null)
                throw new IllegalArgumentException("Hierarchy Action Type cannot be null!");

            if (entityId == null)
                throw new IllegalArgumentException("Hierarchy Action Entity ID cannot be null!");
        }
    }

    private record HierarchyMove(EntityId entityId, EntityId parentId, int insertionIndex) {
        private HierarchyMove {
            if (entityId == null)
                throw new IllegalArgumentException("Dragged Entity ID cannot be null!");

            if (insertionIndex < 0)
                throw new IllegalArgumentException("Hierarchy insertion index cannot be negative!");
        }
    }

}