package com.fireblaze.realistic_furnace.client;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FurnaceRenderState {

    private static final Map<UUID, Boolean> ghostRendering = new HashMap<>();
    private static final Map<UUID, FurnaceControllerBlockEntity.GhostMode> modes = new HashMap<>();
    private static final Map<UUID, Level> worlds = new HashMap<>();
    private static final Map<UUID, String> multiblockNames = new HashMap<>();


    public static boolean isGhostRendering() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        return ghostRendering.getOrDefault(player.getUUID(), false);
    }

    public static void toggleGhostRendering() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean newValue = !ghostRendering.getOrDefault(player.getUUID(), false);
        ghostRendering.put(player.getUUID(), newValue);
    }

    public static void setGhostRendering(boolean value) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ghostRendering.put(player.getUUID(), value);
    }

    public static FurnaceControllerBlockEntity.GhostMode getMode() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return FurnaceControllerBlockEntity.GhostMode.NONE;
        return modes.getOrDefault(player.getUUID(), FurnaceControllerBlockEntity.GhostMode.BLOCK_BY_BLOCK);
    }

    public static void setMode(FurnaceControllerBlockEntity.GhostMode mode) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        modes.put(player.getUUID(), mode);
    }

    public static void cycleMode() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        FurnaceControllerBlockEntity.GhostMode current = getMode();
        FurnaceControllerBlockEntity.GhostMode next = switch (current) {
            case NONE -> FurnaceControllerBlockEntity.GhostMode.BLOCK_BY_BLOCK;
            case BLOCK_BY_BLOCK -> FurnaceControllerBlockEntity.GhostMode.FULL_STRUCTURE;
            case FULL_STRUCTURE -> FurnaceControllerBlockEntity.GhostMode.NONE;
        };
        modes.put(player.getUUID(), next);
    }

    public static void setWorld(Level world) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        worlds.put(player.getUUID(), world);
    }

    public static Level getWorld() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return null;
        return worlds.get(player.getUUID());
    }

    public static void setMultiblock(String name) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        multiblockNames.put(player.getUUID(), name);
    }

    public static String getMultiblock() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return null;
        return multiblockNames.get(player.getUUID());
    }
}
