package org.jephacake.renderer;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh using separate VBOs for clarity.
 * Attribute layout:
 * 0 = vec3 position
 * 1 = vec3 normal
 * 2 = vec2 texcoord
 * 3 = vec4 color/light (RGBA)
 */
public class Mesh implements AutoCloseable {
    private final int vaoId;
    private int vertexCount; // now mutable so update() can change it
    private final int vboPos;
    private final int vboNorm;
    private final int vboTex;
    private final int vboColor;
    private final int ebo;

    public Mesh(float[] positions, float[] normals, float[] texcoords, float[] colors, int[] indices) {
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboPos = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboPos);
        FloatBuffer posBuf = BufferUtils.createFloatBuffer(positions.length);
        posBuf.put(positions).flip();
        glBufferData(GL_ARRAY_BUFFER, posBuf, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        vboNorm = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboNorm);
        FloatBuffer normBuf = BufferUtils.createFloatBuffer(normals.length);
        normBuf.put(normals).flip();
        glBufferData(GL_ARRAY_BUFFER, normBuf, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

        vboTex = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboTex);
        FloatBuffer texBuf = BufferUtils.createFloatBuffer(texcoords.length);
        texBuf.put(texcoords).flip();
        glBufferData(GL_ARRAY_BUFFER, texBuf, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);

        vboColor = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboColor);
        FloatBuffer colBuf = BufferUtils.createFloatBuffer(colors.length);
        colBuf.put(colors).flip();
        glBufferData(GL_ARRAY_BUFFER, colBuf, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 4, GL_FLOAT, false, 0, 0);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer iBuf = BufferUtils.createIntBuffer(indices.length);
        iBuf.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        vertexCount = indices.length;
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * Replace contents of the VBOs/IBO with new data.
     * Uses GL_DYNAMIC_DRAW and re-uploads full buffers (orphaning old storage).
     */
    public void update(float[] positions, float[] normals, float[] texcoords, float[] colors, int[] indices) {
        glBindVertexArray(vaoId);

        // Positions
        glBindBuffer(GL_ARRAY_BUFFER, vboPos);
        FloatBuffer posBuf = BufferUtils.createFloatBuffer(positions.length);
        posBuf.put(positions).flip();
        glBufferData(GL_ARRAY_BUFFER, posBuf, GL_DYNAMIC_DRAW);

        // Normals
        glBindBuffer(GL_ARRAY_BUFFER, vboNorm);
        FloatBuffer normBuf = BufferUtils.createFloatBuffer(normals.length);
        normBuf.put(normals).flip();
        glBufferData(GL_ARRAY_BUFFER, normBuf, GL_DYNAMIC_DRAW);

        // Texcoords
        glBindBuffer(GL_ARRAY_BUFFER, vboTex);
        FloatBuffer texBuf = BufferUtils.createFloatBuffer(texcoords.length);
        texBuf.put(texcoords).flip();
        glBufferData(GL_ARRAY_BUFFER, texBuf, GL_DYNAMIC_DRAW);

        // Colors
        glBindBuffer(GL_ARRAY_BUFFER, vboColor);
        FloatBuffer colBuf = BufferUtils.createFloatBuffer(colors.length);
        colBuf.put(colors).flip();
        glBufferData(GL_ARRAY_BUFFER, colBuf, GL_DYNAMIC_DRAW);

        // Indices
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer iBuf = BufferUtils.createIntBuffer(indices.length);
        iBuf.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        this.vertexCount = indices.length;
    }

    @Override
    public void close() {
        glDeleteBuffers(vboPos);
        glDeleteBuffers(vboNorm);
        glDeleteBuffers(vboTex);
        glDeleteBuffers(vboColor);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vaoId);
    }

    public int getVertexCount() {
        return vertexCount;
    }
}