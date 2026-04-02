package finalproject;

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

    public OceanMesh(int gridSize, float tileSize) {
        this.gridSize = gridSize;
        this.tileSize = tileSize;
        generate();
    }

    public void generate() {
        float[] vertices = new float[(gridSize + 1) * (gridSize + 1) * 3];

        int index = 0;
        for (int z = 0; z <= gridSize; z++) {
            for(int x = 0; x <= gridSize; x++) {
                vertices[index++] = (x - gridSize/2) * tileSize;
                vertices[index++] = 0.0f;
                vertices[index++] = (z - gridSize/2) * tileSize;
            }
        }

        int[] indices = new int[gridSize * gridSize * 6];

        int idx = 0;
        for(int z = 0; z < gridSize; z++) {
            for(int x = 0; x < gridSize; x++) {
                int topLeft = z * (gridSize+1) + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z+1) * (gridSize+1) + x;
                int bottomRight = bottomLeft + 1;

                // Triangle 1
                indices[idx++] = topLeft;
                indices[idx++] = bottomLeft;
                indices[idx++] = topRight;

                // Triangle 2
                indices[idx++] = topRight;
                indices[idx++] = bottomLeft;
                indices[idx++] = bottomRight;
            }
        }

        vertexCount = indices.length;

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);

    }

    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
