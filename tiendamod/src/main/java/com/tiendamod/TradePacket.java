package com.tiendamod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: quiero comprar o vender qty unidades de un item. */
public class TradePacket {
    private final String id;
    private final int qty;
    private final boolean sell;

    public TradePacket(String id, int qty, boolean sell) {
        this.id = id;
        this.qty = qty;
        this.sell = sell;
    }

    public static void encode(TradePacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.id);
        buf.writeVarInt(p.qty);
        buf.writeBoolean(p.sell);
    }

    public static TradePacket decode(FriendlyByteBuf buf) {
        return new TradePacket(buf.readUtf(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(TradePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) TradeHandler.execute(sender, p.id, p.qty, p.sell);
        });
        ctx.get().setPacketHandled(true);
    }
}
