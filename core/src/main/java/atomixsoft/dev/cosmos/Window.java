package atomixsoft.dev.cosmos;

import atomixsoft.dev.cosmos.utils.WindowConfig;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private final String m_Title;
    private final int m_Width, m_Height;

    private GLFWErrorCallback m_ErrorCallback;

    private long m_Handle;
    private int m_FramebufferWidth, m_FramebufferHeight;
    private boolean m_FramebufferResized;

    public Window(WindowConfig config) {
        if(config == null)
            throw new IllegalArgumentException("Window config cannot be null!");

        m_Title = config.title();
        m_Width = config.width();
        m_Height = config.height();

        m_Handle = NULL;

        m_FramebufferWidth = m_Width;
        m_FramebufferHeight = m_Height;

        m_FramebufferResized = false;
    }

    public void create() {
        if(m_Handle != NULL) return;


        m_ErrorCallback = GLFWErrorCallback.createPrint(System.err);
        m_ErrorCallback.set();

        if(!glfwInit()) {
            disposeErrorCallback();
            throw new IllegalStateException("Failed to initialize GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        if(Platform.get() == Platform.MACOSX)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        m_Handle = glfwCreateWindow(m_Width, m_Height, m_Title, 0, 0);
        if(m_Handle == NULL) {
            glfwTerminate();
            disposeErrorCallback();

            throw new IllegalStateException("Failed to create GLFW Window!");
        }

        glfwSetFramebufferSizeCallback(m_Handle, (window, width, height) -> {
            m_FramebufferWidth = width;
            m_FramebufferHeight = height;

            m_FramebufferResized = true;
        });

        updateFramebufferSize();

        glfwMakeContextCurrent(m_Handle);
        glfwSwapInterval(1);
        glfwShowWindow(m_Handle);
    }

    public void update() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(m_Handle);
    }

    public void close() {
        if(m_Handle == NULL) return;

        Callbacks.glfwFreeCallbacks(m_Handle);

        glfwDestroyWindow(m_Handle);
        glfwTerminate();

        m_Handle = NULL;
        disposeErrorCallback();
    }

    public void clearFramebufferResized() {
        m_FramebufferResized = false;
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(m_Handle);
    }

    public boolean hasFramebufferResized() {
        return m_FramebufferResized;
    }

    public int getFramebufferWidth() {
        return m_FramebufferWidth;
    }

    public int getFramebufferHeight() {
        return m_FramebufferHeight;
    }

    long getHandle() {
        if(m_Handle == NULL)
            throw new IllegalStateException("Window has not been created yet!");

        return m_Handle;
    }

    private void updateFramebufferSize() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);

            glfwGetFramebufferSize(m_Handle, width, height);

            m_FramebufferWidth = width.get(0);
            m_FramebufferHeight = height.get(0);
        }
    }

    private void disposeErrorCallback() {
        if(m_ErrorCallback == null) return;

        glfwSetErrorCallback(null);

        m_ErrorCallback.free();
        m_ErrorCallback = null;
    }

}
