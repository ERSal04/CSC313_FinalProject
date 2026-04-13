import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import finalproject.Camera;
import finalproject.CityMesh;
import finalproject.OceanMesh;
import finalproject.ShaderProgram;

public class OceanRenderer {

    private long window;
    private int width = 1280;
    private int height = 720;

    private double lastMouseX = width / 2.0;
    private double lastMouseY = height / 2.0;
    private boolean firstMouse = true;

    private OceanMesh ocean;
    private Camera camera;

    private float time = 0.0f;
    private float timeOfDay = 0.0f;

    private boolean tsunamiActive = false;
    private float tsunamiStrength = 0.0f;
    private float tsunamiTime   = 0.0f;
    private float tsunamiOriginX = -200.0f; // out at sea, west of city
    private float tsunamiOriginZ = -200.0f;
    private float frequency = 0.15f;

    private boolean wireframe = false;

    private CityMesh city;
    private ShaderProgram cityShader;
    private float waterLevel = 0.0f;

    private ShaderProgram shaderProgram;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow(width, height, "Ocean Simulation", 0, 0);
        if (window == 0)
            throw new RuntimeException("Failed to create the GLFW window");

        GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS)
                GLFW.glfwSetWindowShouldClose(win, true);
            if (key == GLFW.GLFW_KEY_T && action == GLFW.GLFW_PRESS)
                tsunamiActive = !tsunamiActive; // toggle on/off
            if (key == GLFW.GLFW_KEY_F && action == GLFW.GLFW_PRESS) {
                wireframe = !wireframe;
            }
        });

        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
        GLFW.glfwSetWindowPos(window,
            (videoMode.width()  - width)  / 2,
            (videoMode.height() - height) / 2);

        GLFW.glfwShowWindow(window);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);

        GL.createCapabilities();

        GL11.glClearColor(0.0f, 0.1f, 0.2f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // Grid size of the ocean. can make it larger using the graphics computer
        ocean  = new OceanMesh(300, 2.0f);
        camera = new Camera(new Vector3f(0, 25, -80), 90.0f);

        String vertSrc = ShaderProgram.loadFile("shaders/ocean.vert");
        String fragSrc = ShaderProgram.loadFile("shaders/ocean.frag");
        shaderProgram  = new ShaderProgram(vertSrc, fragSrc);

        city       = new CityMesh();
        String cityVert = ShaderProgram.loadFile("shaders/city.vert");
        String cityFrag = ShaderProgram.loadFile("shaders/city.frag");
        cityShader = new ShaderProgram(cityVert, cityFrag);

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
        while (!GLFW.glfwWindowShouldClose(window)) {
            camera.processKeyboard(window);

            // --- Sky color driven by time of day ---
            timeOfDay += 0.0001f;
            if (timeOfDay > 1.0f) timeOfDay = 0.0f;

            float[] midnight = {0.03f, 0.03f, 0.12f};
            float[] dawn     = {0.8f,  0.4f,  0.2f };
            float[] noon     = {0.3f,  0.6f,  1.0f };
            float[] dusk     = {0.55f,  0.28f,  0.20f};

            float skyR, skyG, skyB;
            if (timeOfDay < 0.25f) {
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
                float t = (timeOfDay - 0.5f) / 0.25f;
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
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // Tsunami ramp 
            if (tsunamiActive) {
                tsunamiStrength = Math.min(tsunamiStrength + 0.02f, 1.0f);
                tsunamiTime    += 0.016f;
            } else if (tsunamiTime > 0) {
                // Reset after wave passes
                tsunamiTime = 0.0f;
                tsunamiStrength = 0.0f;
            }

            // Tidal amplitude from moon position 
            float sunAngle  = timeOfDay * 2.0f * (float)Math.PI;
            float moonAngle = sunAngle + (float)Math.PI;
            float moonY     = (float)Math.sin(moonAngle);
            float tidalAmplitude = 0.8f + 0.6f * Math.max(moonY, 0.0f);
            float finalAmplitude = tidalAmplitude + tsunamiStrength * 8.0f;

            // Shader uniforms 
            shaderProgram.bind();

            shaderProgram.setUniformMatrix4f("projection", camera.getProjectionMatrix(width, height));
            shaderProgram.setUniformMatrix4f("view",       camera.getViewMatrix());
            shaderProgram.setUniformMatrix4f("model",      new org.joml.Matrix4f());

            // sends camera world position for specular lighting
            shaderProgram.setUniformVec3("cameraPos", camera.getPosition());

            time += 0.016f;
            shaderProgram.setUniformFloat("time",      time);
            float safeAmplitude = Math.min(finalAmplitude, 0.9f / frequency);
            shaderProgram.setUniformFloat("amplitude", safeAmplitude);
            shaderProgram.setUniformFloat("frequency", 0.15f);  // halved — wider waves
            shaderProgram.setUniformFloat("speed",     0.8f);   // slower — more realistic
            shaderProgram.setUniformVec2("direction",  1.0f, 0.0f);

            float sunX = (float)Math.cos(sunAngle);
            float sunY = (float)Math.sin(sunAngle);
            shaderProgram.setUniformVec3("sunDirection", sunX, sunY, 0.3f);

            shaderProgram.setUniformInt  ("tsunamiActive", tsunamiActive ? 1 : 0);
            shaderProgram.setUniformVec2 ("tsunamiOrigin", tsunamiOriginX, tsunamiOriginZ);
            shaderProgram.setUniformFloat("tsunamiTime",   tsunamiTime);

            if (wireframe) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            } else {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            }

            ocean.render();
            shaderProgram.unbind();

            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

            // Distance from tsunami origin to the center of the city
            // City center is roughly at x=0, z=40
            float cityDist = (float)Math.sqrt(
                Math.pow(0 - tsunamiOriginX, 2) +
                Math.pow(40 - tsunamiOriginZ, 2)
            );

            // Has the wave front reached the city yet?
            float waveFrontAtCity = tsunamiTime * 40.0f; // same waveSpeed as shader
            float arrivalFactor   = clamp(
                (waveFrontAtCity - cityDist) / 30.0f, 0.0f, 1.0f);

            waterLevel = tsunamiActive
                ? finalAmplitude * arrivalFactor
                : finalAmplitude * 0.1f * (float)Math.sin(time * 0.5f);
                
            cityShader.bind();
            cityShader.setUniformMatrix4f("projection", camera.getProjectionMatrix(width, height));
            cityShader.setUniformMatrix4f("view",       camera.getViewMatrix());
            cityShader.setUniformMatrix4f("model",      new org.joml.Matrix4f());
            cityShader.setUniformVec3("sunDirection",   sunX, sunY, 0.3f);
            cityShader.setUniformVec3("cameraPos",      camera.getPosition());
            cityShader.setUniformFloat("waterLevel",    waterLevel);
            city.render();
            cityShader.unbind();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        shaderProgram.cleanup();
        ocean.cleanup();
        cityShader.cleanup();
        city.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new OceanRenderer().run();
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}