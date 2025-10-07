package org.jephacake.world;

import java.util.Random;

public class StressTester implements WorldGenerator {
    private final Random random = new Random();

    @Override
    public Chunk generateChunk(int cx, int cy, int cz) {
        Chunk c = new Chunk(cx, cy, cz);

        if(random.nextBoolean()) return c;

        for (int z = 0; z < Chunk.SIZE; z++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int wx = cx * Chunk.SIZE + x;
                    int wy = cy * Chunk.SIZE + y;
                    int wz = cz * Chunk.SIZE + z;

                    int[] blockTypes = {2, 2, 3, 4};
                    int blockType = blockTypes[random.nextInt(blockTypes.length)];
                    c.setBlock(x, y, z, blockType);
                }
            }
        }

        return c;
    }
}
