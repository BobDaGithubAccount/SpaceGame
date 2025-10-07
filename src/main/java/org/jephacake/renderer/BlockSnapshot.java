package org.jephacake.renderer;

import org.jephacake.world.Chunk;

import java.util.HashMap;
import java.util.Map;

/**
 * Light-weight container holding copies of voxel arrays for a chunk and its neighbors.
 * Worker threads use this to query neighbor blocks without touching live chunk data.
 *
 * Keys are "cx,cy,cz".
 */
public class BlockSnapshot {
    private final Map<String, int[]> map = new HashMap<>();

    private static String key(int cx, int cy, int cz) {
        return cx + "," + cy + "," + cz;
    }

    public void put(int cx, int cy, int cz, int[] voxelsCopy) {
        map.put(key(cx, cy, cz), voxelsCopy);
    }

    /**
     * Return block id at global coords. If snapshot doesn't include a chunk, treat as empty (0).
     */
    public int getBlockGlobal(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, Chunk.SIZE);
        int cy = Math.floorDiv(wy, Chunk.SIZE);
        int cz = Math.floorDiv(wz, Chunk.SIZE);
        String k = key(cx, cy, cz);
        int[] vox = map.get(k);
        if (vox == null) return 0;
        int lx = Math.floorMod(wx, Chunk.SIZE);
        int ly = Math.floorMod(wy, Chunk.SIZE);
        int lz = Math.floorMod(wz, Chunk.SIZE);
        int idx = lx + ly * Chunk.SIZE + lz * Chunk.SIZE * Chunk.SIZE;
        return vox[idx];
    }
}
