package com.fireblaze.realistic_furnace.blockentities;

import com.fireblaze.realistic_furnace.blocks.FurnaceControllerBlock;
import com.fireblaze.realistic_furnace.client.FurnaceRenderState;
import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import com.fireblaze.realistic_furnace.multiblock.*;
import com.fireblaze.realistic_furnace.recipe.Realistic_Furnace_Recipe;
import com.fireblaze.realistic_furnace.client.renderer.FurnaceGhostRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRenderer.rotateOffset;

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

    private static OffsetBlock valveOffset;

    public static void setValveOffset(OffsetBlock vent) {
        valveOffset = vent;
    }

    public static OffsetBlock getValveOffset() {
        return valveOffset;
    }

    private boolean showGhost = true;
    private String selectedMultiblockName = "default_furnace";


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

    public boolean shouldRenderGhost() {
        return showGhost;
    }
    public void updateGhostState(Level level) {
        if (level == null || level.isClientSide) return;

        List<OffsetBlock> missing = FurnaceMultiblockRenderer.getMissingBlocks(level, this.worldPosition);
        showGhost = !missing.isEmpty();
    }
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            FurnaceGhostRenderer.register(this);
        }
    }

    /** BE deregistriert sich beim Entfernen */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level.isClientSide) {
            FurnaceGhostRenderer.unregister(this);
        }
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
    public void setSelectedMultiblock(String name) {
        this.selectedMultiblockName = name;
        setChanged();
    }

    public String getSelectedMultiblock() {
        return selectedMultiblockName;
    }


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

        List<OffsetBlock> missingOrMisaligned = FurnaceMultiblockRenderer.getMissingBlocks(level, worldPosition);
        if (!missingOrMisaligned.isEmpty()) {
            heat = 0;
            setChanged();
            turnFurnaceOff();
            return;
        }

        tickCounter++;
        if ((tickCounter % tickInterval) != 0) return;

        ItemStack fuel = itemHandler.getStackInSlot(8);

        boolean isBurning = false;

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

        if (!isBurning && heat <= 50) {
            heat = 0;
            toggleLid(false);
            setCampfireLit(false);
            return;
        }

        heat = calculateNewHeat(heat, isBurning);
        if (heat >= THREATENING_HEAT) {
            if (doesExplode()) {
                spawnExplosionCause();
                causeExplosion();
                turnFurnaceOff();
            }
        } else ticksRunning = 0;

        processItems();
        setChanged();
        toggleLid(true);
        setCampfireLit(true);
    }

    private void turnFurnaceOff() {
        heat = 0;
        ticksRunning = 0;
        resetFuel();
        toggleLid(false);
        setCampfireLit(false);
    }

    private void toggleLid(boolean lightUp) {
        boolean wasLit = this.getBlockState().getValue(FurnaceControllerBlock.LIT);
        boolean isLit = lightUp;

        if (wasLit != isLit) {
            this.level.setBlock(this.worldPosition,
                    this.getBlockState().setValue(FurnaceControllerBlock.LIT, isLit), 3);
        }
    }

    private boolean doesExplode() {
        float baseChance = 0.0002f; // Base Chance Per Tick
        ticksRunning++;
        float chance = baseChance * ticksRunning;

        // Explosionstrigger
        if (level.random.nextFloat() < chance) return true;
        else return false;
    }

    private void spawnExplosionCause() {
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
                    worldPosition.offset(0, 2, 0).getY(),
                    worldPosition.offset(0, 0, 1).getZ(),
                    6.0f,
                    Level.ExplosionInteraction.BLOCK
            );

            dummy.discard();
        }
    }

    private void causeExplosion() {
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
                    double spread = 8;
                    double motionX = (level.random.nextDouble() - 0.5) * spread * 2;
                    double motionY = level.random.nextDouble() * 0.5 + 0.2;
                    double motionZ = (level.random.nextDouble() - 0.5) * spread * 2;

                    entity.setDeltaMovement(motionX, motionY, motionZ);
                }
                level.addFreshEntity(entity);
            }
        }
    }

    private float calculateNewHeat(float heat, boolean isBurning) {
        if (checkDoorClosed()) {
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

        return Mth.clamp(heat, MIN_HEAT, MAX_HEAT);
    }

    private float heatIncreaseCalculation(float multiplier, ItemStack fuel) {
        float normalized = (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
        float exponent = 1.0f; //1.1
        float factor = (float) Math.pow(1.0f - normalized, exponent);
        return FurnaceFuelRegistry.getHeatStrength(fuel) * factor * multiplier;
    }

    private float heatDecreaseCalculation(float multiplier) {
        float factor = (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
        return -HEAT_DECREASE_BASE * factor * multiplier;
    }

    private boolean checkDoorClosed() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        BlockPos temp = rotateOffset(valveOffset, worldPosition, facing);
        assert level != null;

        //System.out.println(valveOffset.x() +  " " + valveOffset.y() +  " " + valveOffset.z());

        BlockState doorState = level.getBlockState(temp);
        return doorState.getBlock() instanceof TrapDoorBlock && !doorState.getValue(DoorBlock.OPEN);
    }

    private void setCampfireLit(boolean lit) {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;

        BlockPos campfirePos = rotateOffset(
                new OffsetBlock(0, 0, 1, List.of(Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE), state),
                worldPosition,
                facing
        );

        assert level != null;
        BlockState campfireState = level.getBlockState(campfirePos);

        if (campfireState.getBlock() instanceof CampfireBlock) {
            boolean currentlyLit = campfireState.getValue(CampfireBlock.LIT);
            if (currentlyLit != lit) {
                level.setBlock(campfirePos, campfireState.setValue(CampfireBlock.LIT, lit), 3);

                // Optional Sound/Partikel
                /*
                if (lit) {
                    level.playSound(null, campfirePos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    level.playSound(null, campfirePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                 */
            }
        }
    }


    private final ItemStack[] lastItems = new ItemStack[8];
    private final boolean[] stalled = new boolean[8];

    private void processItems() {
        for (int i = 0; i < 8; i++) {
            ItemStack input = itemHandler.getStackInSlot(i);

            if (lastItems[i] == null) lastItems[i] = ItemStack.EMPTY;

            if (!ItemStack.matches(input, lastItems[i])) {
                progress[i] = 0;
                lastItems[i] = input.copy();
                continue;
            }

            if (input.isEmpty()) {
                progress[i] = 0;
                continue;
            }

            Optional<Realistic_Furnace_Recipe> recipeOpt = getRecipeForSlot(input);
            if (recipeOpt.isEmpty()) {
                progress[i] = 0;
                stalled[i] = true;
                continue;
            }

            Realistic_Furnace_Recipe recipe = recipeOpt.get();

            float percentageOfRequiredHeat = heat / recipe.getRequiredHeat();
            float reachableProgress = maxProgress * percentageOfRequiredHeat;

            if (percentageOfRequiredHeat >= 1.5f && progress[i] >= (float) maxProgress / 2) {
                itemHandler.extractItem(i, 1, false);

                ItemStack overheated = recipe.getOverheatedResult(level.registryAccess());
                if (overheated != null && !overheated.isEmpty()) {
                    itemHandler.insertItem(i, overheated.copy(), false);
                }

                progress[i] = 0;
                stalled[i] = true;
                continue;
            }



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
                    continue;
                }
            }

            progress[i] = Math.max(0, Math.min(progress[i], maxProgress));

            if (progress[i] >= maxProgress) {
                itemHandler.extractItem(i, 1, false);
                assert level != null;
                itemHandler.insertItem(i, recipe.getResultItem(level.registryAccess()).copy(), false);
                progress[i] = 0;
                stalled[i] = false;
            }
        }
    }

    private Optional<Realistic_Furnace_Recipe> getRecipeForSlot(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();

        // Container nur für dieses Item
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, stack);

        return level.getRecipeManager().getRecipeFor(Realistic_Furnace_Recipe.Type.INSTANCE, container, level);
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


    private void resetFuel() {
        this.burnTime = 0;
        this.burnTimeTotal = 0;
        this.activeFuel = ItemStack.EMPTY;

        this.setChanged();
    }

    private Optional<Realistic_Furnace_Recipe> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());
        for (int i = 0; i< itemHandler.getSlots(); i++) {
            inventory.setItem(i, this.itemHandler.getStackInSlot(i));
        }

        return this.level.getRecipeManager().getRecipeFor(Realistic_Furnace_Recipe.Type.INSTANCE, inventory, level);
    }

    private GhostMode ghostMode = GhostMode.BLOCK_BY_BLOCK;

    public void toggleGhostMode() {
        ghostMode = ghostMode.next();
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public GhostMode getGhostMode() {
        return ghostMode;
    }

    public enum GhostMode {
        BLOCK_BY_BLOCK,
        FULL_STRUCTURE,
        NONE;

        public GhostMode next() {
            return switch (this) {
                case BLOCK_BY_BLOCK -> FULL_STRUCTURE;
                case FULL_STRUCTURE -> NONE;
                case NONE -> BLOCK_BY_BLOCK;
            };
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", itemHandler.serializeNBT());
        tag.putFloat("Heat", heat);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
        tag.put("ActiveFuel", activeFuel.save(new CompoundTag()));

        for (int i = 0; i < progress.length; i++) {
            tag.putFloat("Progress" + i, progress[i]);
            tag.putBoolean("Stalled" + i, stalled[i]);
        }

        tag.putString("GhostMode", ghostMode.name());
        tag.putString("SelectedMultiblock", selectedMultiblockName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Items"));
        heat = tag.getFloat("Heat");
        burnTime = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");
        activeFuel = ItemStack.of(tag.getCompound("ActiveFuel"));

        for (int i = 0; i < progress.length; i++) {
            progress[i] = tag.getFloat("Progress" + i);
            stalled[i] = tag.getBoolean("Stalled" + i);
        }

        ghostMode = GhostMode.valueOf(tag.getString("GhostMode"));
        selectedMultiblockName = tag.getString("SelectedMultiblock");
    }



    // --- Client Sync (Server -> Client Update) --- //

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag); // NBT laden

        // Ghosts nur berechnen, wenn Client
        if (level != null && level.isClientSide) {
            // neu registrieren oder fehlende Blöcke aktualisieren
            FurnaceGhostRenderer.register(this);

            // sofort fehlende Blöcke prüfen
            List<OffsetBlock> missing = FurnaceMultiblockRenderer.getMissingBlocks(level, worldPosition);
            boolean active = !missing.isEmpty();
            FurnaceRenderState.setGhostRendering(active);
        }
    }



    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }


// Wichtig: setChanged() aufrufen, wenn sich Heat/BurnTime/Progress ändern, z.B. in tick()

}
