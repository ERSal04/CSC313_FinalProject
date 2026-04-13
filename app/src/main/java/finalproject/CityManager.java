package finalproject;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class CityManager {

    // Represents one placed instance of a building or prop
    // position is where it sits in world space
    // modelMatrix is computed once at load time
    private static class Instance {
        ObjMesh mesh;
        Matrix4f modelMatrix;

        Instance(ObjMesh mesh, Matrix4f modelMatrix) {
            this.mesh        = mesh;
            this.modelMatrix = modelMatrix;
        }
    }

    private List<Instance> instances = new ArrayList<>();

    // Ground and beach are still simple geometry, not OBJ
    private int groundVaoId;
    private int groundVboId;
    private int groundIndexCount;
    private int beachVaoId;
    private int beachVboId;
    private int beachIndexCount;

    public CityManager() {
        loadBuildings();
        generateGround();
        generateBeach();
    }

    private void loadBuildings() {

        // Load each unique mesh once, reuse for multiple placements
        // If a file doesn't exist yet, pass null for texture and it
        // will fall back to the flat grey in the shader
        ObjMesh small     = loadSafe("models/building_small.obj",
                                     "textures/brick.png");
        ObjMesh medium    = loadSafe("models/building_medium.obj",
                                     "textures/concrete.png");
        ObjMesh tall      = loadSafe("models/building_tall.obj",
                                     "textures/glass_windows.png");
        ObjMesh apartment = loadSafe("models/building_apartment.obj",
                                     "textures/concrete.png");
        ObjMesh seawall   = loadSafe("models/seawall.obj",
                                     "textures/concrete.png");
        ObjMesh dock      = loadSafe("models/dock.obj",
                                     "textures/wood_planks.png");
        ObjMesh boat      = loadSafe("models/boat_small.obj",
                                     null);
        ObjMesh road      = loadSafe("models/road_straight.obj",      "textures/asphalt.png");
        ObjMesh intersect = loadSafe("models/road_intersection.obj",  "textures/asphalt.png");
        ObjMesh sidewalk  = loadSafe("models/sidewalk.obj",           "textures/concrete.png");
        ObjMesh light     = loadSafe("models/streetlight.obj",        null);
        ObjMesh tree      = loadSafe("models/tree.obj",               null);                             
        

        // --- Coastline district (z = 20 to 35) ---
        // Small buildings near the water
        place(small,  translate(  0, 0, 22));
        place(small,  translate( 15, 0, 22));
        place(small,  translate(-15, 0, 22));
        place(small,  translate( 30, 0, 22));
        place(small,  translate(-30, 0, 22));
        place(small,  translate( 45, 0, 28));
        place(small,  translate(-45, 0, 28));

        // Seawall runs along the coast at z = 16
        place(seawall, translate(-60, 0, 16));
        place(seawall, translate(-30, 0, 16));
        place(seawall, translate(  0, 0, 16));
        place(seawall, translate( 30, 0, 16));
        place(seawall, translate( 60, 0, 16));

        // Dock extends into the water on the left side
        place(dock,  translate(-50, 0, 10));
        // Boat sits at the end of the dock
        place(boat,  translate(-50, 0,  5));

        // --- Mid district (z = 35 to 55) ---
        place(medium, translate(  0, 0, 40));
        place(medium, translate( 22, 0, 40));
        place(medium, translate(-22, 0, 40));
        place(medium, translate( 44, 0, 38));
        place(medium, translate(-44, 0, 38));
        place(medium, translate( 12, 0, 52));
        place(medium, translate(-12, 0, 52));

        place(apartment, translate( 34, 0, 48));
        place(apartment, translate(-34, 0, 48));

        // --- Downtown (z = 60 to 100) ---
        place(tall, translate(  0, 0, 65));
        place(tall, translate( 22, 0, 65));
        place(tall, translate(-22, 0, 65));
        place(tall, translate( 38, 0, 70));
        place(tall, translate(-38, 0, 70));
        place(tall, translate(  0, 0, 85));
        place(tall, translate( 28, 0, 82));
        place(tall, translate(-28, 0, 82));

        place(medium, translate( 48, 0, 78));
        place(medium, translate(-48, 0, 78));
        place(medium, translate( 55, 0, 65));
        place(medium, translate(-55, 0, 65));

        place(road, translate( 0, 0.3f, 30));
        place(road, translate( 0, 0.3f, 50));
        place(road, translate( 0, 0.3f, 70));
        place(road, translate( 0, 0.3f, 90));

        // Cross streets
        place(road, translateRotate(-20, 0.3f, 45, 90));
        place(road, translateRotate( 20, 0.3f, 45, 90));
        place(road, translateRotate(-20, 0.3f, 70, 90));
        place(road, translateRotate( 20, 0.3f, 70, 90));

        // Intersections
        place(intersect, translate( 0, 0.3f, 45));
        place(intersect, translate( 0, 0.3f, 70));

        // Sidewalks along main road
        place(sidewalk, translate( 6, 0.3f, 30));
        place(sidewalk, translate(-6, 0.3f, 30));
        place(sidewalk, translate( 6, 0.3f, 50));
        place(sidewalk, translate(-6, 0.3f, 50));
        place(sidewalk, translate( 6, 0.3f, 70));
        place(sidewalk, translate(-6, 0.3f, 70));

        // Streetlights along the main road
        place(light, translate(  8, 0.3f, 25));
        place(light, translate( -8, 0.3f, 25));
        place(light, translate(  8, 0.3f, 40));
        place(light, translate( -8, 0.3f, 40));
        place(light, translate(  8, 0.3f, 55));
        place(light, translate( -8, 0.3f, 55));
        place(light, translate(  8, 0.3f, 70));
        place(light, translate( -8, 0.3f, 70));
        place(light, translate(  8, 0.3f, 85));
        place(light, translate( -8, 0.3f, 85));

        // Trees scattered around the city
        place(tree, translate( 10, 0.3f, 25));
        place(tree, translate(-10, 0.3f, 25));
        place(tree, translate( 10, 0.3f, 35));
        place(tree, translate(-10, 0.3f, 35));
        place(tree, translate( 55, 0.3f, 45));
        place(tree, translate(-55, 0.3f, 45));
        place(tree, translate( 55, 0.3f, 60));
        place(tree, translate(-55, 0.3f, 60));
        place(tree, translate( 10, 0.3f, 75));
        place(tree, translate(-10, 0.3f, 75));
        place(tree, translate( 45, 0.3f, 90));
        place(tree, translate(-45, 0.3f, 90));
    }

    // Safely loads an OBJ — returns a fallback colored box if file not found
    private ObjMesh loadSafe(String objPath, String texturePath) {
        try {
            return new ObjMesh(objPath, texturePath);
        } catch (Exception e) {
            System.err.println("Could not load " + objPath
                + " — using placeholder. Error: " + e.getMessage());
            return null;
        }
    }

    // Places an instance only if the mesh loaded successfully
    private void place(ObjMesh mesh, Matrix4f matrix) {
        if (mesh != null) {
            instances.add(new Instance(mesh, matrix));
        }
    }

    // Shorthand for a translation-only matrix
    private Matrix4f translate(float x, float y, float z) {
        return new Matrix4f().translation(x, y, z);
    }

    // Shorthand for translation + Y rotation (degrees)
    private Matrix4f translateRotate(float x, float y, float z,
                                     float rotY) {
        return new Matrix4f()
            .translation(x, y, z)
            .rotateY((float) Math.toRadians(rotY));
    }

    public void render(ShaderProgram shader) {
        for (Instance inst : instances) {
            shader.setUniformMatrix4f("model", inst.modelMatrix);
            inst.mesh.render(shader);
        }

        // Ground uses identity model matrix and has no texture
        shader.setUniformMatrix4f("model", new Matrix4f());
        shader.setUniformInt("hasTexture", 0);  // ADD THIS LINE
        renderGround();
    }

    private void renderGround() {
        // Ground and beach have no texture — tell shader to use color fallback
        // This requires access to the shader, so renderGround now takes it as param
        // See the render() method below for how this is called

        GL30.glBindVertexArray(groundVaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, groundIndexCount,
            GL11.GL_UNSIGNED_INT, 0);

        GL30.glBindVertexArray(beachVaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, beachIndexCount,
            GL11.GL_UNSIGNED_INT, 0);

        GL30.glBindVertexArray(0);
    }

    private void generateGround() {
        float x0 = -200f, x1 = 200f;
        float z0 =   15f, z1 = 300f;
        float y  =  0.3f;

        // FORMAT: x, y, z, nx, ny, nz, u, v  (8 floats per vertex)
        float[] ground = {
            x0, y, z0,  0,1,0,  0,0,
            x1, y, z0,  0,1,0,  1,0,
            x1, y, z1,  0,1,0,  1,1,
            x0, y, z1,  0,1,0,  0,1,
        };
        int[] indices = { 0,1,2, 0,2,3 };
        groundIndexCount = indices.length;

        groundVaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(groundVaoId);

        groundVboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, groundVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, ground, GL15.GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, 6 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        int ebo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
    }

    private void generateBeach() {
        float x0 = -200f, x1 = 200f;

        // FORMAT: x, y, z, nx, ny, nz, u, v
        float[] beach = {
            x0, -0.5f,  0f,  0f, 0.98f, -0.2f,  0, 0,
            x1, -0.5f,  0f,  0f, 0.98f, -0.2f,  1, 0,
            x1,  0.3f, 15f,  0f, 0.98f, -0.2f,  1, 1,
            x0,  0.3f, 15f,  0f, 0.98f, -0.2f,  0, 1,
        };
        int[] indices = { 0,1,2, 0,2,3 };
        beachIndexCount = indices.length;

        beachVaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(beachVaoId);

        beachVboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, beachVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, beach, GL15.GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, 6 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        int ebo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);
    }

    // In CityManager, add this scale helper for quick adjustments
    private Matrix4f translateScale(float x, float y, float z, float s) {
        return new Matrix4f().translation(x, y, z).scale(s);
    }

    public void cleanup() {
        // Deduplicate — same ObjMesh may be placed multiple times
        // so track which ones we've already cleaned up
        List<ObjMesh> cleaned = new ArrayList<>();
        for (Instance inst : instances) {
            if (!cleaned.contains(inst.mesh)) {
                inst.mesh.cleanup();
                cleaned.add(inst.mesh);
            }
        }
        GL15.glDeleteBuffers(groundVboId);
        GL30.glDeleteVertexArrays(groundVaoId);
        GL15.glDeleteBuffers(beachVboId);
        GL30.glDeleteVertexArrays(beachVaoId);
    }
}