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
import atomixsoft.dev.cosmos.editor.render.EditorSceneOverlayRenderer;
import atomixsoft.dev.cosmos.editor.render.ScenePickingRenderer;
import atomixsoft.dev.cosmos.editor.selection.ScenePickRequest;
import atomixsoft.dev.cosmos.editor.settings.EditorPaths;
import atomixsoft.dev.cosmos.editor.settings.EditorSettings;
import atomixsoft.dev.cosmos.editor.tool.TransformTool;
import atomixsoft.dev.cosmos.editor.ui.EditorWorkspace;
import atomixsoft.dev.cosmos.editor.ui.ImGuiManager;
import atomixsoft.dev.cosmos.input.Input;
import atomixsoft.dev.cosmos.render.*;
import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.spatial.Transform;
import atomixsoft.dev.cosmos.utils.WindowConfig;

import imgui.ImGui;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;

import static atomixsoft.dev.cosmos.editor.asset.EditorAssetKeys.*;

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

    private static final String VERTEX_PATH   = "/shaders/basic.vert";
    private static final String FRAGMENT_PATH = "/shaders/basic.frag";
    private static final String CHECKER_TEXTURE_PATH  = "textures/checkers.png";
    private static final String DEFAULT_MATERIAL_PATH = "materials/default.material";

    private final PerspectiveCamera m_POVCamera;
    private final OrthographicCamera m_OrthoCamera;
    private final FreeCameraController m_CamControl;

    private final Transform m_CamTrans;
    private final Matrix4f m_SceneView;
    private final Matrix4f m_GameView;

    private final Matrix4f m_FocusWorld;
    private final Vector3f m_FocusPosition;
    private final Vector3f m_FocusScale;

    private final ImGuiManager m_ImGui;
    private final EditorWorkspace m_Workspace;

    private final EditorSceneOverlayRenderer m_SceneOverlays;
    private final ScenePickingRenderer m_ScenePickingRenderer;

    private final Scene m_Scene;
    private final SceneRenderer m_SceneRenderer;

    private Camera m_ActiveEditorCamera;

    private Editor() {
        m_POVCamera = new PerspectiveCamera((float) Math.toRadians(60.0), 16.0f / 9.0f, 0.1f, 100.0f);
        m_OrthoCamera = new OrthographicCamera(8.0f, 16.0f / 9.0f, -100.0f, 100.0f);

        m_ActiveEditorCamera = m_POVCamera;

        m_CamTrans = new Transform().setPosition(0.0f, 0.0f, 4.0f);
        m_CamControl = new FreeCameraController(m_CamTrans);

        m_ImGui = new ImGuiManager();
        m_Workspace = new EditorWorkspace();

        m_SceneOverlays = new EditorSceneOverlayRenderer();
        m_ScenePickingRenderer = new ScenePickingRenderer();

        m_Scene = new Scene("Untitled Scene");
        m_SceneRenderer = new SceneRenderer();

        m_SceneView = new Matrix4f();
        m_GameView = new Matrix4f();

        m_FocusWorld = new Matrix4f();
        m_FocusPosition = new Vector3f();
        m_FocusScale = new Vector3f();
    }

    @Override
    public void initialize(Engine engine) {
        final Path workspacePath = EditorPaths.getImGuiPath();
        final boolean savedLayoutExists = Files.isRegularFile(workspacePath);
        final EditorSettings settings = EditorSettings.load(EditorPaths.getSettingsPath());

        m_ImGui.initialize(workspacePath);

        final AssetManager assets = engine.getAssets();
        registerAssets(assets);

        m_Workspace.initialize(engine.getAssets(), settings, savedLayoutExists);

        m_SceneOverlays.initialize();
        m_ScenePickingRenderer.initialize();
    }

    @Override
    public void update(Engine engine, double dt) {
        m_ImGui.beginFrame();
        m_Workspace.draw(engine, m_Scene, m_ActiveEditorCamera, m_CamTrans);
        handleEditorShortcuts(engine);

        if (m_Workspace.isSceneViewportFocused() && !m_CamControl.isCapturingMouse()) {
            final var input = engine.getInput();

            if (input.isKeyPressed(GLFW_KEY_Q))
                m_Workspace.setTransformTool(TransformTool.NONE);

            if (input.isKeyPressed(GLFW_KEY_W))
                m_Workspace.setTransformTool(TransformTool.TRANSLATE);

            if (input.isKeyPressed(GLFW_KEY_E))
                m_Workspace.setTransformTool(TransformTool.ROTATE);

            if (input.isKeyPressed(GLFW_KEY_R))
                m_Workspace.setTransformTool(TransformTool.SCALE);

            if (input.isKeyPressed(GLFW_KEY_X))
                m_Workspace.toggleTransformSpace();

            if (input.isKeyPressed(GLFW_KEY_F))
                frameSelected();

            if (input.isKeyPressed(GLFW_KEY_P)) {
                if (m_ActiveEditorCamera == m_POVCamera) m_ActiveEditorCamera = m_OrthoCamera;
                else m_ActiveEditorCamera = m_POVCamera;
            }
        }

        final boolean sceneCameraInputActive;
        if (m_CamControl.isCapturingMouse()) sceneCameraInputActive = true;
        else sceneCameraInputActive = m_Workspace.isSceneViewportHovered() && !m_Workspace.isSceneGizmoInteracting();

        if (sceneCameraInputActive) m_CamControl.update(engine.getInput(), dt);
        else m_CamControl.release(engine.getInput());
    }

    @Override
    public void render(Engine engine, double alpha) {
        if(m_Workspace.isSceneViewportVisible())
            renderSceneViewport(engine);

        if(m_Workspace.isGameViewportVisible() && m_Scene.hasPrimaryCamera())
            renderGameViewport(engine);

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
                    m_ScenePickingRenderer.dispose();
                } finally {
                    try {
                        m_SceneOverlays.dispose();
                    } finally {
                        try {
                            m_ImGui.dispose();
                        } finally {
                            m_Scene.clear();
                        }
                    }
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

        catalog.register(BASIC_SHADER, editorAssets, new ShaderAssetLoader(VERTEX_PATH, FRAGMENT_PATH));
        catalog.register(CHECKER_TEXTURE, editorAssets, new TextureAssetLoader(CHECKER_TEXTURE_PATH));
        catalog.register(DEFAULT_MATERIAL, editorAssets, new MaterialAssetLoader(DEFAULT_MATERIAL_PATH));

        final BufferLayout cubeLayout = new BufferLayout().addFloat(3).addFloat(3).addFloat(2);
        catalog.registerGenerated(CUBE_MESH, manager -> {
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

            m_CamTrans.getInverseMatrix(m_SceneView);
            m_SceneRenderer.render(graphics, m_Scene, m_ActiveEditorCamera, m_SceneView);
            m_SceneOverlays.render(graphics, m_Scene, m_Workspace.getSelectedEntity(), m_ActiveEditorCamera, m_SceneView);
        } finally {
            Framebuffer.bindDefault();

            graphics.setViewport(previousWidth, previousHeight);
        }

        resolveScenePick(graphics, framebuffer);
    }

    private void renderGameViewport(Engine engine) {
        if(!m_Scene.hasPrimaryCamera())
            return;

        final Entity cameraEntity = m_Scene.getPrimaryCameraEntity();
        final CameraComponent component = cameraEntity.getComponent(CameraComponent.class);

        if(component == null)
            return;

        final Camera camera = component.getCamera();
        final Graphics graphics = engine.getGraphics();
        final Framebuffer framebuffer = m_Workspace.getGameFramebuffer();

        final int prevWidth = graphics.getViewportWidth();
        final int prevHeight = graphics.getViewportHeight();

        try {
            framebuffer.bind();

            graphics.setViewport(framebuffer.getWidth(), framebuffer.getHeight());
            graphics.clear();

            camera.resize(framebuffer.getWidth(), framebuffer.getHeight());
            cameraEntity.getInverseWorldMatrix(m_GameView);

            m_SceneRenderer.render(graphics, m_Scene, camera, m_GameView);
        } finally {
            Framebuffer.bindDefault();
            graphics.setViewport(prevWidth, prevHeight);
        }
    }

    private void resolveScenePick(Graphics graphics, Framebuffer sceneFramebuffer) {
        final ScenePickRequest request = m_Workspace.consumeScenePickRequest();
        if (request == null)
            return;

        m_ScenePickingRenderer.render(graphics, m_Scene, m_ActiveEditorCamera, m_SceneView, sceneFramebuffer.getWidth(), sceneFramebuffer.getHeight());

        final Entity picked = m_ScenePickingRenderer.pick(request.x(), request.y());
        m_Workspace.selectEntity(picked);
    }

    private void frameSelected() {
        final Entity selected = m_Workspace.getSelectedEntity();
        if (selected == null)
            return;

        selected.getWorldMatrix(m_FocusWorld);

        m_FocusWorld.getTranslation(m_FocusPosition);
        m_FocusWorld.getScale(m_FocusScale);

        final float extent = Math.max(Math.max(Math.abs(m_FocusScale.x), Math.abs(m_FocusScale.y)), Math.abs(m_FocusScale.z));
        final float distance = Math.max(2.5f, extent * 4.0f);

        m_CamControl.focus(m_FocusPosition, distance);

        if (m_ActiveEditorCamera instanceof OrthographicCamera orthographic)
            orthographic.setVerticalSize(Math.max(2.0f, extent * 4.0f));
    }

    private void handleEditorShortcuts(Engine engine) {
        if (ImGui.getIO().getWantTextInput())
            return;

        if (ImGui.isAnyItemActive())
            return;

        if (m_CamControl.isCapturingMouse())
            return;

        if (m_Workspace.isSceneGizmoInteracting())
            return;

        final Input input = engine.getInput();
        if (input.isControlDown()) {
            if (input.isKeyPressed(GLFW_KEY_Z)) {
                if (input.isShiftDown())
                    m_Workspace.redo(m_Scene);
                else
                    m_Workspace.undo(m_Scene);

                return;
            }

            if (input.isKeyPressed(GLFW_KEY_Y)) {
                m_Workspace.redo(m_Scene);
                return;
            }

            if (input.isKeyPressed(GLFW_KEY_D)) {
                m_Workspace.duplicateSelected(m_Scene);
                return;
            }
        }

        if (input.isKeyPressed(GLFW_KEY_DELETE))
            m_Workspace.deleteSelected(m_Scene);
    }

    static void main(String[] args) {
        final Editor editor = new Editor();
        final Engine engine = new Engine(editor);

        engine.initialize();
        engine.run();
    }

}
