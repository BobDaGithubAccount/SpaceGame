package org.jephacake.renderer;

import org.lwjgl.BufferUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class TextureGL implements AutoCloseable {
    private final int id;
    private final int width;
    private final int height;

    /**
     * Creates an OpenGL texture from a raw RGBA buffer.
     * Expects buffer in RGBA order, bottom-to-top row order.
     */
    public TextureGL(ByteBuffer rgbaBuffer, int width, int height) {
        this.width = width;
        this.height = height;

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, rgbaBuffer);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Convenience constructor for a BufferedImage.
     * Converts ARGB → RGBA and vertically flips to match GL coords.
     */
//    public TextureGL(BufferedImage img) {
//        this.width = img.getWidth();
//        this.height = img.getHeight();
//
//        // Ensure TYPE_INT_ARGB (non-premultiplied)
//        BufferedImage rgbaImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g = rgbaImg.createGraphics();
//        g.drawImage(img, 0, 0, null);
//        g.dispose();
//
//        int[] pixels = new int[width * height];
//        rgbaImg.getRGB(0, 0, width, height, pixels, 0, width);
//
//        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
//        for (int y = height - 1; y >= 0; y--) {
//            for (int x = 0; x < width; x++) {
//                int pixel = pixels[y * width + x];
//                byte a = (byte) ((pixel >> 24) & 0xFF);
//                byte r = (byte) ((pixel >> 16) & 0xFF);
//                byte gCol = (byte) ((pixel >> 8) & 0xFF);
//                byte b = (byte) (pixel & 0xFF);
//                buffer.put(r).put(gCol).put(b).put(a);
//            }
//        }
//        buffer.flip();
//
//        id = glGenTextures();
//        glBindTexture(GL_TEXTURE_2D, id);
//
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
//                GL_RGBA, GL_UNSIGNED_BYTE, buffer);
//
//        glBindTexture(GL_TEXTURE_2D, 0);
//    }

    public void bind(int unit) {
        if (unit < 0 || unit > 31) throw new IllegalArgumentException("Texture unit out of range");
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getId() { return id; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    @Override
    public void close() {
        glDeleteTextures(id);
    }
}
