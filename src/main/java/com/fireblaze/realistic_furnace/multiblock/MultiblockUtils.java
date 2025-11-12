package com.fireblaze.realistic_furnace.multiblock;

import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import java.util.Set;
import java.util.stream.Collectors;

import static com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig.CUSTOM_FUELS;

public class MultiblockUtils {

    public static Set<Block> getWhitelistedBlocks() {
        return RealisticFurnaceConfig.STRUCTURE_BLOCKS_WHITELIST.get().stream()
                .map(s -> {
                    String[] parts = s.split(":", 2);
                    if (parts.length != 2) {
                        System.err.println("[ERROR] Ungültiger Block/Tag in der Config Whitelist: " + s);
                        return null; // oder ignoriere diesen Eintrag
                    }
                    return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));

                })
                .collect(Collectors.toSet());
    }

    public static Set<Block> getBlacklistedBlocks() {
        return RealisticFurnaceConfig.STRUCTURE_BLOCKS_BLACKLIST.get().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty()) // Leere Strings überspringen
                .map(s -> {
                    String[] parts = s.split(":", 2);
                    if (parts.length != 2) {
                        System.err.println("[ERROR] Ungültiger Block/Tag in der Config Blacklist: " + s);
                        return null;
                    }
                    return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }



    public static Set<TagKey<Block>> getValidTags() {
        return RealisticFurnaceConfig.VALID_TAGS.get().stream()
                .map(s -> {
                    String[] parts = s.split(":", 2);
                    if (parts.length != 2) {
                        System.err.println("[ERROR] Ungültiger Block/Tag in der Config: " + s);
                        return null; // oder ignoriere diesen Eintrag
                    }
                    return TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
                })
                .collect(Collectors.toSet());
    }

    public static void loadCustomFuels() {
        List<? extends String> list = CUSTOM_FUELS.get();

        if (list == null) return;

        for (String raw : list) {
            if (raw == null) continue;
            String entry = raw.trim();
            if (entry.isEmpty()) continue;

            try {
                String[] parts = entry.split(",");
                if (parts.length < 3) {
                    System.err.println("[Realistic Furnace] Invalid fuel entry (too few parts): " + entry);
                    continue;
                }

                String idPart = parts[0].trim();
                int burnTime = Integer.parseInt(parts[1].trim());
                float heatStrength = Float.parseFloat(parts[2].trim());
                @Nullable Integer maxHeat = null;
                if (parts.length >= 4) {
                    String maxPart = parts[3].trim();
                    if (!maxPart.isEmpty()) maxHeat = Integer.parseInt(maxPart);
                }

                if (idPart.startsWith("#")) {
                    // Tag-Registrierung (Tag-IDs mit führendem '#', z.B. "#minecraft:planks")
                    String tagId = idPart.substring(1);
                    String[] tagSplit = tagId.split(":", 2);
                    if (tagSplit.length != 2) {
                        System.err.println("[Realistic Furnace] Invalid tag id: " + tagId);
                        continue;
                    }
                    ResourceLocation tagRL = ResourceLocation.fromNamespaceAndPath(tagSplit[0], tagSplit[1]);
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, tagRL);

                    if (maxHeat != null) {
                        FurnaceFuelRegistry.register(tag, burnTime, heatStrength, maxHeat);
                    } else {
                        FurnaceFuelRegistry.register(tag, burnTime, heatStrength);
                    }
                    System.out.printf("[Realistic Furnace] Registered fuel tag %s -> burn=%d, heat=%f, max=%s%n",
                            tagId, burnTime, heatStrength, maxHeat == null ? "none" : maxHeat);
                } else {
                    String[] idSplit = idPart.split(":", 2);
                    if (idSplit.length != 2) {
                        System.err.println("[Realistic Furnace] Invalid resource id: " + idPart);
                        continue;
                    }
                    ResourceLocation itemRL = ResourceLocation.fromNamespaceAndPath(idSplit[0], idSplit[1]);
                    Item item = ForgeRegistries.ITEMS.getValue(itemRL);
                    if (item == null || item == Items.AIR) {
                        System.err.println("[Realistic Furnace] Could not find item for fuel entry: " + idPart);
                        continue;
                    }
                    if (maxHeat != null) {
                        FurnaceFuelRegistry.register(item, burnTime, heatStrength, maxHeat);
                    } else {
                        FurnaceFuelRegistry.register(item, burnTime, heatStrength);
                    }
                    System.out.printf("[Realistic Furnace] Registered fuel %s -> burn=%d, heat=%f, max=%s%n",
                            idPart, burnTime, heatStrength, maxHeat == null ? "none" : maxHeat);
                }

            } catch (Exception e) {
                System.err.println("[Realistic Furnace] Fehler beim Parsen von fuel-entry: " + entry);
                e.printStackTrace();
            }
        }
    }


    public static int getMaxScanBlocks() {
        return RealisticFurnaceConfig.MAX_SCAN_BLOCKS.get();
    }
    public static String getFallbackFile() {
        return RealisticFurnaceConfig.FALLBACK_MULTIBLOCK_FILE.get();
    }
}
