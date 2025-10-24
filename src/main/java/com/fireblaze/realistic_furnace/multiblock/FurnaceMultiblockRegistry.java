package com.fireblaze.realistic_furnace.multiblock;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FurnaceMultiblockRegistry {

    public static List<OffsetBlock> loadByName(Level world, String name) {
        if (name == null || name.isEmpty()) {
            name = "original_furnace.json";
        }

        Path file = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/multiblocks/" + name);
        if (!Files.exists(file)) return new ArrayList<>();

        try {
            String json = Files.readString(file);
            Gson gson = new Gson();

            JsonElement rootElement = JsonParser.parseString(json);

            List<OffsetBlock> blocks = new ArrayList<>();

            if (rootElement.isJsonArray()) {
                // 🧩 Alte Struktur: direkt eine Liste von Blöcken
                Type listType = new TypeToken<List<SerializableOffsetBlock>>(){}.getType();
                List<SerializableOffsetBlock> serialized = gson.fromJson(rootElement, listType);
                blocks.addAll(serialized.stream().map(SerializableOffsetBlock::toOffsetBlock).toList());

            } else if (rootElement.isJsonObject()) {
                // 🧩 Neue Struktur: { "blocks": [...], "valve": {...} }
                JsonObject obj = rootElement.getAsJsonObject();

                if (obj.has("blocks")) {
                    Type listType = new TypeToken<List<SerializableOffsetBlock>>(){}.getType();
                    List<SerializableOffsetBlock> serialized = gson.fromJson(obj.get("blocks"), listType);
                    blocks.addAll(serialized.stream().map(SerializableOffsetBlock::toOffsetBlock).toList());
                }

                if (obj.has("valve")) {
                    JsonObject valveJson = obj.getAsJsonObject("valve");
                    int x = valveJson.get("x").getAsInt();
                    int y = valveJson.get("y").getAsInt();
                    int z = valveJson.get("z").getAsInt();

                    // Ventil speichern
                    OffsetBlock valve = new OffsetBlock(x, y, z, (Predicate<Block>) null, null);
                    FurnaceControllerBlockEntity.setValveOffset(valve);
                    FurnaceMultiblockRegistry.setValve(world, valve);
                }
            }

            return blocks;

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    private static final Map<Level, Map<String, List<OffsetBlock>>> WORLD_MULTIBLOCKS = new HashMap<>();
    private static final Map<Level, OffsetBlock> WORLD_VALVES = new HashMap<>();
    private static final Map<Level, Boolean> WORLD_HAS_VALVE = new HashMap<>();
    private static final Map<Level, String> WORLD_SELECTED_NAMES = new HashMap<>();

    public static void selectMultiblock(Level world, String name, List<OffsetBlock> blocks) {
        // Schutz: falls Client eine leere Liste bekommt, NICHT überschreiben.
        if (world.isClientSide && (blocks == null || blocks.isEmpty())) {
            return;
        }

        WORLD_MULTIBLOCKS.computeIfAbsent(world, w -> new HashMap<>()).put(name, blocks);
        WORLD_SELECTED_NAMES.put(world, name);

        if (world instanceof ServerLevel serverLevel) {
            FurnaceMultiblockSavedData data = FurnaceMultiblockSavedData.get(serverLevel);
            data.setSelected(serverLevel, name);
        }
    }




    public static List<OffsetBlock> getSelectedMultiblock(Level world, String name) {
        return WORLD_MULTIBLOCKS.getOrDefault(world, Map.of()).getOrDefault(name, List.of());
    }

    public static String getSelectedMultiblockName(Level world) {
        return WORLD_SELECTED_NAMES.getOrDefault(world, "");
    }


    public static void setValve(Level world, OffsetBlock valve) {
        WORLD_VALVES.put(world, valve);
        WORLD_HAS_VALVE.put(world, true);
    }

    public static OffsetBlock getValve(Level world) {
        return WORLD_VALVES.get(world);
    }

    public static boolean hasValve(Level world) {
        return WORLD_HAS_VALVE.getOrDefault(world, false);
    }

    public static void clearValve(Level world) {
        WORLD_VALVES.remove(world);
        WORLD_HAS_VALVE.put(world, false);
    }

    public static void setHasValve(Level world, boolean value) {
        WORLD_HAS_VALVE.put(world, value);
    }


    private static List<OffsetBlock> pendingVentSelection = new ArrayList<>();

    public static void setPendingVentSelection(List<OffsetBlock> trapdoors) {
        if (trapdoors == null || trapdoors.isEmpty()) {
            return;
        }

        pendingVentSelection = new ArrayList<>(trapdoors);
    }

    public static List<OffsetBlock> getPendingVentSelection() {
        return pendingVentSelection;
    }

    public static void clearPendingVentSelection() {
        pendingVentSelection.clear();
    }
}
