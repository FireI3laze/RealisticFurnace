package com.fireblaze.realistic_furnace.commands;

import com.fireblaze.realistic_furnace.multiblock.MultiblockUtils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class DeleteMultiblockCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realistic_furnace")
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    // Vorschläge aller gespeicherten Multiblocks
                                    Path folder = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/multiblocks");
                                    try (Stream<Path> files = Files.list(folder)) {
                                        files.filter(f -> f.toString().endsWith(".json"))
                                                .forEach(f -> builder.suggest(f.getFileName().toString()));
                                    } catch (IOException ignored) {}
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> deleteMultiblock(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));
    }

    private static int deleteMultiblock(CommandSourceStack source, String name) {
        Path folder = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/multiblocks");
        Path file = folder.resolve(name);

        if (!Files.exists(file)) {
            source.sendFailure(Component.literal("File not found: " + name));
            return 0;
        }

        // Schutz für original_furnace
        if (name.equalsIgnoreCase(MultiblockUtils.getFallbackFile())) {
            source.sendFailure(Component.literal("The fallback multiblock cannot be deleted!"));
            return 0;
        }

        // Schutz, falls aktuell ausgewählt

        if (name.equals(FurnaceMultiblockRegistry.getSelectedMultiblockName(source.getLevel()))) {
            source.sendFailure(Component.literal("This multiblock is currently selected. Select another multiblock before deleting this one"));
            return 0;
        }

        try {
            Files.delete(file);
            source.sendSuccess(() -> Component.literal("multiblock '" + name + "' got successfully deleted!"), false);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Exception: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }

        return 1;
    }


}
