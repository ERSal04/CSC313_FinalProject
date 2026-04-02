package finalproject;

import java.io.BufferedReader;
import java.io.InputStream;

import org.lwjgl.opengl.GL20;

public class ShaderProgram {
    
    private int programId;

    public ShaderProgram(String vertecSource, String fragmentSource) {
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertecSource);
        GL20.glCompileShader(vertexShader);

        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Vertex shader error: " + GL20.glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSource);
        GL20.glCompileShader(fragmentShader);

        if(GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Fragment shader error: " +  GL20.glGetShaderInfoLog(fragmentShader));
        }

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShader);
        GL20.glAttachShader(programId, fragmentShader);
        GL20.glLinkProgram(programId);

        if(GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Shader link error: " + GL20.glGetShaderInfoLog(programId));
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public void setUniformMatrix4f(String name, org.joml.Matrix4f matrix) {
        int location = GL20.glGetUniformLocation(programId, name);
        float[] buffer = new float[16];
        matrix.get(buffer);
        GL20.glUniformMatrix4fv(location, false, buffer);
    }

    public void setUniformFloat(String name, float value) {
        int location = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform1f(location, value);
    }

    public void setUniformVec2(String name, float x, float y) {
        int location = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform2f(location, x, y);
    }

    public void setUniformVec3(String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(programId, name);
        GL20.glUniform3f(location, x, y, z);
    }

    public void cleanup() {
        GL20.glDeleteProgram(programId);
    }

    public static String loadFile(String path) {
        try {
            InputStream stream = ShaderProgram.class.getClassLoader().getResourceAsStream(path);
            if(stream == null)
                throw new RuntimeException("Could not find shader file: " + path);

            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            reader.close();
            return builder.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

}

