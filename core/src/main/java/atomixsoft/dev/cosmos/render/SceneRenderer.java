package atomixsoft.dev.cosmos.render;

import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SceneRenderer {

    private static final String MODEL_UNIFORM = "u_Model";
    private static final String VIEW_UNIFORM = "u_View";
    private static final String PROJECTION_UNIFORM = "u_Projection";

    private static final Comparator<RenderSubmission> TRANSPARENT_COMPARATOR = Comparator.comparingDouble(RenderSubmission::viewDepth);

    private final List<RenderSubmission> m_OpaqueSubmissions;
    private final List<RenderSubmission> m_TransparentSubmissions;

    private final Matrix4f m_Model;

    private final Vector3f m_WorldPosition;
    private final Vector3f m_ViewPosition;

    public SceneRenderer() {
        m_OpaqueSubmissions = new ArrayList<>();
        m_TransparentSubmissions = new ArrayList<>();

        m_Model = new Matrix4f();

        m_WorldPosition = new Vector3f();
        m_ViewPosition = new Vector3f();
    }

    public void render(Graphics graphics, Scene scene, Camera camera, Matrix4fc viewMatrix) {
        if(graphics == null) throw new IllegalArgumentException("Graphics cannot be null!");
        if(scene == null) throw new IllegalArgumentException("Scene cannot be null!");
        if(camera == null) throw new IllegalArgumentException("Camera cannot be null!");
        if(viewMatrix == null) throw new IllegalArgumentException("View Matrix cannot be null!");

        m_OpaqueSubmissions.clear();
        m_TransparentSubmissions.clear();

        try {
            collectSubmissions(scene, viewMatrix);
            m_TransparentSubmissions.sort(TRANSPARENT_COMPARATOR);

            final Matrix4fc projection = camera.getProjection();

            renderQueue(graphics, m_OpaqueSubmissions, viewMatrix, projection);
            renderQueue(graphics, m_TransparentSubmissions, viewMatrix, projection);
        } finally {
            m_OpaqueSubmissions.clear();
            m_TransparentSubmissions.clear();
        }
    }

    private void collectSubmissions(Scene scene, Matrix4fc viewMatrix) {
        for(Entity entity : scene.getEntities()) {
            final MeshRenderComponent renderer = entity.getComponent(MeshRenderComponent.class);
            if(renderer == null)
                continue;

            final Material material = renderer.getMaterial();
            if(!material.getRenderState().isBlendingEnabled()) {
                m_OpaqueSubmissions.add(new RenderSubmission(entity, renderer, 0.0f));
                continue;
            }

            entity.getWorldMatrix(m_Model);
            m_Model.getTranslation(m_WorldPosition);

            m_ViewPosition.set(m_WorldPosition).mulPosition(viewMatrix);
            m_TransparentSubmissions.add(new RenderSubmission(entity, renderer, m_ViewPosition.z));
        }
    }

    private void renderQueue(Graphics graphics, List<RenderSubmission> submissions, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        for(RenderSubmission submission : submissions)
            renderSubmission(graphics, submission, viewMatrix, projectionMatrix);
    }

    private void renderSubmission(Graphics graphics, RenderSubmission submission, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        final MeshRenderComponent renderer = submission.renderer();
        final Material material = renderer.getMaterial();
        final Shader shader = material.getShader();

        graphics.applyRenderState(material.getRenderState());

        material.bind();

        try {
            shader.setMatrix4(VIEW_UNIFORM, viewMatrix);
            shader.setMatrix4(PROJECTION_UNIFORM, projectionMatrix);

            submission.entity().getWorldMatrix(m_Model);
            shader.setMatrix4(MODEL_UNIFORM, m_Model);

            graphics.draw(renderer.getMesh());
        } finally {
            material.unbind();
        }
    }

    private record RenderSubmission(Entity entity, MeshRenderComponent renderer, float viewDepth) {}

}
