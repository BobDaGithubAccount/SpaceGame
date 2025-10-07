package org.jephacake.world;

import org.jephacake.renderer.*;
import org.joml.Matrix4f;

public class Chunk implements AutoCloseable {
    public static final int SIZE = 16;

    private final int cx, cy, cz;
    private final int[] voxels;
    private boolean dirty = false;

    // new: per-chunk GL objects
    private Mesh mesh = null;
    private Model model = null;

    public Chunk(int cx, int cy, int cz) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.voxels = new int[SIZE * SIZE * SIZE];
    }

    private int index(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
    }

    /// ////////////
    public void setBlock(int x, int y, int z, int blockId) {
        voxels[index(x,y,z)] = blockId;
        dirty = true;
    }

    public void setBlock(int x, int y, int z, int blockId, boolean shouldUpdate) {
        voxels[index(x,y,z)] = blockId;
        if (shouldUpdate) dirty = true;
    }
    /// ////////////

    public int getBlock(int x, int y, int z) {
        return voxels[index(x,y,z)];
    }

    public void markDirty() { dirty = true; }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }

    /**
     * Rebuild (synchronous) the mesh for this chunk using the provided world as BlockAccessor.
     * If mesh/model do not exist, create them. If they exist, update them in-place.
     */
    public void rebuildMesh(TextureAtlas atlas, World world, float worldOffsetX, float worldOffsetY, float worldOffsetZ) {
        if (!dirty) return;

        ChunkMesher.MeshData data = ChunkMesher.meshDataFromChunk(world, this, atlas, worldOffsetX, worldOffsetY, worldOffsetZ);

        // empty mesh -> free resources
        if (data.indices.length == 0) {
            if (model != null) {
                model.close();
                model = null;
                mesh = null;
            }
            dirty = false;
            return;
        }

        if (mesh == null) {
            mesh = new Mesh(data.positions, data.normals, data.texcoords, data.colors, data.indices);
            model = new Model(mesh, atlas.getTexture()); // Model does NOT own atlas texture
        } else {
            mesh.update(data.positions, data.normals, data.texcoords, data.colors, data.indices);
        }

        dirty = false;
    }

    public void render(Renderer renderer) {
        if (model == null) return;
        // vertices are already in world-space (ChunkMesher added offsets), so identity transform
        renderer.renderModel(model, new Matrix4f().identity());
    }

    @Override
    public void close() {
        if (model != null) {
            model.close(); // this will close mesh too, but Model does NOT own atlas texture by default
            model = null;
            mesh = null;
        }
    }

    public int[] getVoxelData() { return voxels; }
    public int getCX() { return cx; }
    public int getCY() { return cy; }
    public int getCZ() { return cz; }

    public Mesh getMesh() { return mesh; }
}
