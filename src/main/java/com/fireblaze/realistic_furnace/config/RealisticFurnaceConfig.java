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
        BUILDER.comment("Realistic Furnace Settings");
        BUILDER.push("Multiblock");

        VALID_TAGS = BUILDER
                .comment("List of tags that upon scan are considered part of the multiblock if connected with the controller or each other.\nE.g. when an oak trapdoor is used for building, the blueprint allows trapdoors from every kind of wood and iron trapdoors\n" +
                        "Using the Blacklist below disables this setting")
                .defineList("validTags", List.of("minecraft:trapdoors"), obj -> obj instanceof String);

        STRUCTURE_BLOCKS_WHITELIST = BUILDER
                .comment("List of blocks that upon scan are considered part of the multiblock if connected with the controller or each other.\nUsing the blacklist below automatically disables the whitelist")
                .defineList("whitelistedBlocks", List.of(
                        "minecraft:bricks",
                        "minecraft:brick_stairs",
                        "minecraft:brick_slab",
                        "minecraft:brick_wall",
                        "minecraft:campfire"
                ), obj -> obj instanceof String);

        STRUCTURE_BLOCKS_BLACKLIST = BUILDER
                .comment("List of blocks that upon scan are not considered part of the multiblock\n" +
                        "Using the blacklist will automatically disable the whitelist above\n" +
                        "Be careful with this setting. If not operated with caution, unexpected blocks may be scanned.\n" +
                        "Note: If using the blacklist, you probably don't want to consider 'minecraft:air' blocks")
                .defineList("blacklistedBlocks", List.of(
                        ""
                ), obj -> obj instanceof String);

        MAX_SCAN_BLOCKS = BUILDER
                .comment("Max amount of blocks that are considered when scanning the multiblock")
                .defineInRange("maxScanBlocks", 150, 1, Integer.MAX_VALUE);

        FALLBACK_MULTIBLOCK_FILE = BUILDER
                .comment("Default/Fallback multiblock file. Only reconfigure this if you are sure the multiblock you are going to put as fallback is valid\nThis is required if you want others to use by default a custom multiblock when downloading your modpack")
                .define("The Fallback File", "original_furnace.json");

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
