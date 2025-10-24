package com.fireblaze.realistic_furnace.fuel;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class FurnaceFuelRegistry {

    public record FuelData(int burnTime, float heatStrength) {}

    private static final Map<Item, FuelData> ITEM_FUEL = new HashMap<>();

    // Registrierung eines einzelnen Items
    public static void register(Item item, int burnTime, float heatStrength) {
        ITEM_FUEL.put(item, new FuelData(burnTime, heatStrength));
    }

    // Registrierung eines Tags (alle Items in diesem Tag)
    public static void register(TagKey<Item> tag, int burnTime, float heatStrength) {
        int count = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.builtInRegistryHolder().is(tag)) {
                ITEM_FUEL.put(item, new FuelData(burnTime, heatStrength));
                count++;
            }
        }
    }

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return ITEM_FUEL.containsKey(stack.getItem());
    }

    public static int getBurnTime(ItemStack stack) {
        FuelData data = ITEM_FUEL.get(stack.getItem());
        return data != null ? data.burnTime() : 0;
    }

    public static float getHeatStrength(ItemStack stack) {
        FuelData data = ITEM_FUEL.get(stack.getItem());
        return data != null ? data.heatStrength() : 0f;
    }

    // Beispielregistrierung
    public static void init() {
        register(Items.COAL, 1600, 0.9f);
        register(Items.CHARCOAL, 1600, 0.275f);
        register(Items.STICK, 150, 0.05f);

        // Tags
        register(net.minecraft.tags.ItemTags.PLANKS, 300, 0.2f);
        // register(net.minecraft.tags.ItemTags.WOODEN_STAIRS, 450, 0.2f);
        // register(net.minecraft.tags.ItemTags.WOODEN_SLABS, 150, 0.2f);
        // register(net.minecraft.tags.ItemTags.WOODEN_FENCES, 500, 0.2f);
        // register(net.minecraft.tags.ItemTags.FENCE_GATES, 1200, 0.2f);
        // register(net.minecraft.tags.ItemTags.WOODEN_TRAPDOORS, 900, 0.2f);
        // register(net.minecraft.tags.ItemTags.WOODEN_BUTTONS, 300, 0.2f);
        // register(net.minecraft.tags.ItemTags.WOODEN_PRESSURE_PLATES, 600, 0.2f);
    }
}
