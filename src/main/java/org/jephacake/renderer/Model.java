package org.jephacake.renderer;

import org.joml.Matrix4f;

/**
 * Lightweight model wrapper around mesh and optional texture.
 * Does NOT own the texture by default.
 */
public class Model implements AutoCloseable {
    private final Mesh mesh;
    private final TextureGL texture;
    private final boolean ownsTexture; // new

    public Model(Mesh mesh, TextureGL texture) {
        this(mesh, texture, false);
    }

    public Model(Mesh mesh, TextureGL texture, boolean ownsTexture) {
        this.mesh = mesh;
        this.texture = texture;
        this.ownsTexture = ownsTexture;
    }

    public void render(ShaderProgram shader, Matrix4f modelMatrix) {
        shader.setUniform("uModel", modelMatrix);
        if (texture != null) {
            texture.bind(0);
            shader.setUniform("uTexture", 0);
        }
        mesh.render();
        if (texture != null) texture.unbind();
    }

    @Override
    public void close() {
        mesh.close();
        // only close texture if this Model actually owns it
        if (ownsTexture && texture != null) texture.close();
    }
}
