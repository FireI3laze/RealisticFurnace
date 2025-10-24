package com.fireblaze.realistic_furnace.commands;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRegistry;
import com.fireblaze.realistic_furnace.multiblock.MultiblockScanner;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import com.fireblaze.realistic_furnace.multiblock.SerializableOffsetBlock;
import com.fireblaze.realistic_furnace.networking.NetworkHandler;
import com.fireblaze.realistic_furnace.networking.packet.SyncMultiblockDataPacket;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SelectMultiblockCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realistic_furnace")
                .then(Commands.literal("select")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    Path folder = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/multiblocks");
                                    try (Stream<Path> files = Files.list(folder)) {
                                        files.filter(f -> f.toString().endsWith(".json"))
                                                .forEach(f -> builder.suggest(f.getFileName().toString()));
                                    } catch (IOException ignored) {}
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> selectMultiblock(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));
    }

    public static int selectMultiblock(CommandSourceStack source, String name) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/multiblocks/" + name);
        if (!Files.exists(file)) {
            source.sendFailure(Component.literal("File not found: " + name));
            return 0;
        }

        try {
            String json = Files.readString(file);
            JsonElement rootElement = JsonParser.parseString(json);

            List<OffsetBlock> blocks = new ArrayList<>();
            Gson gson = new Gson();

            if (rootElement.isJsonObject()) {
                JsonObject obj = rootElement.getAsJsonObject();
                if (obj.has("blocks")) {
                    Type listType = new TypeToken<List<SerializableOffsetBlock>>(){}.getType();
                    List<SerializableOffsetBlock> serialized = gson.fromJson(obj.get("blocks"), listType);
                    blocks.addAll(serialized.stream().map(SerializableOffsetBlock::toOffsetBlock).toList());
                }

                Level world = source.getLevel(); // CommandSourceStack hat die Welt
                FurnaceMultiblockRegistry.selectMultiblock(world, name, blocks);

// Valve setzen
                if (obj.has("valve")) {
                    JsonObject valveJson = obj.getAsJsonObject("valve");
                    OffsetBlock valve = new OffsetBlock(
                            valveJson.get("x").getAsInt(),
                            valveJson.get("y").getAsInt(),
                            valveJson.get("z").getAsInt(),
                            (Predicate<Block>) null,
                            null
                    );
                    FurnaceMultiblockRegistry.setValve(world, valve);
                    FurnaceControllerBlockEntity.setValveOffset(valve);
                }

            } else if (rootElement.isJsonArray()) {
                Type listType = new TypeToken<List<SerializableOffsetBlock>>(){}.getType();
                List<SerializableOffsetBlock> serialized = gson.fromJson(rootElement, listType);
                blocks.addAll(serialized.stream().map(SerializableOffsetBlock::toOffsetBlock).toList());
            }

            // WICHTIG: Multiblock aktiv setzen

            source.sendSuccess(() -> Component.literal("✅ Multiblock '" + name + "' loaded! (" + blocks.size() + " blocks)"), false);

            Level world = source.getLevel(); // CommandSourceStack hat die Welt
            OffsetBlock valve = FurnaceMultiblockRegistry.getValve(world);

            NetworkHandler.CHANNEL.send(
                    PacketDistributor.ALL.noArg(),
                    new SyncMultiblockDataPacket(
                            name,
                            blocks.stream().map(SerializableOffsetBlock::fromOffsetBlock).toList(),
                            valve != null ? SerializableOffsetBlock.fromOffsetBlock(valve) : null
                    )
            );



        } catch (Exception e) {
            source.sendFailure(Component.literal("Issue while loading: " + e.getMessage()));
            e.printStackTrace();
        }




        return 1;
    }

}
