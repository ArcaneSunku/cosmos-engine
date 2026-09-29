package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.spatial.Transform;

import imgui.ImGui;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiTreeNodeFlags;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public final class EditorWorkspace {

    private Entity m_SelectedEntity;

    private boolean m_ShowHierarchy;
    private boolean m_ShowInspector;
    private boolean m_ShowAssets;

    public EditorWorkspace() {
        m_SelectedEntity = null;

        m_ShowHierarchy = true;
        m_ShowInspector = true;
        m_ShowAssets = true;
    }

    public void draw(Engine engine, Scene scene) {
        if (engine == null)
            throw new IllegalArgumentException("Engine cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (m_SelectedEntity != null && !m_SelectedEntity.isValid())
            m_SelectedEntity = null;

        drawMainMenu(engine);

        ImGui.dockSpaceOverViewport(0, ImGui.getMainViewport(), ImGuiDockNodeFlags.PassthruCentralNode);

        if (m_ShowHierarchy) drawHierarchy(scene);
        if (m_ShowInspector) drawInspector();
        if (m_ShowAssets) drawAssets(engine);
    }

    public void clearSelection() {
        m_SelectedEntity = null;
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
                    if (ImGui.menuItem("Hierarchy", m_ShowHierarchy))
                        m_ShowHierarchy = !m_ShowHierarchy;

                    if (ImGui.menuItem("Inspector", m_ShowInspector))
                        m_ShowInspector = !m_ShowInspector;

                    if (ImGui.menuItem("Assets", m_ShowAssets))
                        m_ShowAssets = !m_ShowAssets;

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
        if (entity == m_SelectedEntity)
            flags |= ImGuiTreeNodeFlags.Selected;

        final boolean leaf = entity.getChildCount() == 0;
        if (leaf)
            flags |= ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;

        final boolean open = ImGui.treeNodeEx(entity.getId().toString(), flags, entity.getName());

        if (ImGui.isItemClicked()) m_SelectedEntity = entity;

        if (!leaf && open) {
            for (Entity child : entity.getChildren())
                drawEntityNode(child);

            ImGui.treePop();
        }
    }

    private void drawInspector() {
        final boolean visible = ImGui.begin("Inspector");
        try {
            if (!visible)
                return;

            if (m_SelectedEntity == null) {
                ImGui.text("No Entity selected.");
                return;
            }

            final Entity entity = m_SelectedEntity;

            ImGui.text(entity.getName());
            ImGui.text("ID: " + entity.getId());

            final Entity parent = entity.getParent();

            ImGui.text("Parent: " + (parent == null ? "None" : parent.getName()));
            ImGui.separatorText("Transform");

            final Transform transform = entity.getTransform();
            final Vector3fc position = transform.getPosition();
            final Quaternionfc rotation = transform.getRotation();
            final Vector3fc scale = transform.getScale();

            ImGui.text("Position: %.3f, %.3f, %.3f".formatted(position.x(), position.y(), position.z()));
            ImGui.text("Rotation: %.3f, %.3f, %.3f, %.3f".formatted(rotation.x(), rotation.y(), rotation.z(), rotation.w()));
            ImGui.text("Scale: %.3f, %.3f, %.3f".formatted(scale.x(), scale.y(), scale.z()));

            ImGui.separatorText("Components");

            if (entity.getComponents().isEmpty()) {
                ImGui.text("No optional components.");
                return;
            }

            for (Component component : entity.getComponents())
                ImGui.bulletText(component.getClass().getSimpleName());

        } finally {
            ImGui.end();
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