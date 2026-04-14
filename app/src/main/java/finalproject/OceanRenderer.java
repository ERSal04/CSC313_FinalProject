import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import finalproject.Camera;
import finalproject.CityManager;
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

    private boolean cinematicMode = false;
    private float   cinematicTime = 0.0f;

    private float time = 0.0f;
    private float timeOfDay = 0.0f;

    private boolean tsunamiActive = false;
    private float tsunamiStrength = 0.0f;
    private float tsunamiTime   = 0.0f;
    private float tsunamiOriginX =    0.0f;  // centered on city
    private float tsunamiOriginZ = -250.0f;  // directly out to sea
    private float frequency = 0.15f;

    private float boatX     =  -70.0f;  // matches dock position in CityManager
    private float boatZ     =   -15.0f;
    private float boatYaw   =    90.0f;  // rotation when swept by tsunami
    private float boatSweepZ=    0.0f;  // how far tsunami has pushed the boat

    private boolean wireframe = false;

    private CityManager city;
    private ShaderProgram cityShader;
    private float waterLevel = 0.0f;
    private float speed = 0.8f;

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
                tsunamiActive = !tsunamiActive;

            if (key == GLFW.GLFW_KEY_F && action == GLFW.GLFW_PRESS)
                wireframe = !wireframe;

            if (key == GLFW.GLFW_KEY_L && action == GLFW.GLFW_PRESS)
                timeOfDay = 0.35f;

            // R resets the entire simulation to starting state
            if (key == GLFW.GLFW_KEY_R && action == GLFW.GLFW_PRESS) {
                tsunamiActive   = false;
                tsunamiStrength = 0.0f;
                tsunamiTime     = 0.0f;
                waterLevel      = 0.0f;
                cinematicMode   = false;
                cinematicTime   = 0.0f;
                boatSweepZ = 0.0f; boatYaw = 0.0f;
                System.out.println("Simulation reset");
            }

            // C toggles cinematic camera mode
            if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS) {
                cinematicMode = !cinematicMode;
                cinematicTime = 0.0f;
                System.out.println("Cinematic mode: " + cinematicMode);
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

        city       = new CityManager();
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
            if (cinematicMode) {
                updateCinematicCamera();
            } else {
                camera.processKeyboard(window);
            }

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
            } else {
                // Let tsunamiTime drain slowly so flood recedes naturally
                if (tsunamiTime > 0) {
                    tsunamiTime     = Math.max(0.0f, tsunamiTime     - 0.005f);
                    tsunamiStrength = Math.max(0.0f, tsunamiStrength - 0.005f);
                    waterLevel      = Math.max(0.0f, waterLevel      - 0.05f);
                }
            }

            float sunAngle  = timeOfDay * 2.0f * (float)Math.PI;
            float moonAngle = sunAngle + (float)Math.PI;
            float moonY     = (float)Math.sin(moonAngle);

            float sunX = (float)Math.cos(sunAngle);
            float sunY = (float)Math.sin(sunAngle);

            float duskFactor = 1.0f - Math.min(1.0f, Math.abs(sunY) * 2.0f);
            float fogStart   = 100.0f - duskFactor * 40.0f;
            float fogEnd     = 400.0f - duskFactor * 150.0f;

            // Spring/neap cycle — stronger tides when sun and moon align
            float sunMoonAlignment = (float)Math.cos(sunAngle - moonAngle);
            float tidalStrength    = 0.7f + 0.3f * sunMoonAlignment;

            // Wave height varies with moon position
            float tidalAmplitude = (0.8f + 0.6f * Math.max(moonY, 0.0f))
                                * tidalStrength;

            // Tidal water level — slow rise and fall of the whole surface
            float tidalWaterLevel = 0.5f * Math.max(moonY, 0.0f) * tidalStrength;

            float finalAmplitude = tsunamiActive
                ? tidalAmplitude * 0.3f
                : tidalAmplitude;

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
            shaderProgram.setUniformFloat("tidalOffset", tidalWaterLevel);

            shaderProgram.setUniformVec3("skyColor", skyR, skyG, skyB);
            shaderProgram.setUniformFloat("fogStart", fogStart);
            shaderProgram.setUniformFloat("fogEnd",   fogEnd);

            shaderProgram.setUniformVec3("sunDirection", sunX, sunY, 0.3f);

            shaderProgram.setUniformFloat("timeOfDay", timeOfDay);

            // Moon direction — opposite side of sky from sun
            float moonX = (float)Math.cos(sunAngle + Math.PI);
            float moonY2 = (float)Math.sin(sunAngle + Math.PI);
            shaderProgram.setUniformVec3("moonDirection", moonX, moonY2, 0.3f);

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
                Math.pow(0   - tsunamiOriginX, 2) +
                Math.pow(60  - tsunamiOriginZ, 2)
            );

            // Has the wave front reached the city yet?
            float waveFrontAtCity = tsunamiTime * 40.0f; // same waveSpeed as shader
            float arrivalFactor   = clamp(
                (waveFrontAtCity - cityDist) / 30.0f, 0.0f, 1.0f);

            float peakFloodHeight = 18.0f;
            float floodDecay = Math.max(0.0f, tsunamiTime - 15.0f) * 0.3f;
            waterLevel = tsunamiActive
                ? peakFloodHeight * arrivalFactor * Math.max(0.3f,
                    1.0f - floodDecay * 0.05f)
                : tidalAmplitude * 0.1f * (float)Math.sin(time * 0.5f);

            
            // --- Boat simulation ---
            float boatWaveY = sampleWaveHeight(boatX, boatZ + boatSweepZ, time) + tidalWaterLevel;

            if (tsunamiActive) {
                // Wave front distance from boat
                float bdx = boatX - tsunamiOriginX;
                float bdz = (boatZ + boatSweepZ) - tsunamiOriginZ;
                float boatDist    = (float)Math.sqrt(bdx*bdx + bdz*bdz);
                float waveFrontB  = tsunamiTime * 40.0f;
                float distFromFrontB = boatDist - waveFrontB;

                // When wave reaches boat, sweep it inland
                if (distFromFrontB < 20.0f && boatWaveY > 2.0f) {
                    boatSweepZ += 0.4f;   // carried forward by surge
                    boatYaw    += 1.2f;   // spinning as it tumbles
                }
            }

            // Pass to city manager
            city.setBoatTransform(boatX, boatWaveY, boatZ + boatSweepZ, boatYaw);
                
            cityShader.bind();
            cityShader.setUniformMatrix4f("projection", camera.getProjectionMatrix(width, height));
            cityShader.setUniformMatrix4f("view",       camera.getViewMatrix());
            cityShader.setUniformMatrix4f("model",      new org.joml.Matrix4f());
            cityShader.setUniformVec3("sunDirection",   sunX, sunY, 0.3f);
            cityShader.setUniformVec3("cameraPos",      camera.getPosition());
            cityShader.setUniformFloat("waterLevel",    waterLevel);

            cityShader.setUniformVec3("skyColor",   skyR, skyG, skyB);
            cityShader.setUniformFloat("fogStart", fogStart);
            cityShader.setUniformFloat("fogEnd",   fogEnd);

            cityShader.setUniformFloat("tsunamiTime",    tsunamiActive ? tsunamiTime : 0.0f);
            cityShader.setUniformInt(  "tsunamiActive",  tsunamiActive ? 1 : 0);
            cityShader.setUniformFloat("tsunamiOriginX", tsunamiOriginX);  // should already exist
            cityShader.setUniformFloat("tsunamiOriginZ", tsunamiOriginZ);  // should already exist

            cityShader.setUniformVec3("moonDirection",  moonX, moonY2, 0.3f);
            
            city.render(cityShader);
            cityShader.unbind();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void updateCinematicCamera() {
        cinematicTime += 0.016f;

        // The cinematic sequence has three shots that cycle:
        //
        // Shot 1 (0-15s)  — Wide establishing shot from out at sea
        //                   looking toward the city
        //
        // Shot 2 (15-30s) — Side angle showing wave approaching
        //                   from the left, city on the right
        //
        // Shot 3 (30-45s) — Low dramatic angle at water level
        //                   looking along the coastline
        //
        // Shot 4 (45-60s) — High overhead looking straight down
        //                   at the city as water floods in
        //
        // After 60s it loops back to shot 1

        float t = cinematicTime % 60.0f;

        Vector3f targetPos;
        float    targetYaw;
        float    targetPitch;

        if (t < 15.0f) {
            // Shot 1 — wide establishing shot from sea
            // Slowly drift forward toward the city
            float drift = t / 15.0f;
            targetPos   = new Vector3f(
                lerp(  0, 0,   drift),
                lerp( 35, 28,  drift),
                lerp(-180, -120, drift)
            );
            targetYaw   = 90.0f;   // looking toward positive Z (city)
            targetPitch = -15.0f;

        } else if (t < 30.0f) {
            // Shot 2 — side angle, wave approaching from left
            float drift = (t - 15.0f) / 15.0f;
            targetPos   = new Vector3f(
                lerp(-150, -100, drift),
                lerp(  20,   25, drift),
                lerp( -50,    0, drift)
            );
            targetYaw   = 30.0f;   // angled toward city and wave
            targetPitch = -10.0f;

        } else if (t < 45.0f) {
            // Shot 3 — low dramatic angle at water level
            float drift = (t - 30.0f) / 15.0f;
            targetPos   = new Vector3f(
                lerp( 60,  20, drift),
                lerp(  6,   8, drift),
                lerp(-30, -10, drift)
            );
            targetYaw   = 100.0f;  // slightly angled
            targetPitch =  -5.0f;  // nearly level with water

        } else {
            // Shot 4 — high overhead
            float drift = (t - 45.0f) / 15.0f;
            targetPos   = new Vector3f(
                lerp(  0,  20, drift),
                lerp(150, 130, drift),
                lerp( 50,  60, drift)
            );
            targetYaw   = 90.0f;
            targetPitch = -85.0f;  // looking almost straight down
        }

        // Smoothly move camera toward target position
        // This prevents jarring cuts between shots
        Vector3f currentPos = camera.getPosition();
        float smoothSpeed   = 0.03f;
        currentPos.x = lerp(currentPos.x, targetPos.x, smoothSpeed);
        currentPos.y = lerp(currentPos.y, targetPos.y, smoothSpeed);
        currentPos.z = lerp(currentPos.z, targetPos.z, smoothSpeed);

        // Smoothly rotate toward target angles
        camera.setCinematicAngles(targetYaw, targetPitch, smoothSpeed);
    }

    private float sampleWaveHeight(float wx, float wz, float t) {
        if (tsunamiActive) {
            float dx        = wx - tsunamiOriginX;
            float dz        = wz - tsunamiOriginZ;
            float dist      = (float)Math.sqrt(dx*dx + dz*dz);
            float waveFront = tsunamiTime * 40.0f;
            float distFromFront = dist - waveFront;

            float depth    = Math.max(0, Math.min(1, (-wz + 15.0f) / 315.0f));
            float shoaling = 1.0f + (1.0f - depth) * 3.0f;
            float tsunamiShoreMask = smoothstep(300.0f, 0.0f, wz);
            shoaling *= tsunamiShoreMask;

            // Main wave envelope
            float frontWidth = 25.0f, backWidth = 80.0f;
            float envelope;
            if (distFromFront <= 0.0f)
                envelope = (float)Math.exp(-(distFromFront*distFromFront)
                        / (backWidth*backWidth));
            else
                envelope = (float)Math.exp(-(distFromFront*distFromFront)
                        / (frontWidth*frontWidth));

            float mainWave = 15.0f * envelope * shoaling;

            // Drawback
            float drawback = 0.0f;
            if (distFromFront > 0.0f && distFromFront < 120.0f) {
                float drawT = distFromFront / 120.0f;
                drawback = -3.5f * (float)Math.sin(drawT * Math.PI)
                        * shoaling * 0.4f;
            }

            // Surge
            float surge = 0.0f;
            if (distFromFront < -30.0f) {
                float surgeDecay = Math.min(1.0f,
                    (-distFromFront - 30.0f) / 200.0f);
                surge = 6.0f * (1.0f - surgeDecay) * shoaling * 0.5f;
            }

            return mainWave + drawback + surge;

        } else {
            // Normal Gerstner — match the 3 waves in ocean.vert
            float moonY  = (float)Math.sin(timeOfDay * 2.0f * Math.PI + Math.PI);
            float tidal  = 0.7f + 0.3f * (float)Math.cos(0.0f); // tidalStrength
            float amp    = Math.min((0.8f + 0.6f * Math.max(moonY, 0)) * tidal,
                                    0.9f / frequency);

            float[][] dirs = {{0.0f, 1.0f}, {-0.3f, 1.0f}, {0.25f, 1.0f}};
            float[]   amps = {amp, amp * 0.6f, amp * 0.4f};
            float[]   freqs= {frequency, frequency*1.3f, frequency*1.8f};
            float[]   spds = {speed, speed*0.8f, speed*1.1f};

            float height = 0.0f;
            for (int i = 0; i < 3; i++) {
                float[] d  = dirs[i];
                float len  = (float)Math.sqrt(d[0]*d[0] + d[1]*d[1]);
                float nx   = d[0]/len, nz = d[1]/len;
                float phase= (nx*wx + nz*wz) * freqs[i] - t * spds[i];
                height    += amps[i] * (float)Math.sin(phase);
            }
            return height;
        }
    }

    // Smoothstep helper needed above
    private float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
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