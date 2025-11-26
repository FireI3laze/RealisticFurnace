package com.fireblaze.realistic_furnace.fuel;

import com.fireblaze.realistic_furnace.multiblock.MultiblockUtils;
import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnaceFuelRegistry {

    public record FuelData(int burnTime, float heatStrength, @Nullable Integer maxHeat, @Nullable String nbtJson) {
        public FuelData(int burnTime, float heatStrength) {
            this(burnTime, heatStrength, null, null);
        }

        public FuelData(int burnTime, float heatStrength, int maxHeat) {
            this(burnTime, heatStrength, maxHeat, null);
        }
    }


    /** Wrapper für ItemStack oder Tag */
    public record StackKey(@Nullable Item item, @Nullable TagKey<Item> tag, @Nullable CompoundTag nbt) {
        public StackKey(ItemStack stack) {
            this(stack.getItem(), null, stack.getTag());
        }

        public StackKey(Item item, @Nullable CompoundTag nbt) {
            this(item, null, nbt);
        }

        public StackKey(TagKey<Item> tag) {
            this(null, tag, null);
        }

        public boolean isTag() {
            return tag != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StackKey other)) return false;

            if (this.isTag() && other.isTag()) return this.tag.equals(other.tag());
            if (this.item != null && other.item != null) {
                if (this.item != other.item) return false;
                if (this.nbt == null && other.nbt == null) return true;
                if (this.nbt != null && other.nbt != null) return this.nbt.equals(other.nbt);
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (isTag()) return tag.hashCode();
            int h = item.hashCode();
            if (nbt != null) h = 31 * h + nbt.hashCode();
            return h;
        }
    }


    public static final Map<StackKey, FuelData> ITEM_FUEL = new HashMap<>();

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
        // Wenn es ein Tag ist, prüfen wir über das Tag
        if (registered.isTag()) {
            return input.getItem().builtInRegistryHolder().is(registered.tag);
        }

        // Item direkt prüfen
        if (registered.item != input.getItem()) {
            return false;
        }

        // NBT prüfen, falls vorhanden
        if (registered.nbt == null || registered.nbt.isEmpty()) {
            return true;
        }

        return registered.nbt.equals(input.getTag());
    }



    public static void init() {
        //MultiblockUtils.loadCustomFuels();
    }

    public static int removeAllForItem(Item item) {
        int count = 0;
        var iterator = ITEM_FUEL.keySet().iterator();
        while (iterator.hasNext()) {
            var key = iterator.next();
            if (key.item() == item) {
                iterator.remove();
                count++;
            }
        }
        return count;
    }

    private static final Path SAVE_PATH = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/fuels.json");

    public static void save() {
        JsonArray array = new JsonArray();
        for (Map.Entry<StackKey, FuelData> entry : ITEM_FUEL.entrySet()) {
            StackKey key = entry.getKey();
            FuelData data = entry.getValue();
            JsonObject obj = new JsonObject();

            if (key.isTag()) {
                // Tags mit # speichern
                obj.addProperty("item", "#" + key.tag.location()); // oder key.tag().location() je nach MC-Version
            } else {
                obj.addProperty("item", ForgeRegistries.ITEMS.getKey(key.item()).toString());
            }

            obj.addProperty("burnTime", data.burnTime());
            obj.addProperty("heatStrength", data.heatStrength());
            obj.addProperty("maxHeat", data.maxHeat() != null ? data.maxHeat() : 0);
            obj.addProperty("nbt", data.nbtJson() != null ? data.nbtJson() : "");
            array.add(obj);
        }
        try (Writer writer = Files.newBufferedWriter(SAVE_PATH)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(array, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void load() {
        if (!Files.exists(SAVE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(SAVE_PATH)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();

            for (JsonElement e : array) {
                JsonObject obj = e.getAsJsonObject();
                String fullName = obj.get("item").getAsString();
                if (fullName.startsWith("#")) {
                    // Tag
                    String tagName = fullName.substring(1); // "minecraft:planks"
                    TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.tryParse(tagName));

                    FuelData data = new FuelData(
                            obj.get("burnTime").getAsInt(),
                            obj.get("heatStrength").getAsFloat(),
                            obj.get("maxHeat").getAsInt(),
                            obj.get("nbt").getAsString().isEmpty() ? null : obj.get("nbt").getAsString()
                    );

                    ITEM_FUEL.put(new StackKey(tag), data);
                } else {
                    // Item wie bisher
                    String[] parts = fullName.split(":", 2);
                    if (parts.length != 2) continue;
                    ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                    Item item = ForgeRegistries.ITEMS.getValue(rl);
                    if (item == null) continue;

                    CompoundTag tag = null;
                    String nbtJson = obj.get("nbt").getAsString();
                    if (!nbtJson.isEmpty()) {
                        try {
                            tag = TagParser.parseTag(nbtJson);
                        } catch (CommandSyntaxException err) {
                            err.printStackTrace();
                        }
                    }

                    FuelData data = new FuelData(
                            obj.get("burnTime").getAsInt(),
                            obj.get("heatStrength").getAsFloat(),
                            obj.get("maxHeat").getAsInt(),
                            nbtJson.isEmpty() ? null : nbtJson
                    );

                    ITEM_FUEL.put(new StackKey(item, tag), data);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void loadAndMergeDefaults() {
        System.out.println("[DEBUG] Loading fuels.json...");
        load();

        List<? extends String> configFuels = RealisticFurnaceConfig.CUSTOM_FUELS.get();

        for (String line : configFuels) {
            if (line.isBlank()) continue;

            String[] parts = line.split(",", -1);
            if (parts.length < 3) continue;

            String itemOrTag = parts[0].trim();
            String nbt = null;
            if (itemOrTag.contains("{") && itemOrTag.contains("}")) {
                nbt = itemOrTag.substring(itemOrTag.indexOf("{"), itemOrTag.indexOf("}") + 1);
                itemOrTag = itemOrTag.replace(nbt, "");
            }

            int burnTime = Integer.parseInt(parts[1].trim());
            float heatStrength = Float.parseFloat(parts[2].trim());
            int maxHeat = parts.length >= 4 ? Integer.parseInt(parts[3].trim()) : 0;

            if (itemOrTag.startsWith("#")) {
                String tagName = itemOrTag.substring(1); // z.B. "minecraft:planks"
                TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.tryParse(tagName));

                // Prüfen, ob Tag schon registriert wurde
                boolean exists = ITEM_FUEL.keySet().stream().anyMatch(k -> k.isTag() && k.tag().equals(tag));
                if (!exists) {
                    ITEM_FUEL.put(new StackKey(tag), new FuelData(burnTime, heatStrength, maxHeat));
                    System.out.println("[DEBUG] Added tag fuel: " + tag);
                }
            }

            else {
                // Einzelnes Item
                ResourceLocation rl = ResourceLocation.tryParse(itemOrTag);
                if (rl == null) continue;
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null) continue;

                boolean exists = ITEM_FUEL.keySet().stream().anyMatch(k -> k.item() == item);
                if (!exists) {
                    CompoundTag tagObj = null;
                    if (nbt != null) {
                        try {
                            tagObj = TagParser.parseTag(nbt);
                        } catch (Exception ignored) {}
                    }
                    ITEM_FUEL.put(new StackKey(item, tagObj), new FuelData(burnTime, heatStrength, maxHeat, nbt));
                    System.out.println("[DEBUG] Added basic fuel from config: " + itemOrTag);
                }
            }
        }

        save(); // JSON speichern falls neue Basis-Fuels hinzugefügt wurden
        System.out.println("[DEBUG] Finished merging basic fuels. Total fuels: " + ITEM_FUEL.size());
    }


}
