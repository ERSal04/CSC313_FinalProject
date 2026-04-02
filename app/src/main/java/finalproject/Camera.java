package finalproject;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Camera {
    private Vector3f position;
    private Vector3f front;
    private Vector3f up;

    private float yaw = -90.0f;
    private float pitch = 0.0f;
    private float speed = 0.05f;
    private float sensitivity = 0.01f;
    private float fov = 45.0f;
    
    public Camera(Vector3f startPosition) {
        this.position = startPosition;
        this.front = new Vector3f(0, 0, -1);
        this.up = new Vector3f(0, 1, 0);
    }

    public Matrix4f getViewMatrix() {
        Vector3f target = new Vector3f();
        position.add(front, target);

        return new Matrix4f().lookAt(position, target, up);
    }

    public Matrix4f getProjectionMatrix(int width, int height) {
        return new Matrix4f().perspective(
            (float) Math.toRadians(fov), 
            (float) width / height, 
            0.1f, 
            1000.0f
        );
    }

    public void processKeyboard(long window) {
        Vector3f right = new Vector3f();
        front.cross(up, right).normalize();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS)
             position.add(new Vector3f(front).mul(speed));

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS)
             position.sub(new Vector3f(front).mul(speed));

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS)
             position.sub(new Vector3f(right).mul(speed));

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS)
             position.add(new Vector3f(right).mul(speed));
    
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Q) == GLFW.GLFW_PRESS)
             position.sub(new Vector3f(up).mul(speed));
            
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_E) == GLFW.GLFW_PRESS)
             position.add(new Vector3f(up).mul(speed));
    }

    public void processMouse(float deltaX, float deltaY) {
        yaw += deltaX * sensitivity;
        pitch -= deltaY * sensitivity;

        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        float x = (float)(Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        float y = (float)(Math.sin(Math.toRadians(pitch)));
        float z = (float)(Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
    
        front = new Vector3f(x, y, z).normalize();
    }
}
