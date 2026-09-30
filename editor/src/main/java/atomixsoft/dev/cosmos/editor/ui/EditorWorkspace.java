package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.render.Framebuffer;
import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.spatial.Transform;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiTreeNodeFlags;

import imgui.internal.flag.ImGuiDockNodeFlags;
import imgui.type.ImInt;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public final class EditorWorkspace {

    private final SceneViewportPanel m_SceneViewport;
    private final InspectorPanel m_Inspector;

    private boolean m_DefaultLayoutBuilt;
    private boolean m_ResetLayoutRequested;

    private boolean m_ShowHierarchy;
    private boolean m_ShowInspector;
    private boolean m_ShowAssets;
    private boolean m_ShowScene;

    public EditorWorkspace() {
        m_SceneViewport = new SceneViewportPanel();
        m_Inspector = new InspectorPanel();

        m_DefaultLayoutBuilt = false;
        m_ResetLayoutRequested = false;

        m_ShowHierarchy = true;
        m_ShowInspector = true;
        m_ShowAssets = true;
        m_ShowScene = true;
    }

    public void initialize() {
        m_SceneViewport.initialize();
    }

    public void draw(Engine engine, Scene scene) {
        if (engine == null)
            throw new IllegalArgumentException("Engine cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        drawMainMenu(engine);

        final ImGuiViewport viewport = ImGui.getMainViewport();
        final int dockSpaceId = ImGui.dockSpaceOverViewport(0, viewport, 0);

        if(!m_DefaultLayoutBuilt || m_ResetLayoutRequested) {
            buildDefaultLayout(dockSpaceId, viewport);

            m_DefaultLayoutBuilt = true;
            m_ResetLayoutRequested = false;
        }

        if(m_ShowScene) m_SceneViewport.draw();
        else m_SceneViewport.hide();

        if (m_ShowHierarchy) drawHierarchy(scene);
        if (m_ShowInspector) m_Inspector.draw(engine, scene);
        if (m_ShowAssets) drawAssets(engine);
    }

    public void dispose() {
        clearSelection();
        m_SceneViewport.dispose();
    }

    public void clearSelection() {
        m_Inspector.clearSelection();
    }

    public Framebuffer getSceneFramebuffer() {
        return m_SceneViewport.getFramebuffer();
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
        imgui.internal.ImGui.dockBuilderFinish(dockSpaceId);
    }

    private void drawMainMenu(Engine engine) {
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

            if (ImGui.beginMenu("View")) {
                try {
                    if(ImGui.menuItem("Scene", m_ShowScene))
                        m_ShowScene = !m_ShowScene;

                    if (ImGui.menuItem("Hierarchy", m_ShowHierarchy))
                        m_ShowHierarchy = !m_ShowHierarchy;

                    if (ImGui.menuItem("Inspector", m_ShowInspector))
                        m_ShowInspector = !m_ShowInspector;

                    if (ImGui.menuItem("Assets", m_ShowAssets))
                        m_ShowAssets = !m_ShowAssets;

                    if(ImGui.menuItem("Reset Layout"))
                        m_ResetLayoutRequested = true;

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