package com.fireblaze.realistic_furnace.multiblock;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SerializableOffsetBlock {
    @SerializedName("x")
    public int x;
    @SerializedName("y")
    public int y;
    @SerializedName("z")
    public int z;
    @SerializedName("block")
    public String blockId;
    @SerializedName("properties")
    public Map<String, String> blockStateProperties = new HashMap<>();

    // 🔹 Konstruktor aus OffsetBlock + BlockState
    public SerializableOffsetBlock(OffsetBlock offset) {
        this.x = offset.x();
        this.y = offset.y();
        this.z = offset.z();
        BlockState state = offset.getStateTemplate();

        if (state != null) {
            this.blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            // Alle BlockStateProperties übernehmen
            state.getValues().forEach((prop, val) -> blockStateProperties.put(prop.getName(), val.toString()));
        }
    }

    public OffsetBlock toOffsetBlock() {
        String[] parts = blockId.split(":", 2);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(parts[0], parts.length > 1 ? parts[1] : "");
        Block block = BuiltInRegistries.BLOCK.get(id);

        if (block == null) {
            System.err.println("⚠ Unbekannter Block: " + blockId + " – Fallback zu AIR");
            return new OffsetBlock(x, y, z, List.of(Blocks.AIR), Blocks.AIR.defaultBlockState());
        }

        BlockState state = block.defaultBlockState();

        for (Map.Entry<String, String> entry : blockStateProperties.entrySet()) {
            var property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property != null) {
                state = applyProperty(state, property, entry.getValue());
            }
        }

        // ✅ Immer mit valider Matcher-Liste und State zurückgeben
        return new OffsetBlock(x, y, z, List.of(block), state);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String valueName) {
        return property.getValue(valueName)
                .map(val -> state.setValue(property, val))
                .orElse(state);
    }

    // Leerer Konstruktor für Gson
    public SerializableOffsetBlock() {}

    // ✅ Hilfsmethode für Netzwerksync (einfacher Aufruf im Packet)
    public static SerializableOffsetBlock fromOffsetBlock(OffsetBlock offset) {
        return new SerializableOffsetBlock(offset);
    }
}
