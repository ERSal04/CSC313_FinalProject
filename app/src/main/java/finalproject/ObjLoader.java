package finalproject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ObjLoader {

    public static class MeshData {
        public float[] vertices;  // interleaved: x,y,z, nx,ny,nz, u,v
        public int[]   indices;
        public String  materialFile; // name of the .mtl file if present
    }

    public static MeshData load(String path) {
        // Raw data read from file
        List<float[]> positions = new ArrayList<>();
        List<float[]> normals   = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();

        // Final interleaved data
        List<Float>   vertexList = new ArrayList<>();
        List<Integer> indexList  = new ArrayList<>();

        // Maps "posIdx/texIdx/normIdx" string to its index in vertexList
        // so we don't duplicate vertices
        java.util.Map<String, Integer> indexMap = new java.util.HashMap<>();

        String materialFile = null;

        try {
            InputStream stream = ObjLoader.class
                .getClassLoader()
                .getResourceAsStream(path);

            if (stream == null)
                throw new RuntimeException("OBJ file not found: " + path);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\s+");

                switch (parts[0]) {

                    case "mtllib":
                        // Material file reference
                        materialFile = parts[1];
                        break;

                    case "v":
                        // Vertex position
                        positions.add(new float[]{
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3])
                        });
                        break;

                    case "vn":
                        // Vertex normal
                        normals.add(new float[]{
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3])
                        });
                        break;

                    case "vt":
                        // Texture coordinate
                        texCoords.add(new float[]{
                            Float.parseFloat(parts[1]),
                            // OBJ V is flipped relative to OpenGL
                            1.0f - Float.parseFloat(parts[2])
                        });
                        break;

                    case "f":
                        // Face — parts[1..n] are vertices
                        // Each is "posIdx/texIdx/normIdx" (1-based)
                        // We only support triangles here
                        // If the face has 4 vertices we split into 2 triangles
                        int[] faceIndices = new int[parts.length - 1];

                        for (int i = 1; i < parts.length; i++) {
                            String key = parts[i];

                            if (indexMap.containsKey(key)) {
                                faceIndices[i - 1] = indexMap.get(key);
                            } else {
                                // Parse the v/vt/vn triple
                                String[] triple = parts[i].split("/");
                                int posIdx  = Integer.parseInt(triple[0]) - 1;
                                int texIdx  = triple.length > 1 && !triple[1].isEmpty()
                                            ? Integer.parseInt(triple[1]) - 1 : 0;
                                int normIdx = triple.length > 2
                                            ? Integer.parseInt(triple[2]) - 1 : 0;

                                float[] pos  = positions.get(posIdx);
                                float[] norm = normals.isEmpty()
                                            ? new float[]{0, 1, 0}
                                            : normals.get(normIdx);
                                float[] uv   = texCoords.isEmpty()
                                            ? new float[]{0, 0}
                                            : texCoords.get(texIdx);

                                // Interleaved: x y z nx ny nz u v
                                vertexList.add(pos[0]);
                                vertexList.add(pos[1]);
                                vertexList.add(pos[2]);
                                vertexList.add(norm[0]);
                                vertexList.add(norm[1]);
                                vertexList.add(norm[2]);
                                vertexList.add(uv[0]);
                                vertexList.add(uv[1]);

                                int newIndex = indexMap.size();
                                indexMap.put(key, newIndex);
                                faceIndices[i - 1] = newIndex;
                            }
                        }

                        // Triangulate — fan from vertex 0
                        // Triangle: 0,1,2 then 0,2,3 etc.
                        for (int i = 1; i < faceIndices.length - 1; i++) {
                            indexList.add(faceIndices[0]);
                            indexList.add(faceIndices[i]);
                            indexList.add(faceIndices[i + 1]);
                        }
                        break;
                }
            }

            reader.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load OBJ: " + path, e);
        }

        // Convert lists to arrays
        MeshData data = new MeshData();
        data.materialFile = materialFile;

        data.vertices = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++)
            data.vertices[i] = vertexList.get(i);

        data.indices = new int[indexList.size()];
        for (int i = 0; i < indexList.size(); i++)
            data.indices[i] = indexList.get(i);

        return data;
    }
}