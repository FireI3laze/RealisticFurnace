package com.fireblaze.realistic_furnace.recipes;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public class FurnaceRecipeType {
    public static final RecipeType<FurnaceRecipe> INSTANCE = new RecipeType<>() {
        @Override
        public String toString() {
            return "realistic_furnace:furnace_recipe";
        }
    };
}
