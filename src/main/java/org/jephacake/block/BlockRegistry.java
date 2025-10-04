package org.jephacake.block;

import org.jephacake.block.blocks.DirtBlock;
import org.jephacake.block.blocks.GrassBlock;
import org.jephacake.block.blocks.StoneBlock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block registry mapping id -> Block instance. Blocks auto-register via their constructor.
 */
public final class BlockRegistry {
    private static int incrementingRegisterID = 2; // start at 2, 0=air, 1=debug

    private static final Map<Integer, Block> REG = new ConcurrentHashMap<>();

    private BlockRegistry() {}

    public static void register(Block block) {
        if (block == null) throw new IllegalArgumentException("block");
        block.id = incrementingRegisterID;
        System.out.println("Registering block id " + block.getId() + ": " + block.getName());
        Block prev = REG.putIfAbsent(block.getId(), block);
        if (prev != null) {
            throw new IllegalStateException("Block id already registered: " + block.getId() + " (" + prev.getName() + ")");
        }

        incrementingRegisterID++;
    }

    public static Block get(int id) { return REG.get(id); }
    public static Block getOrNull(int id) { return REG.get(id); }
    public static boolean contains(int id) { return REG.containsKey(id); }
    public static void clear() { REG.clear(); }

    public static void init() {
        new StoneBlock();
        new DirtBlock();
        new GrassBlock();
    }
}
