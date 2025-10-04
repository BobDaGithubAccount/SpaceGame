package org.jephacake.world;

import org.jephacake.renderer.*;
import org.joml.Matrix4f;

public class Chunk implements AutoCloseable {
    public static final int SIZE = 16;

    private final int cx, cy, cz;
    private final int[] voxels;
    private Model model;
    private boolean dirty = false;

    public Chunk(int cx, int cy, int cz) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.voxels = new int[SIZE * SIZE * SIZE];
    }

    private int index(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
    }

    public void setBlock(int x, int y, int z, int blockId) {
        voxels[index(x,y,z)] = blockId;
        dirty = true;
    }

    public int getBlock(int x, int y, int z) {
        return voxels[index(x,y,z)];
    }

    public void markDirty() { dirty = true; }

    public void rebuildMesh(TextureAtlas atlas) {
        if (!dirty) return;
        if (model != null) model.close();
        Mesh mesh = ChunkMesher.meshFromVoxels(SIZE, SIZE, SIZE, voxels, atlas);
        model = new Model(mesh, atlas.getTexture());
        dirty = false;
    }

    public void render(Renderer renderer) {
        if (model == null) return;
        Matrix4f transform = new Matrix4f().translation(cx * SIZE, cy * SIZE, cz * SIZE);
        renderer.renderModel(model, transform);
    }

    @Override
    public void close() {
        if (model != null) model.close();
    }

    public int[] getVoxelData() { return voxels; }
    public int getCX() { return cx; }
    public int getCY() { return cy; }
    public int getCZ() { return cz; }
}
