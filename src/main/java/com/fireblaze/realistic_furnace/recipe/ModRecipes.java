package com.fireblaze.realistic_furnace.recipe;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    // RecipeSerializer DeferredRegister
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RealisticFurnace.MODID);

    public static final RegistryObject<RecipeSerializer<Realistic_Furnace_Recipe>> REALISTIC_FURNACE_SERIALIZER =
            SERIALIZERS.register("realistic_smelting", () -> Realistic_Furnace_Recipe.Serializer.INSTANCE);

    // RecipeType DeferredRegister
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, RealisticFurnace.MODID);

    public static final RegistryObject<RecipeType<Realistic_Furnace_Recipe>> REALISTIC_FURNACE_TYPE =
            TYPES.register("realistic_smelting", () -> Realistic_Furnace_Recipe.Type.INSTANCE);

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
        TYPES.register(bus);
    }
}
