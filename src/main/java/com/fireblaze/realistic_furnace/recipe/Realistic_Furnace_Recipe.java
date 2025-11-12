package com.fireblaze.realistic_furnace.recipe;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Realistic_Furnace_Recipe implements Recipe<SimpleContainer> {
    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    private final ResourceLocation id;
    private final int requiredHeat;
    private final ItemStack overheatedResult;

    // 🔹 Neu: Überhitzungs-Temperatur (optional)
    private final int overheatedHeat;

    public Realistic_Furnace_Recipe(NonNullList<Ingredient> inputItems, ItemStack output, ResourceLocation id,
                                    int requiredHeat, ItemStack overheatedResult, int overheatedHeat) {
        this.inputItems = inputItems;
        this.output = output;
        this.id = id;
        this.requiredHeat = requiredHeat;
        this.overheatedResult = overheatedResult;
        this.overheatedHeat = overheatedHeat;
    }

    public int getRequiredHeat() {
        return requiredHeat;
    }

    public int getOverheatedHeat() {
        return overheatedHeat;
    }

    @Nullable
    public ItemStack getOverheatedResult(RegistryAccess registryAccess) {
        return overheatedResult == null ? ItemStack.EMPTY : overheatedResult;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide) return false;
        if (container.isEmpty()) return false;
        ItemStack stack = container.getItem(0);
        if (stack.isEmpty()) return false;

        return inputItems.stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputItems;
    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) {return output.copy();}

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<Realistic_Furnace_Recipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "realistic_smelting";
    }

    public static class Serializer implements RecipeSerializer<Realistic_Furnace_Recipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "realistic_smelting");

        @Override
        public Realistic_Furnace_Recipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            int requiredHeat = GsonHelper.getAsInt(pSerializedRecipe, "requiredHeat", 1000);

            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));

            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);
            inputs.set(0, Ingredient.fromJson(ingredients.get(0)));

            // 🔹 Optionaler Überhitzungs-Output
            ItemStack overheatedResult = ItemStack.EMPTY;
            if (pSerializedRecipe.has("overheatedOutput")) {
                overheatedResult = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "overheatedOutput"));
            }

            // 🔹 Optionaler Überhitzungs-Heat-Wert
            int overheatedHeat;
            if (pSerializedRecipe.has("overheatedHeat")) {
                overheatedHeat = GsonHelper.getAsInt(pSerializedRecipe, "overheatedHeat");
            } else {
                // 🔸 Fallback: 50% über requiredHeat
                overheatedHeat = (int) Math.round(requiredHeat * 1.5);
            }

            return new Realistic_Furnace_Recipe(inputs, output, pRecipeId, requiredHeat, overheatedResult, overheatedHeat);
        }

        @Override
        public @Nullable Realistic_Furnace_Recipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(pBuffer.readInt(), Ingredient.EMPTY);
            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(pBuffer));
            }

            ItemStack output = pBuffer.readItem();
            int requiredHeat = pBuffer.readInt();

            ItemStack overheatedResult = pBuffer.readItem();
            int overheatedHeat = pBuffer.readInt(); // 🔹 Neu

            return new Realistic_Furnace_Recipe(inputs, output, pRecipeId, requiredHeat, overheatedResult, overheatedHeat);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, Realistic_Furnace_Recipe pRecipe) {
            pBuffer.writeInt(pRecipe.inputItems.size());
            for (Ingredient ingredient : pRecipe.getIngredients()) {
                ingredient.toNetwork(pBuffer);
            }

            pBuffer.writeItemStack(pRecipe.getResultItem(null), false);
            pBuffer.writeInt(pRecipe.getRequiredHeat());
            pBuffer.writeItemStack(Objects.requireNonNull(pRecipe.getOverheatedResult(null)), false);
            pBuffer.writeInt(pRecipe.getOverheatedHeat()); // 🔹 Neu
        }
    }
}