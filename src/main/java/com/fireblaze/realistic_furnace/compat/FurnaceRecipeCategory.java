package com.fireblaze.realistic_furnace.compat;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.blocks.ModBlocks;
import com.fireblaze.realistic_furnace.config.RealisticFurnaceConfig;
import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import com.fireblaze.realistic_furnace.recipe.Realistic_Furnace_Recipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraft.client.Minecraft;

import java.util.List;


public class FurnaceRecipeCategory implements IRecipeCategory<Realistic_Furnace_Recipe> {
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "furnace_smelting");
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "textures/gui/realistic_furnace_gui.png");

    public static final RecipeType<Realistic_Furnace_Recipe> REALISTIC_FURNACE_TYPE =
            new RecipeType<>(UID, Realistic_Furnace_Recipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable progressAnimated;
    private final IDrawableStatic flameStatic;
    private final IDrawableAnimated flameAnimated;


    public FurnaceRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 85);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.FURNACE_CONTROLLER.get())
        );

        this.progressAnimated = new CustomProgressDrawable(18, 1);

        this.flameStatic = guiHelper.createDrawable(TEXTURE, 176, 0, 14, 14);
        this.flameAnimated = guiHelper.createAnimatedDrawable(flameStatic, 300, IDrawableAnimated.StartDirection.TOP, true);

    }

    @Override
    public RecipeType<Realistic_Furnace_Recipe> getRecipeType() {
        return REALISTIC_FURNACE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.realistic_furnace.furnace_controller");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Realistic_Furnace_Recipe recipe, IFocusGroup focuses) {
        // Fuel
        builder.addSlot(RecipeIngredientRole.INPUT, 15, 34)
                .addIngredients(VanillaTypes.ITEM_STACK, FurnaceFuelRegistry.getAllFuelStacks());

        // input
        builder.addSlot(RecipeIngredientRole.INPUT, 46, 21).addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.INPUT, 68, 21).addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.INPUT, 68, 48).addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.INPUT, 92, 48).addIngredients(recipe.getIngredients().get(0));


        // output
        builder.addSlot(RecipeIngredientRole.OUTPUT, 46, 48).addItemStack(recipe.getResultItem(null));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 21).addItemStack(recipe.getResultItem(null));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 21).addItemStack(recipe.getResultItem(null));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 48).addItemStack(recipe.getResultItem(null));

        ItemStack overheated = recipe.getOverheatedResult(null);
        if (!overheated.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, -1000, -1000) // offscreen
                    .addItemStack(overheated);
        }
    }

    @Override
    public void draw(Realistic_Furnace_Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {
        // Progressbars über den Slots
        int[][] positions = {{45, 18}, {67, 18}, {67, 45}, {91, 45}};
        for (int[] pos : positions) {
            progressAnimated.draw(gui, pos[0], pos[1]);
        }

        // HeatBar
        int heatBarX = 150;
        int heatBarY = 9;
        int heatBarWidth = 8;
        int heatBarHeight = 64;
        int requiredHeat = recipe.getRequiredHeat();
        int overheatedHeat = recipe.getOverheatedHeat();
        int maxHeat = RealisticFurnaceConfig.MAX_HEAT.get();
        ItemStack inputStack = recipe.getIngredients().get(0).getItems()[0];
        ItemStack overheatStack = recipe.getOverheatedResult(null);

        HeatBarDrawable heatBar = new HeatBarDrawable(
                heatBarWidth,
                heatBarHeight,
                requiredHeat,
                recipe.getOverheatedHeat(),
                maxHeat,
                inputStack,
                overheatStack
        );

        heatBar.draw(gui, heatBarX, heatBarY);

        // Tooltip **nur** wenn Maus über HeatBar
        if (mouseX >= heatBarX && mouseX < heatBarX + heatBarWidth &&
                mouseY >= heatBarY && mouseY < heatBarY + heatBarHeight) {

            Font font = Minecraft.getInstance().font;

            // 🔥 Mausposition → Temperatur umrechnen
            float relativeY = (float)(mouseY - heatBarY) / heatBarHeight;
            relativeY = Math.max(0f, Math.min(1f, relativeY));

            float heatFraction = 1f - relativeY; // unten = heiß
            float hoveredHeat = heatFraction * maxHeat;

            String label;

            boolean shiftDown = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            boolean controlDown = net.minecraft.client.gui.screens.Screen.hasControlDown();

            if (shiftDown) {
                label = "Min smelting range: " +
                        HeatBarDrawable.getTemperatureLabel(requiredHeat);
            } else if (controlDown) {
                label = "Max smelting range: " +
                        HeatBarDrawable.getTemperatureLabel(overheatedHeat - 1);
            }

            else {
                if (hoveredHeat < requiredHeat) {
                    label = "Too cold: " + HeatBarDrawable.getTemperatureLabel(hoveredHeat);
                }
                else if (hoveredHeat < overheatedHeat) {
                    label = "Smelting range: " + HeatBarDrawable.getTemperatureLabel(hoveredHeat);
                }
                else {
                    label = "Too hot: " + HeatBarDrawable.getTemperatureLabel(hoveredHeat);
                }
            }

            gui.renderTooltip(
                    font,
                    Component.literal(label),
                    (int) mouseX,
                    (int) mouseY
            );
        }

        int flameX = 15; // Position manuell anpassen
        int flameY = 17; // Position manuell anpassen

        float scale = 0.6f;
        int iconSize = 16;
        int scaledSize = (int)(iconSize * scale);

        // gleiche Berechnung wie im HeatBarDrawable!
        float overheatFraction = Math.min(1f, (float) overheatedHeat / maxHeat);
        int yOverheat = heatBarY + heatBarHeight - (int)(overheatFraction * heatBarHeight);

        int iconX = heatBarX + heatBarWidth + 2;
        int iconY;
        String text;
        if (yOverheat != 9) {
            iconY = yOverheat - (scaledSize / 2) - 1;
            text = "Burns at " +
                    HeatBarDrawable.getTemperatureLabel(overheatedHeat);
        }
        else {
            iconY = yOverheat - (scaledSize / 2) - 1;
            text = "Can't be burned";
        }

        // Bereich definieren (inkl. Pfeil + Output)
        int areaWidth = scaledSize + 2 + 12 + 16; // input + spacing + arrow + output
        int areaHeight = scaledSize;

        if (mouseX >= iconX && mouseX <= iconX + areaWidth &&
                mouseY >= iconY && mouseY <= iconY + areaHeight) {

            Font font = Minecraft.getInstance().font;

            gui.renderTooltip(
                    font,
                    Component.literal(text),
                    (int) mouseX,
                    (int) mouseY
            );
        }

        flameAnimated.draw(gui, flameX, flameY);
    }
}
