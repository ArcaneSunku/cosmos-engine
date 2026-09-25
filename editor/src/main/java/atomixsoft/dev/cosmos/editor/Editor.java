package atomixsoft.dev.cosmos.editor;

import atomixsoft.dev.cosmos.Application;
import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.asset.AssetKey;
import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.camera.OrthographicCamera;
import atomixsoft.dev.cosmos.camera.PerspectiveCamera;
import atomixsoft.dev.cosmos.editor.camera.FreeCameraController;
import atomixsoft.dev.cosmos.render.*;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;
import atomixsoft.dev.cosmos.spatial.Transform;
import atomixsoft.dev.cosmos.utils.ResourceLoader;
import atomixsoft.dev.cosmos.utils.WindowConfig;
import org.joml.Matrix4f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;

public class Editor implements Application {

    private static final float[] CUBE_VERTICES = {
            // Position                  // Color                // UV

            // Front
            -0.5f, -0.5f,  0.5f,        1.0f, 0.7f, 0.7f,       0.0f, 0.0f,
            0.5f, -0.5f,  0.5f,        1.0f, 0.7f, 0.7f,       1.0f, 0.0f,
            0.5f,  0.5f,  0.5f,        1.0f, 0.7f, 0.7f,       1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,        1.0f, 0.7f, 0.7f,       0.0f, 1.0f,

            // Back
            0.5f, -0.5f, -0.5f,        0.7f, 1.0f, 0.7f,       0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f,        0.7f, 1.0f, 0.7f,       1.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,        0.7f, 1.0f, 0.7f,       1.0f, 1.0f,
            0.5f,  0.5f, -0.5f,        0.7f, 1.0f, 0.7f,       0.0f, 1.0f,

            // Left
            -0.5f, -0.5f, -0.5f,        0.7f, 0.7f, 1.0f,       0.0f, 0.0f,
            -0.5f, -0.5f,  0.5f,        0.7f, 0.7f, 1.0f,       1.0f, 0.0f,
            -0.5f,  0.5f,  0.5f,        0.7f, 0.7f, 1.0f,       1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,        0.7f, 0.7f, 1.0f,       0.0f, 1.0f,

            // Right
            0.5f, -0.5f,  0.5f,        1.0f, 1.0f, 0.7f,       0.0f, 0.0f,
            0.5f, -0.5f, -0.5f,        1.0f, 1.0f, 0.7f,       1.0f, 0.0f,
            0.5f,  0.5f, -0.5f,        1.0f, 1.0f, 0.7f,       1.0f, 1.0f,
            0.5f,  0.5f,  0.5f,        1.0f, 1.0f, 0.7f,       0.0f, 1.0f,

            // Top
            -0.5f,  0.5f,  0.5f,        1.0f, 0.7f, 1.0f,       0.0f, 0.0f,
            0.5f,  0.5f,  0.5f,        1.0f, 0.7f, 1.0f,       1.0f, 0.0f,
            0.5f,  0.5f, -0.5f,        1.0f, 0.7f, 1.0f,       1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,        1.0f, 0.7f, 1.0f,       0.0f, 1.0f,

            // Bottom
            -0.5f, -0.5f, -0.5f,        0.7f, 1.0f, 1.0f,       0.0f, 0.0f,
            0.5f, -0.5f, -0.5f,        0.7f, 1.0f, 1.0f,       1.0f, 0.0f,
            0.5f, -0.5f,  0.5f,        0.7f, 1.0f, 1.0f,       1.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,        0.7f, 1.0f, 1.0f,       0.0f, 1.0f
    };

    private static final int[] CUBE_INDICES = {
            0,  1,  2,   2,  3,  0,
            4,  5,  6,   6,  7,  4,
            8,  9, 10,  10, 11,  8,
            12, 13, 14,  14, 15, 12,
            16, 17, 18,  18, 19, 16,
            20, 21, 22,  22, 23, 20
    };

    private static final int CHECKER_SIZE = 8;

    private static final String VERTEX_PATH = "/shaders/basic.vert";
    private static final String FRAGMENT_PATH = "/shaders/basic.frag";

    private static final AssetKey<Shader> BASIC_SHADER_ASSET = AssetKey.of(Shader.class, "editor/shaders/basic");
    private static final AssetKey<Mesh> CUBE_MESH_ASSET = AssetKey.of(Mesh.class, "editor/meshes/cube");
    private static final AssetKey<Texture2D> CHECKER_TEXTURE_ASSET = AssetKey.of(Texture2D.class, "editor/textures/checker");

    private final Shader m_Shader;
    private final Mesh m_Mesh;

    private final PerspectiveCamera m_POVCamera;
    private final OrthographicCamera m_OrthoCamera;
    private final FreeCameraController m_CamControl;

    private final Transform m_CamTrans;
    private final Matrix4f m_View;

    private final Scene m_Scene;
    private final SceneRenderer m_SceneRenderer;

    private final Entity m_TestParent;
    private final Texture2D m_Texture;

    private final Material m_DefaultMaterial;
    private final Material m_AccentMaterial;

    private Camera m_ActiveEditorCamera;
    private boolean m_UseSceneCamera;

    private Editor() {
        m_Shader = new Shader();
        m_Mesh = new Mesh();

        m_POVCamera = new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f);
        m_OrthoCamera = new OrthographicCamera(8.0f, 16.0f / 9.0f, -100.0f, 100.0f);

        m_ActiveEditorCamera = m_POVCamera;

        m_CamTrans = new Transform().setPosition(0.0f, 0.0f, 4.0f);
        m_CamControl = new FreeCameraController(m_CamTrans);

        m_Scene = new Scene("Test Scene");
        m_SceneRenderer = new SceneRenderer();

        m_Texture = new Texture2D();

        m_DefaultMaterial = new Material(m_Shader).setTexture("u_Albedo", m_Texture, 0)
                .setFloat4("u_Tint", 1.0f, 1.0f, 1.0f, 1.0f).setRenderState(RenderState.OPAQUE);

        m_AccentMaterial = new Material(m_Shader).setTexture("u_Albedo", m_Texture, 0)
                .setFloat4("u_Tint", 0.65f, 0.85f, 0.1f, 0.45f).setRenderState(RenderState.TRANSPARENT);

        m_TestParent = m_Scene.createEntity("Parent Cube");
        m_TestParent.getTransform().setPosition(0.0f, 0.0f, -2.0f);
        m_TestParent.addComponent(new MeshRenderComponent(m_Mesh, m_DefaultMaterial));

        final Entity child = m_Scene.createEntity("Child Cube");
        child.getTransform().setPosition(2.0f, 0.0f, 0.0f);
        child.setParent(m_TestParent);
        child.addComponent(new MeshRenderComponent(m_Mesh, m_AccentMaterial));

        final Entity grandchild = m_Scene.createEntity("Grandchild Cube");
        grandchild.getTransform().setPosition(0.0f, 1.5f, 0.0f).setScale(0.5f);
        grandchild.setParent(child);
        grandchild.addComponent(new MeshRenderComponent(m_Mesh, m_DefaultMaterial));

        final Entity greatGrandchild = m_Scene.createEntity("Great Grandchild Cube");
        greatGrandchild.getTransform().setPosition(2.0f, 0.0f, 0.0f).setScale(0.5f);
        greatGrandchild.setParent(grandchild);
        greatGrandchild.addComponent(new MeshRenderComponent(m_Mesh, m_AccentMaterial));

        m_Scene.createEntity("Empty Entity").getTransform().setPosition(-3.0f, 0.0f, -2.0f);

        final Entity sceneCamera = m_Scene.createEntity("Scene Camera");
        sceneCamera.getTransform().setPosition(0.0f, 2.0f, 6.0f);
        sceneCamera.addComponent(new CameraComponent(
                new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f)));

        m_Scene.setPrimaryCamera(sceneCamera);
        m_UseSceneCamera = false;

        m_View = new Matrix4f();
    }

    @Override
    public void initialize(Engine engine) {
        engine.getAssets().register(BASIC_SHADER_ASSET, m_Shader, Shader::dispose);
        engine.getAssets().register(CUBE_MESH_ASSET, m_Mesh, Mesh::dispose);
        engine.getAssets().register(CHECKER_TEXTURE_ASSET, m_Texture, Texture2D::dispose);

        m_Shader.initialize(ResourceLoader.readString(VERTEX_PATH), ResourceLoader.readString(FRAGMENT_PATH));

        final BufferLayout layout = new BufferLayout()
                .addFloat(3)
                .addFloat(3)
                .addFloat(2);

        m_Mesh.create(CUBE_VERTICES, CUBE_INDICES, layout);
        m_Texture.create(CHECKER_SIZE, CHECKER_SIZE, createCheckerboardPixels(CHECKER_SIZE, CHECKER_SIZE));
    }

    @Override
    public void update(Engine engine, double dt) {
        if (!m_UseSceneCamera && engine.getInput().isKeyPressed(GLFW_KEY_P)) {
            if (m_ActiveEditorCamera == m_POVCamera) m_ActiveEditorCamera = m_OrthoCamera;
            else m_ActiveEditorCamera = m_POVCamera;
        }

        if(engine.getInput().isKeyPressed(GLFW_KEY_C)) {
            m_UseSceneCamera = !m_UseSceneCamera;

            if(m_UseSceneCamera)
                m_CamControl.release(engine.getInput());
        }

        if(!m_UseSceneCamera)
            m_CamControl.update(engine.getInput(), dt);

        final float elapsedTime = (float) engine.getElapsedTime();
        m_TestParent.getTransform().setRotationEuler(0.0f, elapsedTime * 0.5f, 0.0f);
    }

    @Override
    public void render(Engine engine, double alpha) {
        final Camera renderCamera = getRenderCamera();

        updateCameraProjection(engine, renderCamera);
        updateViewMatrix();

        m_SceneRenderer.render(engine.getGraphics(), m_Scene, renderCamera, m_View);
    }

    @Override
    public void shutdown(Engine engine) {
        m_CamControl.release(engine.getInput());
        m_Scene.clear();
    }

    @Override
    public WindowConfig getWindowConfig() {
        return new WindowConfig("Cosmos Editor", 1280, 720);
    }

    private void updateCameraProjection(Engine engine, Camera camera) {
        final int width = engine.getGraphics().getViewportWidth();
        final int height = engine.getGraphics().getViewportHeight();

        if (width == 0 || height == 0) return;
        camera.resize(width, height);
    }

    private void updateViewMatrix() {
        if(m_UseSceneCamera) {
            final Entity cameraEntity = m_Scene.getPrimaryCameraEntity();
            if(cameraEntity != null) {
                cameraEntity.getInverseWorldMatrix(m_View);
                return;
            }
        }

        m_CamTrans.getInverseMatrix(m_View);
    }

    private Camera getRenderCamera() {
        if(!m_UseSceneCamera)
            return m_ActiveEditorCamera;

        final CameraComponent component = m_Scene.getPrimaryCameraComponent();
        if(component == null)
            return m_ActiveEditorCamera;

        return component.getCamera();
    }

    private static byte[] createCheckerboardPixels(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Checkerboard dimensions must be greater than zero!");

        final byte[] pixels = new byte[width * height * 4];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final boolean light = ((x + y) & 1) == 0;
                final int value = light ? 255 : 48;
                final int offset = (y * width + x) * 4;

                pixels[offset] = (byte) value;
                pixels[offset + 1] = (byte) value;
                pixels[offset + 2] = (byte) value;
                pixels[offset + 3] = (byte) 255;
            }
        }

        return pixels;
    }

    static void main(String[] args) {
        final Editor editor = new Editor();
        final Engine engine = new Engine(editor);

        engine.initialize();
        engine.run();
    }

}
