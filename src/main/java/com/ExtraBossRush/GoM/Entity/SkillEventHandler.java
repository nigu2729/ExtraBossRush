package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Entity.GoMEntity;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
        Level level      = target.level();  // サーバーレベル取得

        // ランダムなオフセットを計算
        Random rand      = new Random();
        double offsetX   = (rand.nextDouble() - 0.5) * 25.0;
        double offsetZ   = (rand.nextDouble() - 0.5) * 25.0;

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
            explosion.finalizeExplosion(true);
        }

        // サーバー側で速度をゼロ化
        target.setDeltaMovement(0, 0, 0);

        // クライアント側にも速度リセットを通知
        ServerGamePacketListenerImpl conn = target.connection;
        conn.send(new ClientboundSetEntityMotionPacket(target));
        level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
    }
}