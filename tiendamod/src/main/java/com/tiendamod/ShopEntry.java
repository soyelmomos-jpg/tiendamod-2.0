package com.tiendamod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/** Un item de la tienda: precio de compra y de venta (en puntos, por unidad). */
public record ShopEntry(String id, String category, int buy, int sell) {

    public Item item() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
    }
}
