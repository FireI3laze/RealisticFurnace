package com.fireblaze.realistic_furnace.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.fireblaze.realistic_furnace.blocks.FurnaceControllerBlock;

import java.util.ArrayList;
import java.util.List;

public class FurnaceMultiblockRenderer {

    /**
     * Gibt alle fehlenden Blöcke als Liste von Positionen zurück.
     */
    public static List<BlockPos> getMissingBlocks(Level level, BlockPos origin) {
        List<BlockPos> missing = new ArrayList<>();
        BlockState controllerState = level.getBlockState(origin);
        Direction facing = Direction.NORTH;

        if (controllerState.getBlock() instanceof FurnaceControllerBlock) {
            facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        }

        for (OffsetBlock offset : FurnaceMultiblock.STRUCTURE) {
            BlockPos rotatedPos = rotateOffset(offset, origin, facing);
            Block block = level.getBlockState(rotatedPos).getBlock();
            if (!offset.matcher().test(block)) {
                missing.add(rotatedPos);
            }
        }

        return missing;
    }

    private static BlockPos rotateOffset(OffsetBlock offset, BlockPos origin, Direction facing) {
        int x = offset.x();
        int y = offset.y();
        int z = offset.z();

        return switch (facing) {
            case NORTH -> origin.offset(x, y, z);
            case SOUTH -> origin.offset(-x, y, -z);
            case WEST  -> origin.offset(z, y, -x);
            case EAST  -> origin.offset(-z, y, x);
            default -> origin.offset(x, y, z);
        };
    }
}
