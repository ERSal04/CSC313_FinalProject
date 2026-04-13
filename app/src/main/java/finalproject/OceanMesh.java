package finalproject;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class OceanMesh {
    private int vaoId;
    private int vboId;
    private int eboId;
    private int vertexCount;
    private int gridSize;
    private float tileSize;

    // Ocean only renders up to this Z — matches where ground plane starts
    private static final float SHORE_Z = 15.0f;

    public OceanMesh(int gridSize, float tileSize) {
        this.gridSize = gridSize;
        this.tileSize = tileSize;
        generate();
    }

    public void generate() {
        // Build full grid of vertex positions
        float[] allVertices = new float[(gridSize + 1) * (gridSize + 1) * 3];
        int index = 0;
        for (int z = 0; z <= gridSize; z++) {
            for (int x = 0; x <= gridSize; x++) {
                allVertices[index++] = (x - gridSize / 2) * tileSize;
                allVertices[index++] = 0.0f;
                allVertices[index++] = (z - gridSize / 2) * tileSize;
            }
        }

        // Only add indices for quads where the quad's Z is below SHORE_Z
        // This stops ocean triangles from appearing under the city
        List<Integer> indexList = new ArrayList<>();
        for (int z = 0; z < gridSize; z++) {
            for (int x = 0; x < gridSize; x++) {
                int topLeft     = z       * (gridSize + 1) + x;
                int topRight    = topLeft + 1;
                int bottomLeft  = (z + 1) * (gridSize + 1) + x;
                int bottomRight = bottomLeft + 1;

                // World Z of the top-left corner of this quad
                float worldZ = (z - gridSize / 2) * tileSize;

                // Skip quads that are entirely on land side
                if (worldZ >= SHORE_Z) continue;

                indexList.add(topLeft);
                indexList.add(bottomLeft);
                indexList.add(topRight);

                indexList.add(topRight);
                indexList.add(bottomLeft);
                indexList.add(bottomRight);
            }
        }

        int[] indices = indexList.stream().mapToInt(i -> i).toArray();
        vertexCount = indices.length;

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, allVertices, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
    }

    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount,
            GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}