package org.jephacake.block;

/**
 * Abstract block. Subclasses should call super(id, name) which will auto-register the block.
 * Block IDs must be > 0. 0 is reserved for air.
 */
public abstract class Block {
    public int id;
    private final String name;

    protected Block(int id, String name) {
        if (id <= 0) throw new IllegalArgumentException("Block id must be > 0");
        this.id = id; //NOTE: this is overwritten by BlockRegistry upon registration, hence the public field
        this.name = name;
        BlockRegistry.register(this); // auto-register when constructed
    }

    public int getId() { return id; }
    public String getName() { return name; }

    public abstract int getTileForFace(BlockFace face);

    public boolean isOpaque() { return true; }

    public float[] getTintRGBA() { return new float[] { 1f, 1f, 1f, 1f }; }
}
