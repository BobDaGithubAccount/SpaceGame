package org.jephacake.renderer;

import org.jephacake.renderer.ResourceLoader;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

/**
 * Minimal shader program helper (OpenGL 3.3+).
 */
public class ShaderProgram implements AutoCloseable {
    private final int programId;

    public ShaderProgram(String vertResourcePath, String fragResourcePath) throws IOException {
        String vertSrc = ResourceLoader.readResourceAsString(vertResourcePath);
        String fragSrc = ResourceLoader.readResourceAsString(fragResourcePath);
        int vert = compileShader(GL_VERTEX_SHADER, vertSrc);
        int frag = compileShader(GL_FRAGMENT_SHADER, fragSrc);
        programId = glCreateProgram();
        glAttachShader(programId, vert);
        glAttachShader(programId, frag);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Program link error: " + glGetProgramInfoLog(programId));
        }
        glDetachShader(programId, vert);
        glDetachShader(programId, frag);
        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    private int compileShader(int type, String src) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void use() {
        glUseProgram(programId);
    }

    public void stop() {
        glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }

    public void setUniform(String name, Matrix4f m) {
        int loc = getUniformLocation(name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(loc, false, fb);
        }
    }

    public void setUniform(String name, float x, float y, float z) {
        int loc = getUniformLocation(name);
        glUniform3f(loc, x, y, z);
    }

    public void setUniform(String name, float value) {
        int loc = getUniformLocation(name);
        glUniform1f(loc, value);
    }

    public void setUniform(String name, int value) {
        int loc = getUniformLocation(name);
        glUniform1i(loc, value);
    }

    @Override
    public void close() {
        glDeleteProgram(programId);
    }
}
