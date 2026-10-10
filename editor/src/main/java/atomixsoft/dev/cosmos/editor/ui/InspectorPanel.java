package atomixsoft.dev.cosmos.editor.ui;

import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.camera.OrthographicCamera;
import atomixsoft.dev.cosmos.camera.PerspectiveCamera;
import atomixsoft.dev.cosmos.editor.history.EditorHistory;
import atomixsoft.dev.cosmos.editor.history.TransformState;
import atomixsoft.dev.cosmos.editor.history.command.*;
import atomixsoft.dev.cosmos.editor.history.snapshot.CameraComponentSnapshot;
import atomixsoft.dev.cosmos.editor.history.snapshot.MeshRenderComponentSnapshot;
import atomixsoft.dev.cosmos.editor.scene.SceneAuthoring;
import atomixsoft.dev.cosmos.editor.selection.EditorSelection;
import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.EntityId;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;
import atomixsoft.dev.cosmos.spatial.Transform;

import imgui.ImGui;
import imgui.type.ImString;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public final class InspectorPanel {

    private static final float DEFAULT_PERSPECTIVE_FOV = (float) Math.toRadians(60.0);
    private static final float DEFAULT_ORTHOGRAPHIC_SIZE = 10.0f;
    private static final float DEFAULT_PERSPECTIVE_NEAR = 0.1f;

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

    private Entity m_InspectedEntity;

    private EntityId m_NameEditEntityId;
    private String m_NameEditStart;

    private EntityId m_TransformEditEntityId;
    private TransformState m_TransformEditStart;
    private String m_TransformEditName;

    private EntityId m_CameraEditEntityId;
    private CameraComponentSnapshot m_CameraEditStart;

    private EntityId m_CameraEditPrimaryId;
    private String m_CameraEditName;

    public InspectorPanel() {
        m_NameBuffer = new ImString(256);

        m_PositionValues = new float[3];
        m_RotationValues = new float[3];

        m_ScaleValues = new float[3];
        m_ScalarValue = new float[1];
        m_ClipValues = new float[2];

        m_EulerRadians = new Vector3f();

        m_InspectedEntity = null;

        m_NameEditEntityId = null;
        m_NameEditStart = null;

        m_TransformEditEntityId = null;
        m_TransformEditStart = null;
        m_TransformEditName = null;

        m_CameraEditEntityId = null;
        m_CameraEditStart = null;

        m_CameraEditPrimaryId = null;
        m_CameraEditName = null;
    }

    public void draw(Engine engine, Scene scene, SceneAuthoring authoring, EditorSelection selection, EditorHistory history) {
        if (engine == null)
            throw new IllegalArgumentException("Engine cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if(authoring == null)
            throw new IllegalArgumentException("Scene Authoring cannot be null!");

        if(selection == null)
            throw new IllegalArgumentException("Editor Selection cannot be null!");

        selection.validate();
        final Entity selected = selection.getEntity();
        syncSelection(engine.getAssets(), scene, history, selected);

        final boolean visible = ImGui.begin("Inspector");
        try {
            if (!visible) {
                commitPendingEdits(engine.getAssets(), scene, history);
                return;
            }

            if (selected == null) {
                ImGui.textDisabled("No Entity selected.");
                return;
            }

            drawEntityHeader(scene, selected, history);

            ImGui.separatorText("Transform");
            drawTransform(scene, selected, history);
            drawComponents(engine.getAssets(), scene, selected, authoring, history);
        } finally {
            ImGui.end();
        }
    }

    public void commitPendingEdits(AssetManager assets, Scene scene, EditorHistory history) {
        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (history == null)
            throw new IllegalArgumentException("Editor History cannot be null!");

        finishNameEdit(scene, history);
        finishTransformEdit(scene, history);
        finishCameraEdit(scene, assets, history);
    }

    private void syncSelection(AssetManager assets, Scene scene, EditorHistory history, Entity selected) {
        if (m_InspectedEntity == selected)
            return;

        finishNameEdit(scene, history);
        finishTransformEdit(scene, history);
        finishCameraEdit(scene, assets, history);

        m_InspectedEntity = selected;
        if (selected == null) {
            m_NameBuffer.clear();
            return;
        }

        m_NameBuffer.set(selected.getName(), true);
    }

    private void drawEntityHeader(Scene scene, Entity entity, EditorHistory history) {
        if (m_NameEditEntityId == null && !m_NameBuffer.get().equals(entity.getName()))
            m_NameBuffer.set(entity.getName(), true);

        final boolean changed = ImGui.inputText("Name", m_NameBuffer);
        if (ImGui.isItemActivated()) {
            m_NameEditEntityId = entity.getId();
            m_NameEditStart = entity.getName();
        }

        if (changed) {
            final String name = m_NameBuffer.get();
            if (!name.isBlank() && !name.equals(entity.getName()))
                entity.setName(name);
        }

        if (ImGui.isItemDeactivated())
            finishNameEdit(scene, history);

        ImGui.textDisabled("ID: " + entity.getId());

        final Entity parent = entity.getParent();
        ImGui.textDisabled("Parent: " + (parent == null ? "None" : parent.getName()));
    }

    private void drawTransform(Scene scene, Entity entity, EditorHistory history) {
        final Transform transform = entity.getTransform();

        ImGui.textDisabled("Local Space");
        final Vector3fc position = transform.getPosition();

        m_PositionValues[0] = position.x();
        m_PositionValues[1] = position.y();
        m_PositionValues[2] = position.z();

        final boolean positionChanged = ImGui.dragFloat3("Position", m_PositionValues, POSITION_DRAG_SPEED, 0.0f, 0.0f, "%.3f");

        beginTransformEditIfActivated(entity, "Move Entity");

        if (positionChanged)
            transform.setPosition(m_PositionValues[0], m_PositionValues[1], m_PositionValues[2]);

        finishTransformEditIfDeactivated(scene, history);
        transform.getRotation().getEulerAnglesXYZ(m_EulerRadians);

        m_RotationValues[0] = (float) Math.toDegrees(m_EulerRadians.x);
        m_RotationValues[1] = (float) Math.toDegrees(m_EulerRadians.y);
        m_RotationValues[2] = (float) Math.toDegrees(m_EulerRadians.z);

        final boolean rotationChanged = ImGui.dragFloat3("Rotation", m_RotationValues, ROTATION_DRAG_SPEED, 0.0f, 0.0f, "%.2f deg");
        beginTransformEditIfActivated(entity, "Rotate Entity");

        if (rotationChanged)
            transform.setRotationEuler((float) Math.toRadians(m_RotationValues[0]), (float) Math.toRadians(m_RotationValues[1]), (float) Math.toRadians(m_RotationValues[2]));

        finishTransformEditIfDeactivated(scene, history);

        final Vector3fc scale = transform.getScale();

        m_ScaleValues[0] = scale.x();
        m_ScaleValues[1] = scale.y();
        m_ScaleValues[2] = scale.z();

        final boolean scaleChanged = ImGui.dragFloat3("Scale", m_ScaleValues, SCALE_DRAG_SPEED, 0.0f, 0.0f, "%.3f");

        beginTransformEditIfActivated(entity, "Scale Entity");

        if (scaleChanged)
            transform.setScale(m_ScaleValues[0], m_ScaleValues[1], m_ScaleValues[2]);

        finishTransformEditIfDeactivated(scene, history);

        if (ImGui.button("Reset Transform")) {
            final TransformState before = TransformState.capture(transform);
            final TransformState after = TransformState.identity();

            history.execute(new TransformEntityCommand("Reset Transform", scene, entity.getId(), before, after));
        }
    }

    private void drawComponents(AssetManager assets, Scene scene, Entity selected, SceneAuthoring authoring, EditorHistory history) {
        for (Component component : List.copyOf(selected.getComponents())) {
            ImGui.pushID(component.getClass().getName());

            try {
                if (component instanceof CameraComponent camera) {
                    ImGui.separatorText("Camera");
                    drawCamera(assets, scene, selected, camera, history);

                    continue;
                }

                if (component instanceof MeshRenderComponent renderer) {
                    ImGui.separatorText("Mesh Renderer");
                    drawMeshRenderer(assets, renderer);

                    if (ImGui.button("Remove Mesh Renderer")) {
                        final EntityId primaryBefore = getPrimaryCameraId(scene);
                        final MeshRenderComponentSnapshot before = MeshRenderComponentSnapshot.capture(renderer, assets);

                        history.execute(new ComponentChangeCommand("Remove Mesh Renderer", scene, assets, selected.getId(), MeshRenderComponent.class, before, null, primaryBefore, primaryBefore));
                    }

                    continue;
                }

                ImGui.separatorText(component.getClass().getSimpleName());
                ImGui.textDisabled("No Inspector editor is available for this Component.");
            } finally {
                ImGui.popID();
            }
        }

        drawAddComponent(assets, scene, selected, authoring, history);
    }

    private void drawAddComponent(AssetManager assets, Scene scene, Entity selected, SceneAuthoring authoring, EditorHistory history) {
        ImGui.separator();

        if (ImGui.button("Add Component"))
            ImGui.openPopup("AddComponentPopup");

        if (!ImGui.beginPopup("AddComponentPopup"))
            return;

        try {
            boolean hasAvailableComponent = false;
            if (!selected.hasComponent(CameraComponent.class)) {
                hasAvailableComponent = true;

                if (ImGui.menuItem("Camera")) {
                    final EntityId primaryBefore = getPrimaryCameraId(scene);
                    final CameraComponent camera = authoring.addCameraComponent(scene, selected);
                    final EntityId primaryAfter = getPrimaryCameraId(scene);

                    history.recordExecuted(new ComponentChangeCommand("Add Camera", scene, assets, selected.getId(), CameraComponent.class, null, CameraComponentSnapshot.capture(camera), primaryBefore, primaryAfter));
                }
            }

            if (!selected.hasComponent(MeshRenderComponent.class)) {
                hasAvailableComponent = true;

                if (ImGui.menuItem("Mesh Renderer")) {
                    final EntityId primaryBefore = getPrimaryCameraId(scene);
                    final MeshRenderComponent renderer = authoring.addMeshRenderComponent(scene, selected);
                    final EntityId primaryAfter = getPrimaryCameraId(scene);

                    history.recordExecuted(new ComponentChangeCommand("Add Mesh Renderer", scene, assets, selected.getId(), MeshRenderComponent.class, null, MeshRenderComponentSnapshot.capture(renderer, assets), primaryBefore, primaryAfter));
                }
            }

            if (!hasAvailableComponent)
                ImGui.textDisabled("No Components available.");
        } finally {
            ImGui.endPopup();
        }
    }

    private void drawCamera(AssetManager assets, Scene scene, Entity entity, CameraComponent component, EditorHistory history) {
        final Camera camera = component.getCamera();
        final boolean perspective = camera instanceof PerspectiveCamera;
        final boolean orthographic = camera instanceof OrthographicCamera;

        ImGui.text("Projection:");

        if (ImGui.radioButton("Perspective", perspective)) {
            if (!perspective) {
                finishCameraEdit(scene, assets, history);

                final CameraComponentSnapshot before = CameraComponentSnapshot.capture(component);
                final EntityId primary = getPrimaryCameraId(scene);

                component.setCamera(createPerspectiveFrom(component.getCamera()));

                final CameraComponentSnapshot after = CameraComponentSnapshot.capture(component);
                history.recordExecuted(new ComponentChangeCommand("Change Camera Projection", scene, assets, entity.getId(), CameraComponent.class, before, after, primary, primary));
            }
        }

        ImGui.sameLine();

        if (ImGui.radioButton("Orthographic", orthographic)) {
            if (!orthographic) {
                finishCameraEdit(scene, assets, history);

                final CameraComponentSnapshot before = CameraComponentSnapshot.capture(component);
                final EntityId primary = getPrimaryCameraId(scene);

                component.setCamera(createOrthographicFrom(component.getCamera()));

                final CameraComponentSnapshot after = CameraComponentSnapshot.capture(component);
                history.recordExecuted(new ComponentChangeCommand("Change Camera Projection", scene, assets, entity.getId(), CameraComponent.class, before, after, primary, primary));
            }
        }

        ImGui.separator();
        final Camera currentCamera = component.getCamera();

        final EntityId primaryId = getPrimaryCameraId(scene);
        final boolean primary = entity.getId().equals(primaryId);

        if (primary) {
            ImGui.textDisabled("Primary Camera");

            if (ImGui.button("Clear Primary Camera")) {
                finishCameraEdit(scene, assets, history);
                history.execute(new PrimaryCameraCommand(scene, primaryId, null));
            }
        } else {
            if (ImGui.button("Set As Primary Camera")) {
                finishCameraEdit(scene, assets, history);
                history.execute(new PrimaryCameraCommand(scene, primaryId, entity.getId()));
            }
        }

        ImGui.separator();

        if (currentCamera instanceof PerspectiveCamera)
            drawPerspectiveCamera(scene, entity, component, assets, history);
        else if (currentCamera instanceof OrthographicCamera)
            drawOrthographicCamera(scene, entity, component, assets, history);

        ImGui.separator();

        if (ImGui.button("Remove Camera")) {
            finishCameraEdit(scene, assets, history);

            final EntityId primaryBefore = getPrimaryCameraId(scene);
            final EntityId primaryAfter = entity.getId().equals(primaryBefore) ? null : primaryBefore;

            history.execute(new ComponentChangeCommand("Remove Camera", scene, assets, entity.getId(), CameraComponent.class, CameraComponentSnapshot.capture(component), null, primaryBefore, primaryAfter));
        }
    }

    private void drawPerspectiveCamera(Scene scene, Entity selected, CameraComponent component, AssetManager assets, EditorHistory history) {
        ImGui.textDisabled("Projection: Perspective");
        PerspectiveCamera camera = (PerspectiveCamera) component.getCamera();

        m_ScalarValue[0] = (float) Math.toDegrees(camera.getFOV());

        final boolean changedPOV = ImGui.dragFloat("Field of View", m_ScalarValue, 0.25f, 1.0f, 179.0f, "%.1f deg");
        beginCameraEditIfActivated(scene, selected, component, "Change Camera FOV");

        if (changedPOV)
            camera.setPerspective((float) Math.toRadians(m_ScalarValue[0]), camera.getAspectRatio(), camera.getNearPlane(), camera.getFarPlane());

        finishCameraEditIfDeactivated(scene, assets, history);

        m_ClipValues[0] = camera.getNearPlane();
        m_ClipValues[1] = camera.getFarPlane();

        final boolean changedClip = ImGui.dragFloat2("Clip Planes", m_ClipValues, 0.01f, 0.0f, 0.0f, "%.4f");
        beginCameraEditIfActivated(scene, selected, component, "Change Camera Clip Planes");

        if (changedClip) {
            final float near = Math.max(MIN_PERSPECTIVE_NEAR, m_ClipValues[0]);
            final float far = Math.max(near + MIN_CLIP_DISTANCE, m_ClipValues[1]);

            camera.setClipPlanes(near, far);
        }

        finishCameraEditIfDeactivated(scene, assets, history);

        final EntityId primaryId = getPrimaryCameraId(scene);
        final boolean primary = selected.getId().equals(primaryId);

        if(primary)
            ImGui.textDisabled("Aspect Ratio: %.3f (Game View)".formatted(camera.getAspectRatio()));
        else
            ImGui.textDisabled("Aspect Ratio: %.3f".formatted(camera.getAspectRatio()));
    }

    private void drawOrthographicCamera(Scene scene, Entity selected, CameraComponent component, AssetManager assets, EditorHistory history) {
        ImGui.textDisabled("Projection: Orthographic");
        OrthographicCamera camera = (OrthographicCamera) component.getCamera();
        m_ScalarValue[0] = camera.getVerticalSize();

        final boolean changedVerticalSize = ImGui.dragFloat("Vertical Size", m_ScalarValue, 0.05f, MIN_ORTHOGRAPHIC_SIZE, 100000.0f, "%.3f");
        beginCameraEditIfActivated(scene, selected, component, "Change Camera Vertical Size");

        if (changedVerticalSize)
            camera.setVerticalSize(Math.max(MIN_ORTHOGRAPHIC_SIZE, m_ScalarValue[0]));

        finishCameraEditIfDeactivated(scene, assets, history);

        m_ClipValues[0] = camera.getNearPlane();
        m_ClipValues[1] = camera.getFarPlane();

        final boolean changedClip = ImGui.dragFloat2("Clip Planes", m_ClipValues, 0.05f, 0.0f, 0.0f, "%.3f");
        beginCameraEditIfActivated(scene, selected, component, "Change Camera Clip Planes");

        if (changedClip) {
            final float near = m_ClipValues[0];
            final float far = Math.max(near + MIN_CLIP_DISTANCE, m_ClipValues[1]);

            camera.setClipPlanes(near, far);
        }

        finishCameraEditIfDeactivated(scene, assets, history);

        final EntityId primaryId = getPrimaryCameraId(scene);
        final boolean primary = selected.getId().equals(primaryId);

        if(primary)
            ImGui.textDisabled("Aspect Ratio: %.3f (Game View)".formatted(camera.getAspectRatio()));
        else
            ImGui.textDisabled("Aspect Ratio: %.3f".formatted(camera.getAspectRatio()));
    }

    private void beginCameraEditIfActivated(Scene scene, Entity entity, CameraComponent component, String commandName) {
        if (!ImGui.isItemActivated())
            return;

        m_CameraEditEntityId = entity.getId();
        m_CameraEditStart = CameraComponentSnapshot.capture(component);

        m_CameraEditPrimaryId = getPrimaryCameraId(scene);
        m_CameraEditName = commandName;
    }

    private void finishCameraEditIfDeactivated(Scene scene, AssetManager assets, EditorHistory history) {
        if (!ImGui.isItemDeactivated())
            return;

        finishCameraEdit(scene, assets, history);
    }

    private void finishCameraEdit(Scene scene, AssetManager assets, EditorHistory history) {
        if (m_CameraEditEntityId == null || m_CameraEditStart == null || m_CameraEditName == null)
            return;

        final Entity entity = scene.findEntity(m_CameraEditEntityId);
        if (entity != null) {
            final CameraComponent component = entity.getComponent(CameraComponent.class);
            if (component != null)
                history.recordExecuted(new ComponentChangeCommand(m_CameraEditName, scene, assets, m_CameraEditEntityId, CameraComponent.class, m_CameraEditStart, CameraComponentSnapshot.capture(component), m_CameraEditPrimaryId, getPrimaryCameraId(scene)));
        }

        m_CameraEditEntityId = null;
        m_CameraEditStart = null;
        m_CameraEditPrimaryId = null;
        m_CameraEditName = null;
    }

    private void drawMeshRenderer(AssetManager assets, MeshRenderComponent renderer) {
        ImGui.text("Mesh:");
        ImGui.textDisabled(getAssetId(assets, Mesh.class, renderer.getMesh()));

        ImGui.text("Material:");
        ImGui.textDisabled(getAssetId(assets, Material.class, renderer.getMaterial()));
    }

    private void finishNameEdit(Scene scene, EditorHistory history) {
        if (m_NameEditEntityId == null || m_NameEditStart == null)
            return;


        final Entity entity = scene.findEntity(m_NameEditEntityId);
        if (entity != null) {
            if (m_NameBuffer.get().isBlank()) {
                entity.setName(m_NameEditStart);
                if (entity == m_InspectedEntity)
                    m_NameBuffer.set(m_NameEditStart, true);
            }

            history.recordExecuted(new RenameEntityCommand(scene, m_NameEditEntityId, m_NameEditStart, entity.getName()));
        }

        m_NameEditEntityId = null;
        m_NameEditStart = null;
    }

    private void beginTransformEditIfActivated(Entity entity, String commandName) {
        if (!ImGui.isItemActivated())
            return;

        m_TransformEditEntityId = entity.getId();
        m_TransformEditStart = TransformState.capture(entity.getTransform());
        m_TransformEditName = commandName;
    }

    private void finishTransformEditIfDeactivated(Scene scene, EditorHistory history) {
        if (!ImGui.isItemDeactivated())
            return;

        finishTransformEdit(scene, history);
    }

    private void finishTransformEdit(Scene scene, EditorHistory history) {
        if (m_TransformEditEntityId == null || m_TransformEditStart == null || m_TransformEditName == null)
            return;

        final Entity entity = scene.findEntity(m_TransformEditEntityId);
        if (entity != null) {
            final TransformState after = TransformState.capture(entity.getTransform());
            history.recordExecuted(new TransformEntityCommand(m_TransformEditName, scene, m_TransformEditEntityId, m_TransformEditStart, after));
        }

        m_TransformEditEntityId = null;
        m_TransformEditStart = null;
        m_TransformEditName = null;
    }

    private static EntityId getPrimaryCameraId(Scene scene) {
        final Entity primary = scene.getPrimaryCameraEntity();
        return primary == null ? null : primary.getId();
    }

    private static PerspectiveCamera createPerspectiveFrom(Camera source) {
        final float aspectRatio = getCameraAspectRatio(source);
        float nearClip = getCameraNearClip(source);
        final float farClip = getCameraFarClip(source);

        if (nearClip <= 0.0f) nearClip = DEFAULT_PERSPECTIVE_NEAR;

        final float correctedFarClip = farClip > nearClip ? farClip : nearClip + 100.0f;
        return new PerspectiveCamera(DEFAULT_PERSPECTIVE_FOV, aspectRatio, nearClip, correctedFarClip);
    }

    private static OrthographicCamera createOrthographicFrom(Camera source) {
        return new OrthographicCamera(DEFAULT_ORTHOGRAPHIC_SIZE, getCameraAspectRatio(source), getCameraNearClip(source), getCameraFarClip(source));
    }

    private static float getCameraAspectRatio(Camera camera) {
        if (camera instanceof PerspectiveCamera perspectiveCamera)
            return perspectiveCamera.getAspectRatio();

        if (camera instanceof OrthographicCamera orthographicCamera)
            return orthographicCamera.getAspectRatio();

        throw new IllegalArgumentException("Unsupported Camera type: " + camera.getClass().getName());
    }

    private static float getCameraNearClip(Camera camera) {
        if (camera instanceof PerspectiveCamera perspectiveCamera)
            return perspectiveCamera.getNearPlane();

        if (camera instanceof OrthographicCamera orthographicCamera)
            return orthographicCamera.getNearPlane();

        throw new IllegalArgumentException("Unsupported Camera type: " + camera.getClass().getName());
    }

    private static float getCameraFarClip(Camera camera) {
        if (camera instanceof PerspectiveCamera perspectiveCamera)
            return perspectiveCamera.getFarPlane();

        if (camera instanceof OrthographicCamera orthographicCamera)
            return orthographicCamera.getFarPlane();

        throw new IllegalArgumentException("Unsupported Camera type: " + camera.getClass().getName());
    }

    private static <T> String getAssetId(AssetManager assets, Class<T> type, T asset) {
        try {
            return assets.getKey(type, asset).getId();
        } catch (IllegalStateException exception) {
            return "<Unmanaged>";
        }
    }

}