package org.jephacake.world;

import org.jephacake.renderer.*;

public class Chunk implements AutoCloseable {
    public static final int SIZE = 16;

    private final int cx, cy, cz;
    private final int[] voxels;

    private Mesh mesh = null;
    private Model model = null;

    public Chunk(int cx, int cy, int cz) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.voxels = new int[SIZE * SIZE * SIZE];
    }

    private int index(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
    }

    // --- voxel access ---
    public void setBlock(int x, int y, int z, int blockId) {
        voxels[index(x, y, z)] = blockId;
    }

    public int getBlock(int x, int y, int z) {
        return voxels[index(x, y, z)];
    }

    public int[] getVoxelData() {
        return voxels;
    }

    public int getCX() { return cx; }
    public int getCY() { return cy; }
    public int getCZ() { return cz; }

    /** Generate new mesh data on background thread. */
    public ChunkMesher.MeshData generateMeshData(World world, TextureAtlas atlas) {
        return ChunkMesher.meshDataFromChunk(world, this, atlas);
    }

    /** Apply new GPU data on main thread once async meshing completes. */
    public synchronized void applyMeshData(ChunkMesher.MeshData data, TextureAtlas atlas) {
        if (data == null || data.indices == null || data.indices.length == 0) {
            if (model != null) {
                try { model.close(); } catch (Exception e) { e.printStackTrace(); }
                model = null;
                mesh = null;
            }
            return;
        }

        if (mesh == null) {
            mesh = new Mesh(data.positions, data.normals, data.texcoords, data.colors, data.indices);
            model = new Model(mesh, atlas.getTexture());
        } else {
            mesh.update(data.positions, data.normals, data.texcoords, data.colors, data.indices);
        }
    }

    public synchronized Model getCachedModel() { return model; }
    public synchronized Mesh getMesh() { return mesh; }

    @Override
    public synchronized void close() {
        if (model != null) {
            try { model.close(); } catch (Exception e) { e.printStackTrace(); }
            model = null;
            mesh = null;
        }
    }
}
