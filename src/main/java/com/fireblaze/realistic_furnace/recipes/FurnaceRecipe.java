package com.fireblaze.realistic_furnace.recipes;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.compat.JEIRealisticFurnacePlugin;
import com.google.gson.JsonObject;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class FurnaceRecipe implements Recipe<Container> {

    private final Item output;
    private final float requiredHeat;

    // entweder einzelnes Item oder Tag
    private final Item inputItem;
    private final TagKey<Item> inputTag;

    // Konstruktor für einzelnes Item
    public FurnaceRecipe(Item input, Item output, float requiredHeat) {
        this.inputItem = input;
        this.inputTag = null;
        this.output = output;
        this.requiredHeat = requiredHeat;
    }

    // Konstruktor für Tag
    public FurnaceRecipe(TagKey<Item> inputTag, Item output, float requiredHeat) {
        this.inputItem = null;
        this.inputTag = inputTag;
        this.output = output;
        this.requiredHeat = requiredHeat;
    }

    // Prüft, ob der ItemStack zum Rezept passt
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (inputItem != null) return stack.getItem() == inputItem;
        if (inputTag != null) return stack.is(inputTag);
        return false;
    }

    public Item getInputItem() { return inputItem; } // optional, für debug
    public TagKey<Item> getInputTag() { return inputTag; } // optional, für debug
    public Item getOutput() { return output; }
    public float getRequiredHeat() { return requiredHeat; }

    public boolean canProcess(float currentHeat) {
        return currentHeat >= requiredHeat;
    }

    public ItemStack process() {
        return new ItemStack(output);
    }


    // JEI Helper
    public List<Ingredient> getJEIIngredients() {
        if (inputItem != null) {
            return Collections.singletonList(Ingredient.of(inputItem));
        } else if (inputTag != null) {
            return Collections.singletonList(Ingredient.of(inputTag));
        }
        return Collections.emptyList();
    }

    public ItemStack getJEIResult() {
        return new ItemStack(output);
    }

    @Override
    public boolean matches(Container inv, Level world) {
        for (int i = 0; i <= 7; i++) {
            ItemStack stack = inv.getItem(i);
            if (matches(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
        return process();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return process();
    }

    @Override
    public ResourceLocation getId() {
        return getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public RecipeType<?> getType() {
        return FurnaceRecipeType.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<FurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "furnace_recipe");

        @Override
        public FurnaceRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            return null;
        }

        @Override
        public @Nullable FurnaceRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            return null;
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, FurnaceRecipe pRecipe) {

        }
    }
}
