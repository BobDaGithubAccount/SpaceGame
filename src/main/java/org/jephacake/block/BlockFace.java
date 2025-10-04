package org.jephacake.block;

/**
 * Logical block faces. Keep order stable (ordinal used by SimpleBlock).
 *
 * Mapping convention (matches earlier mesher faces):
 *  - NORTH = -Z
 *  - SOUTH = +Z
 *  - BOTTOM = -Y
 *  - TOP    = +Y
 *  - WEST   = -X
 *  - EAST   = +X
 */
public enum BlockFace {
    NORTH, // -Z
    SOUTH, // +Z
    BOTTOM,// -Y
    TOP,   // +Y
    WEST,  // -X
    EAST   // +X
}