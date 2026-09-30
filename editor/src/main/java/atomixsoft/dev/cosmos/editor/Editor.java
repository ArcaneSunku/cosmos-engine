package atomixsoft.dev.cosmos.editor;

import atomixsoft.dev.cosmos.Application;
import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.asset.*;
import atomixsoft.dev.cosmos.asset.loader.MaterialAssetLoader;
import atomixsoft.dev.cosmos.asset.loader.ShaderAssetLoader;
import atomixsoft.dev.cosmos.asset.loader.TextureAssetLoader;
import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.camera.OrthographicCamera;
import atomixsoft.dev.cosmos.camera.PerspectiveCamera;
import atomixsoft.dev.cosmos.editor.camera.FreeCameraController;
import atomixsoft.dev.cosmos.editor.ui.EditorWorkspace;
import atomixsoft.dev.cosmos.editor.ui.ImGuiManager;
import atomixsoft.dev.cosmos.render.*;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;
import atomixsoft.dev.cosmos.spatial.Transform;
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
            0,  1,  2,
            2,  3,  0,

            4,  5,  6,
            6,  7,  4,

            8,  9, 10,
            10, 11,  8,

            12, 13, 14,
            14, 15, 12,

            16, 17, 18,
            18, 19, 16,

            20, 21, 22,
            22, 23, 20
    };

    private static final String VERTEX_PATH = "/shaders/basic.vert";
    private static final String FRAGMENT_PATH = "/shaders/basic.frag";
    private static final String CHECKER_TEXTURE_PATH = "textures/checkers.png";
    private static final String DEFAULT_MATERIAL_PATH = "materials/default.material";
    private static final String ACCENT_MATERIAL_PATH = "materials/accent.material";

    private static final AssetKey<Shader> BASIC_SHADER_ASSET = AssetKey.of(Shader.class, "editor/shaders/basic");
    private static final AssetKey<Mesh> CUBE_MESH_ASSET = AssetKey.of(Mesh.class, "editor/meshes/cube");
    private static final AssetKey<Texture2D> CHECKER_TEXTURE_ASSET = AssetKey.of(Texture2D.class, "editor/textures/checkers");

    private static final AssetKey<Material> DEFAULT_MATERIAL_ASSET = AssetKey.of(Material.class, "editor/materials/default");
    private static final AssetKey<Material> ACCENT_MATERIAL_ASSET = AssetKey.of(Material.class, "editor/materials/accent");

    private final PerspectiveCamera m_POVCamera;
    private final OrthographicCamera m_OrthoCamera;
    private final FreeCameraController m_CamControl;

    private final Transform m_CamTrans;
    private final Matrix4f m_View;

    private final ImGuiManager m_ImGui;
    private final EditorWorkspace m_Workspace;

    private final Scene m_Scene;
    private final SceneRenderer m_SceneRenderer;

    private Camera m_ActiveEditorCamera;
    private boolean m_UseSceneCamera;

    private Editor() {
        m_POVCamera = new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f);
        m_OrthoCamera = new OrthographicCamera(8.0f, 16.0f / 9.0f, -100.0f, 100.0f);

        m_ActiveEditorCamera = m_POVCamera;

        m_CamTrans = new Transform().setPosition(0.0f, 0.0f, 4.0f);
        m_CamControl = new FreeCameraController(m_CamTrans);

        m_ImGui = new ImGuiManager();
        m_Workspace = new EditorWorkspace();

        m_Scene = new Scene("Test Scene");
        m_SceneRenderer = new SceneRenderer();
        m_UseSceneCamera = false;

        m_View = new Matrix4f();
    }

    @Override
    public void initialize(Engine engine) {
        m_ImGui.initialize();
        m_Workspace.initialize();

        final AssetManager assets = engine.getAssets();
        registerAssets(assets);

        final Mesh cubeMesh = assets.load(CUBE_MESH_ASSET);
        final Material defaultMaterial = assets.load(DEFAULT_MATERIAL_ASSET);
        final Material accentMaterial = assets.load(ACCENT_MATERIAL_ASSET);

        final Entity testParent = m_Scene.createEntity("Parent Cube");
        testParent.getTransform().setPosition(0.0f, 0.0f, -2.0f);
        testParent.addComponent(new MeshRenderComponent(cubeMesh, defaultMaterial));

        final Entity testChild = m_Scene.createEntity("Child Cube");
        testChild.getTransform().setPosition(2.0f, 0.0f, 0.0f);
        testChild.setParent(testParent);
        testChild.addComponent(new MeshRenderComponent(cubeMesh, accentMaterial));

        final Entity testGrandchild = m_Scene.createEntity("Grandchild Cube");
        testGrandchild.getTransform().setPosition(0.0f, 0.0f, 0.0f).setScale(0.5f);
        testGrandchild.setParent(testChild);
        testGrandchild.addComponent(new MeshRenderComponent(cubeMesh, defaultMaterial));

        final Entity sceneCamera = m_Scene.createEntity("Scene Camera");
        sceneCamera.getTransform().setPosition(0.0f, 2.0f, 6.0f);
        sceneCamera.addComponent(new CameraComponent(
                new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f)));

        m_Scene.setPrimaryCamera(sceneCamera);
    }

    @Override
    public void update(Engine engine, double dt) {
        m_ImGui.beginFrame();
        m_Workspace.draw(engine, m_Scene);

        if(m_Workspace.isSceneViewportFocused()) {
            if (!m_UseSceneCamera && engine.getInput().isKeyPressed(GLFW_KEY_P)) {
                if (m_ActiveEditorCamera == m_POVCamera) m_ActiveEditorCamera = m_OrthoCamera;
                else m_ActiveEditorCamera = m_POVCamera;
            }

            if(engine.getInput().isKeyPressed(GLFW_KEY_C)) {
                m_UseSceneCamera = !m_UseSceneCamera;

                if(m_UseSceneCamera)
                    m_CamControl.release(engine.getInput());
            }
        }

        final boolean sceneCameraInputActive = !m_UseSceneCamera && (m_Workspace.isSceneViewportHovered() || m_CamControl.isCapturingMouse());

        if(sceneCameraInputActive) m_CamControl.update(engine.getInput(), dt);
        else m_CamControl.release(engine.getInput());
    }

    @Override
    public void render(Engine engine, double alpha) {
        if(m_Workspace.isSceneViewportVisible())
            renderSceneViewport(engine);

        m_ImGui.render();
    }

    @Override
    public void shutdown(Engine engine) {
        try {
            m_CamControl.release(engine.getInput());
        } finally {
            try {
                m_Workspace.dispose();
            } finally {
                try {
                    m_ImGui.dispose();
                } finally {
                    m_Scene.clear();
                }
            }
        }
    }

    @Override
    public WindowConfig getWindowConfig() {
        return new WindowConfig("Cosmos Editor", 1280, 720);
    }

    private void registerAssets(AssetManager assets) {
        final AssetSource editorAssets = new ClassPathAssetSource(Editor.class);
        final AssetCatalog catalog = assets.getCatalog();

        catalog.register(BASIC_SHADER_ASSET, editorAssets, new ShaderAssetLoader(VERTEX_PATH, FRAGMENT_PATH));
        catalog.register(CHECKER_TEXTURE_ASSET, editorAssets, new TextureAssetLoader(CHECKER_TEXTURE_PATH));
        catalog.register(DEFAULT_MATERIAL_ASSET, editorAssets, new MaterialAssetLoader(DEFAULT_MATERIAL_PATH));
        catalog.register(ACCENT_MATERIAL_ASSET, editorAssets, new MaterialAssetLoader(ACCENT_MATERIAL_PATH));

        final BufferLayout cubeLayout = new BufferLayout().addFloat(3).addFloat(3).addFloat(2);
        catalog.registerGenerated(CUBE_MESH_ASSET, manager -> {
            final Mesh mesh = new Mesh();

            mesh.create(CUBE_VERTICES, CUBE_INDICES, cubeLayout);

            return mesh;
        }, Mesh::dispose);
    }

    private void renderSceneViewport(Engine engine) {
        final Graphics graphics = engine.getGraphics();
        final Framebuffer framebuffer = m_Workspace.getSceneFramebuffer();
        final int previousWidth = graphics.getViewportWidth();
        final int previousHeight = graphics.getViewportHeight();

        try {
            framebuffer.bind();
            graphics.setViewport(framebuffer.getWidth(), framebuffer.getHeight());
            graphics.clear();

            final Camera renderCamera = getRenderCamera();
            renderCamera.resize(framebuffer.getWidth(), framebuffer.getHeight());
            updateViewMatrix();

            m_SceneRenderer.render(engine.getGraphics(), m_Scene, renderCamera, m_View);
        } finally {
            Framebuffer.bindDefault();
            graphics.setViewport(previousWidth, previousHeight);
        }
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

    static void main(String[] args) {
        final Editor editor = new Editor();
        final Engine engine = new Engine(editor);

        engine.initialize();
        engine.run();
    }

}
