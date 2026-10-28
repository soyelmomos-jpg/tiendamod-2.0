package com.tiendamod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import java.util.Random;

public final class MissionManager {
    private static final String TAG = "PlayerPersisted";
    private static final String M_KEY = "tiendamod_mission";
    private static final String C_KEY = "tiendamod_mission_count";
    private static final String R_KEY = "tiendamod_mission_required";
    private static final String T_KEY = "tiendamod_mission_type"; // 0: none, 1: easy, 2: epic

    private static final String[] EASY_TARGETS = {"minecraft:zombie", "minecraft:creeper", "minecraft:skeleton"};
    private static final String[] EPIC_TARGETS = {"minecraft:blaze", "minecraft:piglin", "minecraft:wither_skeleton"};

    public static void checkProgress(Player player, String entityId) {
        CompoundTag data = player.getPersistentData().getCompound(TAG);
        String target = data.getString(M_KEY);
        if (target.equals(entityId)) {
            int current = data.getInt(C_KEY) + 1;
            int required = data.getInt(R_KEY);
            data.putInt(C_KEY, current);
            if (current >= required) {
                // Mission complete!
                long reward = data.getInt(T_KEY) == 1 ? 100 : 500;
                PointsData.add(player, reward);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("¡Misión completada! Recompensa: " + reward + " pts"), true);
                data.putString(M_KEY, "");
                data.putInt(C_KEY, 0);
                data.putInt(R_KEY, 0);
                data.putInt(T_KEY, 0);
            }
            player.getPersistentData().put(TAG, data);
        }
    }

    public static void assignMission(Player player) {
        CompoundTag data = player.getPersistentData().getCompound(TAG);
        if (!data.getString(M_KEY).isEmpty()) return;

        Random rand = new Random();
        int type = rand.nextInt(10) < 1 ? 2 : 1; // 10% epic
        String target;
        int required;
        if (type == 1) {
            target = EASY_TARGETS[rand.nextInt(EASY_TARGETS.length)];
            required = 10;
        } else {
            target = EPIC_TARGETS[rand.nextInt(EPIC_TARGETS.length)];
            required = target.equals("minecraft:piglin") ? 20 : 10;
        }
        data.putString(M_KEY, target);
        data.putInt(R_KEY, required);
        data.putInt(T_KEY, type);
        data.putInt(C_KEY, 0);
        player.getPersistentData().put(TAG, data);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Nueva misión: Matar " + required + " " + target), true);
    }
}
