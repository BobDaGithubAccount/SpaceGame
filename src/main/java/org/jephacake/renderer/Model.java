package org.jephacake.renderer;

import org.joml.Matrix4f;

/**
 * Lightweight model wrapper around mesh and optional texture.
 * Not for chunks or voxels, but for entities, items and exotica.
 */
public class Model implements AutoCloseable {
    private final Mesh mesh;
    private final TextureGL texture;

    public Model(Mesh mesh, TextureGL texture) {
        this.mesh = mesh;
        this.texture = texture;
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
        if (texture != null) texture.close();
    }
}
