package org.jephacake.world;

public interface WorldGenerator {
    Chunk generateChunk(int cx, int cy, int cz);
}
