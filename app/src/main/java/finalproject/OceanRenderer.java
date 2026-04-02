import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import finalproject.Camera;
import finalproject.OceanMesh;
import finalproject.ShaderProgram;

public class OceanRenderer {

    private long window;
    private int width = 1280;
    private int height = 720;
    private String title = "Ocean Simulator";

    private double lastMouseX = width / 2.0;
    private double lastMouseY = height / 2.0;
    private boolean firstMouse = true;

    private OceanMesh ocean;
    private Camera camera;

    private float time = 0.0f;
    private float timeOfDay = 0.0f; // 0.0 midnight, 0.5 = noon, 1.0 = midnight again

    private boolean tsunamiActive = false;
    private float tsunamiStrength = 0.0f;

    private ShaderProgram shaderProgram;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // 1. Initialize GLFW
        if (!GLFW.glfwInit()) 
            throw new IllegalStateException("Unable to initialize GLFW");

        // 2. Set window hints (OpenGL version, core profile, etc.)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // 3. Create the window and store it in 'window'
        window = GLFW.glfwCreateWindow(width, height, "Ocean Simulation", 0, 0);
        if(window == 0) {
            throw new RuntimeException("Failed to crea the GLFW window");
        }

        // 4. Set up a key callback so ESC closes the window
        GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(win, true);
            }
            if (key == GLFW.GLFW_KEY_T && action == GLFW.GLFW_PRESS) {
                tsunamiActive = true;
            }
        });

        // 5. Center the window on the monitor (optional but nice)
        long monitor = GLFW.glfwGetPrimaryMonitor();

        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
        int monitorWidth = videoMode.width();
        int monitorHeight = videoMode.height();

        GLFW.glfwSetWindowPos(window, (monitorWidth - width) / 2, (monitorHeight - height) / 2);

        GLFW.glfwShowWindow(window);

        // 6. Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);

        // 7. Call GL.createCapabilities() — don't forget this!
        GL.createCapabilities();

        // 8. Set the clear color
        GL11.glClearColor(0.0f, 0.1f, 0.2f, 1.0f); // deep ocean blue

        GL11.glEnable(GL11.GL_DEPTH_TEST);

        ocean = new OceanMesh(100, 1.0f);
        camera = new Camera(new Vector3f(0, 5, 20));

        String vertSrc = ShaderProgram.loadFile("shaders/ocean.vert");
        String fragSrc = ShaderProgram.loadFile("shaders/ocean.frag");
        shaderProgram = new ShaderProgram(vertSrc, fragSrc); 

        // Mouse Callback
        GLFW.glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }

            float deltaX = (float)(xpos - lastMouseX);
            float deltaY = (float)(ypos - lastMouseY);

            lastMouseX = xpos;
            lastMouseY = ypos;

            camera.processMouse(deltaX, deltaY);
        });

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    private void loop() {
        // Keep running until the window should close
        while (!GLFW.glfwWindowShouldClose(window)) {
            camera.processKeyboard(window);
        
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            
            shaderProgram.bind();

            shaderProgram.setUniformMatrix4f("projection", camera.getProjectionMatrix(width, height));
            shaderProgram.setUniformMatrix4f("view", camera.getViewMatrix());
            shaderProgram.setUniformMatrix4f("model", new org.joml.Matrix4f());

            timeOfDay += 0.0001f; // Controls how fast the day cycles
            if (timeOfDay > 1.0f) timeOfDay = 0.0f;
            time += 0.016f;
            shaderProgram.setUniformFloat("time", time);
            shaderProgram.setUniformFloat("frequency", 0.3f);
            shaderProgram.setUniformFloat("speed", 1.5f);
            shaderProgram.setUniformVec2("direction", 1.0f, 0.0f);

            float[] midnight = {0.01f, 0.01f, 0.05f};
            float[] dawn = {0.8f,  0.4f,  0.2f };
            float[] noon = {0.3f,  0.6f,  1.0f };
            float[] dusk = {0.6f,  0.2f,  0.1f };

            float skyR, skyG, skyB;

            if(timeOfDay < 0.25f) {
                float t = timeOfDay / 0.25f;
                skyR = lerp(midnight[0], dawn[0], t);
                skyG = lerp(midnight[1], dawn[1], t);
                skyB = lerp(midnight[2], dawn[2], t);
            } else if (timeOfDay < 0.5f) {
                float t = (timeOfDay - 0.25f) / 0.25f;
                skyR = lerp(dawn[0], noon[0], t);
                skyG = lerp(dawn[1], noon[1], t);
                skyB = lerp(dawn[2], noon[2], t);
            } else if (timeOfDay < 0.75f) {
                float t = (timeOfDay - 0.50f) / 0.25f;
                skyR = lerp(noon[0], dusk[0], t);
                skyG = lerp(noon[1], dusk[1], t);
                skyB = lerp(noon[2], dusk[2], t);
            } else {
                float t = (timeOfDay - 0.75f) / 0.25f;
                skyR = lerp(dusk[0], midnight[0], t);
                skyG = lerp(dusk[1], midnight[1], t);
                skyB = lerp(dusk[2], midnight[2], t);
            }

            GL11.glClearColor(skyR, skyG, skyB, 1.0f);

            float sunAngle = timeOfDay * 2.0f * (float)Math.PI;
            float sunX = (float)Math.cos(sunAngle);
            float sunY = (float)Math.sin(sunAngle);
            shaderProgram.setUniformVec3("sunDirection", sunX, sunY, 0.0f);

            float moonAngle = sunAngle + (float)Math.PI;
            float moonY = (float)Math.sin(moonAngle);

            float tidalAmplitude = 0.8f + 0.6f * Math.max(moonY, 0.0f);

            if (tsunamiActive) {
                tsunamiStrength = Math.min(tsunamiStrength + 0.02f, 1.0f);
            }

            float finalAmplitude = tidalAmplitude + tsunamiStrength * 8.0f;
            shaderProgram.setUniformFloat("amplitude", finalAmplitude);

            // Rendering goes here
            ocean.render();

            shaderProgram.unbind();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        // Free the window
        GLFW.glfwDestroyWindow(window);

        shaderProgram.cleanup();
        ocean.cleanup();

        // Terminate GLFW
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new OceanRenderer().run();
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}