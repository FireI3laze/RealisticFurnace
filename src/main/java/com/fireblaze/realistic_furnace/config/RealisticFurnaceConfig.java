package com.fireblaze.realistic_furnace.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class RealisticFurnaceConfig {

    // COMMON
    public static final ForgeConfigSpec.Builder COMMON_BUILDER  = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_SPEC;

    // CLIENT
    public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CLIENT_SPEC;

    // === Heat Settings ===
    public static final ForgeConfigSpec.IntValue MAX_HEAT;
    public static final ForgeConfigSpec.IntValue THREATENING_HEAT;

    // === Fuel Settings ===
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_FUELS;

    // === Multiblock Settings ===
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLOCKS_WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLOCKS_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALID_TAGS;
    public static final ForgeConfigSpec.IntValue MAX_SCAN_BLOCKS;
    public static final ForgeConfigSpec.ConfigValue<String> FALLBACK_MULTIBLOCK_FILE;

    // === Gamerules ===
    public static final ForgeConfigSpec.BooleanValue ALLOW_FURNACE_INTERACTION;

    // === Preferences ===
    public static final ForgeConfigSpec.EnumValue<TemperatureUnit> TEMPERATURE_UNIT;
    public static final ForgeConfigSpec.BooleanValue SHOW_FUEL_TOOLTIPS;

    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_BOTTOM_COLOR_GUI;
    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_TOP_COLOR_GUI;

    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_BOTTOM_COLOR_TOO_COLD_JEI;
    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_TOP_COLOR_TOO_COLD_JEI;

    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_SMELT_RANGE_COLOR_JEI;
    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_OVERHEAT_THRESHOLD_LINE_COLOR_JEI;

    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_BOTTOM_COLOR_TOO_HOT_JEI;
    public static final ForgeConfigSpec.ConfigValue<String> HEATBAR_TOP_COLOR_TOO_HOT_JEI;
    public static final ForgeConfigSpec.IntValue HEAT_INCREMENTS;
    public static final ForgeConfigSpec.BooleanValue SHOW_INCREMENTS_STRING;

    public enum TemperatureUnit {
        CELSIUS,
        FAHRENHEIT
    }


    static {
        CLIENT_BUILDER.push("Preferences");
        CLIENT_BUILDER.push("Unit");
        TEMPERATURE_UNIT = CLIENT_BUILDER
                .comment("Temperature unit for GUI display")
                .defineEnum("temperatureUnit", TemperatureUnit.CELSIUS);

        HEAT_INCREMENTS = CLIENT_BUILDER
                .comment("Changes the temperature increments next to the heat bar shown in the furnace UI (in celsius)")
                .defineInRange("increments", 500, 100, 100000);

        SHOW_INCREMENTS_STRING = CLIENT_BUILDER
                .comment("Displays the numeric values of the temperature increments")
                .define("incrementsString", true);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.push("Tooltips");
        SHOW_FUEL_TOOLTIPS = CLIENT_BUILDER
                .comment("Enable tooltip information for fuels")
                .define("showFuelTooltips", true);
        CLIENT_BUILDER.pop();


        CLIENT_BUILDER.push("GUI");
        HEATBAR_BOTTOM_COLOR_GUI = CLIENT_BUILDER
                .comment("Defines the bottom color of the gradient in the heat bar")
                .define("firstGradientColorGUI", "#AAFFD700");

        HEATBAR_TOP_COLOR_GUI = CLIENT_BUILDER
                .comment("Defines the top color of the gradient in the heat bar")
                .define("secondGradientColorGUI", "#AA990000");
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.push("JEI");
        HEATBAR_BOTTOM_COLOR_TOO_COLD_JEI = CLIENT_BUILDER
                .comment("Defines the bottom part of the gradient for the 'too cold' area in the JEI (Default light yellow)")
                .define("firstGradientColorColdJEI", "#AAFFFF33");

        HEATBAR_TOP_COLOR_TOO_COLD_JEI = CLIENT_BUILDER
                .comment("Defines the top part of the gradient for the 'too cold' area in the JEI (Default dark yellow)")
                .define("secondGradientColorColdJEI", "#AAAA8800");

        HEATBAR_SMELT_RANGE_COLOR_JEI = CLIENT_BUILDER
                .comment("Defines the color overlay of the smelt area in the JEI (Default orange)")
                .define("colorSmeltArea", "#66FF6A00");

        HEATBAR_OVERHEAT_THRESHOLD_LINE_COLOR_JEI = CLIENT_BUILDER
                .comment("Defines the color of the line that marks the overheat threshold in the JEI (Default red)")
                .define("colorOverheatThresholdLine", "#FFFF0000");

        HEATBAR_BOTTOM_COLOR_TOO_HOT_JEI = CLIENT_BUILDER
                .comment("Defines the bottom part of the gradient for the 'too hot' area in the JEI (Default light red)")
                .define("firstGradientColorHotJEI", "#AACC2A2A");

        HEATBAR_TOP_COLOR_TOO_HOT_JEI = CLIENT_BUILDER
                .comment("Defines the top part of the gradient for the 'too hot' area in the JEI (Default dark red)")
                .define("secondGradientColorHotJEI", "#AA990000");
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.pop();

        COMMON_BUILDER .push("Gamerules");
        ALLOW_FURNACE_INTERACTION = COMMON_BUILDER
                .comment("Allow interacting with vanilla furnace")
                .define("allowVanillaFurnaceInteraction", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER .push("Heat");
        MAX_HEAT = COMMON_BUILDER
                .comment("[EXPERIMENTAL] Maximum heat the furnace can reach (the fuel burn efficiency will scale accordingly. WARNING! Changes may require reconfiguration of the default and custom fuel settings)")
                .defineInRange("maxHeat", 1800, 0, Integer.MAX_VALUE);

        THREATENING_HEAT = COMMON_BUILDER
                .comment("Heat level at which the furnace has a chance to explode")
                .defineInRange("threateningHeat", 1650, 0, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Multiblock");
        COMMON_BUILDER.comment("Here you can whitelist or blacklist blocks to ensure the scan of your custom structure works properly. You can only use either the tags + whitelist or the blacklist. Putting anything in the blacklist will disable the whitelist and tags");
        COMMON_BUILDER.push("Whitelist");
            VALID_TAGS = COMMON_BUILDER
                    .comment("List of tags that upon scan are considered part of the multiblock if connected with the controller or each other")
                    .defineList("WhitelistedTags", List.of("minecraft:trapdoors"), obj -> obj instanceof String);

            STRUCTURE_BLOCKS_WHITELIST = COMMON_BUILDER
                    .comment("List of blocks that upon scan are considered part of the multiblock if connected with the controller or each other")
                    .defineList("whitelistedBlocks", List.of(
                            "minecraft:bricks",
                            "minecraft:brick_stairs",
                            "minecraft:brick_slab",
                            "minecraft:brick_wall",
                            "minecraft:campfire"
                    ), obj -> obj instanceof String);
            COMMON_BUILDER.pop();


            COMMON_BUILDER.push("Blacklist");
            STRUCTURE_BLOCKS_BLACKLIST = COMMON_BUILDER
                    .comment("List of blocks that upon scan are not considered part of the multiblock\n" +
                            "Be careful with this setting. If not operated with caution, unexpected blocks may be scanned\n" +
                            "Note: If using the blacklist, you probably don't want to consider 'minecraft:air' blocks")
                    .defineList("blacklistedBlocks", List.of(
                            ""
                    ), obj -> obj instanceof String);
            COMMON_BUILDER.pop();


            COMMON_BUILDER.push("Settings");
            MAX_SCAN_BLOCKS = COMMON_BUILDER
                    .comment("Max amount of blocks that are considered when scanning the multiblock. This is to prevent an almost endless scan when the filter above are not set up properly")
                    .defineInRange("maxScanBlocks", 250, 1, Integer.MAX_VALUE);

            FALLBACK_MULTIBLOCK_FILE = COMMON_BUILDER
                    .comment("Default/Fallback multiblock file. Only reconfigure this if you are sure the multiblock you are going to put as fallback is valid\nThis is required if you want others to use by default a custom multiblock when downloading your modpack")
                    .define("TheFallbackFile", "original_furnace.json");
            COMMON_BUILDER.pop();
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Fuel");
        CUSTOM_FUELS = COMMON_BUILDER
                .comment(
                        "I highly recommend leaving these untouched and just using the commands in game, it is much easier. Only modify the list below if you want to get rid of a default fuel.\n" +
                                "If you want to add tags, I recommend adding them in the fuels.json\n" +
                                "Format:\n" +
                                "\"<itemid_or_tag>[,{nbt}],<burnTime>,<heatStrength>[,<maxHeat>]\"\n" +
                                "itemid is shown when pressing F3 + H\n" +
                                "nbt can be optionally added if a fuel is determined purely by nbt and not by the item id (common for modern bucket mods etc.). Use /kjs hand to see the nbt and copy paste everything that is inside the Item.of() bracket and add a '\\' in front of every \" | Running into issues? Join the Discord on the mod page so I can help you!\n" +
                                "burnTime determines the time (in ticks) how long an item burns\n" +
                                "heatStrength is a value that determines the heat raise speed and max heat\n" +
                                "maxHeat can be optionally set to hard force a max heat. Put it with caution, if not somewhat in sync with the dynamically created max heat based on heatStrength, it could lead to unrealistic heat jumps\n" +
                                "Example:\n" +
                                "\"minecraft:coal,{\\\"FuelType\\\":1},1600,0.9\", Note: \"FuelType\" is just an example nbt. It doesn't exist. Use KubeJS to read out existing nbts.\n" +
                                "\"minecraft:charcoal,1600,0.275\",\n" +
                                "\"#minecraft:planks,300,0.2,800\"  (Tags begin with '#')\n" +
                                "\"minecraft:stick,150,0.1,800\""
                )
                .defineList("fuels", List.of(
                        "minecraft:coal,1600,0.9",
                        "minecraft:charcoal,1600,0.275,1350",
                        "#minecraft:planks,300,0.1,800",
                        "minecraft:stick,150,0.1,800"
                ), obj -> obj instanceof String);
        COMMON_BUILDER.pop();


        CLIENT_SPEC = CLIENT_BUILDER.build();
        COMMON_SPEC = COMMON_BUILDER.build();
    }


    public static int parseColor(String value) {
        if (value == null || value.isEmpty()) return 0xFFFFFFFF;

        value = value.trim();

        if (value.startsWith("#")) {
            value = value.substring(1);
        }

        long color = Long.parseLong(value, 16);

        // 6 digits = RRGGBB → Alpha ergänzen
        if (value.length() == 6) {
            color |= 0xFF000000L;
        }

        // 8 digits = AARRGGBB → direkt nutzen
        if (value.length() == 8) {
            return (int) color;
        }

        return (int) color;
    }
}
