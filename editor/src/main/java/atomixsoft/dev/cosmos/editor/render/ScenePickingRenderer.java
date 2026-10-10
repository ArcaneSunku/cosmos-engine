package atomixsoft.dev.cosmos.editor.render;

import atomixsoft.dev.cosmos.camera.Camera;

import atomixsoft.dev.cosmos.render.BlendMode;
import atomixsoft.dev.cosmos.render.BufferLayout;
import atomixsoft.dev.cosmos.render.CullMode;
import atomixsoft.dev.cosmos.render.Graphics;
import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.render.RenderState;
import atomixsoft.dev.cosmos.render.Shader;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;

import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class ScenePickingRenderer {

    private static final RenderState PICK_NONE = new RenderState(true, true, CullMode.NONE, BlendMode.NONE);
    private static final RenderState PICK_BACK = new RenderState(true, true, CullMode.BACK, BlendMode.NONE);
    private static final RenderState PICK_FRONT = new RenderState(true, true, CullMode.FRONT, BlendMode.NONE);

    private static final String PICK_VERTEX_SHADER = """
            #version 330 core
            
            layout(location = 0) in vec3 a_Position;
            
            uniform mat4 u_Model;
            uniform mat4 u_View;
            uniform mat4 u_Projection;
            
            void main() {
                gl_Position =
                    u_Projection
                    * u_View
                    * u_Model
                    * vec4(a_Position, 1.0);
            }
            """;

    private static final String PICK_FRAGMENT_SHADER = """
            #version 330 core
            
            layout(location = 0) out int o_EntityId;
            
            uniform int u_EntityId;
            
            void main() {
                o_EntityId = u_EntityId;
            }
            """;

    private static final float[] CAMERA_PROXY_VERTICES = {
            -0.22f, -0.15f, 0.12f,
            0.22f, -0.15f, 0.12f,
            0.22f, 0.15f, 0.12f,
            -0.22f, 0.15f, 0.12f,

            -0.22f, -0.15f, -0.22f,
            0.22f, -0.15f, -0.22f,
            0.22f, 0.15f, -0.22f,
            -0.22f, 0.15f, -0.22f
    };

    private static final int[] CAMERA_PROXY_INDICES = {
            0, 1, 2,
            2, 3, 0,

            5, 4, 7,
            7, 6, 5,

            4, 0, 3,
            3, 7, 4,

            1, 5, 6,
            6, 2, 1,

            3, 2, 6,
            6, 7, 3,

            4, 5, 1,
            1, 0, 4
    };

    private final PickingFramebuffer m_Framebuffer;
    private final Shader m_Shader;
    private final Mesh m_CameraProxy;

    private final List<Entity> m_PickEntities;

    private final Matrix4f m_Model;
    private final Matrix4f m_World;

    private final Vector3f m_WorldPosition;
    private final Quaternionf m_WorldRotation;

    private boolean m_Initialized;

    public ScenePickingRenderer() {
        m_Framebuffer = new PickingFramebuffer();
        m_Shader = new Shader();
        m_CameraProxy = new Mesh();
        m_PickEntities = new ArrayList<>();

        m_Model = new Matrix4f();
        m_World = new Matrix4f();

        m_WorldPosition = new Vector3f();
        m_WorldRotation = new Quaternionf();

        m_Initialized = false;
    }

    private static RenderState getPickingState(CullMode cullMode) {
        return switch (cullMode) {
            case NONE -> PICK_NONE;
            case BACK -> PICK_BACK;
            case FRONT -> PICK_FRONT;
        };
    }

    public void initialize() {
        if (m_Initialized)
            return;

        try {
            m_Framebuffer.create(1, 1);
            m_Shader.initialize(PICK_VERTEX_SHADER, PICK_FRAGMENT_SHADER);
            m_CameraProxy.create(CAMERA_PROXY_VERTICES, CAMERA_PROXY_INDICES, new BufferLayout().addFloat(3));

            m_Initialized = true;
        } catch (RuntimeException | Error exception) {
            dispose();

            throw exception;
        }
    }

    public void render(Graphics graphics, Scene scene, Camera camera, Matrix4fc viewMatrix, int width, int height) {
        validate();

        if (graphics == null)
            throw new IllegalArgumentException("Graphics cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (camera == null)
            throw new IllegalArgumentException("Camera cannot be null!");

        if (viewMatrix == null)
            throw new IllegalArgumentException("View Matrix cannot be null!");

        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Picking dimensions must be greater than zero!");

        if (m_Framebuffer.getWidth() != width || m_Framebuffer.getHeight() != height)
            m_Framebuffer.resize(width, height);

        final int previousWidth = graphics.getViewportWidth();
        final int previousHeight = graphics.getViewportHeight();

        m_PickEntities.clear();

        try {
            m_Framebuffer.bind();
            graphics.setViewport(width, height);
            m_Framebuffer.clear();

            m_Shader.bind();

            try {
                m_Shader.setMatrix4("u_View", viewMatrix);
                m_Shader.setMatrix4("u_Projection", camera.getProjection());

                for (Entity entity : scene.getEntities())
                    renderEntity(graphics, entity);
            } finally {
                m_Shader.unbind();
            }
        } finally {
            PickingFramebuffer.bindDefault();
            graphics.setViewport(previousWidth, previousHeight);
        }
    }

    public Entity pick(int x, int y) {
        validate();

        final int pickId = m_Framebuffer.readEntityId(x, y);
        if (pickId <= 0 || pickId > m_PickEntities.size())
            return null;

        final Entity entity = m_PickEntities.get(pickId - 1);
        if (!entity.isValid())
            return null;

        return entity;
    }

    public void dispose() {
        if (!m_Initialized) {
            m_CameraProxy.dispose();
            m_Shader.dispose();
            m_Framebuffer.dispose();
            m_PickEntities.clear();

            return;
        }

        try {
            m_CameraProxy.dispose();
        } finally {
            try {
                m_Shader.dispose();
            } finally {
                m_Framebuffer.dispose();
            }
        }

        m_PickEntities.clear();
        m_Initialized = false;
    }

    private void renderEntity(Graphics graphics, Entity entity) {
        final MeshRenderComponent meshRenderer = entity.getComponent(MeshRenderComponent.class);
        final CameraComponent cameraComponent = entity.getComponent(CameraComponent.class);

        if (meshRenderer == null && cameraComponent == null)
            return;

        final int pickId = m_PickEntities.size() + 1;
        m_PickEntities.add(entity);

        m_Shader.setInt("u_EntityId", pickId);

        if (meshRenderer != null) {
            entity.getWorldMatrix(m_Model);
            m_Shader.setMatrix4("u_Model", m_Model);

            final Material material = meshRenderer.getMaterial();

            graphics.applyRenderState(getPickingState(material.getRenderState().getCullMode()));
            graphics.draw(meshRenderer.getMesh());
        }

        if (cameraComponent != null) {
            buildCameraPose(entity, m_Model);
            m_Shader.setMatrix4("u_Model", m_Model);

            graphics.applyRenderState(PICK_BACK);
            graphics.draw(m_CameraProxy);
        }
    }

    private Matrix4f buildCameraPose(Entity entity, Matrix4f destination) {
        entity.getWorldMatrix(m_World);

        m_World.getTranslation(m_WorldPosition);
        m_World.getUnnormalizedRotation(m_WorldRotation).normalize();

        return destination.identity().translation(m_WorldPosition).rotate(m_WorldRotation);
    }

    private void validate() {
        if (!m_Initialized)
            throw new IllegalStateException("Scene Picking Renderer has not been initialized!");
    }

}