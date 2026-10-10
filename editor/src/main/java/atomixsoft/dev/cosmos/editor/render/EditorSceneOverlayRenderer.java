package atomixsoft.dev.cosmos.editor.render;

import atomixsoft.dev.cosmos.camera.Camera;

import atomixsoft.dev.cosmos.render.BlendMode;
import atomixsoft.dev.cosmos.render.BufferLayout;
import atomixsoft.dev.cosmos.render.CullMode;
import atomixsoft.dev.cosmos.render.Graphics;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.render.RenderState;
import atomixsoft.dev.cosmos.render.Shader;
import atomixsoft.dev.cosmos.render.VertexArray;
import atomixsoft.dev.cosmos.render.VertexBuffer;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;

import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EditorSceneOverlayRenderer {

    private static final int GRID_HALF_SIZE = 50;
    private static final float GRID_SPACING = 1.0f;
    private static final float CAMERA_ICON_SCALE = 0.65f;
    private static final float SELECTION_OUTLINE_SCALE = 1.035f;

    private static final RenderState LINE_RENDER_STATE = new RenderState(true, false, CullMode.NONE, BlendMode.ALPHA);
    private static final RenderState SELECTION_OUTLINE_STATE = new RenderState(true, false, CullMode.FRONT, BlendMode.ALPHA);

    private static final String GRID_VERTEX_SHADER = """
            #version 330 core
            
            layout(location = 0) in vec3 a_Position;
            layout(location = 1) in vec4 a_Color;
            
            uniform mat4 u_ViewProjection;
            
            out vec4 v_Color;
            
            void main() {
                v_Color = a_Color;
            
                gl_Position =
                    u_ViewProjection
                    * vec4(a_Position, 1.0);
            }
            """;

    private static final String GRID_FRAGMENT_SHADER = """
            #version 330 core
            
            in vec4 v_Color;
            
            out vec4 FragColor;
            
            void main() {
                FragColor = v_Color;
            }
            """;

    private static final String LINE_VERTEX_SHADER = """
            #version 330 core
            
            layout(location = 0) in vec3 a_Position;
            
            uniform mat4 u_ViewProjection;
            uniform mat4 u_Model;
            
            void main() {
                gl_Position =
                    u_ViewProjection
                    * u_Model
                    * vec4(a_Position, 1.0);
            }
            """;

    private static final String LINE_FRAGMENT_SHADER = """
            #version 330 core
            
            uniform vec4 u_Color;
            
            out vec4 FragColor;
            
            void main() {
                FragColor = u_Color;
            }
            """;

    private static final String FRUSTUM_VERTEX_SHADER = """
            #version 330 core
            
            layout(location = 0) in vec3 a_ClipPosition;
            
            uniform mat4 u_EditorViewProjection;
            uniform mat4 u_CameraPose;
            uniform mat4 u_CameraInverseProjection;
            
            void main() {
                vec4 cameraPosition =
                    u_CameraInverseProjection
                    * vec4(a_ClipPosition, 1.0);
            
                cameraPosition /= cameraPosition.w;
            
                gl_Position =
                    u_EditorViewProjection
                    * u_CameraPose
                    * cameraPosition;
            }
            """;

    private static final String FRUSTUM_FRAGMENT_SHADER = """
            #version 330 core
            
            uniform vec4 u_Color;
            
            out vec4 FragColor;
            
            void main() {
                FragColor = u_Color;
            }
            """;

    private static final String OUTLINE_VERTEX_SHADER = """
            #version 330 core
            
            layout(location = 0) in vec3 a_Position;
            
            uniform mat4 u_ViewProjection;
            uniform mat4 u_Model;
            
            void main() {
                gl_Position =
                    u_ViewProjection
                    * u_Model
                    * vec4(a_Position, 1.0);
            }
            """;

    private static final String OUTLINE_FRAGMENT_SHADER = """
            #version 330 core
            
            uniform vec4 u_Color;
            
            out vec4 FragColor;
            
            void main() {
                FragColor = u_Color;
            }
            """;

    private static final float[] CAMERA_ICON_VERTICES = {
            // Body: front rectangle.
            -0.35f, -0.22f, -0.18f, 0.35f, -0.22f, -0.18f,

            0.35f, -0.22f, -0.18f, 0.35f, 0.22f, -0.18f,

            0.35f, 0.22f, -0.18f, -0.35f, 0.22f, -0.18f,

            -0.35f, 0.22f, -0.18f, -0.35f, -0.22f, -0.18f,

            // Body: rear rectangle.
            -0.35f, -0.22f, 0.18f, 0.35f, -0.22f, 0.18f,

            0.35f, -0.22f, 0.18f, 0.35f, 0.22f, 0.18f,

            0.35f, 0.22f, 0.18f, -0.35f, 0.22f, 0.18f,

            -0.35f, 0.22f, 0.18f, -0.35f, -0.22f, 0.18f,

            // Join front/rear rectangles.
            -0.35f, -0.22f, -0.18f, -0.35f, -0.22f, 0.18f,

            0.35f, -0.22f, -0.18f, 0.35f, -0.22f, 0.18f,

            0.35f, 0.22f, -0.18f, 0.35f, 0.22f, 0.18f,

            -0.35f, 0.22f, -0.18f, -0.35f, 0.22f, 0.18f,

            // Lens rectangle.
            -0.16f, -0.10f, -0.45f, 0.16f, -0.10f, -0.45f,

            0.16f, -0.10f, -0.45f, 0.16f, 0.10f, -0.45f,

            0.16f, 0.10f, -0.45f, -0.16f, 0.10f, -0.45f,

            -0.16f, 0.10f, -0.45f, -0.16f, -0.10f, -0.45f,

            // Connect body to lens.
            -0.35f, -0.22f, -0.18f, -0.16f, -0.10f, -0.45f,

            0.35f, -0.22f, -0.18f, 0.16f, -0.10f, -0.45f,

            0.35f, 0.22f, -0.18f, 0.16f, 0.10f, -0.45f,

            -0.35f, 0.22f, -0.18f, -0.16f, 0.10f, -0.45f};

    private static final float[] FRUSTUM_VERTICES = {
            // Near plane.
            -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,

            1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f,

            1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f,

            -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f,

            // Far plane.
            -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f,

            1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,

            1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f,

            -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f,

            // Near -> far.
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f,

            1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f,

            1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,

            -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f};

    private final VertexArray m_GridVAO;
    private final VertexBuffer m_GridVBO;
    private final Shader m_GridShader;

    private final VertexArray m_CameraIconVAO;
    private final VertexBuffer m_CameraIconVBO;
    private final Shader m_LineShader;

    private final VertexArray m_FrustumVAO;
    private final VertexBuffer m_FrustumVBO;
    private final Shader m_FrustumShader;

    private final Shader m_OutlineShader;

    private final Matrix4f m_ViewProjection;

    private final Matrix4f m_WorldMatrix;
    private final Matrix4f m_CameraPose;
    private final Matrix4f m_CameraInverseProjection;
    private final Matrix4f m_SelectionModel;

    private final Vector3f m_WorldPosition;
    private final Quaternionf m_WorldRotation;

    private int m_GridVertexCount;
    private int m_CameraIconVertexCount;
    private int m_FrustumVertexCount;

    private boolean m_Initialized;

    public EditorSceneOverlayRenderer() {
        m_GridVAO = new VertexArray();
        m_GridVBO = new VertexBuffer();

        m_GridShader = new Shader();

        m_CameraIconVAO = new VertexArray();
        m_CameraIconVBO = new VertexBuffer();

        m_LineShader = new Shader();

        m_FrustumVAO = new VertexArray();
        m_FrustumVBO = new VertexBuffer();

        m_FrustumShader = new Shader();
        m_OutlineShader = new Shader();

        m_ViewProjection = new Matrix4f();
        m_WorldMatrix = new Matrix4f();
        m_CameraPose = new Matrix4f();
        m_CameraInverseProjection = new Matrix4f();
        m_SelectionModel = new Matrix4f();

        m_WorldPosition = new Vector3f();
        m_WorldRotation = new Quaternionf();

        m_GridVertexCount = 0;
        m_CameraIconVertexCount = 0;
        m_FrustumVertexCount = 0;

        m_Initialized = false;
    }

    private static float[] createGridVertices() {
        final int lineCount = GRID_HALF_SIZE * 2 + 1;

        final float[] vertices = new float[lineCount * 2 * 2 * 7];
        final float extent = GRID_HALF_SIZE * GRID_SPACING;

        int index = 0;

        for (int i = -GRID_HALF_SIZE; i <= GRID_HALF_SIZE; i++) {
            final float coordinate = i * GRID_SPACING;

            final float zr = i == 0 ? 0.25f : 0.55f;
            final float zg = i == 0 ? 0.45f : 0.55f;
            final float zb = i == 0 ? 0.95f : 0.55f;
            final float za = i == 0 ? 0.90f : 0.28f;

            index = putVertex(vertices, index, coordinate, 0.0f, -extent, zr, zg, zb, za);
            index = putVertex(vertices, index, coordinate, 0.0f, extent, zr, zg, zb, za);

            final float xr = i == 0 ? 0.95f : 0.55f;
            final float xg = i == 0 ? 0.25f : 0.55f;
            final float xb = i == 0 ? 0.25f : 0.55f;
            final float xa = i == 0 ? 0.90f : 0.28f;

            index = putVertex(vertices, index, -extent, 0.0f, coordinate, xr, xg, xb, xa);
            index = putVertex(vertices, index, extent, 0.0f, coordinate, xr, xg, xb, xa);
        }

        return vertices;
    }

    private static int putVertex(float[] vertices, int index, float x, float y, float z, float r, float g, float b, float a) {
        vertices[index++] = x;
        vertices[index++] = y;
        vertices[index++] = z;

        vertices[index++] = r;
        vertices[index++] = g;
        vertices[index++] = b;
        vertices[index++] = a;

        return index;
    }

    private static Throwable captureFailure(Throwable failure, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error exception) {
            if (failure == null)
                return exception;

            failure.addSuppressed(exception);
        }

        return failure;
    }

    public void initialize() {
        if (m_Initialized)
            return;

        final float[] gridVertices = createGridVertices();
        final BufferLayout gridLayout = new BufferLayout().addFloat(3).addFloat(4);
        final BufferLayout positionLayout = new BufferLayout().addFloat(3);

        try {
            m_GridShader.initialize(GRID_VERTEX_SHADER, GRID_FRAGMENT_SHADER);

            m_GridVAO.create();
            m_GridVBO.create(gridVertices);
            m_GridVAO.configure(m_GridVBO, gridLayout);

            m_GridVertexCount = gridVertices.length / 7;

            m_LineShader.initialize(LINE_VERTEX_SHADER, LINE_FRAGMENT_SHADER);

            m_CameraIconVAO.create();
            m_CameraIconVBO.create(CAMERA_ICON_VERTICES);
            m_CameraIconVAO.configure(m_CameraIconVBO, positionLayout);
            m_CameraIconVertexCount = CAMERA_ICON_VERTICES.length / 3;

            m_FrustumShader.initialize(FRUSTUM_VERTEX_SHADER, FRUSTUM_FRAGMENT_SHADER);

            m_FrustumVAO.create();
            m_FrustumVBO.create(FRUSTUM_VERTICES);
            m_FrustumVAO.configure(m_FrustumVBO, positionLayout);

            m_FrustumVertexCount = FRUSTUM_VERTICES.length / 3;
            m_OutlineShader.initialize(OUTLINE_VERTEX_SHADER, OUTLINE_FRAGMENT_SHADER);

            m_Initialized = true;
        } catch (RuntimeException | Error exception) {
            try {
                dispose();
            } catch (RuntimeException | Error cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }

            throw exception;
        }
    }

    public void render(Graphics graphics, Scene scene, Entity selectedEntity, Camera editorCamera, Matrix4fc editorViewMatrix) {
        validate();

        if (graphics == null)
            throw new IllegalArgumentException("Graphics cannot be null!");

        if (scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        if (editorCamera == null)
            throw new IllegalArgumentException("Editor Camera cannot be null!");

        if (editorViewMatrix == null)
            throw new IllegalArgumentException("Editor View Matrix cannot be null!");

        m_ViewProjection.set(editorCamera.getProjection()).mul(editorViewMatrix);

        renderGrid(graphics);
        renderCameraIcons(graphics, scene, selectedEntity);

        if (selectedEntity != null && selectedEntity.isValid()) {
            final CameraComponent cameraComponent = selectedEntity.getComponent(CameraComponent.class);
            if (cameraComponent != null)
                renderCameraFrustum(graphics, selectedEntity, cameraComponent);

            final MeshRenderComponent meshRenderer = selectedEntity.getComponent(MeshRenderComponent.class);
            if (meshRenderer != null)
                renderSelectionOutline(graphics, selectedEntity, meshRenderer.getMesh());
        }
    }

    public void dispose() {
        Throwable failure = null;

        failure = captureFailure(failure, m_GridVAO::dispose);
        failure = captureFailure(failure, m_GridVBO::dispose);
        failure = captureFailure(failure, m_GridShader::dispose);
        failure = captureFailure(failure, m_CameraIconVAO::dispose);
        failure = captureFailure(failure, m_CameraIconVBO::dispose);
        failure = captureFailure(failure, m_LineShader::dispose);
        failure = captureFailure(failure, m_FrustumVAO::dispose);
        failure = captureFailure(failure, m_FrustumVBO::dispose);
        failure = captureFailure(failure, m_FrustumShader::dispose);
        failure = captureFailure(failure, m_OutlineShader::dispose);

        m_GridVertexCount = 0;
        m_CameraIconVertexCount = 0;
        m_FrustumVertexCount = 0;

        m_Initialized = false;

        if (failure == null)
            return;

        if (failure instanceof RuntimeException exception)
            throw exception;

        throw (Error) failure;
    }

    private void renderGrid(Graphics graphics) {
        graphics.applyRenderState(LINE_RENDER_STATE);
        m_GridShader.bind();

        try {
            m_GridShader.setMatrix4("u_ViewProjection", m_ViewProjection);
            graphics.drawLines(m_GridVAO, m_GridVertexCount);
        } finally {
            m_GridShader.unbind();
        }
    }

    private void renderCameraIcons(Graphics graphics, Scene scene, Entity selectedEntity) {
        graphics.applyRenderState(LINE_RENDER_STATE);
        m_LineShader.bind();

        try {
            m_LineShader.setMatrix4("u_ViewProjection", m_ViewProjection);

            for (Entity entity : scene.getEntitiesWithComponent(CameraComponent.class)) {
                buildCameraPose(entity, m_CameraPose);

                m_CameraPose.scale(CAMERA_ICON_SCALE);
                m_LineShader.setMatrix4("u_Model", m_CameraPose);

                if (entity == selectedEntity)
                    m_LineShader.setFloat4("u_Color", 1.0f, 0.85f, 0.20f, 1.0f);
                else
                    m_LineShader.setFloat4("u_Color", 0.80f, 0.72f, 0.28f, 0.85f);

                graphics.drawLines(m_CameraIconVAO, m_CameraIconVertexCount);
            }
        } finally {
            m_LineShader.unbind();
        }
    }

    private void renderCameraFrustum(Graphics graphics, Entity cameraEntity, CameraComponent cameraComponent) {
        buildCameraPose(cameraEntity, m_CameraPose);
        m_CameraInverseProjection.set(cameraComponent.getCamera().getProjection()).invert();

        graphics.applyRenderState(LINE_RENDER_STATE);
        m_FrustumShader.bind();

        try {
            m_FrustumShader.setMatrix4("u_EditorViewProjection", m_ViewProjection);
            m_FrustumShader.setMatrix4("u_CameraPose", m_CameraPose);
            m_FrustumShader.setMatrix4("u_CameraInverseProjection", m_CameraInverseProjection);
            m_FrustumShader.setFloat4("u_Color", 1.0f, 0.78f, 0.15f, 0.78f);

            graphics.drawLines(m_FrustumVAO, m_FrustumVertexCount);
        } finally {
            m_FrustumShader.unbind();
        }
    }

    private void renderSelectionOutline(Graphics graphics, Entity selectedEntity, Mesh mesh) {
        selectedEntity.getWorldMatrix(m_SelectionModel);

        m_SelectionModel.scale(SELECTION_OUTLINE_SCALE);
        graphics.applyRenderState(SELECTION_OUTLINE_STATE);

        m_OutlineShader.bind();

        try {
            m_OutlineShader.setMatrix4("u_ViewProjection", m_ViewProjection);
            m_OutlineShader.setMatrix4("u_Model", m_SelectionModel);
            m_OutlineShader.setFloat4("u_Color", 1.0f, 0.52f, 0.10f, 0.95f);

            graphics.draw(mesh);
        } finally {
            m_OutlineShader.unbind();
        }
    }

    private Matrix4f buildCameraPose(Entity entity, Matrix4f destination) {
        entity.getWorldMatrix(m_WorldMatrix);

        m_WorldMatrix.getTranslation(m_WorldPosition);
        m_WorldMatrix.getUnnormalizedRotation(m_WorldRotation).normalize();

        return destination.identity().translation(m_WorldPosition).rotate(m_WorldRotation);
    }

    private void validate() {
        if (!m_Initialized)
            throw new IllegalStateException("Editor Scene Overlay Renderer has not been initialized!");
    }

}