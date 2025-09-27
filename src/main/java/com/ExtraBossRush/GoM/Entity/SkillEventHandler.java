package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Entity.GoMEntity;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID)
public class SkillEventHandler {

    @SubscribeEvent
    public static void onGoMSkill(GoMSkillEvent event) {
        GoMEntity boss = event.getBoss();
        ServerPlayer target = event.getTarget();
        Level level = target.level(); // または boss.level()

        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();

        Explosion explosion = level.explode(null, x, y, z, 25.0F, Level.ExplosionInteraction.NONE);
        if (explosion != null) {
            explosion.finalizeExplosion(true);
        }

        level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
        target.sendSystemMessage(Component.literal("GoMのスキルが発動しました！"));
    }
    public static class ExplosionHandler {
        @SubscribeEvent
        public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
            event.getAffectedEntities().removeIf(entity ->
                    entity.getType() == GoMEntities.MAGIC_GUARDIAN.get()
            );
        }
    }
}