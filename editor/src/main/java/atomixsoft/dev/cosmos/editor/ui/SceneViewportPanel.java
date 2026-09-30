package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.render.Framebuffer;

import imgui.ImGui;
import imgui.flag.ImGuiStyleVar;

public final class SceneViewportPanel {

    private final Framebuffer m_Framebuffer;

    private boolean m_Visible;
    private boolean m_Hovered;
    private boolean m_Focused;

    public SceneViewportPanel() {
        m_Framebuffer = new Framebuffer();

        m_Visible = false;
        m_Hovered = false;
        m_Focused = false;
    }

    public void initialize() {
        m_Framebuffer.create(1, 1);
    }

    public void draw() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);
        final boolean visible = ImGui.begin("Scene");

        try {
            m_Visible = visible;

            if (!visible) {
                m_Hovered = false;
                m_Focused = false;
                return;
            }

            final float availableWidth = ImGui.getContentRegionAvailX();
            final float availableHeight = ImGui.getContentRegionAvailY();

            if (availableWidth <= 0.0f || availableHeight <= 0.0f) {
                m_Hovered = false;
                m_Focused = ImGui.isWindowFocused();

                return;
            }

            final int framebufferWidth = Math.max(1, Math.round(availableWidth));
            final int framebufferHeight = Math.max(1, Math.round(availableHeight));

            if (m_Framebuffer.getWidth() != framebufferWidth || m_Framebuffer.getHeight() != framebufferHeight)
                m_Framebuffer.resize(framebufferWidth, framebufferHeight);

            ImGui.image(m_Framebuffer.getColorAttachmentId(), availableWidth, availableHeight, 0.0f, 1.0f, 1.0f, 0.0f);

            m_Hovered = ImGui.isItemHovered();
            m_Focused = ImGui.isWindowFocused();
        } finally {
            ImGui.end();

            ImGui.popStyleVar();
        }
    }

    public void hide() {
        m_Visible = false;
        m_Hovered = false;
        m_Focused = false;
    }

    public void dispose() {
        m_Framebuffer.dispose();

        m_Visible = false;
        m_Hovered = false;
        m_Focused = false;
    }

    public Framebuffer getFramebuffer() {
        return m_Framebuffer;
    }

    public boolean isVisible() {
        return m_Visible;
    }

    public boolean isHovered() {
        return m_Hovered;
    }

    public boolean isFocused() {
        return m_Focused;
    }

}