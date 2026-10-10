package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.render.Framebuffer;
import imgui.ImGui;
import imgui.flag.ImGuiStyleVar;

public final class GameViewportPanel {

    private final Framebuffer m_Framebuffer;

    private boolean m_Visible;
    private boolean m_Focused;

    public GameViewportPanel() {
        m_Framebuffer = new Framebuffer();

        m_Visible = false;
        m_Focused = false;
    }

    public void initialize() {
        m_Framebuffer.create(1, 1);
    }

    public void draw(boolean hasPrimaryCamera) {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);
        final boolean visible = ImGui.begin("Game");

        try {
            m_Visible = visible;

            if(!visible) {
                m_Focused = false;
                return;
            }

            final float availWidth = ImGui.getContentRegionAvailX();
            final float availHeight = ImGui.getContentRegionAvailY();

            if(availWidth <= 0.0f || availHeight <= 0.0f) {
                m_Focused = ImGui.isWindowFocused();
                return;
            }

            final int framebufferWidth = Math.max(1, Math.round(availWidth));
            final int framebufferHeight = Math.max(1, Math.round(availHeight));

            if(m_Framebuffer.getWidth() != framebufferWidth || m_Framebuffer.getHeight() != framebufferHeight)
                m_Framebuffer.resize(framebufferWidth, framebufferHeight);

            if(hasPrimaryCamera)
                ImGui.image(m_Framebuffer.getColorAttachmentId(), availWidth, availHeight, 0.0f, 1.0f, 1.0f, 0.0f);
            else
                ImGui.textDisabled("No Primary Camera!");

            m_Focused = ImGui.isWindowFocused();
        } finally {
            ImGui.end();
            ImGui.popStyleVar();
        }
    }

    public void hide() {
        m_Visible = false;
        m_Focused = false;
    }

    public void dispose() {
        m_Framebuffer.dispose();

        m_Visible = false;
        m_Focused = false;
    }

    public Framebuffer getFramebuffer() {
        return m_Framebuffer;
    }

    public boolean isVisible() {
        return m_Visible;
    }

    public boolean isFocused() {
        return m_Focused;
    }

}
