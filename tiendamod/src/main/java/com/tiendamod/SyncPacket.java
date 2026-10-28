package com.tiendamod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> cliente: puntos y (opcionalmente) catalogo, y (opcionalmente) abrir la pantalla. */
public class SyncPacket {
    public static final int POINTS = 0;   // solo puntos
    public static final int CATALOG = 1;  // puntos + catalogo
    public static final int OPEN = 2;     // puntos + catalogo + abrir pantalla

    public final int mode;
    public final long points;
    public final List<ShopEntry> entries;

    public SyncPacket(int mode, long points, List<ShopEntry> entries) {
        this.mode = mode;
        this.points = points;
        this.entries = entries;
    }

    public static void encode(SyncPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.mode);
        buf.writeLong(p.points);
        if (p.mode >= CATALOG) {
            buf.writeVarInt(p.entries.size());
            for (ShopEntry e : p.entries) {
                buf.writeUtf(e.id());
                buf.writeUtf(e.category());
                buf.writeVarInt(e.buy());
                buf.writeVarInt(e.sell());
            }
        }
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        int mode = buf.readVarInt();
        long points = buf.readLong();
        List<ShopEntry> list = new ArrayList<>();
        if (mode >= CATALOG) {
            int n = buf.readVarInt();
            for (int i = 0; i < n; i++) {
                list.add(new ShopEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
            }
        }
        return new SyncPacket(mode, points, list);
    }

    public static void handle(SyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.tiendamod.client.ClientHandler.onSync(p)));
        ctx.get().setPacketHandled(true);
    }
}
