package com.fireblaze.realistic_furnace.compat;

import com.fireblaze.realistic_furnace.items.ModItems;
import com.fireblaze.realistic_furnace.recipe.Realistic_Furnace_Recipe;
import com.fireblaze.realistic_furnace.screens.FurnaceScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.fireblaze.realistic_furnace.RealisticFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class JEIRealisticFurnacePlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        // registration.addRecipeCategories(new FurnaceRecipeCategory(guiHelper));
        registration.addRecipeCategories(new FurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Rezepte aus dem RecipeManager holen
        /*
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;

        var manager = level.getRecipeManager();
        var recipes = manager.getAllRecipesFor(FurnaceRecipeType.INSTANCE);

        registration.addRecipes(FURNACE_TYPE, recipes);
        */

        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<Realistic_Furnace_Recipe> furnaceRecipes = recipeManager.getAllRecipesFor(Realistic_Furnace_Recipe.Type.INSTANCE);
        registration.addRecipes(FurnaceRecipeCategory.REALISTIC_FURNACE_TYPE, furnaceRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // IModPlugin.super.registerGuiHandlers(registration);

        registration.addRecipeClickArea(FurnaceScreen.class, 15, 20, 15, 15, FurnaceRecipeCategory.REALISTIC_FURNACE_TYPE);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // dein Ofen-Block als "Katalysator"
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.FURNACE_CONTROLLER_ITEM.get()),
                FurnaceRecipeCategory.REALISTIC_FURNACE_TYPE
        );
    }
}
