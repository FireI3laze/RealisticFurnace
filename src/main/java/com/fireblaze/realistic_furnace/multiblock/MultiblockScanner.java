package com.fireblaze.realistic_furnace.multiblock;

import com.fireblaze.realistic_furnace.blocks.FurnaceControllerBlock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class MultiblockScanner {

    private static boolean isValidBrickBlock(Block block) {
        Set<Block> blacklistedBlocks = MultiblockUtils.getBlacklistedBlocks();
        Set<Block> whitelistedBlocks = MultiblockUtils.getWhitelistedBlocks();
        Set<TagKey<Block>> validTags = MultiblockUtils.getValidTags();

        if (!blacklistedBlocks.isEmpty() && blacklistedBlocks.stream().anyMatch(Objects::nonNull)) {
            return !blacklistedBlocks.contains(block);
        }

        // Sonst Whitelist + Tags prüfen
        if (whitelistedBlocks.contains(block)) return true;

        for (TagKey<Block> tag : validTags) {
            if (block.defaultBlockState().is(tag)) return true;
        }

        return false;
    }



    private static final Set<Block> VALID_BRICK_BLOCKS = Set.of(
            Blocks.BRICKS, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, Blocks.BRICK_WALL, Blocks.CAMPFIRE
    );

    public static List<OffsetBlock> LAST_SCAN = new ArrayList<>();

    public static List<OffsetBlock> scanFromController(CommandSourceStack source, Level level, BlockPos controllerPos) {
        LAST_SCAN.clear();
        Set<BlockPos> visited = new HashSet<>();
        List<OffsetBlock> foundBlocks = new ArrayList<>();

        BlockState controllerState = level.getBlockState(controllerPos);
        Direction controllerFacing = controllerState.hasProperty(HorizontalDirectionalBlock.FACING)
                ? controllerState.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;

        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(controllerPos);

        final int SCAN_LIMIT = MultiblockUtils.getMaxScanBlocks(); // Maximal 500 Blöcke scannen

        while (!queue.isEmpty()) {
            if (foundBlocks.size() >= SCAN_LIMIT) {
                source.sendFailure(Component.literal("Scan limit reached. Did you configure the block white/black list properly?"));
                break;
            }

            BlockPos current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            BlockState state = level.getBlockState(current);
            Block block = state.getBlock();

            if (isValidBrickBlock(block) || block instanceof FurnaceControllerBlock) {
                BlockPos relative = current.subtract(controllerPos);
                BlockPos rotated = rotateOffsetToSouth(relative, controllerFacing);

                BlockState savedState = state;

                if (block instanceof StairBlock) {
                    Direction stairFacing = state.getValue(StairBlock.FACING);
                    Direction relativeFacing = rotateDirectionRelativeToController(stairFacing, controllerFacing);

                    savedState = state
                            .setValue(StairBlock.FACING, relativeFacing)
                            .setValue(StairBlock.HALF, state.getValue(StairBlock.HALF))
                            .setValue(StairBlock.SHAPE, state.getValue(StairBlock.SHAPE));
                }

                foundBlocks.add(new OffsetBlock(rotated.getX(), rotated.getY(), rotated.getZ(),
                        List.of(block), savedState));

                // Nachbarn hinzufügen
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor)) {
                        BlockState neighborState = level.getBlockState(neighbor);
                        Block neighborBlock = neighborState.getBlock();
                        if (isValidBrickBlock(neighborBlock)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        foundBlocks.sort(Comparator.comparingInt(OffsetBlock::y)
                .thenComparingInt(OffsetBlock::x)
                .thenComparingInt(OffsetBlock::z));

        LAST_SCAN.addAll(foundBlocks);
        return foundBlocks;
    }

    private static Direction rotateDirectionRelativeToController(Direction original, Direction controllerFacing) {
        int originalIndex = original.get2DDataValue();
        int controllerIndex = controllerFacing.get2DDataValue();

        int relativeIndex = (originalIndex - controllerIndex + 4) % 4;
        return Direction.from2DDataValue(relativeIndex);
    }

    private static BlockPos rotateOffsetToSouth(BlockPos offset, Direction facing) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();

        // Wir drehen die Koordinaten so, dass "controllerFacing" auf SOUTH zeigt
        return switch (facing) {
            case SOUTH -> new BlockPos(-x, y, -z);     // 180° (vorher falsch)
            case NORTH -> new BlockPos(x, y, z);       // keine Drehung (Standard)
            case EAST  -> new BlockPos(z, y, -x);      // 90° nach rechts
            case WEST  -> new BlockPos(-z, y, x);      // 90° nach links
            default    -> new BlockPos(x, y, z);
        };
    }
}
