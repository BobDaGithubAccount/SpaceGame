package org.jephacake.world;

public class FlatWorldGenerator implements WorldGenerator {
    private final int groundHeight;

    public FlatWorldGenerator(int groundHeight) {
        this.groundHeight = groundHeight;
    }

    @Override
    public Chunk generateChunk(int cx, int cy, int cz) {
        Chunk c = new Chunk(cx, cy, cz);

        // Simple flat terrain: everything below y=groundHeight is stone
        for (int z=0; z<Chunk.SIZE; z++) {
            for (int y=0; y<Chunk.SIZE; y++) {
                for (int x=0; x<Chunk.SIZE; x++) {
                    int wy = cy * Chunk.SIZE + y;
                    if (wy < groundHeight) {
                        c.setBlock(x,y,z, 2);
                    }
                }
            }
        }

        return c;
    }
}
