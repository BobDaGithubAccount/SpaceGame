package org.jephacake.block;

import java.util.Arrays;

/**
 * Simple cube block. Provide per-face tile indices (length 6) OR convenience constructor (top,bottom,side).
 * When constructed it auto-registers via Block super constructor.
 */
public class SimpleBlock extends Block {
    private final int[] perFaceTile; // index per BlockFace.ordinal()
    private final float[] tint;
    private final boolean opaque;

    public SimpleBlock(String name, int topTile, int bottomTile, int sideTile) {
        this(1, name, new int[] { sideTile, sideTile, bottomTile, topTile, sideTile, sideTile }, new float[]{1,1,1,1}, true);
    }

    public SimpleBlock(int id, String name, int[] perFaceTiles, float[] tintRGBA, boolean opaque) {
        super(id, name);
        if (perFaceTiles == null || perFaceTiles.length != 6) throw new IllegalArgumentException("perFaceTiles length must be 6");
        this.perFaceTile = Arrays.copyOf(perFaceTiles, 6);
        this.tint = (tintRGBA == null) ? new float[]{1,1,1,1} : Arrays.copyOf(tintRGBA, 4);
        this.opaque = opaque;
    }

    @Override
    public int getTileForFace(BlockFace face) {
        return perFaceTile[face.ordinal()];
    }

    @Override
    public float[] getTintRGBA() { return Arrays.copyOf(tint, 4); }

    @Override
    public boolean isOpaque() { return opaque; }
}
