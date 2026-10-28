package com.tiendamod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Guarda los puntos dentro de los datos persistentes del jugador (sobreviven a la muerte). */
public final class PointsData {
    private static final String TAG = "PlayerPersisted"; // mismo tag que usa Forge
    private static final String KEY = "tiendamod_points";

    private PointsData() {}

    public static long get(Player player) {
        return player.getPersistentData().getCompound(TAG).getLong(KEY);
    }

    public static void set(Player player, long value) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(TAG);
        persisted.putLong(KEY, Math.max(0L, value));
        data.put(TAG, persisted);
    }

    public static void add(Player player, long amount) {
        set(player, get(player) + amount);
    }
}
