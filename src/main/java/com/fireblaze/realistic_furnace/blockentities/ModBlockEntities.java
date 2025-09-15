package com.fireblaze.realistic_furnace.blockentities;

import com.fireblaze.realistic_furnace.blocks.ModBlocks;
import com.fireblaze.realistic_furnace.RealisticFurnace;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;


public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RealisticFurnace.MODID);

    public static final RegistryObject<BlockEntityType<FurnaceControllerBlockEntity>> FURNACE_CONTROLLER =
            BLOCK_ENTITIES.register("furnace_controller.json",
                    () -> BlockEntityType.Builder.of(FurnaceControllerBlockEntity::new, ModBlocks.FURNACE_CONTROLLER.get())
                            .build(null));
}

