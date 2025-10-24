package com.fireblaze.realistic_furnace.blocks;

import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.blockentities.ModBlockEntities;
import com.fireblaze.realistic_furnace.client.FurnaceRenderState;
import com.fireblaze.realistic_furnace.containers.FurnaceContainer;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblock;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRenderer;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import java.util.List;

public class FurnaceControllerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FurnaceControllerBlock(Properties properties) {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction playerFacing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, playerFacing);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnaceControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.FURNACE_CONTROLLER.get()) return null;

        return (lvl, pos, s, be) -> {
            if (!lvl.isClientSide) {
                ((FurnaceControllerBlockEntity) be).tick();
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FurnaceControllerBlockEntity furnaceBE)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            List<OffsetBlock> missing = FurnaceMultiblockRenderer.getMissingBlocks(level, pos);
            if (missing.isEmpty()) {
                NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Realistic Furnace");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new FurnaceContainer(id, inv, furnaceBE);
                    }
                }, pos);
            }
            return InteractionResult.SUCCESS;
        }

        List<OffsetBlock> missing = FurnaceMultiblockRenderer.getMissingBlocks(level, pos);

        if (!missing.isEmpty()) {
            FurnaceRenderState.toggleGhostRendering();
            FurnaceRenderState.cycleMode();
            FurnaceControllerBlockEntity.GhostMode mode = FurnaceRenderState.getMode();

            boolean active = mode != FurnaceControllerBlockEntity.GhostMode.NONE;
            FurnaceRenderState.setGhostRendering(active);

            if (!active) {
                player.displayClientMessage(Component.literal("§cGhost Rendering Deactivated"), true);
            } else {
                player.displayClientMessage(Component.literal("§aGhost Rendering: " + formatString(mode.toString())), true);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }


    public static String formatString(String input) {
        if (input == null || input.isEmpty()) return input;

        // 1. Unterstriche durch Leerzeichen ersetzen
        input = input.replace('_', ' ');

        // 2. Alles in Kleinbuchstaben umwandeln
        input = input.toLowerCase();

        // 3. Jeden Wortanfang großschreiben
        StringBuilder formatted = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                formatted.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
        }

        return formatted.toString();
    }



    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof PickaxeItem;
    }


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FurnaceControllerBlockEntity furnaceBE) {
                for (int i = 0; i < furnaceBE.getItemHandler().getSlots(); i++) {
                    ItemStack stack = furnaceBE.getItemHandler().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FurnaceControllerBlock.LIT)) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z,
                    net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, 1.0F, false);
        }

        Direction direction = state.getValue(FurnaceControllerBlock.FACING);
        Direction.Axis axis = direction.getAxis();

        double offset = 0.52D;
        double dx = random.nextDouble() * 0.6D - 0.3D;
        double dy = random.nextDouble() * 6.0D / 16.0D;
        double dz = random.nextDouble() * 0.6D - 0.3D;

        switch (axis) {
            case X -> {
                level.addParticle(ParticleTypes.SMOKE, x + direction.getStepX() * offset, y + dy, z + dz, 0, 0, 0);
                level.addParticle(ParticleTypes.FLAME, x + direction.getStepX() * offset, y + dy, z + dz, 0, 0, 0);
            }
            case Z -> {
                level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + direction.getStepZ() * offset, 0, 0, 0);
                level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + direction.getStepZ() * offset, 0, 0, 0);
            }
        }
    }

}
