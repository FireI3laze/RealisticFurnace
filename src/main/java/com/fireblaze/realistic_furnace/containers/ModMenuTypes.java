package com.fireblaze.realistic_furnace.containers;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.blocks.ModBlocks;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.network.IContainerFactory;
import net.minecraft.world.flag.FeatureFlags;

import static com.fireblaze.realistic_furnace.blockentities.ModBlockEntities.BLOCK_ENTITIES;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, RealisticFurnace.MODID);

    public static final RegistryObject<MenuType<FurnaceContainer>> FURNACE =
            MENUS.register("realistic_furnace",
                    () -> new MenuType<>(
                            (IContainerFactory<FurnaceContainer>) FurnaceContainer::new,
                            FeatureFlags.VANILLA_SET
                    ));
}



