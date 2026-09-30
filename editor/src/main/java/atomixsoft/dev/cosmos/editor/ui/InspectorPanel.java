package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.camera.OrthographicCamera;
import atomixsoft.dev.cosmos.camera.PerspectiveCamera;
import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;
import atomixsoft.dev.cosmos.spatial.Transform;

import imgui.ImGui;
import imgui.type.ImString;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class InspectorPanel {

    private static final float POSITION_DRAG_SPEED = 0.05f;
    private static final float ROTATION_DRAG_SPEED = 0.5f;
    private static final float SCALE_DRAG_SPEED = 0.02f;

    private static final float MIN_PERSPECTIVE_NEAR = 0.0001f;
    private static final float MIN_CLIP_DISTANCE = 0.0001f;

    private static final float MIN_ORTHOGRAPHIC_SIZE = 0.01f;

    private final ImString m_NameBuffer;

    private final float[] m_PositionValues;
    private final float[] m_RotationValues;
    private final float[] m_ScaleValues;

    private final float[] m_ScalarValue;
    private final float[] m_ClipValues;

    private final Vector3f m_EulerRadians;

    private Entity m_SelectedEntity;

    public InspectorPanel() {
        m_NameBuffer = new ImString(256);

        m_PositionValues = new float[3];
        m_RotationValues = new float[3];

        m_ScaleValues = new float[3];
        m_ScalarValue = new float[1];
        m_ClipValues = new float[2];

        m_EulerRadians = new Vector3f();

        m_SelectedEntity = null;
    }

    public void draw(Engine engine, Scene scene) {
        if (engine == null)
            throw new IllegalArgumentException("Engine cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (m_SelectedEntity != null && !m_SelectedEntity.isValid())
            clearSelection();

        final boolean visible = ImGui.begin("Inspector");
        try {
            if (!visible)
                return;

            if (m_SelectedEntity == null) {
                ImGui.textDisabled("No Entity selected.");
                return;
            }

            drawEntityHeader(m_SelectedEntity);

            ImGui.separatorText("Transform");
            drawTransform(m_SelectedEntity.getTransform());
            drawComponents(engine, scene, m_SelectedEntity);
        } finally {
            ImGui.end();
        }
    }

    public void select(Entity entity) {
        if (entity == null) {
            clearSelection();
            return;
        }

        if (!entity.isValid())
            throw new IllegalArgumentException("Cannot select an invalid Entity!");

        if (m_SelectedEntity == entity)
            return;

        m_SelectedEntity = entity;
        m_NameBuffer.set(entity.getName(), true);
    }

    public void clearSelection() {
        m_SelectedEntity = null;
        m_NameBuffer.clear();
    }

    public boolean isSelected(Entity entity) {
        return m_SelectedEntity == entity;
    }

    public Entity getSelectedEntity() {
        return m_SelectedEntity;
    }

    private void drawEntityHeader(Entity entity) {
        if (ImGui.inputText("Name", m_NameBuffer)) {
            final String name = m_NameBuffer.get();

            if (!name.isBlank() && !name.equals(entity.getName()))
                entity.setName(name);
        }

        if (ImGui.isItemDeactivatedAfterEdit() && m_NameBuffer.get().isBlank())
            m_NameBuffer.set(entity.getName(), true);

        ImGui.textDisabled("ID: " + entity.getId());

        final Entity parent = entity.getParent();
        ImGui.textDisabled("Parent: " + (parent == null ? "None" : parent.getName()));
    }

    private void drawTransform(Transform transform) {
        ImGui.textDisabled("Local Space");
        final Vector3fc position = transform.getPosition();

        m_PositionValues[0] = position.x();
        m_PositionValues[1] = position.y();
        m_PositionValues[2] = position.z();

        if (ImGui.dragFloat3("Position", m_PositionValues, POSITION_DRAG_SPEED, 0.0f, 0.0f, "%.3f"))
            transform.setPosition(m_PositionValues[0], m_PositionValues[1], m_PositionValues[2]);

        transform.getRotation().getEulerAnglesXYZ(m_EulerRadians);

        m_RotationValues[0] = (float) Math.toDegrees(m_EulerRadians.x);
        m_RotationValues[1] = (float) Math.toDegrees(m_EulerRadians.y);
        m_RotationValues[2] = (float) Math.toDegrees(m_EulerRadians.z);

        if (ImGui.dragFloat3("Rotation", m_RotationValues, ROTATION_DRAG_SPEED, 0.0f, 0.0f, "%.2f deg"))
            transform.setRotationEuler((float) Math.toRadians(m_RotationValues[0]), (float) Math.toRadians(m_RotationValues[1]), (float) Math.toRadians(m_RotationValues[2]));

        final Vector3fc scale = transform.getScale();

        m_ScaleValues[0] = scale.x();
        m_ScaleValues[1] = scale.y();
        m_ScaleValues[2] = scale.z();

        if (ImGui.dragFloat3("Scale", m_ScaleValues, SCALE_DRAG_SPEED, 0.0f, 0.0f, "%.3f"))
            transform.setScale(m_ScaleValues[0], m_ScaleValues[1], m_ScaleValues[2]);

        if (ImGui.button("Reset Transform"))
            transform.setPosition(0.0f, 0.0f, 0.0f).setRotationEuler(0.0f, 0.0f, 0.0f).setScale(1.0f);
    }

    private void drawComponents(Engine engine, Scene scene, Entity entity) {
        for (Component component : entity.getComponents()) {
            ImGui.pushID(component.getClass().getName());

            try {
                if (component instanceof CameraComponent camera) {
                    ImGui.separatorText("Camera");
                    drawCamera(scene, entity, camera);

                    continue;
                }

                if (component instanceof MeshRenderComponent renderer) {
                    ImGui.separatorText("Mesh Renderer");
                    drawMeshRenderer(engine.getAssets(), renderer);

                    continue;
                }

                ImGui.separatorText(component.getClass().getSimpleName());
                ImGui.textDisabled("No Inspector editor is available for this Component.");
            } finally {
                ImGui.popID();
            }
        }
    }

    private void drawCamera(Scene scene, Entity entity, CameraComponent component) {
        final Camera camera = component.getCamera();

        if (scene.getPrimaryCameraEntity() == entity)
            ImGui.textDisabled("Primary Camera");

        if (camera instanceof PerspectiveCamera perspective) {
            drawPerspectiveCamera(perspective);
            return;
        }

        if (camera instanceof OrthographicCamera orthographic) {
            drawOrthographicCamera(orthographic);
            return;
        }

        ImGui.textDisabled("Projection: " + camera.getClass().getSimpleName());
    }

    private void drawPerspectiveCamera(PerspectiveCamera camera) {
        ImGui.textDisabled("Projection: Perspective");
        m_ScalarValue[0] = (float) Math.toDegrees(camera.getFOV());

        if (ImGui.dragFloat("Field of View", m_ScalarValue, 0.25f, 1.0f, 179.0f, "%.1f deg"))
            camera.setPerspective((float) Math.toRadians(m_ScalarValue[0]), camera.getAspectRatio(), camera.getNearPlane(), camera.getFarPlane());

        m_ClipValues[0] = camera.getNearPlane();
        m_ClipValues[1] = camera.getFarPlane();

        if (ImGui.dragFloat2("Clip Planes", m_ClipValues, 0.01f, 0.0f, 0.0f, "%.4f")) {
            final float near = Math.max(MIN_PERSPECTIVE_NEAR, m_ClipValues[0]);
            final float far = Math.max(near + MIN_CLIP_DISTANCE, m_ClipValues[1]);

            camera.setClipPlanes(near, far);
        }

        ImGui.textDisabled("Aspect Ratio: %.3f (Viewport)".formatted(camera.getAspectRatio()));
    }

    private void drawOrthographicCamera(OrthographicCamera camera) {
        ImGui.textDisabled("Projection: Orthographic");
        m_ScalarValue[0] = camera.getVerticalSize();

        if (ImGui.dragFloat("Vertical Size", m_ScalarValue, 0.05f, MIN_ORTHOGRAPHIC_SIZE, 100000.0f, "%.3f"))
            camera.setVerticalSize(Math.max(MIN_ORTHOGRAPHIC_SIZE, m_ScalarValue[0]));

        m_ClipValues[0] = camera.getNearPlane();
        m_ClipValues[1] = camera.getFarPlane();

        if (ImGui.dragFloat2("Clip Planes", m_ClipValues, 0.05f, 0.0f, 0.0f, "%.3f")) {
            final float near = m_ClipValues[0];
            final float far = Math.max(near + MIN_CLIP_DISTANCE, m_ClipValues[1]);

            camera.setClipPlanes(near, far);
        }

        ImGui.textDisabled("Aspect Ratio: %.3f (Viewport)".formatted(camera.getAspectRatio()));
    }

    private void drawMeshRenderer(AssetManager assets, MeshRenderComponent renderer) {
        ImGui.text("Mesh:");
        ImGui.textDisabled(getAssetId(assets, Mesh.class, renderer.getMesh()));

        ImGui.text("Material:");
        ImGui.textDisabled(getAssetId(assets, Material.class, renderer.getMaterial()));
    }

    private static <T> String getAssetId(AssetManager assets, Class<T> type, T asset) {
        try {
            return assets.getKey(type, asset).getId();
        } catch (IllegalStateException exception) {
            return "<Unmanaged>";
        }
    }

}