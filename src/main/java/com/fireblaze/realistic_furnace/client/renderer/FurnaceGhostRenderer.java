package com.fireblaze.realistic_furnace.client.renderer;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.blockentities.FurnaceControllerBlockEntity;
import com.fireblaze.realistic_furnace.client.FurnaceRenderState;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRenderer;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static com.fireblaze.realistic_furnace.client.renderer.FurnaceGhostRenderer.ModRenderTypes.GHOST_LINES;

@Mod.EventBusSubscriber(modid = RealisticFurnace.MODID, value = Dist.CLIENT)
public class FurnaceGhostRenderer {

    private static final List<FurnaceControllerBlockEntity> ACTIVE_CONTROLLERS = new ArrayList<>();
    private static final java.util.Map<FurnaceControllerBlockEntity, Integer> ghostIndices = new java.util.HashMap<>();


    public static void register(FurnaceControllerBlockEntity be) {
        if (!ACTIVE_CONTROLLERS.contains(be)) {
            ACTIVE_CONTROLLERS.add(be);
        }
    }

    public static void unregister(FurnaceControllerBlockEntity be) {
        ACTIVE_CONTROLLERS.remove(be);
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (!FurnaceRenderState.isGhostRendering()) {
            return;
        }

        FurnaceControllerBlockEntity.GhostMode mode = FurnaceRenderState.getMode();


        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        for (FurnaceControllerBlockEntity controller : new ArrayList<>(ACTIVE_CONTROLLERS)) {

            if (mode == FurnaceControllerBlockEntity.GhostMode.BLOCK_BY_BLOCK) {
                if (!controller.shouldRenderGhost()) continue;
                if (controller.isRemoved() || controller.getLevel() != level) continue;

                // Ghost-State prüfen
                controller.updateGhostState(level);
                if (!controller.shouldRenderGhost()) continue;

                BlockPos origin = controller.getBlockPos();
                List<OffsetBlock> missing = FurnaceMultiblockRenderer.getMissingBlocks(level, origin);

                // Nur unvollständige Furnaces rendern
                if (missing.isEmpty()) continue;

                renderGhosts(event, missing, origin, level, controller);

            }
            else if (mode == FurnaceControllerBlockEntity.GhostMode.FULL_STRUCTURE) {
                if (controller.isRemoved() || controller.getLevel() != level) continue;

                BlockPos origin = controller.getBlockPos();
                List<OffsetBlock> missing = FurnaceMultiblockRenderer.getMissingBlocks(level, origin);

                // Nur rendern, wenn Struktur unvollständig ist
                if (missing.isEmpty()) continue;

                renderFullStructure(event, missing, origin, level);
            } else return;
        }
    }

    private static void renderGhosts(RenderLevelStageEvent event, List<OffsetBlock> missing, BlockPos origin, Level level, FurnaceControllerBlockEntity controller) {
        if (missing.isEmpty()) return;

        int index = ghostIndices.getOrDefault(controller, 0);
        if (index >= missing.size()) return;

        OffsetBlock nextGhost = missing.get(index);
        BlockPos pos = FurnaceMultiblockRenderer.rotateOffset(nextGhost, origin,
                level.getBlockState(origin).getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH));

        BlockState currentState = level.getBlockState(pos);
        BlockState expectedState = nextGhost.getStateTemplate();

        if (expectedState.getBlock() instanceof StairBlock) {
            expectedState = FurnaceMultiblockRenderer.rotateStair(expectedState,
                    level.getBlockState(origin).getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH));
        }

        boolean missingBlock = currentState.isAir();


        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();

        poseStack.pushPose();
        poseStack.translate(
                pos.getX() - camera.getPosition().x,
                pos.getY() - camera.getPosition().y,
                pos.getZ() - camera.getPosition().z
        );

        // Rendern des Ghost-Blocks normal
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        if (missingBlock) dispatcher.renderSingleBlock(expectedState, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
        buffer.endBatch(); // Standard-Batch flushen

        // Lines immer sichtbar durch Tesselator
        MultiBufferSource.BufferSource buffer2 = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(GHOST_LINES);
        float r = missingBlock ? 0f : 1f;      // Cyan = 0, Rot = 1
        float g = missingBlock ? 0.6f : 0f;
        float b = missingBlock ? 0.6f : 0f;
        float a = 0.5f;

        LevelRenderer.renderLineBox(poseStack, consumer, 0, 0, 0, 1, 1, 1, r, g, b, a);

        buffer2.endBatch(GHOST_LINES);


        poseStack.popPose();
    }

    private static void renderFullStructure(RenderLevelStageEvent event, List<OffsetBlock> missing, BlockPos origin, Level level) {
        if (missing.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();

        // Schleife über ALLE fehlenden Blöcke
        for (OffsetBlock offset : missing) {
            BlockPos pos = FurnaceMultiblockRenderer.rotateOffset(offset, origin,
                    level.getBlockState(origin).getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH));

            BlockState currentState = level.getBlockState(pos);
            BlockState expectedState = offset.getStateTemplate();

            if (expectedState.getBlock() instanceof StairBlock) {
                expectedState = FurnaceMultiblockRenderer.rotateStair(expectedState,
                        level.getBlockState(origin).getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(Direction.NORTH));
            }

            boolean missingBlock = currentState.isAir();

            if (!missingBlock) continue; // Nur fehlende Blöcke rendern

            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - camera.getPosition().x,
                    pos.getY() - camera.getPosition().y,
                    pos.getZ() - camera.getPosition().z
            );

            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
            dispatcher.renderSingleBlock(expectedState, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
            buffer.endBatch();

            poseStack.popPose();
        }
    }



    public static class ModRenderTypes extends RenderType {
        // Dummy-Konstruktor -> nur für create()
        private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufSize,
                               boolean affectsCrumbling, boolean sortOnUpload,
                               Runnable setup, Runnable clear) {
            super(name, format, mode, bufSize, affectsCrumbling, sortOnUpload, setup, clear);
        }

        public static final RenderType GHOST_LINES = RenderType.create(
                "ghost_lines",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.LINES,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.empty()))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        );

    }

}

