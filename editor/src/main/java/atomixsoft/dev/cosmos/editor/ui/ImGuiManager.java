package atomixsoft.dev.cosmos.editor.ui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class ImGuiManager {

    private final ImGuiImplGlfw m_Glfw;
    private final ImGuiImplGl3 m_Gl3;

    private boolean m_ContextCreated;
    private boolean m_GlfwInitialized;
    private boolean m_Gl3Initialized;

    private boolean m_FrameStarted;
    private boolean m_Initialized;

    public ImGuiManager() {
        m_Glfw = new ImGuiImplGlfw();

        m_Gl3 = new ImGuiImplGl3();

        m_ContextCreated = false;
        m_GlfwInitialized = false;
        m_Gl3Initialized = false;

        m_FrameStarted = false;
        m_Initialized = false;
    }

    public void initialize() {
        if (m_Initialized)
            return;

        final long window = glfwGetCurrentContext();
        if (window == NULL)
            throw new IllegalStateException("ImGui requires an active GLFW OpenGL context!");

        try {
            ImGui.createContext();
            m_ContextCreated = true;

            final ImGuiIO io = ImGui.getIO();

            io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
            io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
            io.setIniFilename(null);

            ImGui.styleColorsDark();

            if (!m_Glfw.initForOpenGL(window, true))
                throw new IllegalStateException("Failed to initialize the ImGui GLFW backend!");

            m_GlfwInitialized = true;

            if (!m_Gl3.init("#version 330 core"))
                throw new IllegalStateException("Failed to initialize the ImGui OpenGL backend!");

            m_Gl3Initialized = true;
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

    public void beginFrame() {
        validate();
        if (m_FrameStarted)
            throw new IllegalStateException("An ImGui frame has already been started!");

        m_Gl3.newFrame();
        m_Glfw.newFrame();

        ImGui.newFrame();

        m_FrameStarted = true;
    }

    public void render() {
        validate();
        if (!m_FrameStarted)
            throw new IllegalStateException("No ImGui frame has been started!");

        ImGui.render();
        m_FrameStarted = false;

        m_Gl3.renderDrawData(ImGui.getDrawData());
    }

    public boolean wantsMouse() {
        validate();
        return ImGui.getIO().getWantCaptureMouse();
    }

    public boolean wantsKeyboard() {
        validate();
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    public void dispose() {
        if (!m_ContextCreated && !m_GlfwInitialized && !m_Gl3Initialized) {
            m_Initialized = false;
            m_FrameStarted = false;
            return;
        }

        Throwable failure = null;
        if (m_ContextCreated && m_FrameStarted) {
            failure = captureFailure(failure, ImGui::endFrame);
            m_FrameStarted = false;
        }

        if (m_Gl3Initialized) {
            failure = captureFailure(failure, m_Gl3::shutdown);
            m_Gl3Initialized = false;
        }

        if (m_GlfwInitialized) {
            failure = captureFailure(failure, m_Glfw::shutdown);
            m_GlfwInitialized = false;
        }

        if (m_ContextCreated) {
            failure = captureFailure(failure, ImGui::destroyContext);
            m_ContextCreated = false;
        }

        m_Initialized = false;
        if (failure == null)
            return;

        if (failure instanceof RuntimeException exception)
            throw exception;

        throw (Error) failure;
    }

    private void validate() {
        if (!m_Initialized)
            throw new IllegalStateException("ImGui has not been initialized!");
    }

    private static Throwable captureFailure(Throwable failure, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | Error exception) {
            if (failure == null)
                return exception;

            failure.addSuppressed(exception);
        }

        return failure;
    }

}