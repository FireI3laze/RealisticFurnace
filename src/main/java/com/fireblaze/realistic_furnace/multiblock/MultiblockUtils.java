package com.fireblaze.realistic_furnace.multiblock;

import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

import java.util.Set;
import java.util.stream.Collectors;

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


    public static int getMaxScanBlocks() {
        return RealisticFurnaceConfig.MAX_SCAN_BLOCKS.get();
    }
    public static String getFallbackFile() {
        return RealisticFurnaceConfig.FALLBACK_MULTIBLOCK_FILE.get();
    }
}
