package com.fireblaze.realistic_furnace.multiblock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnaceMultiblockSavedData extends SavedData {

    private static final String DATA_NAME = "realistic_furnace_multiblock_data";
    private final Map<String, String> selectedMultiblocks = new HashMap<>(); // pro Dimension: Name

    public static FurnaceMultiblockSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FurnaceMultiblockSavedData::load,
                FurnaceMultiblockSavedData::new,
                DATA_NAME
        );
    }

    public FurnaceMultiblockSavedData() {}

    public static FurnaceMultiblockSavedData load(CompoundTag tag) {
        FurnaceMultiblockSavedData data = new FurnaceMultiblockSavedData();

        ListTag list = tag.getList("Selections", 10); // Compound pro Welt
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String dimension = entry.getString("Dimension");
            String selected = entry.getString("Selected");
            data.selectedMultiblocks.put(dimension, selected);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Map.Entry<String, String> e : selectedMultiblocks.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", e.getKey());
            entry.putString("Selected", e.getValue());
            list.add(entry);
        }

        tag.put("Selections", list);
        return tag;
    }

    public void setSelected(ServerLevel level, String selectedName) {
        String dimension = level.dimension().location().toString();
        selectedMultiblocks.put(dimension, selectedName);
        setDirty();
    }

    public String getSelected(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        return selectedMultiblocks.getOrDefault(dimension, "");
    }
}
