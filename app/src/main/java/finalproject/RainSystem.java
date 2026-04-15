package finalproject;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class RainSystem {

    // Each raindrop is a short line from (x,y,z) to (x, y-length, z)
    // We store top vertex only — bottom is computed in update()
    // FORMAT per drop: x, y, z  (top point)

    private static final int   DROP_COUNT = 2500;   // was 4000 — less clutter
    private static final float SPREAD_X   = 200.0f; // was 300 — tighter around city
    private static final float SPREAD_Z   = 200.0f; // was 300
    private static final float HEIGHT_MIN =  50.0f; // was 60
    private static final float HEIGHT_MAX =  75.0f; // was 90
    private static final float FALL_SPEED  = 40.0f;   // units per second
    private static final float DROP_LENGTH =  2.5f;   // visual length of streak

    private float[] dropX   = new float[DROP_COUNT];
    private float[] dropY   = new float[DROP_COUNT];
    private float[] dropZ   = new float[DROP_COUNT];

    // Slight wind angle — rain tilts during tsunami
    private float windX = 0.8f;
    private float windZ = 0.3f;

    private int vaoId;
    private int vboId;

    // Two vertices per drop (top + bottom of streak)
    // 3 floats per vertex = 6 floats per drop
    private FloatBuffer vertexBuffer;

    private ShaderProgram rainShader;

    public RainSystem() {
        // Scatter drops randomly across the rain area
        for (int i = 0; i < DROP_COUNT; i++) {
            resetDrop(i, true);
        }

        vertexBuffer = BufferUtils.createFloatBuffer(DROP_COUNT * 6);

        // Upload initial empty buffer
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
            (long) DROP_COUNT * 6 * Float.BYTES,
            GL15.GL_DYNAMIC_DRAW);  // DYNAMIC — updated every frame

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false,
            3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL30.glBindVertexArray(0);

        // Simple rain shader — just position + solid color
        String vert =
            "#version 330 core\n" +
            "layout(location=0) in vec3 position;\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 view;\n" +
            "void main() {\n" +
            "    gl_Position = projection * view * vec4(position, 1.0);\n" +
            "}\n";

        String frag =
            "#version 330 core\n" +
            "uniform vec4 rainColor;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    fragColor = rainColor;\n" +
            "}\n";

        rainShader = new ShaderProgram(vert, frag);
    }

    // Reset a single drop to a random position at the top
    private void resetDrop(int i, boolean randomY) {
        dropX[i] = (float)(Math.random() * SPREAD_X - SPREAD_X / 2.0);
        dropZ[i] = (float)(Math.random() * SPREAD_Z - SPREAD_Z / 2.0 + 60.0);
        // Always respawn at top — randomY only used for initial scatter
        dropY[i] = randomY
            ? (float)(Math.random() * (HEIGHT_MAX - HEIGHT_MIN) + HEIGHT_MIN)
            : HEIGHT_MAX + (float)(Math.random() * 10.0f);
            // Small random offset so they don't all appear on same frame
    }

    public void update(float deltaTime, float intensity) {
        float speed = FALL_SPEED * intensity;

        for (int i = 0; i < DROP_COUNT; i++) {
            dropY[i] -= speed  * deltaTime;
            dropX[i] += windX  * deltaTime * intensity * 3.0f;
            dropZ[i] += windZ  * deltaTime * intensity * 3.0f;

            // Reset immediately when drop hits ground — no gap frame
            if (dropY[i] < 1.0f) {   // clip at y=1 instead of 0 or -5
                resetDrop(i, false);
            }
        }
    }

    public void render(Camera camera, int width, int height, float intensity) {
        if (intensity < 0.01f) return;

        // Build vertex buffer — two points per drop
        // Must write ALL drops every frame to avoid stale data flickering
        vertexBuffer.clear();
        for (int i = 0; i < DROP_COUNT; i++) {
            // Skip drops that are outside the rain area — write degenerate
            // lines at y=1000 so they're off screen rather than leaving
            // stale data in the buffer
            if (dropY[i] < -5.0f || dropY[i] > HEIGHT_MAX + 5.0f) {
                // Degenerate line far off screen — invisible but fills slot
                vertexBuffer.put(0).put(1000).put(0);
                vertexBuffer.put(0).put(1000).put(0);
                continue;
            }

            // Top of streak
            vertexBuffer.put(dropX[i]);
            vertexBuffer.put(dropY[i]);
            vertexBuffer.put(dropZ[i]);
            // Bottom of streak
            vertexBuffer.put(dropX[i] - windX * DROP_LENGTH * 0.1f);
            vertexBuffer.put(dropY[i] - DROP_LENGTH);
            vertexBuffer.put(dropZ[i] - windZ * DROP_LENGTH * 0.1f);
        }
        vertexBuffer.flip();

        // Upload entire buffer every frame — no partial writes
        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        rainShader.bind();
        rainShader.setUniformMatrix4f("projection",
            camera.getProjectionMatrix(width, height));
        rainShader.setUniformMatrix4f("view", camera.getViewMatrix());

        float alpha = intensity * 0.55f;
        int loc = org.lwjgl.opengl.GL20.glGetUniformLocation(
            rainShader.getProgramId(), "rainColor");
        org.lwjgl.opengl.GL20.glUniform4f(loc,
            0.75f, 0.82f, 0.92f, intensity * 0.35f);  // lower alpha, cooler color

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glDrawArrays(GL11.GL_LINES, 0, DROP_COUNT * 2);
        GL30.glBindVertexArray(0);

        GL11.glDisable(GL11.GL_BLEND);
        rainShader.unbind();
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL30.glDeleteVertexArrays(vaoId);
        rainShader.cleanup();
    }
}