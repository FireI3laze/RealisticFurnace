package com.fireblaze.realistic_furnace.containers;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.recipe.Realistic_Furnace_Recipe;
import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FurnaceContainer extends AbstractContainerMenu {

    private final FurnaceControllerBlockEntity blockEntity;
    private final IItemHandler itemHandler;

    // getrennte DataSets
    private final ContainerData progressData;
    private final ContainerData heatData;
    private final ContainerData burnTimeData;
    private final List<Realistic_Furnace_Recipe> recipes;

    public FurnaceContainer(int id, Inventory playerInventory, FurnaceControllerBlockEntity blockEntity) {
        super(ModMenuTypes.FURNACE.get(), id);
        this.blockEntity = blockEntity;
        this.itemHandler = blockEntity.getItemHandler();

        if (!playerInventory.player.level().isClientSide) {
            RecipeManager recipeManager = playerInventory.player.level().getRecipeManager();
            this.recipes = recipeManager.getAllRecipesFor(Realistic_Furnace_Recipe.Type.INSTANCE);
        } else {
            this.recipes = Collections.emptyList();
        }

        // === Progress-Daten (pro Slot) ===
        this.progressData = new ContainerData() {
            @Override
            public int get(int index) {
                int slot = index / 2;
                int type = index % 2;

                if (slot >= 0 && slot < blockEntity.getProgress().length) {
                    return switch (type) {
                        case 0 -> (int) (blockEntity.getProgress()[slot] * 10); // Fortschritt
                        case 1 -> blockEntity.isStalled(slot) ? 1 : 0;          // stalled
                        default -> 0;
                    };
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                int slot = index / 2;
                int type = index % 2;

                if (slot >= 0 && slot < blockEntity.getProgress().length) {
                    if (type == 0) {
                        blockEntity.getProgress()[slot] = value / 10f;
                    } else if (type == 1) {
                        blockEntity.setStalled(slot, value != 0);
                    }
                }
            }

            @Override
            public int getCount() {
                return blockEntity.getProgress().length * 2;
            }
        };

        // === Heat-Daten (vom BlockEntity direkt) ===
        this.heatData = blockEntity.getHeatData();
        this.burnTimeData = blockEntity.getBurnTimeData();

        // ContainerData registrieren
        this.addDataSlots(progressData);
        this.addDataSlots(heatData);
        this.addDataSlots(burnTimeData);

        // Slots
        this.addSlot(new SlotItemHandler(itemHandler, 0, 46, 21));
        this.addSlot(new SlotItemHandler(itemHandler, 1, 46, 48));
        this.addSlot(new SlotItemHandler(itemHandler, 2, 68, 21));
        this.addSlot(new SlotItemHandler(itemHandler, 3, 68, 48));
        this.addSlot(new SlotItemHandler(itemHandler, 4, 92, 21));
        this.addSlot(new SlotItemHandler(itemHandler, 5, 92, 48));
        this.addSlot(new SlotItemHandler(itemHandler, 6, 114, 21));
        this.addSlot(new SlotItemHandler(itemHandler, 7, 114, 48));
        this.addSlot(new SlotItemHandler(itemHandler, 8, 15, 34));

        // Player Inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        // Hotbar
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    public FurnaceContainer(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory,
                (FurnaceControllerBlockEntity) Objects.requireNonNull(playerInventory.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = getSlot(index).getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack copy = stack.copy();
        boolean moved = false;

        if (index < 9) { // Container -> Inventory
            if (!moveItemStackTo(stack, 9, 36, true)) return ItemStack.EMPTY;
        } else { // Inventory -> Container
            // Prüfe, ob Item zu einem Recipe passt
            for (Realistic_Furnace_Recipe recipe : recipes) {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.test(stack)) {
                        moved = moveItemStackTo(stack, 0, 8, false); // Input-Slots
                        break;
                    }
                }
                if (moved) break;
            }

            // Prüfe Fuel
            if (!moved && FurnaceFuelRegistry.isFuel(stack)) {
                moved = moveItemStackTo(stack, 8, 9, false); // Fuel-Slot
            }

            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) getSlot(index).set(ItemStack.EMPTY);
        else getSlot(index).setChanged();

        return copy;
    }





    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved();
    }

    public FurnaceControllerBlockEntity getBlockEntity() { return blockEntity; }
    public ContainerData getProgressData() { return progressData; }
    public ContainerData getHeatData() { return heatData; }
    public ContainerData getBurnTimeData() { return burnTimeData; }
}
