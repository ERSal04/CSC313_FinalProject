package finalproject;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class ObjMesh {

    private int vaoId;
    private int vboId;
    private int eboId;
    private int indexCount;
    private int textureId;

    // Load from OBJ file path and a texture path
    public ObjMesh(String objPath, String texturePath) {
        ObjLoader.MeshData data = ObjLoader.load(objPath);
        indexCount = data.indices.length;

        // Load texture if provided
        if (texturePath != null) {
            textureId = TextureLoader.load(texturePath);
        }

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
            data.vertices, GL15.GL_STATIC_DRAW);

        // Stride = 8 floats * 4 bytes = 32 bytes
        int stride = 8 * Float.BYTES;

        // location 0 — position (x, y, z)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(0);

        // location 1 — normal (nx, ny, nz)
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false,
            stride, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        // location 2 — texture coordinates (u, v)
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false,
            stride, 6 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER,
            data.indices, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
    }

    public void render() {
        // Tell shader whether this mesh has a texture
        // The shader uses this to switch between textured and flat rendering
        // We need a way to pass hasTexture — add a render overload that takes the shader
        if (textureId != 0) {
            TextureLoader.bind(textureId, 0);
        }

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES,
            indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);

        if (textureId != 0) {
            TextureLoader.unbind();
        }
    }

    // ADD this overload — used by CityManager so the shader knows
    // whether to sample the texture or use fallback color
    public void render(ShaderProgram shader) {
        shader.setUniformInt("hasTexture", textureId != 0 ? 1 : 0);
        if (textureId != 0) {
            shader.setUniformInt("diffuseTexture", 0); // texture unit 0
            TextureLoader.bind(textureId, 0);
        }
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES,
            indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
        if (textureId != 0) {
            TextureLoader.unbind();
        }
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
        GL30.glDeleteVertexArrays(vaoId);
        if (textureId != 0) {
            TextureLoader.delete(textureId);
        }
    }
}