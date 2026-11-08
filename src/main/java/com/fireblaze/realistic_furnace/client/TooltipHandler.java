package com.fireblaze.realistic_furnace.client;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealisticFurnace.MODID, value = Dist.CLIENT)
public class TooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {

        if (!RealisticFurnaceConfig.SHOW_FUEL_TOOLTIPS.get()) return;

        ItemStack stack = event.getItemStack();

        if (!FurnaceFuelRegistry.isFuel(stack)) return;

        if (!Screen.hasShiftDown()) {
            event.getToolTip().add(Component.literal(
                    "Hold SHIFT for more info"
            ).withStyle(ChatFormatting.YELLOW));
            return;
        }

        float heat = FurnaceFuelRegistry.getHeatStrength(stack);
        int maxHeat = FurnaceFuelRegistry.getMaxHeat(stack);
        int burnTime = FurnaceFuelRegistry.getBurnTime(stack);

        event.getToolTip().add(Component.literal(
                "Heat Strength: " + heat
        ).withStyle(ChatFormatting.RED));

        if (maxHeat > 0) {
            event.getToolTip().add(Component.literal(
                    "Max Heat: " + maxHeat
            ).withStyle(ChatFormatting.DARK_RED));
        }

        event.getToolTip().add(Component.literal(
                "Burn Time: " + burnTime
        ).withStyle(ChatFormatting.GRAY));
    }
}