package com.fireblaze.realistic_furnace.networking.packet;

import com.fireblaze.realistic_furnace.client.ClientPacketHandler;
import com.fireblaze.realistic_furnace.multiblock.FurnaceMultiblockRegistry;
import com.fireblaze.realistic_furnace.multiblock.OffsetBlock;
import com.fireblaze.realistic_furnace.multiblock.SerializableOffsetBlock;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class SyncMultiblockDataPacket {
    public final String name;
    public final List<SerializableOffsetBlock> blocks;
    public final SerializableOffsetBlock valve;

    public SyncMultiblockDataPacket(String name, List<SerializableOffsetBlock> blocks, SerializableOffsetBlock valve) {
        this.name = name;
        this.blocks = blocks;
        this.valve = valve;
    }

    public static void encode(SyncMultiblockDataPacket msg, FriendlyByteBuf buf) {
        // Name zuerst
        buf.writeUtf(msg.name);

        // Anzahl der Blöcke
        buf.writeInt(msg.blocks.size());
        for (SerializableOffsetBlock b : msg.blocks) {
            buf.writeInt(b.x);
            buf.writeInt(b.y);
            buf.writeInt(b.z);
            buf.writeUtf(b.blockId != null ? b.blockId : "minecraft:air");
            buf.writeInt(b.blockStateProperties.size());
            for (Map.Entry<String, String> entry : b.blockStateProperties.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeUtf(entry.getValue());
            }
        }

        // Valve
        boolean hasValve = msg.valve != null;
        buf.writeBoolean(hasValve);
        if (hasValve) {
            buf.writeInt(msg.valve.x);
            buf.writeInt(msg.valve.y);
            buf.writeInt(msg.valve.z);
            buf.writeUtf(msg.valve.blockId != null ? msg.valve.blockId : "minecraft:air");
            buf.writeInt(msg.valve.blockStateProperties.size());
            for (Map.Entry<String, String> entry : msg.valve.blockStateProperties.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeUtf(entry.getValue());
            }
        }
    }


    public static SyncMultiblockDataPacket decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        int size = buf.readInt();
        List<SerializableOffsetBlock> blocks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            String blockId = buf.readUtf();
            int propCount = buf.readInt();
            Map<String, String> props = new java.util.HashMap<>();
            for (int j = 0; j < propCount; j++) {
                String key = buf.readUtf();
                String value = buf.readUtf();
                props.put(key, value);
            }

            SerializableOffsetBlock block = new SerializableOffsetBlock();
            block.x = x;
            block.y = y;
            block.z = z;
            block.blockId = blockId;
            block.blockStateProperties = props;

            blocks.add(block);
        }

        SerializableOffsetBlock valve = null;
        if (buf.readBoolean()) {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            String valveId = buf.readUtf();
            int valvePropCount = buf.readInt();
            Map<String, String> valveProps = new java.util.HashMap<>();
            for (int j = 0; j < valvePropCount; j++) {
                String key = buf.readUtf();
                String value = buf.readUtf();
                valveProps.put(key, value);
            }
            valve = new SerializableOffsetBlock();
            valve.x = x;
            valve.y = y;
            valve.z = z;
            valve.blockId = valveId;
            valve.blockStateProperties = valveProps;
        }

        return new SyncMultiblockDataPacket(name, blocks, valve);
    }



    public static void handle(SyncMultiblockDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Server-sided logic
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                Level world = Objects.requireNonNull(ctx.get().getSender()).level();
                List<OffsetBlock> blocks = msg.blocks.stream().map(SerializableOffsetBlock::toOffsetBlock).toList();
                FurnaceMultiblockRegistry.selectMultiblock(world, msg.name, blocks);

                if (msg.valve != null) {
                    FurnaceMultiblockRegistry.setValve(world, msg.valve.toOffsetBlock());
                }
            }

            // Client-sided logic
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandler.handleMultiblockData(msg)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }




}
