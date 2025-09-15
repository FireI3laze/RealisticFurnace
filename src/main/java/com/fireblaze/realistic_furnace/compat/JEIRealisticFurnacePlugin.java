package com.fireblaze.realistic_furnace.compat;

import com.fireblaze.realistic_furnace.items.ModItems;
import com.fireblaze.realistic_furnace.recipes.FurnaceRecipeType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.recipes.FurnaceRecipe;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class JEIRealisticFurnacePlugin implements IModPlugin {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "jei_plugin");

    // unser RecipeType
    public static final RecipeType<FurnaceRecipe> FURNACE_TYPE =
            RecipeType.create(RealisticFurnace.MODID, "realistic_furnace", FurnaceRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new FurnaceRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Rezepte aus dem RecipeManager holen
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;

        var manager = level.getRecipeManager();
        var recipes = manager.getAllRecipesFor(FurnaceRecipeType.INSTANCE);

        registration.addRecipes(FURNACE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // dein Ofen-Block als "Katalysator"
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.FURNACE_CONTROLLER_ITEM.get()), // 👈 hier dein Item
                FURNACE_TYPE
        );
    }
}
