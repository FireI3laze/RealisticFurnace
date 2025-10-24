package com.fireblaze.realistic_furnace.commands;

import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRegistry;
import com.fireblaze.realistic_furnace.multiblock.MultiblockScanner;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import com.fireblaze.realistic_furnace.multiblock.SerializableOffsetBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SaveMultiblockCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realistic_furnace")
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> saveMultiblock(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));
    }

    private static int saveMultiblock(CommandSourceStack source, String name) {
        // 1️⃣ Kein Scan vorhanden?
        if (MultiblockScanner.LAST_SCAN.isEmpty()) {
            source.sendFailure(Component.literal("⚠ No scan found!"));
            return 0;
        }

        // 2️⃣ Kein Ventil gesetzt?
        if (!FurnaceMultiblockRegistry.hasValve(source.getLevel())) {
            source.sendFailure(Component.literal("⚠ No valve (trapdoor) detected!"));
            return 0;
        }

        Path folder = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/multiblocks");
        try {
            Files.createDirectories(folder);

            Level world = source.getLevel();

            // --- prüfe Ventil ---
            if (!FurnaceMultiblockRegistry.hasValve(world) || FurnaceMultiblockRegistry.getValve(world) == null) {
                source.sendFailure(Component.literal("Please set a valve (trapdoor) before saving"));
                return 0;
            }

            // --- Datei erstellen oder mit (1) benennen falls vorhanden ---
            Path file = folder.resolve(name + ".json");
            int counter = 1;
            while (Files.exists(file)) {
                file = folder.resolve(name + "(" + counter + ").json");
                counter++;
            }

            // --- Struktur vorbereiten ---
            List<SerializableOffsetBlock> serializable = MultiblockScanner.LAST_SCAN.stream()
                    .map(SerializableOffsetBlock::new)
                    .toList();

            JsonObject root = new JsonObject();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            root.add("blocks", gson.toJsonTree(serializable));

            JsonObject valveJson = new JsonObject();
            valveJson.addProperty("x", FurnaceMultiblockRegistry.getValve(world).x());
            valveJson.addProperty("y", FurnaceMultiblockRegistry.getValve(world).y());
            valveJson.addProperty("z", FurnaceMultiblockRegistry.getValve(world).z());
            root.add("valve", valveJson);

            Files.writeString(file, gson.toJson(root));

            Path finalFile = file;
            source.sendSuccess(() -> Component.literal("Multiblock saved: " + finalFile), false);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Couldn't save file: " + e.getMessage()));
            e.printStackTrace();
        }


        return 1;
    }

}
