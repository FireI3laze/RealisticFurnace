package com.fireblaze.realistic_furnace.multiblock;

import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Predicate;

public class OffsetBlock {
    private final int x, y, z;
    private final Predicate<Block> matcher;

    // Konstruktor nimmt jetzt eine Liste erlaubter Blöcke
    public OffsetBlock(int x, int y, int z, List<Block> allowedBlocks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.matcher = allowedBlocks::contains;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public Predicate<Block> matcher() { return matcher; }
}
