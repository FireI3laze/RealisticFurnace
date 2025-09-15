package com.fireblaze.realistic_furnace.compat;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.items.ModItems;
import com.fireblaze.realistic_furnace.recipes.FurnaceRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FurnaceRecipeCategory implements IRecipeCategory<FurnaceRecipe> {
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "furnace_smelting");
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "textures/gui/furnace_jei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FurnaceRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 150, 60);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModItems.FURNACE_CONTROLLER_ITEM.get())
        );
    }

    @Override
    public RecipeType<FurnaceRecipe> getRecipeType() {
        return JEIRealisticFurnacePlugin.FURNACE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui." + RealisticFurnace.MODID + ".furnace");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FurnaceRecipe recipe, IFocusGroup focuses) {
        // Input
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 20)
                .addIngredients(recipe.getJEIIngredients().get(0));

        // Output
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 20)
                .addItemStack(recipe.getJEIResult());
    }

}
