package org.jephacake.renderer;

import org.jephacake.block.Block;
import org.jephacake.block.BlockFace;
import org.jephacake.block.BlockRegistry;
import org.jephacake.world.Chunk;
import org.jephacake.world.World;

import java.util.ArrayList;

/**
 * Chunk mesher producing mesh data in *local chunk coordinates*.
 * i.e. vertex positions range with each chunk from 0..Chunk.SIZE (plus unit extents for block quads).
 */
public class ChunkMesher {

    public static class MeshData {
        public final float[] positions;
        public final float[] normals;
        public final float[] texcoords;
        public final float[] colors;
        public final int[] indices;

        public MeshData(float[] positions, float[] normals, float[] texcoords, float[] colors, int[] indices) {
            this.positions = positions;
            this.normals = normals;
            this.texcoords = texcoords;
            this.colors = colors;
            this.indices = indices;
        }
    }

    /**
     * Build mesh data for a single chunk. OUTPUT IS IN LOCAL CHUNK COORDINATES.
     * World is used only to query neighbor blocks (global coords) for occlusion checks.
     */
    public static MeshData meshDataFromChunk(World world, Chunk c, TextureAtlas atlas) {
        ArrayList<Float> pos = new ArrayList<>();
        ArrayList<Float> norm = new ArrayList<>();
        ArrayList<Float> tex = new ArrayList<>();
        ArrayList<Float> col = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();

        final int baseCX = c.getCX() * Chunk.SIZE;
        final int baseCY = c.getCY() * Chunk.SIZE;
        final int baseCZ = c.getCZ() * Chunk.SIZE;
        final int[] vox = c.getVoxelData();

        for (int z = 0; z < Chunk.SIZE; z++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int blockId = vox[x + y * Chunk.SIZE + z * Chunk.SIZE * Chunk.SIZE];
                    if (blockId == 0) continue;
                    Block block = BlockRegistry.getOrNull(blockId);
                    if (block == null) continue;

                    float[] tint = block.getTintRGBA();

                    addFaceIfEmptyLocal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.NORTH, 0f, 0f, -1f, block, atlas, tint);
                    addFaceIfEmptyLocal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.SOUTH, 0f, 0f, 1f, block, atlas, tint);
                    addFaceIfEmptyLocal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.BOTTOM, 0f, -1f, 0f, block, atlas, tint);
                    addFaceIfEmptyLocal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.TOP, 0f, 1f, 0f, block, atlas, tint);
                    addFaceIfEmptyLocal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.WEST, -1f, 0f, 0f, block, atlas, tint);
                    addFaceIfEmptyLocal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.EAST, 1f, 0f, 0f, block, atlas, tint);
                }
            }
        }

        return new MeshData(
                toArray(pos),
                toArray(norm),
                toArray(tex),
                toArray(col),
                idx.stream().mapToInt(i -> i).toArray()
        );
    }

    /**
     * Add a face if there is no neighbour block (neighbor check uses world/global coords).
     * Vertex coordinates produced here are local to the chunk (no world offsets).
     */
    private static void addFaceIfEmptyLocal(ArrayList<Float> pos, ArrayList<Float> norm, ArrayList<Float> tex, ArrayList<Float> col,
                                            ArrayList<Integer> idx,
                                            int baseCX, int baseCY, int baseCZ, int x, int y, int z,
                                            World world,
                                            BlockFace face, float nx, float ny, float nz, Block block, TextureAtlas atlas, float[] tint) {
        int dx = 0, dy = 0, dz = 0;
        switch (face) {
            case NORTH -> dz = -1;
            case SOUTH -> dz = 1;
            case BOTTOM -> dy = -1;
            case TOP -> dy = 1;
            case WEST -> dx = -1;
            case EAST -> dx = 1;
        }

        int neighGlobalX = baseCX + x + dx;
        int neighGlobalY = baseCY + y + dy;
        int neighGlobalZ = baseCZ + z + dz;

        // If neighbor is empty (or chunk missing), emit the face
        if (world.getBlockGlobal(neighGlobalX, neighGlobalY, neighGlobalZ) == 0) {
            int tile = block.getTileForFace(face);
            float[] uv = atlas.getUVRect(tile);
            float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

            int base = pos.size() / 3;

            // local coordinates (no baseCX/CY/CZ added)
            float wx = x;
            float wy = y;
            float wz = z;

            switch (face) {
                case NORTH -> {
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 0f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 0f, nx, ny, nz, u1, v1, tint);
                }
                case SOUTH -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 1f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 1f, nx, ny, nz, u0, v1, tint);
                }
                case BOTTOM -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 1f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 1f, nx, ny, nz, u0, v1, tint);
                }
                case TOP -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 0f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 0f, nx, ny, nz, u0, v1, tint);
                }
                case WEST -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 1f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 0f, nx, ny, nz, u1, v1, tint);
                }
                case EAST -> {
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 0f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 1f, nx, ny, nz, u1, v1, tint);
                }
            }

            idx.add(base + 0); idx.add(base + 1); idx.add(base + 2);
            idx.add(base + 2); idx.add(base + 3); idx.add(base + 0);
        }
    }

    public static MeshData meshDataFromChunkSnapshot(BlockSnapshot snap, int cx, int cy, int cz, int[] voxels, TextureAtlas atlas) {
        ArrayList<Float> pos = new ArrayList<>();
        ArrayList<Float> norm = new ArrayList<>();
        ArrayList<Float> tex = new ArrayList<>();
        ArrayList<Float> col = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();

        final int baseCX = cx * Chunk.SIZE;
        final int baseCY = cy * Chunk.SIZE;
        final int baseCZ = cz * Chunk.SIZE;

        for (int z = 0; z < Chunk.SIZE; z++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int blockId = voxels[x + y * Chunk.SIZE + z * Chunk.SIZE * Chunk.SIZE];
                    if (blockId == 0) continue;
                    Block block = BlockRegistry.getOrNull(blockId);
                    if (block == null) continue;

                    float[] tint = block.getTintRGBA();

                    addFaceIfEmptySnapshot(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            snap, BlockFace.NORTH, 0f, 0f, -1f, block, atlas, tint);
                    addFaceIfEmptySnapshot(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            snap, BlockFace.SOUTH, 0f, 0f, 1f, block, atlas, tint);
                    addFaceIfEmptySnapshot(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            snap, BlockFace.BOTTOM, 0f, -1f, 0f, block, atlas, tint);
                    addFaceIfEmptySnapshot(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            snap, BlockFace.TOP, 0f, 1f, 0f, block, atlas, tint);
                    addFaceIfEmptySnapshot(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            snap, BlockFace.WEST, -1f, 0f, 0f, block, atlas, tint);
                    addFaceIfEmptySnapshot(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            snap, BlockFace.EAST, 1f, 0f, 0f, block, atlas, tint);
                }
            }
        }

        return new MeshData(
                toArray(pos),
                toArray(norm),
                toArray(tex),
                toArray(col),
                idx.stream().mapToInt(i -> i).toArray()
        );
    }

    private static void addFaceIfEmptySnapshot(ArrayList<Float> pos, ArrayList<Float> norm, ArrayList<Float> tex, ArrayList<Float> col,
                                               ArrayList<Integer> idx,
                                               int baseCX, int baseCY, int baseCZ, int x, int y, int z,
                                               BlockSnapshot snap,
                                               BlockFace face, float nx, float ny, float nz, Block block, TextureAtlas atlas, float[] tint) {
        int dx = 0, dy = 0, dz = 0;
        switch (face) {
            case NORTH -> dz = -1;
            case SOUTH -> dz = 1;
            case BOTTOM -> dy = -1;
            case TOP -> dy = 1;
            case WEST -> dx = -1;
            case EAST -> dx = 1;
        }

        int neighGlobalX = baseCX + x + dx;
        int neighGlobalY = baseCY + y + dy;
        int neighGlobalZ = baseCZ + z + dz;

        // Query snapshot (not world). Missing neighbors -> treated as empty/air.
        if (snap.getBlockGlobal(neighGlobalX, neighGlobalY, neighGlobalZ) == 0) {
            int tile = block.getTileForFace(face);
            float[] uv = atlas.getUVRect(tile);
            float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

            int base = pos.size() / 3;

            // local chunk coordinates (no world offsets)
            float wx = x;
            float wy = y;
            float wz = z;

            switch (face) {
                case NORTH -> {
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 0f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 0f, nx, ny, nz, u1, v1, tint);
                }
                case SOUTH -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 1f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 1f, nx, ny, nz, u0, v1, tint);
                }
                case BOTTOM -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 1f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 1f, nx, ny, nz, u0, v1, tint);
                }
                case TOP -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 0f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 0f, nx, ny, nz, u0, v1, tint);
                }
                case WEST -> {
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 0f, wz + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 1f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f, wy + 1f, wz + 0f, nx, ny, nz, u1, v1, tint);
                }
                case EAST -> {
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 0f, wz + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 0f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 1f, wy + 1f, wz + 1f, nx, ny, nz, u1, v1, tint);
                }
            }

            idx.add(base + 0); idx.add(base + 1); idx.add(base + 2);
            idx.add(base + 2); idx.add(base + 3); idx.add(base + 0);
        }
    }

    private static float[] toArray(ArrayList<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static void addVertex(ArrayList<Float> pos, ArrayList<Float> norm, ArrayList<Float> tex, ArrayList<Float> col,
                                  float px, float py, float pz,
                                  float nx, float ny, float nz,
                                  float u, float v,
                                  float[] tintRGBA) {
        pos.add(px); pos.add(py); pos.add(pz);
        norm.add(nx); norm.add(ny); norm.add(nz);
        tex.add(u); tex.add(v);
        col.add(tintRGBA[0]); col.add(tintRGBA[1]); col.add(tintRGBA[2]); col.add(tintRGBA[3]);
    }
}
