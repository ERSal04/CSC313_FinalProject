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

    private float boatRenderX   = -70.0f;
    private float boatRenderY   =   0.0f;
    private float boatRenderZ   =  -8.0f;
    private float boatRenderYaw =   0.0f;

    private ObjMesh boatMesh = null;  

    public CityManager() {
        loadBuildings();
        generateGround();
        generateBeach();
    }

    private void loadBuildings() {
        ObjMesh small     = loadSafe("models/building_small.obj",     "textures/brick.png");
        ObjMesh medium    = loadSafe("models/building_medium.obj",    "textures/concrete.png");
        ObjMesh tall      = loadSafe("models/building_tall.obj",      "textures/glass_windows.png");
        ObjMesh apartment = loadSafe("models/building_apartment.obj", "textures/concrete.png");
        ObjMesh seawall   = loadSafe("models/seawall.obj",            "textures/concrete.png");
        ObjMesh dock      = loadSafe("models/dock.obj",               "textures/wood_planks.png");
        ObjMesh boat      = loadSafe("models/boat_small.obj",         null);
        ObjMesh road      = loadSafe("models/road_straight.obj",      "textures/asphalt.png");
        ObjMesh intersect = loadSafe("models/road_intersection.obj",  "textures/asphalt.png");
        ObjMesh sidewalk  = loadSafe("models/sidewalk.obj",           "textures/concrete.png");
        ObjMesh light     = loadSafe("models/streetlight.obj",        null);
        ObjMesh tree      = loadSafe("models/tree.obj",               null);

        // -------------------------------------------------------
        // SEAWALL — seawall is 20 wide, place every 20 units
        // centered at z=16, 1 unit deep so front face at z=15
        // -------------------------------------------------------
        place(seawall, translate(-40, 0.3f, 16));
        place(seawall, translate(-20, 0.3f, 16));
        place(seawall, translate(  0, 0.3f, 16));
        place(seawall, translate( 20, 0.3f, 16));
        place(seawall, translate( 40, 0.3f, 16));

        // -------------------------------------------------------
        // DOCK + BOAT — dock is 15 wide x 20 deep
        // place it extending into the water from the beach
        // at x=-70 it is well clear of the seawall which ends at x=-50
        // -------------------------------------------------------
        place(dock, translate(-70, 0.0f, -8));
        // place(boat, translate(-70, 0.1f, -14));
        boatMesh = boat;

        // -------------------------------------------------------
        // ROAD LAYOUT
        // road_straight is 10 wide x 20 deep
        // Main road runs north-south along x=0
        // Road occupies x=-5 to x=+5
        //
        // Cross roads run east-west at z=48 and z=70
        // When rotated 90deg, road becomes 20 wide x 10 deep
        // so cross road at z=48 occupies z=43 to z=53
        // and cross road at z=70 occupies z=65 to z=75
        //
        // road_intersection is 20x20, placed at each crossing
        // -------------------------------------------------------

        // Main road north-south — covers z=18 to z=98 with segments
        place(road, translate(0, 0.31f, 27)); // z=17 to z=37
        place(road, translate(0, 0.31f, 47)); // z=37 to z=57 — but intersection at z=48 covers this
        place(road, translate(0, 0.31f, 67)); // z=57 to z=77 — intersection at z=70 covers this
        place(road, translate(0, 0.31f, 87)); // z=77 to z=97

        // Intersections — 20x20, placed at z=48 and z=70
        place(intersect, translate(0, 0.31f, 48));
        place(intersect, translate(0, 0.31f, 70));

        // Cross roads east-west at z=48
        // rotated 90deg: now 20 wide (Z) x 10 deep (X)
        // need to fill from x=-10 to x=-80 and x=+10 to x=+80
        place(road, translateRotate(-20, 0.31f, 48, 90)); // x=-30 to x=-10
        place(road, translateRotate(-40, 0.31f, 48, 90)); // x=-50 to x=-30
        place(road, translateRotate(-60, 0.31f, 48, 90)); // x=-70 to x=-50
        place(road, translateRotate( 20, 0.31f, 48, 90)); // x=+10 to x=+30
        place(road, translateRotate( 40, 0.31f, 48, 90)); // x=+30 to x=+50
        place(road, translateRotate( 60, 0.31f, 48, 90)); // x=+50 to x=+70

        // Cross roads at z=70
        place(road, translateRotate(-20, 0.31f, 70, 90));
        place(road, translateRotate(-40, 0.31f, 70, 90));
        place(road, translateRotate(-60, 0.31f, 70, 90));
        place(road, translateRotate( 20, 0.31f, 70, 90));
        place(road, translateRotate( 40, 0.31f, 70, 90));
        place(road, translateRotate( 60, 0.31f, 70, 90));

        // -------------------------------------------------------
        // SIDEWALKS — sidewalk tile is 5x5
        // Place continuously along both sides of main road
        // Road edge is at x=±5, sidewalk starts at x=±5
        // So right sidewalk centers at x=7.5, left at x=-7.5
        // Tiles placed every 5 units in Z to form continuous path
        // -------------------------------------------------------
        float[] swZ = {20,25,30,35,40,45,53,58,63,75,80,85,90,95};
        for (float z : swZ) {
            place(sidewalk, translate( 7.5f, 0.32f, z));
            place(sidewalk, translate(-7.5f, 0.32f, z));
        }

        // -------------------------------------------------------
        // STREETLIGHTS — streetlight base is 0.6x0.6
        // Place at x=±13 — outside sidewalk edge at x=±10,
        // inside building_small edge at x=±14 (half of 8 = 4,
        // building center at x=18 so inner edge at x=14)
        // Place every 20 units so they don't crowd
        // -------------------------------------------------------
        float[] lightZ = {22, 42, 62, 82};
        for (float z : lightZ) {
            place(light, translate( 13, 0.32f, z));
            place(light, translate(-13, 0.32f, z));
        }

        // -------------------------------------------------------
        // COASTLINE DISTRICT — z=19 to z=43
        // building_small footprint: 8 wide x 8 deep
        // Half = 4 units each side
        // Road occupies x=-5 to +5
        // Sidewalk at x=5 to 10 and x=-5 to -10
        // First safe building center: x=±14 (edge at x=10, +4 half = 14)
        // -------------------------------------------------------
        place(small, translate( 14, 0.3f, 22)); // right edge x=18, clear of sidewalk at x=10
        place(small, translate(-14, 0.3f, 22));
        place(small, translate( 24, 0.3f, 22)); // next column
        place(small, translate(-24, 0.3f, 22));
        place(small, translate( 36, 0.3f, 22));
        place(small, translate(-36, 0.3f, 22));
        place(small, translate( 14, 0.3f, 32));
        place(small, translate(-14, 0.3f, 32));
        place(small, translate( 24, 0.3f, 32));
        place(small, translate(-24, 0.3f, 32));
        place(small, translate( 36, 0.3f, 32));
        place(small, translate(-36, 0.3f, 32));

        // -------------------------------------------------------
        // MID DISTRICT — z=53 to z=65 (between cross roads)
        // building_medium footprint: 10 wide x 10 deep, half=5
        // First safe center: x=±15 (sidewalk ends at 10, +5 half = 15)
        // -------------------------------------------------------
        place(medium, translate( 15, 0.3f, 40));
        place(medium, translate(-15, 0.3f, 40));
        place(medium, translate( 27, 0.3f, 40));
        place(medium, translate(-27, 0.3f, 40));
        place(medium, translate( 41, 0.3f, 40));
        place(medium, translate(-41, 0.3f, 40));

        // Between cross roads z=53 to z=65
        place(apartment, translate( 15, 0.3f, 59));
        place(apartment, translate(-15, 0.3f, 59));
        place(medium,    translate( 29, 0.3f, 59));
        place(medium,    translate(-29, 0.3f, 59));
        place(medium,    translate( 43, 0.3f, 59));
        place(medium,    translate(-43, 0.3f, 59));

        // -------------------------------------------------------
        // DOWNTOWN — z=75 onwards
        // building_tall footprint: 13 wide x 13 deep, half=6.5
        // First safe center: x=±16.5 — round to x=±17
        // -------------------------------------------------------
        place(tall, translate( 17, 0.3f, 80));
        place(tall, translate(-17, 0.3f, 80));
        place(tall, translate( 32, 0.3f, 80));
        place(tall, translate(-32, 0.3f, 80));
        place(tall, translate( 48, 0.3f, 80));
        place(tall, translate(-48, 0.3f, 80));

        place(tall, translate( 17, 0.3f, 95));
        place(tall, translate(-17, 0.3f, 95));
        place(tall, translate( 32, 0.3f, 95));
        place(tall, translate(-32, 0.3f, 95));

        place(medium, translate( 50, 0.3f, 95));
        place(medium, translate(-50, 0.3f, 95));

        // -------------------------------------------------------
        // TREES — in gaps between buildings, clear of roads
        // tree footprint: 5x5 so half=2.5
        // Place in the open areas that have no buildings
        // -------------------------------------------------------

        // Along coastline between building columns
        place(tree, translate( 48, 0.3f, 27));
        place(tree, translate(-48, 0.3f, 27));
        place(tree, translate( 48, 0.3f, 37));
        place(tree, translate(-48, 0.3f, 37));

        // Mid district open corners near cross roads
        place(tree, translate( 55, 0.3f, 43));
        place(tree, translate(-55, 0.3f, 43));
        place(tree, translate( 55, 0.3f, 59));
        place(tree, translate(-55, 0.3f, 59));

        // Downtown side areas
        place(tree, translate( 55, 0.3f, 80));
        place(tree, translate(-55, 0.3f, 80));
        place(tree, translate( 55, 0.3f, 95));
        place(tree, translate(-55, 0.3f, 95));
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
        shader.setUniformInt("isGround", 0);

        for (Instance inst : instances) {
            shader.setUniformMatrix4f("model", inst.modelMatrix);
            inst.mesh.render(shader);
        }

        // Boat rendered separately with live transform
        if (boatMesh != null) {
            Matrix4f boatModel = new Matrix4f()
                .translate(boatRenderX, boatRenderY, boatRenderZ)
                .rotateY((float)Math.toRadians(boatRenderYaw));
            shader.setUniformMatrix4f("model", boatModel);
            shader.setUniformInt("isGround", 0);
            boatMesh.render(shader);
        }

        // Ground and beach
        shader.setUniformMatrix4f("model", new Matrix4f());
        shader.setUniformInt("hasTexture", 0);
        shader.setUniformInt("isGround", 1);
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

    private Matrix4f translateScale(float x, float y, float z, float s) {
        return new Matrix4f().translation(x, y, z).scale(s);
    }

    public void setBoatTransform(float x, float y, float z, float yaw) {
        boatRenderX   = x;
        boatRenderY   = y;
        boatRenderZ   = z;
        boatRenderYaw = yaw;
    }

    public void cleanup() {
        List<ObjMesh> cleaned = new ArrayList<>();
        for (Instance inst : instances) {
            if (!cleaned.contains(inst.mesh)) {
                inst.mesh.cleanup();
                cleaned.add(inst.mesh);
            }
        }
        // Clean up boat separately since it's not in instances
        if (boatMesh != null && !cleaned.contains(boatMesh)) {
            boatMesh.cleanup();
        }
        GL15.glDeleteBuffers(groundVboId);
        GL30.glDeleteVertexArrays(groundVaoId);
        GL15.glDeleteBuffers(beachVboId);
        GL30.glDeleteVertexArrays(beachVaoId);
    }
}