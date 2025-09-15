package com.fireblaze.realistic_furnace.recipes;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class FurnaceRecipes {
    public static final List<FurnaceRecipe> RECIPES = new ArrayList<>();

    static {
        // Beispiel-Rezepte: Input -> Output bei Mindesthitze
        RECIPES.add(new FurnaceRecipe(Items.RAW_IRON, Items.IRON_INGOT, 1500f));
        RECIPES.add(new FurnaceRecipe(Items.RAW_GOLD, Items.GOLD_INGOT, 1150f));
        RECIPES.add(new FurnaceRecipe(Items.RAW_COPPER, Items.COPPER_INGOT, 1300f));
        RECIPES.add(new FurnaceRecipe(Items.SAND, Items.GLASS, 1400f));


        RECIPES.add(new FurnaceRecipe(ItemTags.LOGS, Items.CHARCOAL, 350f));
    }
}
