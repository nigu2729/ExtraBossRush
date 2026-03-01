package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Entity.GoMEntity;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import com.ExtraBossRush.GoM.Support.SU;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.FORGE
)
public class SkillEventHandler {

    @SubscribeEvent
    public static void onGoMSkill(GoMSkillEvent event) {
        GoMEntity boss   = event.getBoss();
        ServerPlayer target = event.getTarget();
        Level level      = target.level();
        int SkillId = event.getSkillId();
        switch (SkillId) {
            case 1 -> {
                PE(boss, target, level);
            }
            case 2 -> {
                float Er = 1024;
                List<Entity> targets = SU.getEntitiesWithinRadius((ServerLevel)level, boss.getX(),  boss.getY(), boss.getZ(), Er);
                for (Entity entity : targets) {
                    if (entity != boss && (entity instanceof Monster || (entity instanceof Mob mob && mob.getTarget() != null) || entity instanceof ServerPlayer)){
                        AK(boss, entity, level);
                    }
                }
            }
        }
    }
    private static void PE(GoMEntity boss, ServerPlayer target, Level level) {
        // ランダムなオフセットを計算
        Random rand = new Random();
        double offsetX = (rand.nextDouble() - 0.5) * 25.0;
        double offsetZ = (rand.nextDouble() - 0.5) * 25.0;

        // 実際の爆発位置
        double x = target.getX() + offsetX;
        double y = target.getY() + 5.0;
        double z = target.getZ() + offsetZ;

        // 爆発を発生（ダメージ＋ノックバック発生）
        Explosion explosion = level.explode(
                boss,     // 爆発の発生元を boss に
                x, y, z,
                25.0F,
                ExplosionInteraction.NONE  // ブロック破壊なし
        );
        if (explosion != null) {
            explosion.finalizeExplosion(false);
        }

        // サーバー側で速度をゼロ化
        target.setDeltaMovement(0, 0, 0);

        // クライアント側にも速度リセットを通知
        ServerGamePacketListenerImpl conn = target.connection;
        conn.send(new ClientboundSetEntityMotionPacket(target));
    }
    private static void AK(GoMEntity boss, Entity entity, Level level) {
        Random rand = new Random();
        double offsetX = (rand.nextDouble() - 0.5) * 25.0;
        double offsetZ = (rand.nextDouble() - 0.5) * 25.0;

        Explosion explosion = level.explode(
                boss,
                entity.getX() + offsetX, entity.getY() + 5.0, entity.getZ() + offsetZ,
                25.0F,
                ExplosionInteraction.NONE  // ブロック破壊なし
        );
        if (explosion != null) {
            explosion.finalizeExplosion(false);
        }

        // サーバー側で速度をゼロ化
        entity.setDeltaMovement(0, 0, 0);
        if (entity instanceof ServerPlayer serverPlayer) {
            ServerGamePacketListenerImpl conn = serverPlayer.connection;
            conn.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }

    }
}