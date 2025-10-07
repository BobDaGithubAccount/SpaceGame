package org.jephacake.renderer;

import org.jephacake.world.Chunk;
import org.jephacake.world.World;
import org.joml.Matrix4f;
import org.joml.FrustumIntersection;

import java.util.Collection;

/**
 * Small helper that does frustum test + chunk rendering.
 * Expects chunk mesh vertex positions to be in local chunk coordinates (0..Chunk.SIZE).
 * Applies translation at render time: translate = (cx*SIZE, cy*SIZE, cz*SIZE) + world.position.
 */
public class ChunkRenderer {

    private final Matrix4f modelMat = new Matrix4f();

    public void renderChunks(Collection<Chunk> chunks, Renderer renderer, World world) {

        FrustumIntersection fi = renderer.getFrustum();

        int totalVerts = 0;
        int renderedChunks = 0;

        for (Chunk c : chunks) {
            float minX = c.getCX() * Chunk.SIZE + world.position.x;
            float minY = c.getCY() * Chunk.SIZE + world.position.y;
            float minZ = c.getCZ() * Chunk.SIZE + world.position.z;
            float maxX = minX + Chunk.SIZE;
            float maxY = minY + Chunk.SIZE;
            float maxZ = minZ + Chunk.SIZE;

            if (!fi.testAab(minX, minY, minZ, maxX, maxY, maxZ)) continue;

            Model model = c.getCachedModel();
            if (model == null) continue;

            modelMat.identity();
            modelMat.translate(minX, minY, minZ);

            renderer.renderModel(model, modelMat);

            if (c.getMesh() != null) totalVerts += c.getMesh().getVertexCount();
            renderedChunks++;
        }

//        System.out.println("Rendered " + renderedChunks + " chunks with total " + totalVerts + " vertices.");
    }
}
