package com.tiendamod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Logica de compra/venta. Siempre se ejecuta en el servidor. */
public final class TradeHandler {
    private TradeHandler() {}

    public static void execute(ServerPlayer p, String id, int qty, boolean sell) {
        if (qty != 1 && qty != 10 && qty != 50) return;
        if (!hasShopItem(p)) {
            msg(p, "Necesitas tener la Tienda en el inventario.", ChatFormatting.RED);
            return;
        }
        ShopEntry e = ShopManager.get(id);
        if (e == null || e.item() == null) {
            msg(p, "Ese item ya no esta en la tienda.", ChatFormatting.RED);
            Network.sendSync(p, SyncPacket.CATALOG);
            return;
        }
        Item item = e.item();
        String name = new ItemStack(item).getHoverName().getString();

        if (sell) {
            int removed = removeItems(p, item, qty);
            if (removed == 0) {
                msg(p, "No tenes " + name + " (sin dano, nombre ni encantamientos).", ChatFormatting.RED);
                return;
            }
            long gain = (long) removed * e.sell();
            PointsData.add(p, gain);
            msg(p, "Vendiste " + removed + "x " + name + " por " + gain + " puntos.", ChatFormatting.GREEN);
        } else {
            long cost = (long) e.buy() * qty;
            long pts = PointsData.get(p);
            if (pts < cost) {
                msg(p, "Te faltan " + (cost - pts) + " puntos.", ChatFormatting.RED);
                return;
            }
            PointsData.set(p, pts - cost);
            giveItems(p, item, qty);
            msg(p, "Compraste " + qty + "x " + name + " por " + cost + " puntos.", ChatFormatting.GREEN);
        }
        Network.sendSync(p, SyncPacket.POINTS);
    }

    private static boolean hasShopItem(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (ItemStack s : inv.items) if (s.is(TiendaMod.TIENDA.get())) return true;
        for (ItemStack s : inv.offhand) if (s.is(TiendaMod.TIENDA.get())) return true;
        return false;
    }

    private static int removeItems(ServerPlayer p, Item item, int qty) {
        int remaining = qty;
        Inventory inv = p.getInventory();
        for (ItemStack s : inv.items) {
            if (remaining <= 0) break;
            if (s.is(item) && !s.hasTag()) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
        for (ItemStack s : inv.offhand) {
            if (remaining <= 0) break;
            if (s.is(item) && !s.hasTag()) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
        inv.setChanged();
        p.containerMenu.broadcastChanges();
        return qty - remaining;
    }

    private static void giveItems(ServerPlayer p, Item item, int qty) {
        int remaining = qty;
        int max = Math.max(1, item.getMaxStackSize());
        while (remaining > 0) {
            int n = Math.min(max, remaining);
            ItemStack stack = new ItemStack(item, n);
            if (!p.getInventory().add(stack) && !stack.isEmpty()) {
                p.drop(stack, false); // inventario lleno: cae al piso
            }
            remaining -= n;
        }
        p.containerMenu.broadcastChanges();
    }

    private static void msg(ServerPlayer p, String text, ChatFormatting color) {
        p.displayClientMessage(Component.literal(text).withStyle(color), true);
    }
}
