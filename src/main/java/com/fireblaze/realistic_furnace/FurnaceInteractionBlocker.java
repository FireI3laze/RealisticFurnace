package com.fireblaze.realistic_furnace;

import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FurnaceInteractionBlocker {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {

        // Config: darf man den Ofen öffnen?
        if (RealisticFurnaceConfig.ALLOW_FURNACE_INTERACTION.get()) {
            return; // erlaubt → nichts tun
        }

        // Nur Server-Seite
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Prüfen ob der Block ein Vanilla Furnace ist
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();

        if (block == Blocks.FURNACE) {

            // Öffnen des Inventars verhindern
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            // Optional Nachricht
            event.getEntity().displayClientMessage(
                    Component.literal("Furnace is deactivated in realistic_furnace.toml"), true
            );
        }
    }
}
