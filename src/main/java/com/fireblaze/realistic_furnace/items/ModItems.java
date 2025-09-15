package com.fireblaze.realistic_furnace.items;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.blocks.ModBlocks;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RealisticFurnace.MODID);

    public static final RegistryObject<Item> FURNACE_CONTROLLER_ITEM =
            ITEMS.register("furnace_controller",
                    () -> new BlockItem(ModBlocks.FURNACE_CONTROLLER.get(), new Item.Properties()));

}
