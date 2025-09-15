package com.fireblaze.realistic_furnace;

import com.fireblaze.realistic_furnace.capability.Stamina;
import com.fireblaze.realistic_furnace.capability.StaminaProvider;
import com.fireblaze.realistic_furnace.config.StaminaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.block.state.BlockState;

@Mod.EventBusSubscriber(modid = RealisticFurnace.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            // Verhindert Doppel-Registrierung
            if (!player.getCapability(StaminaProvider.PLAYER_STAMINA).isPresent()) {
                event.addCapability(ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "properties"), new StaminaProvider());
                System.out.println("Attaching Stamina Capability from onAttachCapabilitiesPlayer");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Caps vom Original kurz wiederbeleben, damit wir lesen können
            event.getOriginal().reviveCaps();

            event.getOriginal().getCapability(StaminaProvider.PLAYER_STAMINA).ifPresent(oldStore -> {
                event.getEntity().getCapability(StaminaProvider.PLAYER_STAMINA).ifPresent(newStore -> {
                    if (StaminaConfig.KEEP_LEVEL_ON_DEATH.get()) {
                        System.out.println("Config says yes");
                        // Alles kopieren (inkl. Level & XP)
                        newStore.copyFrom(oldStore);
                    } else {
                        System.out.println("Config says no");

                        // Standard-Kopie
                        newStore.copyFrom(oldStore);

                        // Falls gewünscht: Stamina-Werte vom Original erhalten
                        newStore.setStaminaLvl(0);
                        newStore.setStaminaExp(0);
                    }
                });
            });

            // Caps wieder invalidieren
            event.getOriginal().invalidateCaps();
        }
    }
}