package com.fireblaze.realistic_furnace.fuel;

import com.fireblaze.realistic_furnace.multiblock.MultiblockUtils;
import net.minecraft.nbt.CompoundTag;
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

    /** Wrapper für ItemStack, damit HashMap korrekt funktioniert */
    private record StackKey(Item item, @Nullable CompoundTag tag) {
        public StackKey(ItemStack stack) {
            this(stack.getItem(), stack.getTag());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StackKey other)) return false;
            if (item != other.item) return false;
            if (tag == null && other.tag == null) return true;
            if (tag != null && other.tag != null) return tag.equals(other.tag);
            return false;
        }

        @Override
        public int hashCode() {
            int h = item.hashCode();
            if (tag != null) h = 31 * h + tag.hashCode();
            return h;
        }
    }

    private static final Map<StackKey, FuelData> ITEM_FUEL = new HashMap<>();

    // Registrierung eines Items
    public static void register(Item item, int burnTime, float heatStrength) {
        ITEM_FUEL.put(new StackKey(item, null), new FuelData(burnTime, heatStrength));
    }

    public static void register(Item item, int burnTime, float heatStrength, int maxHeat) {
        ITEM_FUEL.put(new StackKey(item, null), new FuelData(burnTime, heatStrength, maxHeat));
    }

    // Registrierung eines Tags
    public static void register(TagKey<Item> tag, int burnTime, float heatStrength) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.builtInRegistryHolder().is(tag)) {
                ITEM_FUEL.put(new StackKey(item, null), new FuelData(burnTime, heatStrength));
            }
        }
    }

    public static void register(TagKey<Item> tag, int burnTime, float heatStrength, int maxHeat) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.builtInRegistryHolder().is(tag)) {
                ITEM_FUEL.put(new StackKey(item, null), new FuelData(burnTime, heatStrength, maxHeat));
            }
        }
    }

    public static void register(ItemStack stack, int burnTime, float heatStrength, @Nullable Integer maxHeat) {
        ITEM_FUEL.put(new StackKey(stack.copy()), new FuelData(burnTime, heatStrength, maxHeat));
    }

    public static boolean isFuel(ItemStack stack) {
        for (StackKey key : ITEM_FUEL.keySet()) {
            if (stackMatches(key, stack)) return true;
        }
        return false;
    }

    public static int getBurnTime(ItemStack stack) {
        for (Map.Entry<StackKey, FuelData> entry : ITEM_FUEL.entrySet()) {
            if (stackMatches(entry.getKey(), stack)) return entry.getValue().burnTime();
        }
        return 0;
    }

    public static float getHeatStrength(ItemStack stack) {
        for (Map.Entry<StackKey, FuelData> entry : ITEM_FUEL.entrySet()) {
            if (stackMatches(entry.getKey(), stack)) return entry.getValue().heatStrength();
        }
        return 0f;
    }

    public static int getMaxHeat(ItemStack stack) {
        for (Map.Entry<StackKey, FuelData> entry : ITEM_FUEL.entrySet()) {
            if (stackMatches(entry.getKey(), stack)) {
                Integer max = entry.getValue().maxHeat();
                return max != null ? max : 0;
            }
        }
        return 0;
    }


    private static boolean stackMatches(StackKey registered, ItemStack input) {

        // Itemvergleich
        if (registered.item != input.getItem()) {
            return false;
        }

        // Kein NBT gefordert
        if (registered.tag == null || registered.tag.isEmpty()) {
            return true;
        }

        return registered.tag.equals(input.getTag());
    }

    public static void init() {
        MultiblockUtils.loadCustomFuels();
    }
}
