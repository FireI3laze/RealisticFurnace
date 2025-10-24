package com.fireblaze.realistic_furnace.multiblock;

import com.fireblaze.realistic_furnace.blocks.FurnaceControllerBlock;
import com.fireblaze.realistic_furnace.client.renderer.FurnaceGhostRenderer;
import com.fireblaze.realistic_furnace.commands.SelectMultiblockCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.List;

public class FurnaceMultiblock {

    // Offsets relativ zum Controller, bevor Rotation angewendet wird


    public static List<OffsetBlock> getCurrentStructure(Level world) {
        String selectedName = FurnaceMultiblockRegistry.getSelectedMultiblockName(world);

        List<OffsetBlock> structure = FurnaceMultiblockRegistry.getSelectedMultiblock(world, selectedName);
        if (structure.isEmpty()) {
            // Fallback laden
            List<OffsetBlock> fallback = FurnaceMultiblockRegistry.loadByName(world, MultiblockUtils.getFallbackFile());
            FurnaceMultiblockRegistry.selectMultiblock(world, MultiblockUtils.getFallbackFile(), fallback);
            return fallback;
        }

        return structure;
    }

    public static boolean validateStructure(Level world, BlockPos origin) {
        BlockState controllerState = world.getBlockState(origin);
        Direction facing = Direction.NORTH; // Default fallback
        if (controllerState.getBlock() instanceof FurnaceControllerBlock) {
            facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        }

        List<OffsetBlock> structure = getCurrentStructure(world);

        for (OffsetBlock offset : structure) {
            BlockPos rotatedPos = FurnaceMultiblockRenderer.rotateOffset(offset, origin, facing);
            Block block = world.getBlockState(rotatedPos).getBlock();
            if (!offset.matcher().test(block)) return false;
        }

        return true;
    }

}
