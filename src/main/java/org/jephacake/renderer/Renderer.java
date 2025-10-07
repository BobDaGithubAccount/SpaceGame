package org.jephacake.renderer;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Top-level rendering facade. Holds shader, projection & view matrices and exposes simple render API.
 */
public class Renderer implements AutoCloseable {
    private final ShaderProgram shader;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final FrustumIntersection frustum = new FrustumIntersection();

    public Renderer(int width, int height) throws Exception {
        shader = new ShaderProgram("org/jephacake/assets/shaders/voxel.vert", "org/jephacake/assets/shaders/voxel.frag");
        setProjection(width, height);

        glEnable(GL_DEPTH_TEST);

        glEnable(GL_REPEAT);

        glDepthFunc(GL_LESS);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
    }

    public void setProjection(int width, int height) {
        float aspect = (float) width / (float) height;
        projection.identity();
        projection.perspective((float)Math.toRadians(70.0f), aspect, 0.01f, 2000f);
    }

    public void setView(Matrix4f viewMatrix) {
        this.view.set(viewMatrix);
    }

    public void beginFrame() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        glEnable(GL_REPEAT);

        glClearColor(0.2f, 0.6f, 0.9f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.use();
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", view);

        // Update frustum from projection * view matrix (JOML expects projection * view)
        Matrix4f vp = new Matrix4f(projection).mul(view);
        frustum.set(vp);
    }

    public FrustumIntersection getFrustum() {
        return frustum;
    }

    public void renderModel(Model model, Matrix4f modelMatrix) {
        model.render(shader, modelMatrix);
    }

    // NOTE: Directional light will be from the sun and mobile lights...
    public void setDirectionalLight(Vector3f dir, Vector3f color, boolean enabled) {
        shader.use();
        shader.setUniform("uDirectionalLightDir", dir.x, dir.y, dir.z);
        shader.setUniform("uDirectionalLightColor", color.x, color.y, color.z);
        shader.setUniform("uUseDynamicLight", enabled ? 1f : 0f);
    }

    public void endFrame() {
        shader.stop();
    }

    @Override
    public void close() {
        shader.close();
    }
}
