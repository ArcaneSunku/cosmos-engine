package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.editor.scene.SceneAuthoring;
import atomixsoft.dev.cosmos.editor.settings.EditorSettings;
import atomixsoft.dev.cosmos.render.Framebuffer;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiTreeNodeFlags;

import imgui.internal.flag.ImGuiDockNodeFlags;
import imgui.type.ImInt;

public final class EditorWorkspace {

    private static final int WORKSPACE_LAYOUT_VERSION = 1;

    private final SceneViewportPanel m_SceneViewport;
    private final GameViewportPanel m_GameViewport;
    private final InspectorPanel m_Inspector;

    private SceneAuthoring m_Authoring;
    private EditorSettings m_Settings;

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

    public void draw(Engine engine, Scene scene) {
        if(m_Authoring == null)
            throw new IllegalStateException("Editor Workspace has not been initialized!");

        if (engine == null)
            throw new IllegalArgumentException("Engine cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        drawMainMenu(engine, scene);

        final ImGuiViewport viewport = ImGui.getMainViewport();
        final int dockSpaceId = ImGui.dockSpaceOverViewport(0, viewport, 0);

        if(!m_DefaultLayoutBuilt || m_ResetLayoutRequested) {
            buildDefaultLayout(dockSpaceId, viewport);

            m_Settings.setWorkspaceLayoutVersion(WORKSPACE_LAYOUT_VERSION);
            m_DefaultLayoutBuilt = true;
            m_ResetLayoutRequested = false;
        }

        if(m_ShowScene) m_SceneViewport.draw();
        else m_SceneViewport.hide();

        if(m_ShowGame) m_GameViewport.draw(scene.hasPrimaryCamera());
        else m_GameViewport.hide();

        if (m_ShowHierarchy) drawHierarchy(scene);
        if (m_ShowInspector) m_Inspector.draw(engine, scene, m_Authoring);
        if (m_ShowAssets) drawAssets(engine);
    }

    public void dispose() {
        clearSelection();

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

        m_Authoring = null;
        m_Settings = null;

        if(failure == null)
            return;

        if(failure instanceof RuntimeException exception)
            throw exception;

        throw (Error) failure;
    }

    public void clearSelection() {
        m_Inspector.clearSelection();
    }

    public Framebuffer getSceneFramebuffer() {
        return m_SceneViewport.getFramebuffer();
    }

    public Framebuffer getGameFramebuffer() {
        return m_GameViewport.getFramebuffer();
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

            if(ImGui.beginMenu("Entity")) {
                try {
                    if(ImGui.menuItem("Create Empty"))
                        m_Inspector.select(m_Authoring.createEmpty(scene));

                    if(ImGui.menuItem("Create Cube"))
                        m_Inspector.select(m_Authoring.createCube(scene));

                    if(ImGui.menuItem("Create Camera"))
                        m_Inspector.select(m_Authoring.createCamera(scene));

                    final Entity selected = m_Inspector.getSelectedEntity();
                    if(selected != null) {
                        ImGui.separator();

                        if(ImGui.menuItem("Delete Selected")) {
                            m_Authoring.deleteEntity(scene, selected);
                            m_Inspector.clearSelection();
                        }
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
        try {
            if (!visible)
                return;

            if(scene.getEntityCount() == 0) {
                ImGui.textDisabled("Scene is Empty.");
                return;
            }

            ImGui.text(scene.getName());
            ImGui.separator();

            for (Entity entity : scene.getRootEntities())
                drawEntityNode(entity);

        } finally {
            ImGui.end();
        }
    }

    private void drawEntityNode(Entity entity) {
        int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.SpanAvailWidth;
        if (m_Inspector.isSelected(entity))
            flags |= ImGuiTreeNodeFlags.Selected;

        final boolean leaf = entity.getChildCount() == 0;
        if (leaf)
            flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;

        final boolean open = ImGui.treeNodeEx(entity.getId().toString(), flags, entity.getName());

        if (ImGui.isItemClicked()) m_Inspector.select(entity);

        if (!leaf && open) {
            for (Entity child : entity.getChildren())
                drawEntityNode(child);

            ImGui.treePop();
        }
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

}