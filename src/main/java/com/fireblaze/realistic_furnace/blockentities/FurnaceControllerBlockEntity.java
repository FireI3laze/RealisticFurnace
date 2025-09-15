package com.fireblaze.realistic_furnace.blockentities;

import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblock;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRenderer;
import com.fireblaze.realistic_furnace.recipe.Realistic_Furnace_Recipe;
import com.fireblaze.realistic_furnace.recipes.FurnaceRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.List;
import java.util.Optional;

public class FurnaceControllerBlockEntity extends BlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(9) { // 0-7: input, 8: fuel
        @Override
        public int getSlotLimit(int slot) {
            if (slot >= 0 && slot <= 7) return 1; // Input-Slots
            if (slot == 8) return 8; // Fuel-Slot
            return super.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 8) return FurnaceFuelRegistry.isFuel(stack); // Fuel-Slot nur Fuel
            return true; // Input-Slots alles erlaubt
        }
    };

    private float heat = 0f;
    private int maxHeat = 1800;
    private final float[] progress = new float[8];
    private final int maxProgress = 1000;
    private int burnTime = 0;
    private int burnTimeTotal = 0;
    private ItemStack activeFuel = ItemStack.EMPTY;

    private static final float HEAT_DECREASE_BASE = 0.15f;
    private static final int MAX_HEAT = 1800;
    private static final int THREATENING_HEAT = 1650;
    private static final int MIN_HEAT = 0;

    private static final float DOOR_OPEN_MULTIPLIER = 20f;
    private static final float DOOR_CLOSED_MULTIPLIER = 12f;
    private static final int SCATTER_CHANCE = 75;

    private int tickCounter = 0;
    public final static int tickInterval = 5;


    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;


    private final ContainerData heatData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) heat;
                case 1 -> MAX_HEAT;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> heat = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private final ContainerData progressData = new ContainerData() {
        @Override
        public int get(int index) {
            int slot = index / 2;
            boolean isStalled = stalled[slot];
            if (index % 2 == 0) {
                return (int) (progress[slot] * 100); // Fortschritt
            } else {
                return isStalled ? 1 : 0; // 1 = rot, 0 = normal
            }
        }

        @Override
        public void set(int index, int value) {
            int slot = index / 2;
            if (index % 2 == 0) {
                progress[slot] = value / 100f; // Fortschritt
            } else {
                stalled[slot] = value != 0; // boolean zurücksetzen
            }
        }

        @Override
        public int getCount() {
            return progress.length * 2; // für jeden Slot 2 Werte: Fortschritt + stalled
        }
    };


    private final ContainerData burnTimeData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnTimeTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };


    public FurnaceControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_CONTROLLER.get(), pos, state);
    }

    public ContainerData getProgressData() {
        return progressData;
    }

    public ContainerData getHeatData() {
        return heatData;
    }

    public ContainerData getBurnTimeData() {
        return burnTimeData;
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }
    public float getHeat() { return heat; }
    public int getMaxHeat() { return MAX_HEAT; }
    public static int getThreateningHeat() { return THREATENING_HEAT; }
    public float[] getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }

    // Am Ende der Klasse, direkt über tickClient() oder processItems():

    // --- Stalled-Zugriff für Container ---
    public boolean isStalled(int slot) {
        if (slot < 0 || slot >= stalled.length) return false;
        return stalled[slot];
    }

    public void setStalled(int slot, boolean value) {
        if (slot < 0 || slot >= stalled.length) return;
        stalled[slot] = value;
    }


    public void setHeat(float heat) {
        this.heat = Mth.clamp(heat, MIN_HEAT, MAX_HEAT);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.of(() -> itemHandler).cast();
        }
        return super.getCapability(cap, side);
    }

    private int ticksRunning = 0;

    public void tick() {
        if (level == null || level.isClientSide) return;

        // Multiblock prüfen
        if (!FurnaceMultiblock.validateStructure(level, worldPosition)) {
            heat = 0;
            setChanged();
            return;
        }

        tickCounter++;
        if ((tickCounter % tickInterval) != 0) return;

        ItemStack fuel = itemHandler.getStackInSlot(8);

        if (fuel.isEmpty() && heat <= 50) {
            heat = 0;
            return;
        }

        boolean isBurning = false;

        // Fuel starten
        if (burnTime <= 0 && !fuel.isEmpty() && FurnaceFuelRegistry.isFuel(fuel)) {
            burnTime = FurnaceFuelRegistry.getBurnTime(fuel);
            burnTimeTotal = burnTime;
            activeFuel = fuel.copy();
            fuel.shrink(1);
            itemHandler.setStackInSlot(8, fuel);
        }

        if (burnTime > 0) {
            burnTime -= tickInterval;
            isBurning = true;
        }

        boolean doorClosed = checkDoorClosed();

        float oldHeat = heat;

        // Heat Management
        if (doorClosed) {
            if (isBurning) {
                heat += heatIncreaseCalculation(DOOR_CLOSED_MULTIPLIER, activeFuel) + heatDecreaseCalculation(1) * tickInterval;
            } else {
                heat += heatDecreaseCalculation(1) * tickInterval;
            }
        } else {
            if (isBurning) {
                heat += heatIncreaseCalculation(1, activeFuel) + heatDecreaseCalculation(DOOR_OPEN_MULTIPLIER) * tickInterval;
            } else {
                heat += heatDecreaseCalculation(DOOR_OPEN_MULTIPLIER) * tickInterval;
            }
        }

        heat = Mth.clamp(heat, MIN_HEAT, MAX_HEAT);
        // System.out.println(heat);
        processItems();

        if (heat >= THREATENING_HEAT) {
            ticksRunning++;

            // Chance steigt mit der Zeit
            float baseChance = 0.0003f; // Grundchance pro Tick
            float chance = baseChance * ticksRunning;
            System.out.println("in explosion threshold! Chance: " + chance);

            // Explosionstrigger
            if (level.random.nextFloat() < chance) {
                ArmorStand dummy = EntityType.ARMOR_STAND.create(level);
                if (dummy != null) {
                    dummy.setInvisible(true);
                    dummy.setInvulnerable(true);
                    dummy.setCustomName(Component.literal("Furnace"));
                    dummy.setCustomNameVisible(false);
                    dummy.moveTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);

                    level.addFreshEntity(dummy);

                    level.explode(
                            dummy,
                            worldPosition.offset(0, 0, 0).getX(),
                            worldPosition.offset(0, 1, 0).getY(),
                            worldPosition.offset(0, 0, 1).getZ(),
                            4.0f,
                            Level.ExplosionInteraction.BLOCK
                    );

                    dummy.discard();
                }

                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    ItemStack stack = itemHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStack drop = stack.copy();
                        itemHandler.setStackInSlot(i, ItemStack.EMPTY);

                        double dx = worldPosition.getX() + 0.5;
                        double dy = worldPosition.getY() + 0.5;
                        double dz = worldPosition.getZ() + 0.5;
                        ItemEntity entity = new ItemEntity(level, dx, dy, dz, drop);

                        if (level.random.nextInt(100) < SCATTER_CHANCE) {
                            double spread = 4; // Stärke, wie weit die Items fliegen können
                            double motionX = (level.random.nextDouble() - 0.5) * spread * 2;
                            double motionY = level.random.nextDouble() * 0.5 + 0.2;
                            double motionZ = (level.random.nextDouble() - 0.5) * spread * 2;

                            entity.setDeltaMovement(motionX, motionY, motionZ);
                        }
                        level.addFreshEntity(entity);
                    }
                }

                ticksRunning = 0;
            }
        }
        else ticksRunning = 0;

        if (heat != oldHeat || burnTime > 0) {
            setChanged();
        }

    }

    private float heatIncreaseCalculation(float multiplier, ItemStack fuel) {
        float normalized = (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
        float exponent = 1.1f;
        float factor = (float) Math.pow(1.0f - normalized, exponent);
        return FurnaceFuelRegistry.getHeatStrength(fuel) * factor * multiplier;
    }

    private float heatDecreaseCalculation(float multiplier) {
        float factor = (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
        return -HEAT_DECREASE_BASE * factor * multiplier;
    }

    private boolean checkDoorClosed() {
        BlockPos doorPos = worldPosition.offset(0, 3, 1);
        assert level != null;
        Block block = level.getBlockState(doorPos).getBlock();
        return block instanceof TrapDoorBlock && !level.getBlockState(doorPos).getValue(DoorBlock.OPEN);
    }

    private final ItemStack[] lastItems = new ItemStack[8];
    private final boolean[] stalled = new boolean[8];

    private void processItems() {
        for (int i = 0; i < 8; i++) {
            ItemStack input = itemHandler.getStackInSlot(i);

            // Initialisierung: falls noch null, setzen wir einen leeren Stack
            if (lastItems[i] == null) lastItems[i] = ItemStack.EMPTY;

            // Fortschritt zurücksetzen, wenn Slot leer oder Item gewechselt
            if (!ItemStack.matches(input, lastItems[i])) {
                progress[i] = 0;
                lastItems[i] = input.copy();
                continue;
            }

            if (input.isEmpty()) {
                progress[i] = 0;
                continue;
            }

            for (var recipe : FurnaceRecipes.RECIPES) { // uses old recipe code
                if (recipe.matches(input)) { // uses old recipe code
                    float percentageOfRequiredHeat = heat / recipe.getRequiredHeat();
                    float reachableProgress = maxProgress * percentageOfRequiredHeat;

                    if (progress[i] < reachableProgress && percentageOfRequiredHeat >= 0.8) {
                        progress[i] += 1 * percentageOfRequiredHeat * tickInterval;
                        stalled[i] = false;
                    } else {
                        if (progress[i] > 0) {
                            if (progress[i] - 0.5f * tickInterval < reachableProgress)
                                progress[i] = reachableProgress;
                            else
                                progress[i] -= 0.5f * tickInterval;

                            stalled[i] = true;
                            break;
                        }
                    }

                    progress[i] = Math.max(0, Math.min(progress[i], maxProgress));

                    if (progress[i] >= maxProgress) {
                        itemHandler.extractItem(i, 1, false);
                        itemHandler.insertItem(i, recipe.process(), false);
                        progress[i] = 0;
                        stalled[i] = false;
                    }
                    break;
                }
                //System.out.println("Stalled id [" + i + "] on Server: " + stalled[i]);
            }
        }
    }

    public void tickClient() {
        if (level == null || !level.isClientSide) return;

        List<BlockPos> missing = FurnaceMultiblockRenderer.getMissingBlocks(level, worldPosition);

        for (BlockPos pos : missing) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    0, 0.05, 0);
        }
    }

    private void smeltItem(int slot) {
        Optional<Realistic_Furnace_Recipe> recipe = getCurrentRecipe();
        ItemStack result = recipe.get().getResultItem(null);

        this.itemHandler.extractItem(slot, 1, false);

        this.itemHandler.setStackInSlot(slot, new ItemStack(result.getItem(),
                this.itemHandler.getStackInSlot(slot).getCount() + result.getCount()));
    }

    private boolean hasRecipe() {
        Optional<Realistic_Furnace_Recipe> recipe = getCurrentRecipe();

        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().getResultItem(null);

        return canInsertAmountIntoOutputSlot(result.getCount()) && canInsertItemIntoOutputSlot(result.getItem());
    }

    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    private Optional<Realistic_Furnace_Recipe> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());
        for (int i = 0; i< itemHandler.getSlots(); i++) {
            inventory.setItem(i, this.itemHandler.getStackInSlot(i));
        }

        return this.level.getRecipeManager().getRecipeFor(Realistic_Furnace_Recipe.Type.INSTANCE, inventory, level);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);

        // NBT: Items speichern
        tag.put("Items", itemHandler.serializeNBT());

        // NBT: Heat speichern
        tag.putFloat("Heat", heat);
        tag.putInt("MaxHeat", MAX_HEAT);

        // NBT: BurnTime speichern
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);

        // NBT: Active Fuel speichern
        tag.put("ActiveFuel", activeFuel.save(new net.minecraft.nbt.CompoundTag()));

        // NBT: Progress Array speichern
        for (int i = 0; i < progress.length; i++) {
            tag.putFloat("Progress" + i, progress[i]);
            tag.putBoolean("Stalled" + i, stalled[i]);
        }
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);

        // NBT: Items laden
        itemHandler.deserializeNBT(tag.getCompound("Items"));

        // NBT: Heat laden
        heat = tag.getFloat("Heat");

        // NBT: BurnTime laden
        burnTime = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");

        // NBT: Active Fuel laden
        activeFuel = ItemStack.of(tag.getCompound("ActiveFuel"));

        // NBT: Progress Array laden
        for (int i = 0; i < progress.length; i++) {
            progress[i] = tag.getFloat("Progress" + i);
            stalled[i] = tag.getBoolean("Stalled" + i);
        }
    }

// Wichtig: setChanged() aufrufen, wenn sich Heat/BurnTime/Progress ändern, z.B. in tick()

}
