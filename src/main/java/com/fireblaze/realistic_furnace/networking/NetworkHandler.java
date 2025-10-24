package com.fireblaze.realistic_furnace.networking;

import com.fireblaze.realistic_furnace.RealisticFurnace;
import com.fireblaze.realistic_furnace.networking.packet.SyncMultiblockDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(RealisticFurnace.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int nextId() { return packetId++; }

    /**
     * Registriert alle Netzwerkpakete, die Server und Client austauschen können.
     * Diese Methode muss einmalig beim Mod-Init aufgerufen werden.
     */
    public static void register() {
        CHANNEL.registerMessage(
                nextId(),
                SyncMultiblockDataPacket.class,
                SyncMultiblockDataPacket::encode,
                SyncMultiblockDataPacket::decode,
                SyncMultiblockDataPacket::handle
        );
    }
}
