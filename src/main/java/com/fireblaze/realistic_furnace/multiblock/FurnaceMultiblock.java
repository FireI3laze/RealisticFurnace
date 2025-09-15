package com.fireblaze.realistic_furnace.multiblock;

import com.fireblaze.realistic_furnace.blocks.FurnaceControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class FurnaceMultiblock {

    // Offsets relativ zum Controller, bevor Rotation angewendet wird
    public static final List<OffsetBlock> STRUCTURE = List.of(
            // === Inner Space ===
            new OffsetBlock(0, 0, 1, List.of(net.minecraft.world.level.block.Blocks.CAMPFIRE)),
            new OffsetBlock(1, 0, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(-1, 0, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(0, 0, 2, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),

            new OffsetBlock(0, 1, 0, List.of(net.minecraft.world.level.block.Blocks.BRICK_SLAB)),
            new OffsetBlock(1, 1, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(-1, 1, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(0, 1, 2, List.of(net.minecraft.world.level.block.Blocks.BRICK_SLAB)),

            new OffsetBlock(0, 2, 0, List.of(net.minecraft.world.level.block.Blocks.BRICK_STAIRS)),
            new OffsetBlock(1, 2, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(-1, 2, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(0, 2, 2, List.of(net.minecraft.world.level.block.Blocks.BRICK_STAIRS)),

            new OffsetBlock(0, 3, 0, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(1, 3, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(-1, 3, 1, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),
            new OffsetBlock(0, 3, 2, List.of(net.minecraft.world.level.block.Blocks.BRICKS)),

            new OffsetBlock(0, 4, 0, List.of(net.minecraft.world.level.block.Blocks.BRICK_STAIRS)),
            new OffsetBlock(1, 4, 1, List.of(net.minecraft.world.level.block.Blocks.BRICK_STAIRS)),
            new OffsetBlock(-1, 4, 1, List.of(net.minecraft.world.level.block.Blocks.BRICK_STAIRS)),
            new OffsetBlock(0, 4, 2, List.of(net.minecraft.world.level.block.Blocks.BRICK_STAIRS)),

            new OffsetBlock(0, 5, 0, List.of(Blocks.BRICK_SLAB)),
            new OffsetBlock(1, 5, 1, List.of(Blocks.BRICK_SLAB)),
            new OffsetBlock(-1, 5, 1, List.of(Blocks.BRICK_SLAB)),
            new OffsetBlock(0, 5, 2, List.of(Blocks.BRICK_SLAB)),

            new OffsetBlock(0, 3, 1, List.of(net.minecraft.world.level.block.Blocks.OAK_TRAPDOOR)),

            // === Front ===
            new OffsetBlock(1, 0, 0, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(2, 0, 0, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(-1, 0, 0, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(-2, 0, 0, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(1, 1, 0, List.of(Blocks.BRICK_WALL)),
            new OffsetBlock(-1, 1, 0, List.of(Blocks.BRICK_WALL)),
            new OffsetBlock(1, 2, 0, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(-1, 2, 0, List.of(Blocks.BRICK_STAIRS)),

            // === Back ===
            new OffsetBlock(1, 0, 2, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(2, 0, 2, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(-1, 0, 2, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(-2, 0, 2, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(1, 1, 2, List.of(Blocks.BRICK_WALL)),
            new OffsetBlock(-1, 1, 2, List.of(Blocks.BRICK_WALL)),
            new OffsetBlock(1, 2, 2, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(-1, 2, 2, List.of(Blocks.BRICK_STAIRS)),

            // === Left ===
            new OffsetBlock(2, 0, 1, List.of(Blocks.BRICKS)),
            new OffsetBlock(2, 1, 1, List.of(Blocks.BRICK_WALL)),
            new OffsetBlock(2, 2, 1, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(2, 3, 1, List.of(Blocks.BRICK_SLAB)),


            // === Right ===
            new OffsetBlock(-2, 0, 1, List.of(Blocks.BRICKS)),
            new OffsetBlock(-2, 1, 1, List.of(Blocks.BRICK_WALL)),
            new OffsetBlock(-2, 2, 1, List.of(Blocks.BRICK_STAIRS)),
            new OffsetBlock(-2, 3, 1, List.of(Blocks.BRICK_SLAB))

            // === Optional ===
            //new OffsetBlock(0, 3, -1, List.of(Blocks.LEVER))

    );

    public static boolean validateStructure(Level level, BlockPos origin) {
        BlockState controllerState = level.getBlockState(origin);
        Direction facing = Direction.NORTH; // Default fallback
        if (controllerState.getBlock() instanceof FurnaceControllerBlock) {
            facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        }

        for (OffsetBlock offset : STRUCTURE) {
            BlockPos rotatedPos = rotateOffset(offset, origin, facing);
            Block block = level.getBlockState(rotatedPos).getBlock();
            if (!offset.matcher().test(block)) return false;
        }
        return true;
    }

    private static BlockPos rotateOffset(OffsetBlock offset, BlockPos origin, Direction facing) {
        int x = offset.x();
        int y = offset.y();
        int z = offset.z();

        // Drehung um die Y-Achse basierend auf Facing
        return switch (facing) {
            case NORTH -> origin.offset(x, y, z);          // Original
            case SOUTH -> origin.offset(-x, y, -z);       // 180°
            case WEST  -> origin.offset(z, y, -x);        // 90° gegen Uhrzeigersinn
            case EAST  -> origin.offset(-z, y, x);        // 90° im Uhrzeigersinn
            default -> origin.offset(x, y, z);
        };
    }
}
