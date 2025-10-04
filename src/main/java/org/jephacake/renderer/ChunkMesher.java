package org.jephacake.renderer;

import org.jephacake.block.Block;
import org.jephacake.block.BlockFace;
import org.jephacake.block.BlockRegistry;

import java.util.ArrayList;

public class ChunkMesher {

    public static Mesh meshFromVoxels(int width, int height, int depth, int[] voxels, TextureAtlas atlas) {
        ArrayList<Float> pos = new ArrayList<>();
        ArrayList<Float> norm = new ArrayList<>();
        ArrayList<Float> tex = new ArrayList<>();
        ArrayList<Float> col = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();

        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int blockId = voxels[x + y * width + z * width * height];
                    if (blockId == 0) continue;
                    Block block = BlockRegistry.getOrNull(blockId);
                    if (block == null) continue;

                    float[] tint = block.getTintRGBA();

                    // --- Faces ---
                    addFaceIfEmpty(pos, norm, tex, col, idx, x, y, z, voxels, width, height, depth, BlockFace.NORTH, 0f, 0f, -1f, block, atlas, tint);
                    addFaceIfEmpty(pos, norm, tex, col, idx, x, y, z, voxels, width, height, depth, BlockFace.SOUTH, 0f, 0f, 1f, block, atlas, tint);
                    addFaceIfEmpty(pos, norm, tex, col, idx, x, y, z, voxels, width, height, depth, BlockFace.BOTTOM, 0f, -1f, 0f, block, atlas, tint);
                    addFaceIfEmpty(pos, norm, tex, col, idx, x, y, z, voxels, width, height, depth, BlockFace.TOP, 0f, 1f, 0f, block, atlas, tint);
                    addFaceIfEmpty(pos, norm, tex, col, idx, x, y, z, voxels, width, height, depth, BlockFace.WEST, -1f, 0f, 0f, block, atlas, tint);
                    addFaceIfEmpty(pos, norm, tex, col, idx, x, y, z, voxels, width, height, depth, BlockFace.EAST, 1f, 0f, 0f, block, atlas, tint);
                }
            }
        }

        return new Mesh(
                toArray(pos),
                toArray(norm),
                toArray(tex),
                toArray(col),
                idx.stream().mapToInt(i -> i).toArray()
        );
    }

    private static void addFaceIfEmpty(ArrayList<Float> pos, ArrayList<Float> norm, ArrayList<Float> tex, ArrayList<Float> col,
                                       ArrayList<Integer> idx, int x, int y, int z, int[] voxels, int width, int height, int depth,
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

        if (isEmpty(x + dx, y + dy, z + dz, width, height, depth, voxels)) {
            int tile = block.getTileForFace(face);
            float[] uv = atlas.getUVRect(tile);
            float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

            int base = pos.size() / 3;

            switch (face) {
                case NORTH -> {
                    addVertex(pos, norm, tex, col, x + 1f, y + 0f, z + 0f, nx, ny, nz, u1, v0, tint); // bottom-right
                    addVertex(pos, norm, tex, col, x + 0f, y + 0f, z + 0f, nx, ny, nz, u0, v0, tint); // bottom-left
                    addVertex(pos, norm, tex, col, x + 0f, y + 1f, z + 0f, nx, ny, nz, u0, v1, tint); // top-left
                    addVertex(pos, norm, tex, col, x + 1f, y + 1f, z + 0f, nx, ny, nz, u1, v1, tint); // top-right
                }
                case SOUTH -> {
                    addVertex(pos, norm, tex, col, x + 0f, y + 0f, z + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 0f, z + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 1f, z + 1f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, x + 0f, y + 1f, z + 1f, nx, ny, nz, u0, v1, tint);
                }
                case BOTTOM -> {
                    addVertex(pos, norm, tex, col, x + 0f, y + 0f, z + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 0f, z + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 0f, z + 1f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, x + 0f, y + 0f, z + 1f, nx, ny, nz, u0, v1, tint);
                }
                case TOP -> {
                    addVertex(pos, norm, tex, col, x + 0f, y + 1f, z + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 1f, z + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 1f, z + 0f, nx, ny, nz, u1, v1, tint);
                    addVertex(pos, norm, tex, col, x + 0f, y + 1f, z + 0f, nx, ny, nz, u0, v1, tint);
                }
                case WEST -> {
                    addVertex(pos, norm, tex, col, x + 0f, y + 0f, z + 0f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, x + 0f, y + 0f, z + 1f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, x + 0f, y + 1f, z + 1f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, x + 0f, y + 1f, z + 0f, nx, ny, nz, u1, v1, tint);
                }
                case EAST -> {
                    addVertex(pos, norm, tex, col, x + 1f, y + 0f, z + 1f, nx, ny, nz, u1, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 0f, z + 0f, nx, ny, nz, u0, v0, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 1f, z + 0f, nx, ny, nz, u0, v1, tint);
                    addVertex(pos, norm, tex, col, x + 1f, y + 1f, z + 1f, nx, ny, nz, u1, v1, tint);
                }
            }

            // Add two triangles per quad
            idx.add(base + 0); idx.add(base + 1); idx.add(base + 2);
            idx.add(base + 2); idx.add(base + 3); idx.add(base + 0);
        }
    }

    private static boolean isEmpty(int x, int y, int z, int width, int height, int depth, int[] voxels) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) return true;
        return voxels[x + y * width + z * width * height] == 0;
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

    private static float[] toArray(ArrayList<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
