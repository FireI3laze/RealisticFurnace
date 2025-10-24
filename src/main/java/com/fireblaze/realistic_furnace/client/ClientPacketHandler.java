package com.fireblaze.realistic_furnace.client;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRegistry;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import com.fireblaze.realistic_furnace.networking.packet.SyncMultiblockDataPacket;
import com.fireblaze.realistic_furnace.multiblock.SerializableOffsetBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.util.List;

public class ClientPacketHandler {

    public static void handleMultiblockData(SyncMultiblockDataPacket msg) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return;

        // Struktur vom Server übernehmen
        List<OffsetBlock> blocks = msg.blocks.stream()
                .map(SerializableOffsetBlock::toOffsetBlock)
                .toList();

        FurnaceMultiblockRegistry.selectMultiblock(world, msg.name, blocks);

        if (msg.valve != null)
            FurnaceMultiblockRegistry.setValve(world, msg.valve.toOffsetBlock());

        FurnaceRenderState.setWorld(world);
        FurnaceRenderState.setMultiblock(msg.name);
        FurnaceRenderState.setGhostRendering(true);
        FurnaceRenderState.setMode(FurnaceControllerBlockEntity.GhostMode.FULL_STRUCTURE);
    }

}

