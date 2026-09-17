package atomixsoft.dev.cosmos.input;

import static org.lwjgl.glfw.GLFW.*;

public enum CursorMode {

    NORMAL(GLFW_CURSOR_NORMAL),
    HIDDEN(GLFW_CURSOR_HIDDEN),
    DISABLED(GLFW_CURSOR_DISABLED),
    CAPTURED(GLFW_CURSOR_CAPTURED);

    private final int m_GLFWValue;

    CursorMode(int glfwValue) {
        m_GLFWValue = glfwValue;
    }

    int getGLFWValue() {
        return m_GLFWValue;
    }

}
