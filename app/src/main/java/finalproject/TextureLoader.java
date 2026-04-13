package finalproject;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class TextureLoader {

    public static int load(String path) {
        int textureId;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width    = stack.mallocInt(1);
            IntBuffer height   = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load the file into a ByteBuffer
            InputStream stream = TextureLoader.class
                .getClassLoader()
                .getResourceAsStream(path);

            if (stream == null)
                throw new RuntimeException("Texture not found: " + path);

            // Read stream into a direct ByteBuffer
            ByteBuffer imageBuffer = readStreamToBuffer(stream);

            // Tell STB to flip vertically so Y=0 is bottom (OpenGL convention)
            STBImage.stbi_set_flip_vertically_on_load(true);

            ByteBuffer pixels = STBImage.stbi_load_from_memory(
                imageBuffer, width, height, channels, 4); // 4 = force RGBA

            if (pixels == null)
                throw new RuntimeException("Failed to decode texture: "
                    + path + " — " + STBImage.stbi_failure_reason());

            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            // Upload pixel data to GPU
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D, 0,
                GL11.GL_RGBA,
                width.get(0), height.get(0), 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                pixels
            );

            // Generate mipmaps for better quality at distance
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            // Texture filtering
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            // Wrapping — repeat so textures tile across large surfaces
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

            STBImage.stbi_image_free(pixels);

        } catch (Exception e) {
            throw new RuntimeException("TextureLoader error: " + path, e);
        }

        return textureId;
    }

    // Bind a texture to a texture unit before drawing
    public static void bind(int textureId, int unit) {
        // unit 0 = GL_TEXTURE0, unit 1 = GL_TEXTURE1, etc.
        org.lwjgl.opengl.GL13.glActiveTexture(
            org.lwjgl.opengl.GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    public static void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public static void delete(int textureId) {
        GL11.glDeleteTextures(textureId);
    }

    private static ByteBuffer readStreamToBuffer(InputStream stream)
            throws Exception {
        ReadableByteChannel channel = Channels.newChannel(stream);
        ByteBuffer buffer = ByteBuffer.allocateDirect(8 * 1024);

        while (true) {
            int read = channel.read(buffer);
            if (read == -1) break;
            if (buffer.remaining() == 0) {
                // Grow buffer
                ByteBuffer bigger = ByteBuffer.allocateDirect(
                    buffer.capacity() * 2);
                buffer.flip();
                bigger.put(buffer);
                buffer = bigger;
            }
        }

        buffer.flip();
        return buffer;
    }
}