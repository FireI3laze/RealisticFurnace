package com.fireblaze.realistic_furnace.events;

import com.fireblaze.realistic_furnace.multiblock.*;
import com.fireblaze.realistic_furnace.networking.NetworkHandler;
import com.fireblaze.realistic_furnace.networking.packet.SyncMultiblockDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * ServerEventHandler sorgt dafür, dass beim Beitritt eines Spielers
 * die aktuell gewählte Multiblock-Struktur erneut an den Client gesendet wird.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        String selectedName = FurnaceMultiblockRegistry.getSelectedMultiblockName(level);

        if (selectedName == null || selectedName.isEmpty()) {
            return;
        }

        List<OffsetBlock> structure = FurnaceMultiblockRegistry.getSelectedMultiblock(level, selectedName);
        if (structure.isEmpty()) {
            return;
        }

        OffsetBlock valve = FurnaceMultiblockRegistry.getValve(level);
        if (valve == null) {
            return;
        }

        List<SerializableOffsetBlock> serializable = structure.stream()
                .map(SerializableOffsetBlock::fromOffsetBlock)
                .toList();

        SyncMultiblockDataPacket packet = new SyncMultiblockDataPacket(
                selectedName,
                serializable,
                SerializableOffsetBlock.fromOffsetBlock(valve)
        );

        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels()) {
            FurnaceMultiblockSavedData data = FurnaceMultiblockSavedData.get(level);
            String selected = data.getSelected(level);

            if (!selected.isEmpty()) {
                List<OffsetBlock> blocks = FurnaceMultiblockRegistry.loadByName(level, selected);
                FurnaceMultiblockRegistry.selectMultiblock(level, selected, blocks);

                //System.out.println("[Realistic Furnace] ✅ Wiederhergestellt: " + selected + " in " + level.dimension().location());
            }
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // nur Overworld
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        FurnaceMultiblockSavedData data = FurnaceMultiblockSavedData.get(level);

        if (data.getSelected(level).isEmpty()) {
            List<OffsetBlock> defaultBlocks = FurnaceMultiblockRegistry.loadByName(level, MultiblockUtils.getFallbackFile());
            FurnaceMultiblockRegistry.selectMultiblock(level, MultiblockUtils.getFallbackFile(), defaultBlocks);
        }
    }


}
