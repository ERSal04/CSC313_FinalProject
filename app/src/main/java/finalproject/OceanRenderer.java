import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import finalproject.Camera;
import finalproject.CityManager;
import finalproject.OceanMesh;
import finalproject.RainSystem;
import finalproject.ShaderProgram;
import finalproject.SoundManager;

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
    private float tsunamiTime     = 0.0f;
    private float tsunamiOriginX  =    0.0f;
    private float tsunamiOriginZ  = -250.0f;
    private float frequency = 0.15f;

    private float boatX      =  -70.0f;
    private float boatZ      =  -15.0f;
    private float boatYaw    =   90.0f;
    private float boatSweepZ =    0.0f;

    private boolean wireframe = false;
    private boolean underwater = false;

    private CityManager city;
    private ShaderProgram cityShader;
    private float waterLevel    = 0.0f;
    private float speed         = 0.8f;
    private float arrivalFactor = 0.0f;

    private RainSystem rain;
    private float rainIntensity = 0.0f;

    private SoundManager sound;

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

            if (key == GLFW.GLFW_KEY_R && action == GLFW.GLFW_PRESS) {
                tsunamiActive   = false;
                tsunamiStrength = 0.0f;
                tsunamiTime     = 0.0f;
                waterLevel      = 0.0f;
                cinematicMode   = false;
                cinematicTime   = 0.0f;
                boatSweepZ      = 0.0f;
                boatYaw         = 0.0f;
                System.out.println("Simulation reset");
            }

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

        GL11.glEnable(GL11.GL_DEPTH_TEST);

        ocean  = new OceanMesh(300, 2.0f);
        camera = new Camera(new Vector3f(0, 25, -80), 90.0f);

        String vertSrc = ShaderProgram.loadFile("shaders/ocean.vert");
        String fragSrc = ShaderProgram.loadFile("shaders/ocean.frag");
        shaderProgram  = new ShaderProgram(vertSrc, fragSrc);

        city = new CityManager();
        String cityVert = ShaderProgram.loadFile("shaders/city.vert");
        String cityFrag = ShaderProgram.loadFile("shaders/city.frag");
        cityShader = new ShaderProgram(cityVert, cityFrag);

        rain  = new RainSystem();
        sound = new SoundManager();

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

            // --- Time of day ---
            timeOfDay += 0.0001f;
            if (timeOfDay > 1.0f) timeOfDay = 0.0f;

            float[] midnight = {0.03f, 0.03f, 0.12f};
            float[] dawn     = {0.8f,  0.4f,  0.2f };
            float[] noon     = {0.3f,  0.6f,  1.0f };
            float[] dusk     = {0.55f, 0.28f, 0.20f};

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

            // --- Tsunami ---
            if (tsunamiActive) {
                tsunamiStrength = Math.min(tsunamiStrength + 0.02f, 1.0f);
                float timeScale = cinematicMode ? 0.3f : 1.0f;
                tsunamiTime += 0.016f * timeScale;
            } else {
                if (tsunamiTime > 0) {
                    tsunamiTime     = Math.max(0.0f, tsunamiTime     - 0.005f);
                    tsunamiStrength = Math.max(0.0f, tsunamiStrength - 0.005f);
                    waterLevel      = Math.max(0.0f, waterLevel      - 0.05f);
                }
            }

            float sunAngle = timeOfDay * 2.0f * (float)Math.PI;
            float moonY    = (float)Math.sin(sunAngle + Math.PI);
            float sunX     = (float)Math.cos(sunAngle);
            float sunY     = (float)Math.sin(sunAngle);
            float moonX    = (float)Math.cos(sunAngle + Math.PI);
            float moonY2   = (float)Math.sin(sunAngle + Math.PI);

            float stormDark = rainIntensity * 0.35f;
            float fogStart  = 100.0f - rainIntensity * 50.0f;
            float fogEnd    = 400.0f - rainIntensity * 180.0f;

            float stormSkyR = lerp(skyR, 0.15f, rainIntensity * 0.6f) * (1.0f - stormDark * 0.3f);
            float stormSkyG = lerp(skyG, 0.18f, rainIntensity * 0.6f) * (1.0f - stormDark * 0.3f);
            float stormSkyB = lerp(skyB, 0.25f, rainIntensity * 0.6f) * (1.0f - stormDark * 0.1f);

            float sunMoonAlignment = (float)Math.cos(sunAngle - (sunAngle + Math.PI));
            float tidalStrength    = 0.7f + 0.3f * sunMoonAlignment;
            float tidalAmplitude   = (0.8f + 0.6f * Math.max(moonY, 0.0f)) * tidalStrength;
            float tidalWaterLevel  = 0.5f * Math.max(moonY, 0.0f) * tidalStrength;
            float finalAmplitude   = tsunamiActive ? tidalAmplitude * 0.3f : tidalAmplitude;

            // --- Underwater detection ---
            float surfaceAtCamera = sampleWaveHeight(
                camera.getPosition().x,
                camera.getPosition().z,
                time) + tidalWaterLevel;
            underwater = camera.getPosition().y < surfaceAtCamera;

            // Sky and fog change when underwater
            float skyRFinal, skyGFinal, skyBFinal;
            float fogStartFinal, fogEndFinal;

            if (underwater) {
                float depth       = Math.max(0, surfaceAtCamera - camera.getPosition().y);
                float depthFactor = clamp(depth / 20.0f, 0.0f, 1.0f);

                skyRFinal = lerp(0.02f, 0.0f,  depthFactor);
                skyGFinal = lerp(0.25f, 0.08f, depthFactor);
                skyBFinal = lerp(0.35f, 0.18f, depthFactor);

                fogStartFinal = 0.0f;
                fogEndFinal   = lerp(25.0f, 8.0f, depthFactor);
            } else {
                skyRFinal     = stormSkyR;
                skyGFinal     = stormSkyG;
                skyBFinal     = stormSkyB;
                fogStartFinal = fogStart;
                fogEndFinal   = fogEnd;
            }

            GL11.glClearColor(skyRFinal, skyGFinal, skyBFinal, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // --- Ocean shader ---
            shaderProgram.bind();
            shaderProgram.setUniformMatrix4f("projection", camera.getProjectionMatrix(width, height));
            shaderProgram.setUniformMatrix4f("view",       camera.getViewMatrix());
            shaderProgram.setUniformMatrix4f("model",      new org.joml.Matrix4f());
            shaderProgram.setUniformVec3("cameraPos",      camera.getPosition());

            time += 0.016f;
            shaderProgram.setUniformFloat("time",        time);
            float safeAmplitude = Math.min(finalAmplitude, 0.9f / frequency);
            shaderProgram.setUniformFloat("amplitude",   safeAmplitude);
            shaderProgram.setUniformFloat("frequency",   0.15f);
            shaderProgram.setUniformFloat("speed",       0.8f);
            shaderProgram.setUniformVec2("direction",    1.0f, 0.0f);
            shaderProgram.setUniformFloat("tidalOffset", tidalWaterLevel);
            shaderProgram.setUniformVec3("skyColor",     skyRFinal, skyGFinal, skyBFinal);
            shaderProgram.setUniformFloat("fogStart",    fogStartFinal);
            shaderProgram.setUniformFloat("fogEnd",      fogEndFinal);
            shaderProgram.setUniformVec3("sunDirection", sunX, sunY, 0.3f);
            shaderProgram.setUniformFloat("timeOfDay",   timeOfDay);
            shaderProgram.setUniformVec3("moonDirection", moonX, moonY2, 0.3f);
            shaderProgram.setUniformInt  ("tsunamiActive", tsunamiActive ? 1 : 0);
            shaderProgram.setUniformVec2 ("tsunamiOrigin", tsunamiOriginX, tsunamiOriginZ);
            shaderProgram.setUniformFloat("tsunamiTime",   tsunamiTime);

            if (wireframe)
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            else
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            ocean.render();
            shaderProgram.unbind();
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

            GL11.glDisable(GL11.GL_BLEND);

            // --- Flood calculation ---
            float cityDist = (float)Math.sqrt(
                Math.pow(0  - tsunamiOriginX, 2) +
                Math.pow(60 - tsunamiOriginZ, 2));

            float waveFrontAtCity = tsunamiTime * 40.0f;
            arrivalFactor = clamp((waveFrontAtCity - cityDist) / 30.0f, 0.0f, 1.0f);

            float peakFloodHeight = 18.0f;
            float floodDecay      = Math.max(0.0f, tsunamiTime - 15.0f) * 0.3f;
            waterLevel = tsunamiActive
                ? peakFloodHeight * arrivalFactor * Math.max(0.3f, 1.0f - floodDecay * 0.05f)
                : tidalAmplitude * 0.1f * (float)Math.sin(time * 0.5f);

            // --- Boat ---
            float boatWaveY = sampleWaveHeight(boatX, boatZ + boatSweepZ, time) + tidalWaterLevel;

            if (tsunamiActive) {
                float bdx            = boatX - tsunamiOriginX;
                float bdz            = (boatZ + boatSweepZ) - tsunamiOriginZ;
                float boatDist       = (float)Math.sqrt(bdx*bdx + bdz*bdz);
                float waveFrontB     = tsunamiTime * 40.0f;
                float distFromFrontB = boatDist - waveFrontB;

                if (distFromFrontB < 20.0f && boatWaveY > 2.0f) {
                    boatSweepZ  = Math.min(boatSweepZ + 0.6f, 75.0f);
                    boatYaw    += 2.0f;
                }
            }

            // --- Rain ---
            float targetRain = tsunamiActive ? 1.0f : 0.0f;
            rainIntensity = lerp(rainIntensity, targetRain, 0.008f);

            if (!underwater) {
                rain.update(0.016f, rainIntensity);
                rain.render(camera, width, height, rainIntensity);
            }

            // --- City shader ---
            city.setBoatTransform(boatX, boatWaveY, boatZ + boatSweepZ, boatYaw);

            cityShader.bind();
            cityShader.setUniformMatrix4f("projection",    camera.getProjectionMatrix(width, height));
            cityShader.setUniformMatrix4f("view",          camera.getViewMatrix());
            cityShader.setUniformMatrix4f("model",         new org.joml.Matrix4f());
            cityShader.setUniformVec3("sunDirection",      sunX, sunY, 0.3f);
            cityShader.setUniformVec3("cameraPos",         camera.getPosition());
            cityShader.setUniformFloat("waterLevel",       waterLevel);
            cityShader.setUniformVec3("skyColor",          skyRFinal, skyGFinal, skyBFinal);
            cityShader.setUniformFloat("fogStart",         fogStartFinal);
            cityShader.setUniformFloat("fogEnd",           fogEndFinal);
            cityShader.setUniformFloat("tsunamiTime",      tsunamiActive ? tsunamiTime : 0.0f);
            cityShader.setUniformInt(  "tsunamiActive",    tsunamiActive ? 1 : 0);
            cityShader.setUniformFloat("tsunamiOriginX",   tsunamiOriginX);
            cityShader.setUniformFloat("tsunamiOriginZ",   tsunamiOriginZ);
            cityShader.setUniformVec3("moonDirection",     moonX, moonY2, 0.3f);

            city.render(cityShader);
            cityShader.unbind();

            sound.update(tsunamiActive, rainIntensity, arrivalFactor, tsunamiTime);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void updateCinematicCamera() {
        cinematicTime += 0.016f;
        float t = cinematicTime % 71.0f;

        Vector3f targetPos;
        float    targetYaw;
        float    targetPitch;

        if (t < 10.0f) {
            // Street level in the city looking toward the ocean
            float drift = t / 10.0f;
            targetPos   = new Vector3f(
                lerp(5, -5, drift),
                lerp(3, 3, drift),
                lerp(40, 35, drift)
            );
            targetYaw   = 270.0f;
            targetPitch = -2.0f;

        } else if (t < 20.0f) {
            // Down on the beach watching the horizon
            float drift = (t - 10.0f) / 10.0f;
            targetPos   = new Vector3f(
                lerp(-30, -20, drift),
                lerp(1.5f, 2.0f, drift),
                lerp(10, 5, drift)
            );
            targetYaw   = 260.0f;
            targetPitch = -1.0f;

        } else if (t < 27.0f) {
            // Close on the boat as the wave arrives
            float drift = (t - 20.0f) / 7.0f;
            targetPos   = new Vector3f(
                lerp(-85, -75, drift),
                lerp(  5,   8, drift),
                lerp(-25, -15, drift)
            );
            targetYaw   = 30.0f;
            targetPitch = lerp(-5.0f, -10.0f, drift);

        }  else if (t < 39.0f) {
            // Start high above the boat then slowly descend as it settles on land
            float drift     = (t - 27.0f) / 12.0f;
            float liveBoatZ = boatZ + boatSweepZ;
            targetPos = new Vector3f(
                -70,
                lerp(90, 45, drift),        // starts very high, descends as boat slows
                lerp(liveBoatZ - 10, liveBoatZ + 5, drift)
            );
            targetYaw   = 90.0f;
            targetPitch = lerp(-89.0f, -70.0f, drift);  // nearly straight down at first, tilts to angle as we descend

        } else if (t < 51.0f) {
            // Rising overhead as the wave pushes into the city
            float drift = (t - 39.0f) / 12.0f;
            targetPos   = new Vector3f(
                lerp(  0,  10, drift),
                lerp( 40, 120, drift),
                lerp( 20,  60, drift)
            );
            targetYaw   = 90.0f;
            targetPitch = lerp(-25.0f, -80.0f, drift);

        } else if (t < 61.0f) {
            // Straight down over the flooded city
            float drift = (t - 51.0f) / 10.0f;
            targetPos   = new Vector3f(
                lerp(10,   0, drift),
                lerp(120, 140, drift),
                lerp( 60,  60, drift)
            );
            targetYaw   = 90.0f;
            targetPitch = -89.0f;

        } else {
            // Pulling back out to sea looking at the submerged skyline
            float drift = (t - 61.0f) / 10.0f;
            targetPos   = new Vector3f(
                lerp(  0,  20, drift),
                lerp( 12,  18, drift),
                lerp(-60, -80, drift)
            );
            targetYaw   = 90.0f;
            targetPitch = lerp(-5.0f, -12.0f, drift);
        }

        float smoothSpeed   = 0.035f;
        Vector3f currentPos = camera.getPosition();
        currentPos.x = lerp(currentPos.x, targetPos.x, smoothSpeed);
        currentPos.y = lerp(currentPos.y, targetPos.y, smoothSpeed);
        currentPos.z = lerp(currentPos.z, targetPos.z, smoothSpeed);
        camera.setCinematicAngles(targetYaw, targetPitch, smoothSpeed);
    }

    private float sampleWaveHeight(float wx, float wz, float t) {
        if (tsunamiActive) {
            float dx            = wx - tsunamiOriginX;
            float dz            = wz - tsunamiOriginZ;
            float dist          = (float)Math.sqrt(dx*dx + dz*dz);
            float waveFront     = tsunamiTime * 40.0f;
            float distFromFront = dist - waveFront;

            float depth    = Math.max(0, Math.min(1, (-wz + 15.0f) / 315.0f));
            float shoaling = 1.0f + (1.0f - depth) * 3.0f;
            shoaling      *= smoothstep(300.0f, 0.0f, wz);

            float frontWidth = 25.0f, backWidth = 80.0f;
            float envelope;
            if (distFromFront <= 0.0f)
                envelope = (float)Math.exp(-(distFromFront*distFromFront) / (backWidth*backWidth));
            else
                envelope = (float)Math.exp(-(distFromFront*distFromFront) / (frontWidth*frontWidth));

            float mainWave = 15.0f * envelope * shoaling;

            float drawback = 0.0f;
            if (distFromFront > 0.0f && distFromFront < 120.0f) {
                float drawT = distFromFront / 120.0f;
                drawback = -3.5f * (float)Math.sin(drawT * Math.PI) * shoaling * 0.4f;
            }

            float surge = 0.0f;
            if (distFromFront < -30.0f) {
                float surgeDecay = Math.min(1.0f, (-distFromFront - 30.0f) / 200.0f);
                surge = 6.0f * (1.0f - surgeDecay) * shoaling * 0.5f;
            }

            return mainWave + drawback + surge;

        } else {
            float moonYLocal = (float)Math.sin(timeOfDay * 2.0f * Math.PI + Math.PI);
            float tidal      = 0.7f + 0.3f * (float)Math.cos(0.0f);
            float amp        = Math.min((0.8f + 0.6f * Math.max(moonYLocal, 0)) * tidal,
                                        0.9f / frequency);

            float[][] dirs = {{0.0f, 1.0f}, {-0.3f, 1.0f}, {0.25f, 1.0f}};
            float[]   amps = {amp, amp * 0.6f, amp * 0.4f};
            float[]   freqs= {frequency, frequency * 1.3f, frequency * 1.8f};
            float[]   spds = {speed, speed * 0.8f, speed * 1.1f};

            float height = 0.0f;
            for (int i = 0; i < 3; i++) {
                float[] d   = dirs[i];
                float len   = (float)Math.sqrt(d[0]*d[0] + d[1]*d[1]);
                float nx    = d[0]/len, nz = d[1]/len;
                float phase = (nx*wx + nz*wz) * freqs[i] - t * spds[i];
                height     += amps[i] * (float)Math.sin(phase);
            }
            return height;
        }
    }

    private float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    private void cleanup() {
        shaderProgram.cleanup();
        ocean.cleanup();
        cityShader.cleanup();
        city.cleanup();
        rain.cleanup();
        sound.cleanup();
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