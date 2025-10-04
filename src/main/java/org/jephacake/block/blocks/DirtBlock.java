package org.jephacake.block.blocks;

import org.jephacake.block.SimpleBlock;

import static org.jephacake.Main.atlas;

public class DirtBlock extends SimpleBlock {
    public DirtBlock() {
        int dirtTile = atlas.getTileIndex("dirt");
        super("dirt", dirtTile, dirtTile, dirtTile);
    }
}
