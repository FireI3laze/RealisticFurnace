package com.fireblaze.realistic_furnace.multiblock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OffsetBlock {
    private final int x, y, z;
    private final Predicate<Block> matcher;
    private final BlockState stateTemplate;

    public OffsetBlock(int x, int y, int z, Predicate<Block> matcher, BlockState stateTemplate) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.matcher = matcher;
        this.stateTemplate = stateTemplate;
    }

    public OffsetBlock(int x, int y, int z, List<Block> allowedBlocks, BlockState stateTemplate) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.matcher = allowedBlocks::contains;
        this.stateTemplate = stateTemplate;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    public Predicate<Block> matcher() { return matcher; }
    public BlockState getStateTemplate() { return stateTemplate; }

    // 🔹 Für Debug-Ausgabe
    public String describeState() {
        if (stateTemplate == null) return "";
        // Wände/Zäune ignorieren
        if (stateTemplate.getBlock() instanceof net.minecraft.world.level.block.WallBlock
                || stateTemplate.getBlock() instanceof net.minecraft.world.level.block.FenceBlock) {
            return "";
        }

        Map<String, String> props = stateTemplate.getValues().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getName(),
                        e -> e.getValue().toString()
                ));
        return props.isEmpty() ? "" :
                props.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", ", " [", "]"));
    }

    public String describeDebug() {
        String name = getBlockName();
        String props = describeState();
        return String.format("Offset(%d,%d,%d) -> Block=%s%s", x, y, z, name, props.isEmpty() ? "" : props);
    }

    public String getBlockName() {
        return stateTemplate.getBlock().getName().getString();
    }
}
