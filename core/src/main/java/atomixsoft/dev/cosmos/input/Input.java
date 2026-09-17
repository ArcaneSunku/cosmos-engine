package atomixsoft.dev.cosmos.input;

import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Input {

    private final boolean[] m_KeysDown;
    private final boolean[] m_KeysPressed;
    private final boolean[] m_KeysReleased;

    private final boolean[] m_MouseButtonsDown;
    private final boolean[] m_MouseButtonsPressed;
    private final boolean[] m_MouseButtonsReleased;

    private final StringBuilder m_TextInput;

    private GLFWKeyCallback m_KeyCallback;
    private GLFWCharCallback m_CharCallback;
    private GLFWMouseButtonCallback m_MouseButtonCallback;
    private GLFWCursorPosCallback m_CursorPosCallback;
    private GLFWScrollCallback m_ScrollCallback;
    private GLFWWindowFocusCallback m_WindowFocusCallback;

    private long m_WindowHandle;

    private double m_CursorX;
    private double m_CursorY;

    private double m_CursorDeltaX;
    private double m_CursorDeltaY;

    private double m_ScrollDeltaX;
    private double m_ScrollDeltaY;

    private CursorMode m_CursorMode;

    private boolean m_Focused;
    private boolean m_RawMouseMotionEnabled;
    private boolean m_Initialized;

    public Input() {
        m_KeysDown = new boolean[GLFW_KEY_LAST + 1];
        m_KeysPressed = new boolean[GLFW_KEY_LAST + 1];
        m_KeysReleased = new boolean[GLFW_KEY_LAST + 1];

        m_MouseButtonsDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
        m_MouseButtonsPressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
        m_MouseButtonsReleased = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

        m_TextInput = new StringBuilder();

        m_WindowHandle = NULL;

        m_CursorX = 0.0;
        m_CursorY = 0.0;

        m_CursorDeltaX = 0.0;
        m_CursorDeltaY = 0.0;

        m_ScrollDeltaX = 0.0;
        m_ScrollDeltaY = 0.0;

        m_CursorMode = CursorMode.NORMAL;

        m_Focused = false;
        m_RawMouseMotionEnabled = false;
        m_Initialized = false;
    }

    public void initialize(long windowHandle) {
        if(m_Initialized) return;

        if(windowHandle == NULL)
            throw new IllegalArgumentException("Input requires a valid window handle!");

        m_WindowHandle = windowHandle;

        try {
            m_Focused = glfwGetWindowAttrib(m_WindowHandle, GLFW_FOCUSED) == GLFW_TRUE;
            updateCursorPosition();

            m_KeyCallback = GLFWKeyCallback.create((window, key, scancode, action, mods) -> handleKey(key, action));
            m_CharCallback = GLFWCharCallback.create((window, codepoint) -> handleCharacter(codepoint));
            m_MouseButtonCallback = GLFWMouseButtonCallback.create((window, button, action, mods) -> handleMouseButton(button, action));
            m_CursorPosCallback = GLFWCursorPosCallback.create((window, x, y) -> handleCursorPosition(x, y));
            m_ScrollCallback = GLFWScrollCallback.create((window, xOffset, yOffset) -> handleScroll(xOffset, yOffset));
            m_WindowFocusCallback = GLFWWindowFocusCallback.create((window, focused) -> handleWindowFocus(focused));

            glfwSetKeyCallback(m_WindowHandle, m_KeyCallback);
            glfwSetCharCallback(m_WindowHandle, m_CharCallback);
            glfwSetMouseButtonCallback(m_WindowHandle, m_MouseButtonCallback);
            glfwSetCursorPosCallback(m_WindowHandle, m_CursorPosCallback);
            glfwSetScrollCallback(m_WindowHandle, m_ScrollCallback);
            glfwSetWindowFocusCallback(m_WindowHandle, m_WindowFocusCallback);

            m_Initialized = true;
        } catch(RuntimeException | Error exception) {
            disposeCallbacks();

            m_WindowHandle = NULL;

            throw exception;
        }
    }

    public void beginFrame() {
        validate();

        Arrays.fill(m_KeysPressed, false);
        Arrays.fill(m_KeysReleased, false);

        Arrays.fill(m_MouseButtonsPressed, false);
        Arrays.fill(m_MouseButtonsReleased, false);

        m_TextInput.setLength(0);

        m_CursorDeltaX = 0.0;
        m_CursorDeltaY = 0.0;

        m_ScrollDeltaX = 0.0;
        m_ScrollDeltaY = 0.0;
    }

    public void dispose() {
        if(m_WindowHandle == NULL) return;

        if(m_RawMouseMotionEnabled)
            glfwSetInputMode(m_WindowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);

        glfwSetInputMode(m_WindowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        disposeCallbacks();
        clearInputState();

        m_WindowHandle = NULL;

        m_CursorMode = CursorMode.NORMAL;
        m_Focused = false;
        m_RawMouseMotionEnabled = false;
        m_Initialized = false;
    }

    public boolean isKeyDown(int key) {
        validate();
        validateKey(key);

        return m_KeysDown[key];
    }

    public boolean isKeyPressed(int key) {
        validate();
        validateKey(key);

        return m_KeysPressed[key];
    }

    public boolean isKeyReleased(int key) {
        validate();
        validateKey(key);

        return m_KeysReleased[key];
    }

    public boolean hasTextInput() {
        validate();

        return !m_TextInput.isEmpty();
    }

    public boolean isMouseButtonDown(int button) {
        validate();
        validateMouseButton(button);

        return m_MouseButtonsDown[button];
    }

    public boolean isMouseButtonPressed(int button) {
        validate();
        validateMouseButton(button);

        return m_MouseButtonsPressed[button];
    }

    public boolean isMouseButtonReleased(int button) {
        validate();
        validateMouseButton(button);

        return m_MouseButtonsReleased[button];
    }

    public String getTextInput() {
        validate();
        return m_TextInput.toString();
    }

    public boolean isShiftDown() {
        validate();

        return m_KeysDown[GLFW_KEY_LEFT_SHIFT] ||
                m_KeysDown[GLFW_KEY_RIGHT_SHIFT];
    }

    public boolean isControlDown() {
        validate();

        return m_KeysDown[GLFW_KEY_LEFT_CONTROL] ||
                m_KeysDown[GLFW_KEY_RIGHT_CONTROL];
    }

    public boolean isAltDown() {
        validate();

        return m_KeysDown[GLFW_KEY_LEFT_ALT] ||
                m_KeysDown[GLFW_KEY_RIGHT_ALT];
    }

    public boolean isSuperDown() {
        validate();

        return m_KeysDown[GLFW_KEY_LEFT_SUPER] ||
                m_KeysDown[GLFW_KEY_RIGHT_SUPER];
    }

    public double getCursorX() {
        validate();
        return m_CursorX;
    }

    public double getCursorY() {
        validate();
        return m_CursorY;
    }

    public double getCursorDeltaX() {
        validate();
        return m_CursorDeltaX;
    }

    public double getCursorDeltaY() {
        validate();
        return m_CursorDeltaY;
    }

    public double getScrollDeltaX() {
        validate();
        return m_ScrollDeltaX;
    }

    public double getScrollDeltaY() {
        validate();
        return m_ScrollDeltaY;
    }

    public boolean isFocused() {
        validate();
        return m_Focused;
    }

    public CursorMode getCursorMode() {
        validate();
        return m_CursorMode;
    }

    public void setCursorMode(CursorMode cursorMode) {
        validate();

        if(cursorMode == null)
            throw new IllegalArgumentException("Cursor mode cannot be null!");

        if(m_CursorMode == cursorMode)
            return;

        glfwSetInputMode(m_WindowHandle, GLFW_CURSOR, cursorMode.getGLFWValue());

        m_CursorMode = cursorMode;
        updateCursorPosition();

        m_CursorDeltaX = 0.0;
        m_CursorDeltaY = 0.0;
    }

    public boolean isRawMouseMotionSupported() {
        validate();
        return glfwRawMouseMotionSupported();
    }

    public boolean isRawMouseMotionEnabled() {
        validate();
        return m_RawMouseMotionEnabled;
    }

    public void setRawMouseMotionEnabled(boolean enabled) {
        validate();

        if(enabled && !glfwRawMouseMotionSupported())
            throw new IllegalStateException("Raw mouse motion is not supported on this system!");

        glfwSetInputMode(m_WindowHandle, GLFW_RAW_MOUSE_MOTION, enabled ? GLFW_TRUE : GLFW_FALSE);
        m_RawMouseMotionEnabled = enabled;
    }

    private void handleKey(int key, int action) {
        if(key < 0 || key > GLFW_KEY_LAST)
            return;

        switch(action) {
            case GLFW_PRESS -> {
                m_KeysDown[key] = true;
                m_KeysPressed[key] = true;
            }

            case GLFW_RELEASE -> {
                m_KeysDown[key] = false;
                m_KeysReleased[key] = true;
            }

            case GLFW_REPEAT ->
                    m_KeysDown[key] = true;
        }
    }

    private void handleCharacter(int codepoint) {
        m_TextInput.appendCodePoint(codepoint);
    }

    private void handleMouseButton(int button, int action) {
        if(button < 0 || button > GLFW_MOUSE_BUTTON_LAST)
            return;

        switch(action) {
            case GLFW_PRESS -> {
                m_MouseButtonsDown[button] = true;
                m_MouseButtonsPressed[button] = true;
            }

            case GLFW_RELEASE -> {
                m_MouseButtonsDown[button] = false;
                m_MouseButtonsReleased[button] = true;
            }
        }
    }

    private void handleCursorPosition(double x, double y) {
        m_CursorDeltaX += x - m_CursorX;
        m_CursorDeltaY += y - m_CursorY;

        m_CursorX = x;
        m_CursorY = y;
    }

    private void handleScroll(double xOffset, double yOffset) {
        m_ScrollDeltaX += xOffset;
        m_ScrollDeltaY += yOffset;
    }

    private void handleWindowFocus(boolean focused) {
        m_Focused = focused;

        if(!focused) {
            releaseInputState();

            m_CursorDeltaX = 0.0;
            m_CursorDeltaY = 0.0;

            m_ScrollDeltaX = 0.0;
            m_ScrollDeltaY = 0.0;

            m_TextInput.setLength(0);

            return;
        }

        updateCursorPosition();

        m_CursorDeltaX = 0.0;
        m_CursorDeltaY = 0.0;
    }

    private void releaseInputState() {
        for(int key = 0; key < m_KeysDown.length; key++) {
            if(!m_KeysDown[key])
                continue;

            m_KeysDown[key] = false;
            m_KeysPressed[key] = false;
            m_KeysReleased[key] = true;
        }

        for(int button = 0; button < m_MouseButtonsDown.length; button++) {
            if(!m_MouseButtonsDown[button])
                continue;

            m_MouseButtonsDown[button] = false;
            m_MouseButtonsPressed[button] = false;
            m_MouseButtonsReleased[button] = true;
        }
    }

    private void clearInputState() {
        Arrays.fill(m_KeysDown, false);
        Arrays.fill(m_KeysPressed, false);
        Arrays.fill(m_KeysReleased, false);

        Arrays.fill(m_MouseButtonsDown, false);
        Arrays.fill(m_MouseButtonsPressed, false);
        Arrays.fill(m_MouseButtonsReleased, false);

        m_TextInput.setLength(0);

        m_CursorDeltaX = 0.0;
        m_CursorDeltaY = 0.0;

        m_ScrollDeltaX = 0.0;
        m_ScrollDeltaY = 0.0;
    }

    private void updateCursorPosition() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);

            glfwGetCursorPos(m_WindowHandle, x, y);

            m_CursorX = x.get(0);
            m_CursorY = y.get(0);
        }
    }

    private void disposeCallbacks() {
        if(m_KeyCallback != null) {
            glfwSetKeyCallback(m_WindowHandle, null);

            m_KeyCallback.free();
            m_KeyCallback = null;
        }

        if(m_CharCallback != null) {
            glfwSetCharCallback(m_WindowHandle, null);

            m_CharCallback.free();
            m_CharCallback = null;
        }

        if(m_MouseButtonCallback != null) {
            glfwSetMouseButtonCallback(m_WindowHandle, null);

            m_MouseButtonCallback.free();
            m_MouseButtonCallback = null;
        }

        if(m_CursorPosCallback != null) {
            glfwSetCursorPosCallback(m_WindowHandle, null);

            m_CursorPosCallback.free();
            m_CursorPosCallback = null;
        }

        if(m_ScrollCallback != null) {
            glfwSetScrollCallback(m_WindowHandle, null);

            m_ScrollCallback.free();
            m_ScrollCallback = null;
        }

        if(m_WindowFocusCallback != null) {
            glfwSetWindowFocusCallback(m_WindowHandle, null);

            m_WindowFocusCallback.free();
            m_WindowFocusCallback = null;
        }
    }

    private void validateKey(int key) {
        if(key < 0 || key > GLFW_KEY_LAST)
            throw new IllegalArgumentException("Invalid keyboard key: " + key);
    }

    private void validateMouseButton(int button) {
        if(button < 0 || button > GLFW_MOUSE_BUTTON_LAST)
            throw new IllegalArgumentException("Invalid mouse button: " + button);
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Input has not been initialized!");
    }

}