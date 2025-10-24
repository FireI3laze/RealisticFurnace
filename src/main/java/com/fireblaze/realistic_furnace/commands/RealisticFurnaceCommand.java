package com.fireblaze.realistic_furnace.commands;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRegistry;
import com.fireblaze.realistic_furnace.multiblock.MultiblockScanner;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.stream.Collectors;

public class RealisticFurnaceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("realistic_furnace")
                        .then(Commands.literal("scan")
                                .executes(context -> scanStructure(context.getSource()))
                        )
        );
    }

    private static int scanStructure(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can execute this command"));
            return Command.SINGLE_SUCCESS;
        }

        Level world = player.level();

        var hitResult = player.pick(10, 0, false);
        if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("You are not facing a block or the block is too far"));
            return Command.SINGLE_SUCCESS;
        }
        BlockPos controllerPos = ((net.minecraft.world.phys.BlockHitResult) hitResult).getBlockPos();

        List<OffsetBlock> found = MultiblockScanner.scanFromController(source, world, controllerPos);
        if (found.isEmpty()) {
            source.sendFailure(Component.literal("No connected blocks found!"));
            FurnaceMultiblockRegistry.setHasValve(world, false);
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("=== Realistic Furnace Multiblock Scan ==="), false);

        // Blockarten + Anzahl zählen
        found.stream()
                .collect(Collectors.groupingBy(OffsetBlock::getBlockName, Collectors.counting()))
                .forEach((name, count) -> source.sendSuccess(() -> Component.literal(name + ": " + count), false));

        // Trapdoors für Ventil prüfen
        List<OffsetBlock> trapdoors = found.stream()
                .filter(b -> b.getStateTemplate().getBlock().builtInRegistryHolder().is(BlockTags.TRAPDOORS))
                .toList();

        if (trapdoors.isEmpty()) {
            player.sendSystemMessage(Component.literal("⚠ No valve found! Add at least one trapdoor."));
            FurnaceMultiblockRegistry.clearValve(world);
            FurnaceMultiblockRegistry.setHasValve(world, false);
        } else if (trapdoors.size() > 1) {
            player.sendSystemMessage(Component.literal(
                    "⚠ Multiple trapdoors found! Choose a valve with: /realistic_furnace registerTrapdoor <x> <y> <z>"
            ));
            FurnaceMultiblockRegistry.setPendingVentSelection(trapdoors);
            FurnaceMultiblockRegistry.setHasValve(world, false); // Valve noch nicht gesetzt

            trapdoors.forEach(td -> source.sendSuccess(() -> Component.literal(
                    td.getBlockName() + " [offset: " + td.x() + ", " + td.y() + ", " + td.z() + "]"
            ), false));
        }
        else {
            FurnaceMultiblockRegistry.setValve(world, trapdoors.get(0));
            FurnaceMultiblockRegistry.setHasValve(world, true);
        }

        source.sendSuccess(() -> Component.literal("Total: " + found.size() + " blocks"), false);

        return Command.SINGLE_SUCCESS;
    }
}
