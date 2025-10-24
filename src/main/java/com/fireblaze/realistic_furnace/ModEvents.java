package com.fireblaze.realistic_furnace;

import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealisticFurnace.MODID)
public class ModEvents {
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        FurnaceFuelRegistry.init();
    }
}