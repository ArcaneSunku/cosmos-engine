package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.editor.history.EditorHistory;
import atomixsoft.dev.cosmos.editor.selection.EditorSelection;
import atomixsoft.dev.cosmos.editor.selection.ScenePickRequest;
import atomixsoft.dev.cosmos.editor.tool.EditorToolState;
import atomixsoft.dev.cosmos.editor.tool.TransformSpace;
import atomixsoft.dev.cosmos.editor.tool.TransformTool;
import atomixsoft.dev.cosmos.render.Framebuffer;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.spatial.Transform;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiStyleVar;
import org.joml.Matrix4f;

public final class SceneViewportPanel {

    private final Framebuffer m_Framebuffer;
    private final TransformGizmo m_Gizmo;
    private final Matrix4f m_ViewMatrix;

    private final float[] m_SnapValue;

    private ScenePickRequest m_PickRequest;

    private float m_ImageX;
    private float m_ImageY;
    private float m_ImageWidth;
    private float m_ImageHeight;

    private boolean m_Visible;
    private boolean m_Hovered;
    private boolean m_Focused;

    public SceneViewportPanel() {
        m_Framebuffer = new Framebuffer();
        m_Gizmo = new TransformGizmo();
        m_ViewMatrix = new Matrix4f();

        m_SnapValue = new float[1];

        m_PickRequest = null;

        m_ImageX = 0.0f;
        m_ImageY = 0.0f;
        m_ImageWidth = 0.0f;
        m_ImageHeight = 0.0f;

        m_Visible = false;
        m_Hovered = false;
        m_Focused = false;
    }

    public void initialize() {
        m_Framebuffer.create(1, 1);
    }

    public void draw(Scene scene, Camera camera, Transform cameraTransform, EditorSelection selection, EditorToolState tools, EditorHistory history, boolean temporarySnap) {
        if (camera == null)
            throw new IllegalArgumentException("Camera cannot be null!");

        if (cameraTransform == null)
            throw new IllegalArgumentException("Camera Transform cannot be null!");

        if (selection == null)
            throw new IllegalArgumentException("Editor Selection cannot be null!");

        if (tools == null)
            throw new IllegalArgumentException("Editor Tool State cannot be null!");

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 4.0f, 4.0f);

        final boolean visible = ImGui.begin("Scene");
        try {
            m_Visible = visible;
            if (!visible) {
                commitPendingEdit(scene, history);
                resetInteractionState();
                return;
            }

            drawToolbar(tools);

            final float availableWidth = ImGui.getContentRegionAvailX();
            final float availableHeight = ImGui.getContentRegionAvailY();

            if (availableWidth <= 0.0f || availableHeight <= 0.0f) {
                commitPendingEdit(scene, history);
                resetImageState();

                m_Focused = ImGui.isWindowFocused();
                return;
            }

            final int framebufferWidth = Math.max(1, Math.round(availableWidth));
            final int framebufferHeight = Math.max(1, Math.round(availableHeight));

            if (m_Framebuffer.getWidth() != framebufferWidth || m_Framebuffer.getHeight() != framebufferHeight)
                m_Framebuffer.resize(framebufferWidth, framebufferHeight);

            camera.resize(framebufferWidth, framebufferHeight);

            m_ImageX = ImGui.getCursorScreenPosX();
            m_ImageY = ImGui.getCursorScreenPosY();

            m_ImageWidth = availableWidth;
            m_ImageHeight = availableHeight;

            ImGui.image(m_Framebuffer.getColorAttachmentId(), availableWidth, availableHeight, 0.0f, 1.0f, 1.0f, 0.0f);

            m_Hovered = ImGui.isItemHovered();
            final boolean imageClicked = ImGui.isItemClicked(ImGuiMouseButton.Left);
            m_Focused = ImGui.isWindowFocused();

            cameraTransform.getInverseMatrix(m_ViewMatrix);

            final Entity selected = selection.getEntity();
            m_Gizmo.draw(scene, selected, camera, m_ViewMatrix, tools, history, m_ImageX, m_ImageY, m_ImageWidth, m_ImageHeight, temporarySnap);

            if(imageClicked && !m_Gizmo.isInteracting())
                queuePickRequest();
        } finally {
            ImGui.end();
            ImGui.popStyleVar();
        }
    }

    public void hide(Scene scene, EditorHistory history) {
        commitPendingEdit(scene, history);

        m_Visible = false;
        m_Hovered = false;
        m_Focused = false;

        m_PickRequest = null;
    }

    public void dispose() {
        m_Framebuffer.dispose();

        m_Visible = false;
        m_Hovered = false;
        m_Focused = false;

        m_PickRequest = null;
    }

    public ScenePickRequest consumePickRequest() {
        final ScenePickRequest request = m_PickRequest;
        m_PickRequest = null;

        return request;
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

    public boolean isGizmoInteracting() {
        return m_Gizmo.isInteracting();
    }

    public void commitPendingEdit(Scene scene, EditorHistory history) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (history == null)
            throw new IllegalArgumentException("Editor History cannot be null!");

        m_Gizmo.commitPendingEdit(scene, history);
    }

    public float getImageX() {
        return m_ImageX;
    }

    public float getImageY() {
        return m_ImageY;
    }

    public float getImageWidth() {
        return m_ImageWidth;
    }

    public float getImageHeight() {
        return m_ImageHeight;
    }

    private void queuePickRequest() {
        if (m_ImageWidth <= 0.0f || m_ImageHeight <= 0.0f)
            return;

        final float mouseX = ImGui.getMousePosX();
        final float mouseY = ImGui.getMousePosY();

        final float localX = mouseX - m_ImageX;
        final float localY = mouseY - m_ImageY;

        if (localX < 0.0f || localY < 0.0f || localX >= m_ImageWidth || localY >= m_ImageHeight)
            return;

        final int framebufferWidth = m_Framebuffer.getWidth();
        final int framebufferHeight = m_Framebuffer.getHeight();

        int pixelX = (int) (localX / m_ImageWidth * framebufferWidth);
        int pixelYFromTop = (int) (localY / m_ImageHeight * framebufferHeight);

        pixelX = Math.max(0, Math.min(framebufferWidth - 1, pixelX));
        pixelYFromTop = Math.max(0, Math.min(framebufferHeight - 1, pixelYFromTop));

        final int pixelY = framebufferHeight - 1 - pixelYFromTop;
        m_PickRequest = new ScenePickRequest(pixelX, pixelY);
    }

    private void drawToolbar(EditorToolState tools) {
        if (ImGui.radioButton("Select (Q)", tools.getTransformTool() == TransformTool.NONE))
            tools.setTransformTool(TransformTool.NONE);

        ImGui.sameLine();

        if (ImGui.radioButton("Move (W)", tools.getTransformTool() == TransformTool.TRANSLATE))
            tools.setTransformTool(TransformTool.TRANSLATE);

        ImGui.sameLine();

        if (ImGui.radioButton("Rotate (E)", tools.getTransformTool() == TransformTool.ROTATE))
            tools.setTransformTool(TransformTool.ROTATE);

        ImGui.sameLine();

        if (ImGui.radioButton("Scale (R)", tools.getTransformTool() == TransformTool.SCALE))
            tools.setTransformTool(TransformTool.SCALE);

        ImGui.sameLine();

        if (ImGui.button(tools.getTransformSpace() == TransformSpace.LOCAL ? "Local" : "World"))
            tools.toggleTransformSpace();

        ImGui.sameLine();

        if (ImGui.button(tools.isSnapEnabled() ? "Snap: On" : "Snap: Off"))
            tools.toggleSnap();

        ImGui.sameLine();
        drawSnapValue(tools);
    }

    private void drawSnapValue(EditorToolState tools) {
        switch (tools.getTransformTool()) {
            case TRANSLATE -> {
                m_SnapValue[0] = tools.getTranslationSnap();

                ImGui.setNextItemWidth(70.0f);

                if (ImGui.dragFloat("##TranslationSnap", m_SnapValue, 0.05f, 0.01f, 1000.0f, "%.2f"))
                    tools.setTranslationSnap(Math.max(0.01f, m_SnapValue[0]));

                ImGui.sameLine();
                ImGui.textDisabled("units");
            }

            case ROTATE -> {
                m_SnapValue[0] = tools.getRotationSnap();

                ImGui.setNextItemWidth(70.0f);

                if (ImGui.dragFloat("##RotationSnap", m_SnapValue, 1.0f, 1.0f, 180.0f, "%.0f"))
                    tools.setRotationSnap(Math.max(1.0f, m_SnapValue[0]));

                ImGui.sameLine();
                ImGui.textDisabled("deg");
            }

            case SCALE -> {
                m_SnapValue[0] = tools.getScaleSnap();

                ImGui.setNextItemWidth(70.0f);

                if (ImGui.dragFloat("##ScaleSnap", m_SnapValue, 0.01f, 0.01f, 10.0f, "%.2f"))
                    tools.setScaleSnap(Math.max(0.01f, m_SnapValue[0]));
            }

            case NONE -> ImGui.textDisabled("Hold Ctrl for snapping");
        }
    }

    private void resetInteractionState() {
        resetImageState();

        m_Focused = false;
    }

    private void resetImageState() {
        m_Hovered = false;

        m_ImageX = 0.0f;
        m_ImageY = 0.0f;
        m_ImageWidth = 0.0f;
        m_ImageHeight = 0.0f;
    }

}