package com.tiendamod;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEvents {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ShopManager.load();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ShopCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public void onLivingSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Monster && Math.random() < 1.0 / 200.0) {
            net.minecraft.world.entity.LivingEntity entity = event.getEntity();
            // Twice as strong (attack damage)
            if (entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(
                        entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() * 2);
            }
            // Health
            entity.setHealth(entity.getMaxHealth() * 2);
            // Elite bone drop
            entity.setCustomName(net.minecraft.network.chat.Component.literal("Elite " + entity.getName().getString()));
            entity.setCustomNameVisible(true);
            entity.getPersistentData().putBoolean("isElite", true);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            String entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();
            MissionManager.checkProgress(player, entityId);
        }
    }

    @SubscribeEvent
    public void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("isElite")) {
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                    event.getEntity().level(),
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    new net.minecraft.world.item.ItemStack(TiendaMod.ELITE_BONE.get())
            ));
        }
    }
}
