package com.fireblaze.realistic_furnace;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.blockentities.ModBlockEntities;
import com.fireblaze.realistic_furnace.blocks.ModBlocks;
import com.fireblaze.realistic_furnace.commands.*;
import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import com.fireblaze.realistic_furnace.containers.ModMenuTypes;
import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import com.fireblaze.realistic_furnace.networking.NetworkHandler;
import com.fireblaze.realistic_furnace.recipe.ModRecipes;
import com.fireblaze.realistic_furnace.screens.FurnaceScreen;
import com.fireblaze.realistic_furnace.items.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RealisticFurnace.MODID)
public class RealisticFurnace
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "realistic_furnace";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public RealisticFurnace(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        Path configDir = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create config directory for Realistic Furnace", e);
        }

        // === Config im Unterordner registrieren ===
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                RealisticFurnaceConfig.SPEC,
                configDir.resolve("realistic_furnace-common.toml").toString()
        );

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModRecipes.register(modEventBus);

        NetworkHandler.register();
    }

    public static void clientSetup(final FMLClientSetupEvent event) {
    }

    public void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Fuel-Daten laden
            FurnaceFuelRegistry.loadAndMergeDefaults();
            LOGGER.info("[Realistic Furnace] Custom fuels loaded and merged with defaults.");

            // Multiblocks laden
            ensureDefaultMultiblockExists();
        });
    }

    private static void ensureDefaultMultiblockExists() {
        Path folder = FMLPaths.CONFIGDIR.get().resolve("realistic_furnace/multiblocks");
        Path file = folder.resolve("original_furnace.json");

        if (!Files.exists(file)) {
            try {
                Files.createDirectories(folder);

                // Datei aus JAR kopieren
                try (InputStream in = RealisticFurnace.class.getResourceAsStream(
                        "/assets/realistic_furnace/multiblocks/original_furnace.json")) {

                    if (in == null) {
                        LOGGER.error("[Realistic Furnace] Default multiblock JSON not found in resources!");
                        return;
                    }

                    Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("[Realistic Furnace] Default multiblock JSON created at " + file);

                }
            } catch (IOException e) {
                LOGGER.error("[Realistic Furnace] Failed to create default multiblock JSON!", e);
            }
        }
    }


    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.FURNACE_CONTROLLER_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            MenuScreens.register(ModMenuTypes.FURNACE.get(), FurnaceScreen::new);
        }
    }

    @Mod.EventBusSubscriber(modid = RealisticFurnace.MODID)
    public static class RealisticFurnaceCommandRegistration {

        @SubscribeEvent
        public static void onCommandRegister(RegisterCommandsEvent event) {
            RealisticFurnaceCommand.register(event.getDispatcher());
            SaveMultiblockCommand.register(event.getDispatcher());
            SelectMultiblockCommand.register(event.getDispatcher());
            DeleteMultiblockCommand.register(event.getDispatcher());
            RegisterTrapdoorCommand.register(event.getDispatcher());

            // FuelCommand registrieren
            event.getDispatcher().register(FuelCommand.register());
        }
    }
}
