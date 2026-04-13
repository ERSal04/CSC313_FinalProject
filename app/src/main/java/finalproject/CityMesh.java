package finalproject;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class CityMesh {

    // Each building is defined by:
    // x, z position (center), width, depth, height
    private static final float[][] BUILDINGS = {
        // coastline district — low buildings near water
        { 0,   10,  8,  8,  5  },
        { 15,  10,  6,  6,  8  },
        {-15,  10,  7,  7,  6  },
        { 30,  10,  5,  5,  10 },
        {-30,  10,  5,  5,  9  },

        // mid district
        { 0,   25,  10, 10, 20 },
        { 20,  25,  8,  8,  15 },
        {-20,  25,  8,  8,  18 },
        { 40,  25,  6,  6,  12 },
        {-40,  25,  6,  6,  14 },
        { 10,  40,  7,  7,  25 },
        {-10,  40,  7,  7,  22 },

        // downtown — tall buildings inland
        { 0,   60,  12, 12, 50 },
        { 20,  60,  10, 10, 40 },
        {-20,  60,  10, 10, 45 },
        { 35,  60,  8,  8,  35 },
        {-35,  60,  8,  8,  38 },
        { 0,   80,  15, 15, 65 },
        { 25,  80,  10, 10, 30 },
        {-25,  80,  10, 10, 32 },
    };

    private int vaoId;
    private int vboId;
    private int eboId;
    private int indexCount;

    // City sits on a flat ground plane at y = 0
    // positioned so coastline starts at z = 5 (just above water edge)
    private float offsetX = 0;
    private float offsetZ = 5;

    public CityMesh() {
        generate();
    }

    private void generate() {
        // 24 vertices per building (4 per face, 6 faces)
        // 36 indices per building (6 per face, 6 faces)
        int buildingCount = BUILDINGS.length;
        float[] vertices = new float[buildingCount * 24 * 6]; // x,y,z,nx,ny,nz
        int[]   indices  = new int  [buildingCount * 36];

        int vIdx = 0;
        int iIdx = 0;
        int vertexBase = 0;

        for (float[] b : BUILDINGS) {
            float cx = b[0] + offsetX;
            float cz = b[2] + offsetZ;  // NOTE: b[1] is Z position
            float bz = b[1];
            float w  = b[2] / 2f;
            float d  = b[3] / 2f;
            float h  = b[4];

            // Recalculate with actual building data
            cx = b[0] + offsetX;
            bz = b[1] + offsetZ;
            w  = b[2] / 2f;
            d  = b[3] / 2f;
            h  = b[4];

            float x0 = cx - w, x1 = cx + w;
            float y0 = 0,      y1 = h;
            float z0 = bz - d, z1 = bz + d;

            // Front face (z1, normal 0,0,1)
            vIdx = addFace(vertices, vIdx, 
                x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,  0, 0, 1);
            // Back face (z0, normal 0,0,-1)
            vIdx = addFace(vertices, vIdx,
                x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0,  0, 0,-1);
            // Left face (x0, normal -1,0,0)
            vIdx = addFace(vertices, vIdx,
                x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1, 0, 0);
            // Right face (x1, normal 1,0,0)
            vIdx = addFace(vertices, vIdx,
                x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1,  1, 0, 0);
            // Top face (y1, normal 0,1,0)
            vIdx = addFace(vertices, vIdx,
                x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0,  0, 1, 0);
            // Bottom face — skip, never visible

            // 6 faces x 6 indices each = 36 indices per building
            for (int face = 0; face < 5; face++) {
                int b0 = vertexBase + face * 4;
                indices[iIdx++] = b0;
                indices[iIdx++] = b0 + 1;
                indices[iIdx++] = b0 + 2;
                indices[iIdx++] = b0;
                indices[iIdx++] = b0 + 2;
                indices[iIdx++] = b0 + 3;
            }

            vertexBase += 20; // 5 faces * 4 vertices
        }

        indexCount = iIdx;

        // Trim arrays to actual used size
        float[] vTrimmed = new float[vIdx];
        int[]   iTrimmed = new int  [iIdx];
        System.arraycopy(vertices, 0, vTrimmed, 0, vIdx);
        System.arraycopy(indices,  0, iTrimmed, 0, iIdx);

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vTrimmed, GL15.GL_STATIC_DRAW);

        // position attribute — location 0
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * 4, 0);
        GL20.glEnableVertexAttribArray(0);

        // normal attribute — location 1
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * 4, 3 * 4);
        GL20.glEnableVertexAttribArray(1);

        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, iTrimmed, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
    }

    // Writes 4 vertices (x,y,z,nx,ny,nz) for one quad face, returns new index
    private int addFace(float[] v, int i,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz) {
        v[i++]=x0; v[i++]=y0; v[i++]=z0; v[i++]=nx; v[i++]=ny; v[i++]=nz;
        v[i++]=x1; v[i++]=y1; v[i++]=z1; v[i++]=nx; v[i++]=ny; v[i++]=nz;
        v[i++]=x2; v[i++]=y2; v[i++]=z2; v[i++]=nx; v[i++]=ny; v[i++]=nz;
        v[i++]=x3; v[i++]=y3; v[i++]=z3; v[i++]=nx; v[i++]=ny; v[i++]=nz;
        return i;
    }

    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}