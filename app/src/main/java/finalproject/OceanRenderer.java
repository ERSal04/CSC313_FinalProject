import org.lwjgl.glfw.GLFW;

public class OceanRenderer {

    private long window;
    private int width = 1280;
    private int height = 720;
    private String title = "Ocean Simulator";

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

        // 3. Create the window and store it in 'window'
        window = GLFW.glfwCreateWindow(width, height, "Ocean Simulation", 0, 0);

        // 4. Set up a key callback so ESC closes the window
        

        // 5. Center the window on the monitor (optional but nice)

        // 6. Make the OpenGL context current

        // 7. Call GL.createCapabilities() — don't forget this!

        // 8. Set the clear color
    }

    private void loop() {
        // Keep running until the window should close
        // while ( /* window should not close */ ) {

        //     // 1. Clear the screen

        //     // 2. --- YOUR RENDERING GOES HERE LATER ---

        //     // 3. Swap buffers

        //     // 4. Poll for input events
        // }
    }

    private void cleanup() {
        // 1. Free the window

        // 2. Terminate GLFW
    }

    public static void main(String[] args) {
        new OceanRenderer().run();
    }
}