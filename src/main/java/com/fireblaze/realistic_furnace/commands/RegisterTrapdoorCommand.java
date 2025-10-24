package com.fireblaze.realistic_furnace.commands;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRegistry;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RegisterTrapdoorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realistic_furnace")
                .then(Commands.literal("registerTrapdoor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();

                                            int x = IntegerArgumentType.getInteger(context, "x");
                                            int y = IntegerArgumentType.getInteger(context, "y");
                                            int z = IntegerArgumentType.getInteger(context, "z");

                                            // Spieler hat Koordinaten angegeben
                                            OffsetBlock playerOffset = new OffsetBlock(
                                                    x, y, z,
                                                    block -> block instanceof net.minecraft.world.level.block.TrapDoorBlock,
                                                    null
                                            );

                                            // Prüfen, ob diese Koordinaten in pendingVentSelection enthalten sind
                                            List<OffsetBlock> allowed = FurnaceMultiblockRegistry.getPendingVentSelection();
                                            if (allowed == null || allowed.isEmpty()) {
                                                source.sendFailure(Component.literal("No trapdoor available. Scan a multiblock first."));
                                                return 0;
                                            }

                                            boolean valid = allowed.stream().anyMatch(off ->
                                                    off.x() == playerOffset.x() &&
                                                            off.y() == playerOffset.y() &&
                                                            off.z() == playerOffset.z()
                                            );

                                            if (!valid) {
                                                source.sendFailure(Component.literal("No trapdoor found. Select a displayed offset."));
                                                return 0;
                                            }
                                            FurnaceMultiblockRegistry.setValve(source.getLevel(), playerOffset);
                                            //FurnaceControllerBlockEntity.setValveOffset(playerOffset);
                                            source.sendSuccess(() -> Component.literal(
                                                    "Valve position registered: " +
                                                            x + ", " + y + ", " + z), true);
                                            return 1;
                                        }))))));
    }
}
