package com.fireblaze.realistic_furnace.multiblock;

import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

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
                String[] parts = splitFuelEntry(entry);
                String idPart = parts[0];
                String nbtPart = "";
                String[] restParts;

                if (parts[1].startsWith("{")) {
                    nbtPart = parts[1];
                    restParts = Arrays.copyOfRange(parts, 2, parts.length);
                } else {
                    restParts = Arrays.copyOfRange(parts, 1, parts.length);
                }


// burnTime, heatStrength, maxHeat aus restParts lesen
                int burnTime = restParts.length > 0 ? Integer.parseInt(restParts[0].trim()) : 0;
                float heatStrength = restParts.length > 1 ? Float.parseFloat(restParts[1].trim()) : 0f;
                @Nullable Integer maxHeat = restParts.length > 2 ? Integer.parseInt(restParts[2].trim()) : null;

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
                    ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(idSplit[0], idSplit[1]);
                    Item item = ForgeRegistries.ITEMS.getValue(rl);
                    if (item == null || item == Items.AIR) {
                        continue;
                    }

                    // ItemStack erstellen
                    ItemStack stack = new ItemStack(item);

// NBT setzen, falls vorhanden
                    if (!nbtPart.isEmpty()) {
                        try {
                            CompoundTag nbt = net.minecraft.nbt.TagParser.parseTag(nbtPart);
                            stack.setTag(nbt);
                        } catch (Exception e) {
                            System.err.println("[Realistic Furnace] Invalid NBT for fuel " + idPart + ": " + nbtPart);
                        }
                    }

// Registrierung
                    FurnaceFuelRegistry.register(stack, burnTime, heatStrength, maxHeat);







                    System.out.printf("[Realistic Furnace] Registered fuel %s -> burn=%d, heat=%f, max=%s%n",
                            idPart, burnTime, heatStrength, maxHeat == null ? "none" : maxHeat);
                }

            } catch (Exception e) {
                System.err.println("[Realistic Furnace] Fehler beim Parsen von fuel-entry: " + entry);
                e.printStackTrace();
            }
        }
    }

    private static String[] splitFuelEntry(String entry) {
        int braceLevel = 0;
        StringBuilder current = new StringBuilder();
        List<String> parts = new ArrayList<>();

        for (int i = 0; i < entry.length(); i++) {
            char c = entry.charAt(i);

            if (c == '{') braceLevel++;
            if (c == '}') braceLevel--;

            if (c == ',' && braceLevel == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) parts.add(current.toString().trim());

        return parts.toArray(new String[0]);
    }



    public static int getMaxScanBlocks() {
        return RealisticFurnaceConfig.MAX_SCAN_BLOCKS.get();
    }
    public static String getFallbackFile() {
        return RealisticFurnaceConfig.FALLBACK_MULTIBLOCK_FILE.get();
    }
}
