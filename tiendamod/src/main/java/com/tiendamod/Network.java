package com.tiendamod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;

public final class Network {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TiendaMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Network() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CHANNEL.registerMessage(id++, TradePacket.class, TradePacket::encode, TradePacket::decode, TradePacket::handle);
    }

    public static void sendSync(ServerPlayer player, int mode) {
        List<ShopEntry> entries = mode >= SyncPacket.CATALOG ? ShopManager.all() : List.of();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncPacket(mode, PointsData.get(player), entries));
    }

    /** Reenvia el catalogo a todos los jugadores (despues de cambiar precios). */
    public static void broadcastCatalog(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendSync(p, SyncPacket.CATALOG);
    }
}
