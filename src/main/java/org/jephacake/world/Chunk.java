package org.jephacake.world;

import org.jephacake.renderer.*;
import org.joml.Matrix4f;

public class Chunk implements AutoCloseable {
    public static final int SIZE = 16;

    private final int cx, cy, cz;
    private final int[] voxels;
    private boolean dirty = false;

    // per-chunk GL objects (cached)
    private Mesh mesh = null;
    private Model model = null;

    public Chunk(int cx, int cy, int cz) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.voxels = new int[SIZE * SIZE * SIZE];
    }

    private int index(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
    }

    // voxel manipulation
    public void setBlock(int x, int y, int z, int blockId) {
        voxels[index(x,y,z)] = blockId;
        dirty = true;
    }

    public void setBlock(int x, int y, int z, int blockId, boolean shouldUpdate) {
        voxels[index(x,y,z)] = blockId;
        if (shouldUpdate) dirty = true;
    }

    public int getBlock(int x, int y, int z) {
        return voxels[index(x,y,z)];
    }

    public int[] getVoxelData() { return voxels; }

    public int getCX() { return cx; }
    public int getCY() { return cy; }
    public int getCZ() { return cz; }

    public boolean isDirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clearDirty() { dirty = false; }

    /**
     * Rebuild (synchronous) the mesh for this chunk.
     * NOTE: the mesher now produces vertex positions in LOCAL CHUNK COORDINATES (0..SIZE).
     * The renderer will translate by (cx*SIZE, cy*SIZE, cz*SIZE) + world.position at draw-time.
     */
    public synchronized void rebuildMesh(TextureAtlas atlas, World world) {
        if (!dirty) return;

        // Use mesher that emits local coordinates
        ChunkMesher.MeshData data = ChunkMesher.meshDataFromChunk(world, this, atlas);

        // If the mesh is empty -> free existing resources (if any) and clear dirty flag
        if (data.indices == null || data.indices.length == 0) {
            if (model != null) {
                try { model.close(); } catch (Exception e) { e.printStackTrace(); }
                model = null;
                mesh = null;
            }
            dirty = false;
            return;
        }

        if (mesh == null) {
            // Create new mesh and model
            mesh = new Mesh(data.positions, data.normals, data.texcoords, data.colors, data.indices);
            model = new Model(mesh, atlas.getTexture()); // Model does NOT own atlas texture by default
        } else {
            // Update existing GPU buffers with new data (orphan + reupload)
            mesh.update(data.positions, data.normals, data.texcoords, data.colors, data.indices);
        }

        dirty = false;
    }

    /**
     * Return the cached Model (may be null). The renderer will call Renderer.renderModel(model, modelMatrix)
     * with the appropriate translation matrix.
     */
    public synchronized Model getCachedModel() {
        return model;
    }

    /**
     * Return the mesh for diagnostics (may be null).
     */
    public synchronized Mesh getMesh() {
        return mesh;
    }

    @Override
    public synchronized void close() {
        if (model != null) {
            try { model.close(); } catch (Exception e) { e.printStackTrace(); }
            model = null;
            mesh = null;
        }
    }
}
