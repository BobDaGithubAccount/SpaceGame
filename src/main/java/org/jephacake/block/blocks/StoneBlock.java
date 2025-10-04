package org.jephacake.block.blocks;

import org.jephacake.block.SimpleBlock;

import static org.jephacake.Main.atlas;

public class StoneBlock extends SimpleBlock {
    public StoneBlock() {
        int stoneTile = atlas.getTileIndex("stone");
        super("stone", stoneTile, stoneTile, stoneTile);
    }
}
