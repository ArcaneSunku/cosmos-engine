package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.camera.OrthographicCamera;
import atomixsoft.dev.cosmos.editor.history.EditorHistory;
import atomixsoft.dev.cosmos.editor.history.TransformState;
import atomixsoft.dev.cosmos.editor.history.command.TransformEntityCommand;
import atomixsoft.dev.cosmos.editor.tool.EditorToolState;
import atomixsoft.dev.cosmos.editor.tool.TransformSpace;
import atomixsoft.dev.cosmos.editor.tool.TransformTool;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.spatial.Transform;

import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TransformGizmo {

    private final Matrix4f m_WorldMatrix;
    private final Matrix4f m_LocalMatrix;
    private final Matrix4f m_ParentInverse;

    private final float[] m_ViewValues;
    private final float[] m_ProjectionValues;
    private final float[] m_WorldValues;

    private final Vector3f m_LocalPosition;
    private final Quaternionf m_LocalRotation;
    private final Vector3f m_LocalScale;

    private final float[] m_TranslationSnap;
    private final float[] m_RotationSnap;
    private final float[] m_ScaleSnap;

    private EntityId m_EditEntityId;
    private TransformState m_EditStart;
    private String m_EditCommandName;

    private boolean m_Over;
    private boolean m_Using;

    public TransformGizmo() {
        m_WorldMatrix = new Matrix4f();
        m_LocalMatrix = new Matrix4f();
        m_ParentInverse = new Matrix4f();

        m_ViewValues = new float[16];
        m_ProjectionValues = new float[16];

        m_WorldValues = new float[16];

        m_LocalPosition = new Vector3f();
        m_LocalRotation = new Quaternionf();
        m_LocalScale = new Vector3f();

        m_TranslationSnap = new float[3];
        m_RotationSnap = new float[1];
        m_ScaleSnap = new float[1];

        m_EditEntityId = null;
        m_EditStart = null;
        m_EditCommandName = null;

        m_Over = false;
        m_Using = false;
    }

    public void draw(Scene scene, Entity entity, Camera camera, Matrix4fc viewMatrix, EditorToolState tools, EditorHistory history,
                     float x, float y, float width, float height, boolean temporarySnap) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (history == null)
            throw new IllegalArgumentException("Editor History cannot be null!");

        if (entity == null || tools.getTransformTool() == TransformTool.NONE) {
            commitPendingEdit(scene, history);
            return;
        }

        if (camera == null)
            throw new IllegalArgumentException("Camera cannot be null!");

        if (viewMatrix == null)
            throw new IllegalArgumentException("View Matrix cannot be null!");

        if (width <= 0.0f || height <= 0.0f) {
            commitPendingEdit(scene, history);
            return;
        }

        viewMatrix.get(m_ViewValues);
        camera.getProjection().get(m_ProjectionValues);
        entity.getWorldMatrix(m_WorldMatrix).get(m_WorldValues);

        ImGuizmo.setDrawList();
        ImGuizmo.setRect(x, y, width, height);
        ImGuizmo.setOrthographic(camera instanceof OrthographicCamera);

        final int operation = getOperation(tools.getTransformTool());
        final int mode = tools.getTransformSpace() == TransformSpace.LOCAL ? Mode.LOCAL : Mode.WORLD;
        final boolean snapping = tools.isSnapEnabled() || temporarySnap;

        final boolean wasUsing = m_Using;

        if (snapping)
            ImGuizmo.manipulate(m_ViewValues, m_ProjectionValues, operation, mode, m_WorldValues, null, getSnapValues(tools));
        else
            ImGuizmo.manipulate(m_ViewValues, m_ProjectionValues, operation, mode, m_WorldValues);

        m_Over = ImGuizmo.isOver();
        m_Using = ImGuizmo.isUsing();

        if (m_Using && !wasUsing)
            beginEdit(entity, tools.getTransformTool());

        if(m_Using) {
            m_WorldMatrix.set(m_WorldValues);

            final Entity parent = entity.getParent();
            if (parent == null) {
                m_LocalMatrix.set(m_WorldMatrix);
            } else {
                parent.getWorldMatrix(m_ParentInverse).invert();
                m_LocalMatrix.set(m_ParentInverse).mul(m_WorldMatrix);
            }

            applyLocalMatrix(entity.getTransform(), tools.getTransformTool());
        }

        if (!m_Using && wasUsing)
            finishEdit(scene, history);
    }

    public boolean isOver() {
        return m_Over;
    }

    public boolean isUsing() {
        return m_Using;
    }

    public boolean isInteracting() {
        return m_Over || m_Using;
    }

    public void commitPendingEdit(Scene scene, EditorHistory history) {
        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (history == null)
            throw new IllegalArgumentException("Editor History cannot be null!");

        finishEdit(scene, history);

        m_Over = false;
        m_Using = false;
    }

    private float[] getSnapValues(EditorToolState tools) {
        return switch (tools.getTransformTool()) {
            case TRANSLATE -> {
                final float value = tools.getTranslationSnap();

                m_TranslationSnap[0] = value;
                m_TranslationSnap[1] = value;
                m_TranslationSnap[2] = value;

                yield m_TranslationSnap;
            }

            case ROTATE -> {
                m_RotationSnap[0] = tools.getRotationSnap();
                yield m_RotationSnap;
            }

            case SCALE -> {
                m_ScaleSnap[0] = tools.getScaleSnap();
                yield m_ScaleSnap;
            }

            case NONE -> throw new IllegalStateException("NONE does not have snap values!");
        };
    }

    private void applyLocalMatrix(Transform transform, TransformTool tool) {
        switch (tool) {
            case TRANSLATE -> {
                m_LocalMatrix.getTranslation(m_LocalPosition);
                transform.setPosition(m_LocalPosition);
            }

            case ROTATE -> {
                m_LocalMatrix.getUnnormalizedRotation(m_LocalRotation);
                m_LocalRotation.normalize();

                transform.setRotation(m_LocalRotation);
            }

            case SCALE -> {
                m_LocalMatrix.getScale(m_LocalScale);
                transform.setScale(m_LocalScale);
            }

            case NONE -> {
            }
        }
    }

    private void beginEdit(Entity entity, TransformTool tool) {
        m_EditEntityId = entity.getId();
        m_EditStart = TransformState.capture(entity.getTransform());

        m_EditCommandName = switch (tool) {
            case TRANSLATE -> "Move Entity";
            case ROTATE -> "Rotate Entity";
            case SCALE -> "Scale Entity";
            case NONE -> throw new IllegalStateException("Cannot begin a Transform edit with no active tool!");
        };
    }

    private void finishEdit(Scene scene, EditorHistory history) {
        if (m_EditEntityId == null || m_EditStart == null || m_EditCommandName == null)
            return;

        final Entity entity = scene.findEntity(m_EditEntityId);
        if (entity != null)
            history.recordExecuted(new TransformEntityCommand(m_EditCommandName, scene, m_EditEntityId, m_EditStart, TransformState.capture(entity.getTransform())));

        m_EditEntityId = null;
        m_EditStart = null;
        m_EditCommandName = null;
    }

    private static int getOperation(TransformTool tool) {
        return switch (tool) {
            case TRANSLATE -> Operation.TRANSLATE;
            case ROTATE -> Operation.ROTATE;
            case SCALE -> Operation.SCALE;
            case NONE -> throw new IllegalStateException("NONE does not have an ImGuizmo operation!");
        };
    }

}