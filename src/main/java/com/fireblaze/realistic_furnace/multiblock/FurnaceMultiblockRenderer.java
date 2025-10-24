package com.fireblaze.realistic_furnace.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.fireblaze.realistic_furnace.blocks.FurnaceControllerBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.List;

public class FurnaceMultiblockRenderer {

    public static List<OffsetBlock> getMissingBlocks(Level world, BlockPos origin) {
        List<OffsetBlock> missing = new ArrayList<>();
        BlockState controllerState = world.getBlockState(origin);
        Direction facing = controllerState.hasProperty(HorizontalDirectionalBlock.FACING)
                ? controllerState.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;

        // Aktuelle Multiblock-Struktur für die Welt holen
        List<OffsetBlock> structure = FurnaceMultiblockRegistry.getSelectedMultiblock(
                world,
                FurnaceMultiblockRegistry.getSelectedMultiblockName(world)
        );

        for (OffsetBlock offset : structure) {
            BlockPos rotatedPos = rotateOffset(offset, origin, facing);
            BlockState currentState = world.getBlockState(rotatedPos);
            BlockState expectedState = offset.getStateTemplate();

            if (expectedState.getBlock() instanceof StairBlock) {
                expectedState = rotateStair(expectedState, facing);
            }

            boolean isTrapdoor = expectedState.getBlock() instanceof TrapDoorBlock;
            boolean isStair = expectedState.getBlock() instanceof StairBlock;

            boolean blockMatches =
                    isTrapdoor ? currentState.getBlock() instanceof TrapDoorBlock
                            : offset.matcher().test(currentState.getBlock());

            boolean mismatch = !blockMatches
                    || (isStair && !currentState.equals(expectedState));

            if (mismatch) {
                missing.add(offset);
            }
        }
        return missing;
    }



    public static BlockPos rotateOffset(OffsetBlock offset, BlockPos origin, Direction facing) {
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

    public static BlockState rotateStair(BlockState template, Direction controllerFacing) {
        Direction original = template.getValue(StairBlock.FACING); // Standardrichtung aus STRUCTURE
        Direction rotated = rotateDirection(original, controllerFacing);

        return template
                .setValue(StairBlock.FACING, rotated)
                .setValue(StairBlock.HALF, template.getValue(StairBlock.HALF))
                .setValue(StairBlock.SHAPE, template.getValue(StairBlock.SHAPE));
    }

    private static Direction rotateDirection(Direction original, Direction controllerFacing) {
        int originalIndex = original.get2DDataValue();     // 0=NORTH, 1=EAST, 2=SOUTH, 3=WEST
        int controllerIndex = controllerFacing.get2DDataValue();

        int newIndex = (originalIndex + controllerIndex) % 4;
        return Direction.from2DDataValue(newIndex);
    }
}
