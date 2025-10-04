package org.jephacake.block.blocks;

import org.jephacake.block.SimpleBlock;

import static org.jephacake.Main.atlas;

public class GrassBlock extends SimpleBlock {
    public GrassBlock() {
        int grassTop  = atlas.getTileIndex("grass_top");
        int grassSide = atlas.getTileIndex("grass_side");
        int grassBottom  = atlas.getTileIndex("dirt");

        super("grass", grassTop, grassBottom, grassSide);
        //            new SimpleBlock(3, "grass", new int[] { grassSide, grassSide, grassBottom, grassTop, grassSide, grassSide }, new float[] {1f,1f,1f,1f}, true);
    }
}
