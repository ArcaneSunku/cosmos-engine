package atomixsoft.dev.cosmos.editor.camera;

import atomixsoft.dev.cosmos.input.CursorMode;
import atomixsoft.dev.cosmos.input.Input;
import atomixsoft.dev.cosmos.spatial.Transform;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public final class FreeCameraController {

    private static final float MAX_PITCH = (float) Math.toRadians(89.0);

    private final Transform m_Transform;

    private final Vector3f m_Movement;
    private final Vector3f m_Direction;
    private final Quaternionf m_Rotation;

    private float m_Yaw;
    private float m_Pitch;

    private final float m_MoveSpeed;
    private final float m_FastMultiplier;
    private final float m_MouseSensitivity;

    private boolean m_CapturingMouse;

    public FreeCameraController(Transform transform) {
        if (transform == null) throw new IllegalArgumentException("Camera transform cannot be null!");

        m_Transform = transform;

        m_Movement = new Vector3f();
        m_Direction = new Vector3f();
        m_Rotation = new Quaternionf();

        m_Yaw = 0.0f;
        m_Pitch = 0.0f;

        m_MoveSpeed = 3.0f;
        m_FastMultiplier = 3.0f;

        m_MouseSensitivity = 0.0025f;

        m_CapturingMouse = false;
    }

    public void update(Input input, double deltaTime) {
        if (input == null) throw new IllegalArgumentException("Input cannot be null!");

        if (!input.isFocused()) {
            releaseMouse(input);
            return;
        }

        updateMouseCapture(input);
        if (!m_CapturingMouse) return;

        updateRotation(input);
        updateMovement(input, (float) deltaTime);
    }

    public void release(Input input) {
        if (input == null) throw new IllegalArgumentException("Input cannot be null!");
        releaseMouse(input);
    }

    private void updateMouseCapture(Input input) {
        if (input.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT))
            captureMouse(input);

        if (input.isMouseButtonReleased(GLFW_MOUSE_BUTTON_RIGHT))
            releaseMouse(input);
    }

    private void captureMouse(Input input) {
        if (m_CapturingMouse) return;
        input.setCursorMode(CursorMode.DISABLED);

        if (input.isRawMouseMotionSupported())
            input.setRawMouseMotionEnabled(true);

        m_CapturingMouse = true;
    }

    private void releaseMouse(Input input) {
        if (!m_CapturingMouse) return;

        if (input.isRawMouseMotionEnabled())
            input.setRawMouseMotionEnabled(false);

        input.setCursorMode(CursorMode.NORMAL);
        m_CapturingMouse = false;
    }

    private void updateRotation(Input input) {
        m_Yaw -= (float) input.getCursorDeltaX() * m_MouseSensitivity;
        m_Pitch -= (float) input.getCursorDeltaY() * m_MouseSensitivity;

        m_Pitch = Math.clamp(m_Pitch, -MAX_PITCH, MAX_PITCH);
        m_Rotation.identity().rotationYXZ(m_Yaw, m_Pitch, 0.0f);

        m_Transform.setRotation(m_Rotation);
    }

    private void updateMovement(Input input, float deltaTime) {
        m_Movement.zero();

        if (input.isKeyDown(GLFW_KEY_W)) {
            m_Transform.getForward(m_Direction);
            m_Movement.add(m_Direction);
        }

        if (input.isKeyDown(GLFW_KEY_S)) {
            m_Transform.getForward(m_Direction);
            m_Movement.sub(m_Direction);
        }

        if (input.isKeyDown(GLFW_KEY_D)) {
            m_Transform.getRight(m_Direction);
            m_Movement.add(m_Direction);
        }

        if (input.isKeyDown(GLFW_KEY_A)) {
            m_Transform.getRight(m_Direction);
            m_Movement.sub(m_Direction);
        }

        if (input.isKeyDown(GLFW_KEY_SPACE))
            m_Movement.y += 1.0f;

        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL))
            m_Movement.y -= 1.0f;

        if (m_Movement.lengthSquared() == 0.0f) return;

        float speed = m_MoveSpeed;
        if (input.isShiftDown()) speed *= m_FastMultiplier;

        m_Movement.normalize().mul(speed * deltaTime);
        m_Transform.translate(m_Movement.x, m_Movement.y, m_Movement.z);
    }

}