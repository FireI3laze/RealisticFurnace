package com.fireblaze.realistic_furnace.blocks;

import com.fireblaze.realistic_furnace.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.fireblaze.realistic_furnace.RealisticFurnace;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RealisticFurnace.MODID);
    public static final RegistryObject<FurnaceControllerBlock> FURNACE_CONTROLLER =
            BLOCKS.register("furnace_controller", () -> new FurnaceControllerBlock(BlockBehaviour.Properties.copy(Blocks.BRICKS).noOcclusion()));
}
