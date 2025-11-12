package com.fireblaze.realistic_furnace.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class RealisticFurnaceConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // === Multiblock Settings ===
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLOCKS_WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLOCKS_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALID_TAGS;
    public static final ForgeConfigSpec.IntValue MAX_SCAN_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<String> FALLBACK_MULTIBLOCK_FILE;


    static {
        BUILDER.push("Multiblock");
        BUILDER.comment("Here you can whitelist or blacklist blocks to ensure the scan of your custom structure works properly. You can only use either the tags + whitelist or the blacklist. Putting anything in the blacklist will disable the whitelist and tags");
            BUILDER.push("Whitelist");
            VALID_TAGS = BUILDER
                    .comment("List of tags that upon scan are considered part of the multiblock if connected with the controller or each other")
                    .defineList("WhitelistedTags", List.of("minecraft:trapdoors"), obj -> obj instanceof String);

            STRUCTURE_BLOCKS_WHITELIST = BUILDER
                    .comment("List of blocks that upon scan are considered part of the multiblock if connected with the controller or each other")
                    .defineList("whitelistedBlocks", List.of(
                            "minecraft:bricks",
                            "minecraft:brick_stairs",
                            "minecraft:brick_slab",
                            "minecraft:brick_wall",
                            "minecraft:campfire"
                    ), obj -> obj instanceof String);
            BUILDER.pop();


            BUILDER.push("Blacklist");
            STRUCTURE_BLOCKS_BLACKLIST = BUILDER
                    .comment("List of blocks that upon scan are not considered part of the multiblock\n" +
                            "Be careful with this setting. If not operated with caution, unexpected blocks may be scanned\n" +
                            "Note: If using the blacklist, you probably don't want to consider 'minecraft:air' blocks")
                    .defineList("blacklistedBlocks", List.of(
                            ""
                    ), obj -> obj instanceof String);
            BUILDER.pop();


            BUILDER.push("Settings");
            MAX_SCAN_BLOCKS = BUILDER
                    .comment("Max amount of blocks that are considered when scanning the multiblock. This is to prevent an almost endless scan when the filter above are not set up properly")
                    .defineInRange("maxScanBlocks", 250, 1, Integer.MAX_VALUE);

            FALLBACK_MULTIBLOCK_FILE = BUILDER
                    .comment("Default/Fallback multiblock file. Only reconfigure this if you are sure the multiblock you are going to put as fallback is valid\nThis is required if you want others to use by default a custom multiblock when downloading your modpack")
                    .define("TheFallbackFile", "original_furnace.json");
            BUILDER.pop();
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
