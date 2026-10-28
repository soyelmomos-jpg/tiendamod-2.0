package com.tiendamod.client;

import com.tiendamod.ShopEntry;
import com.tiendamod.SyncPacket;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Estado de la tienda en el cliente. Solo se carga en el lado cliente. */
public final class ClientHandler {
    public static long points = 0;
    public static List<ShopEntry> entries = List.of();

    private ClientHandler() {}

    public static void onSync(SyncPacket p) {
        points = p.points;
        if (p.mode >= SyncPacket.CATALOG) entries = p.entries;
        Minecraft mc = Minecraft.getInstance();
        if (p.mode == SyncPacket.OPEN) {
            mc.setScreen(new ShopScreen());
        } else if (mc.screen instanceof ShopScreen s) {
            s.refresh();
        }
    }
}
