package org.jephacake.renderer;

import org.jephacake.block.Block;
import org.jephacake.block.BlockFace;
import org.jephacake.block.BlockRegistry;
import org.jephacake.world.Chunk;
import org.jephacake.world.World;

import java.util.ArrayList;

//TODO HANDLE TRANSPARENCY (check neighbouring blocks for transparency, too)

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

    // existing meshFromChunks can remain; add this smaller, chunk-level builder:
    public static MeshData meshDataFromChunk(World world, Chunk c, TextureAtlas atlas, float worldOffsetX, float worldOffsetY, float worldOffsetZ) {
        ArrayList<Float> pos = new ArrayList<>();
        ArrayList<Float> norm = new ArrayList<>();
        ArrayList<Float> tex = new ArrayList<>();
        ArrayList<Float> col = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();

        int baseCX = c.getCX() * Chunk.SIZE;
        int baseCY = c.getCY() * Chunk.SIZE;
        int baseCZ = c.getCZ() * Chunk.SIZE;
        int[] vox = c.getVoxelData();

        for (int z = 0; z < Chunk.SIZE; z++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int blockId = vox[x + y * Chunk.SIZE + z * Chunk.SIZE * Chunk.SIZE];
                    if (blockId == 0) continue;
                    Block block = BlockRegistry.getOrNull(blockId);
                    if (block == null) continue;

                    float[] tint = block.getTintRGBA();

                    addFaceIfEmptyGlobal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.NORTH, 0f, 0f, -1f, block, atlas, tint,
                            worldOffsetX, worldOffsetY, worldOffsetZ);
                    addFaceIfEmptyGlobal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.SOUTH, 0f, 0f, 1f, block, atlas, tint,
                            worldOffsetX, worldOffsetY, worldOffsetZ);
                    addFaceIfEmptyGlobal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.BOTTOM, 0f, -1f, 0f, block, atlas, tint,
                            worldOffsetX, worldOffsetY, worldOffsetZ);
                    addFaceIfEmptyGlobal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.TOP, 0f, 1f, 0f, block, atlas, tint,
                            worldOffsetX, worldOffsetY, worldOffsetZ);
                    addFaceIfEmptyGlobal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.WEST, -1f, 0f, 0f, block, atlas, tint,
                            worldOffsetX, worldOffsetY, worldOffsetZ);
                    addFaceIfEmptyGlobal(pos, norm, tex, col, idx,
                            baseCX, baseCY, baseCZ, x, y, z,
                            world, BlockFace.EAST, 1f, 0f, 0f, block, atlas, tint,
                            worldOffsetX, worldOffsetY, worldOffsetZ);
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

    // Small adaptation of your existing method but querying the World for neighbors
    private static void addFaceIfEmptyGlobal(ArrayList<Float> pos, ArrayList<Float> norm, ArrayList<Float> tex, ArrayList<Float> col,
                                             ArrayList<Integer> idx,
                                             int baseCX, int baseCY, int baseCZ, int x, int y, int z,
                                             World world,
                                             BlockFace face, float nx, float ny, float nz, Block block, TextureAtlas atlas, float[] tint,
                                             float worldOffsetX, float worldOffsetY, float worldOffsetZ) {
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

        // Ask world for the neighbour block (treat missing chunk as empty -> face needed)
        if (world.getBlockGlobal(neighGlobalX, neighGlobalY, neighGlobalZ) == 0) {
            int tile = block.getTileForFace(face);
            float[] uv = atlas.getUVRect(tile);
            float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

            int base = pos.size() / 3;

            float wx = baseCX + x;
            float wy = baseCY + y;
            float wz = baseCZ + z;

            switch (face) {
                case NORTH -> {
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 0f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 0f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 1f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 1f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u1, v1, tint);
                }
                case SOUTH -> {
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 0f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 0f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 1f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 1f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u0, v1, tint);
                }
                case BOTTOM -> {
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 0f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 0f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 0f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 0f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u0, v1, tint);
                }
                case TOP -> {
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 1f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 1f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 1f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 1f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u0, v1, tint);
                }
                case WEST -> {
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 0f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 0f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 1f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 0f + worldOffsetX, wy + 1f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u1, v1, tint);
                }
                case EAST -> {
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 0f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 0f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 1f + worldOffsetY, wz + 0f + worldOffsetZ, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, wx + 1f + worldOffsetX, wy + 1f + worldOffsetY, wz + 1f + worldOffsetZ, nx, ny, nz, u1, v1, tint);
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
