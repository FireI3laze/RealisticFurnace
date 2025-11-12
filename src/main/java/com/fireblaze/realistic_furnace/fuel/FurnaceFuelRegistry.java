package com.fireblaze.realistic_furnace.fuel;

import com.fireblaze.realistic_furnace.multiblock.MultiblockUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FurnaceFuelRegistry {

    public record FuelData(int burnTime, float heatStrength, @Nullable Integer maxHeat) {
        public FuelData(int burnTime, float heatStrength) {
            this(burnTime, heatStrength, null);
        }
    }

    private static final Map<Item, FuelData> ITEM_FUEL = new HashMap<>();

    // Registrierung eines einzelnen Items
    public static void register(Item item, int burnTime, float heatStrength) {
        ITEM_FUEL.put(item, new FuelData(burnTime, heatStrength));
    }

    public static void register(Item item, int burnTime, float heatStrength, int maxHeat) {
        ITEM_FUEL.put(item, new FuelData(burnTime, heatStrength, maxHeat));
    }

    // Registrierung eines Tags
    public static void register(TagKey<Item> tag, int burnTime, float heatStrength) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.builtInRegistryHolder().is(tag)) {
                ITEM_FUEL.put(item, new FuelData(burnTime, heatStrength));
            }
        }
    }

    public static void register(TagKey<Item> tag, int burnTime, float heatStrength, int maxHeat) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.builtInRegistryHolder().is(tag)) {
                ITEM_FUEL.put(item, new FuelData(burnTime, heatStrength, maxHeat));
            }
        }
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && ITEM_FUEL.containsKey(stack.getItem());
    }

    public static int getBurnTime(ItemStack stack) {
        FuelData data = ITEM_FUEL.get(stack.getItem());
        return data != null ? data.burnTime() : 0;
    }

    public static float getHeatStrength(ItemStack stack) {
        FuelData data = ITEM_FUEL.get(stack.getItem());
        return data != null ? data.heatStrength() : 0f;
    }

    public static int getMaxHeat(ItemStack stack) {
        FuelData data = ITEM_FUEL.get(stack.getItem());
        return (data != null && data.maxHeat() != null) ? data.maxHeat() : 0;
    }

    public static void init() {
        MultiblockUtils.loadCustomFuels();
    }
}
