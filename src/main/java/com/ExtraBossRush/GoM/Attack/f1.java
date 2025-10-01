package com.ExtraBossRush.GoM.Attack;

import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class f1 {
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide() && event.level instanceof ServerLevel serverLevel) {
            Vec3 bossCenter = new Vec3(0, 100, 0); // 例：ボスの中心座標
            f1.onServerTick(serverLevel, bossCenter);
        }
    }
    private static final Map<UUID, Integer> floatTimers = new HashMap<>();
    private static final int MAX_TICKS = 300; // 15秒
    private static final double RANGE = 100.0;

    // 毎tick呼び出す（サーバー側）
    public static void onServerTick(ServerLevel level, Vec3 center) {
        for (ServerPlayer player : level.players()) {
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL &&
                    player.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            if (player.distanceToSqr(center) > RANGE * RANGE) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            if (player.onGround()) {
                floatTimers.put(player.getUUID(), 0);
            } else {
                int ticks = floatTimers.getOrDefault(player.getUUID(), 0) + 1;
                floatTimers.put(player.getUUID(), ticks);

                if (ticks >= MAX_TICKS) {
                    double x = player.getX();
                    double y = player.getY();
                    double z = player.getZ();

                    for (int i = 0; i < 10; i++) {
                        level.explode(null, x, y, z, 25.0F, Level.ExplosionInteraction.NONE);
                        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
                    }

                    floatTimers.put(player.getUUID(), 0);
                }
            }
        }
    }
}