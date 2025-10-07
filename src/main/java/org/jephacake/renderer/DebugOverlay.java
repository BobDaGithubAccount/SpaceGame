package org.jephacake.renderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Simple bitmap text overlay for debug information.
 * Renders white monospace text in screen-space (top-right corner).
 */
public class DebugOverlay implements AutoCloseable {
    private static final int FONT_BITMAP_SIZE = 512;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;

    private final int texId;
    private final STBTTBakedChar.Buffer cdata;

    public DebugOverlay() throws Exception {
        cdata = STBTTBakedChar.malloc(NUM_CHARS);

        ByteBuffer ttf;
        try (var is = ResourceLoader.getResource("/org/jephacake/assets/fonts/RobotoMono-Regular.ttf")) {
            ttf = ioResourceToByteBuffer(is.readAllBytes());
        }

        ByteBuffer bitmap = memAlloc(FONT_BITMAP_SIZE * FONT_BITMAP_SIZE);
        stbtt_BakeFontBitmap(ttf, 16, bitmap, FONT_BITMAP_SIZE, FONT_BITMAP_SIZE, FIRST_CHAR, cdata);

        texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, FONT_BITMAP_SIZE, FONT_BITMAP_SIZE, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        memFree(bitmap);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private static ByteBuffer ioResourceToByteBuffer(byte[] bytes) {
        ByteBuffer buffer = memAlloc(bytes.length);
        buffer.put(bytes).flip();
        return buffer;
    }

    public void renderText(String text, int x, int y, int windowWidth, int windowHeight) {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texId);
        glColor3f(1f, 1f, 1f);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(x);
            FloatBuffer yb = stack.floats(y);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
            glBegin(GL_QUADS);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
                stbtt_GetBakedQuad(cdata, FONT_BITMAP_SIZE, FONT_BITMAP_SIZE, c - FIRST_CHAR, xb, yb, q, true);
                float x0 = q.x0(), y0 = q.y0(), x1 = q.x1(), y1 = q.y1();
                float s0 = q.s0(), t0 = q.t0(), s1 = q.s1(), t1 = q.t1();
                glTexCoord2f(s0, t0); glVertex2f(x0, y0);
                glTexCoord2f(s1, t0); glVertex2f(x1, y0);
                glTexCoord2f(s1, t1); glVertex2f(x1, y1);
                glTexCoord2f(s0, t1); glVertex2f(x0, y1);
            }
            glEnd();
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    @Override
    public void close() {
        glDeleteTextures(texId);
        cdata.free();
    }
}
