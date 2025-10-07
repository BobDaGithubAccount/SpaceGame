package org.jephacake.renderer;

import org.jephacake.configuration.Options;
import org.jephacake.world.Chunk;
import org.jephacake.world.World;
import org.joml.FrustumIntersection;
import java.util.Collection;

/**
 * Small helper that does frustum test + chunk rendering.
 */
public class ChunkRenderer {

    public Collection<Chunk> oldChunksForDebug;

    public void renderChunks(Collection<Chunk> chunks, Renderer renderer, World world) {

        FrustumIntersection fi = renderer.getFrustum();

        int a = 0;

        for (Chunk c : chunks) {
            float minX = c.getCX() * Chunk.SIZE + world.position.x;
            float minY = c.getCY() * Chunk.SIZE + world.position.y;
            float minZ = c.getCZ() * Chunk.SIZE + world.position.z;
            float maxX = minX + Chunk.SIZE;
            float maxY = minY + Chunk.SIZE;
            float maxZ = minZ + Chunk.SIZE;

            if (!fi.testAab(minX, minY, minZ, maxX, maxY, maxZ)) continue;

            c.render(renderer);

            if(c.getMesh() == null || c.getMesh().getVertexCount() == 0) continue;
            a = a + c.getMesh().getVertexCount();
        }

        System.out.println("Rendered " + chunks.size() + " chunks with total " + a + " vertices.");
    }

}
