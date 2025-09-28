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
import java.util.Random;


@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID)
public class SkillEventHandler {

    @SubscribeEvent
    public static void onGoMSkill(GoMSkillEvent event) {
        GoMEntity boss = event.getBoss();
        ServerPlayer target = event.getTarget();
        Level level = target.level(); // または boss.level()
        Random random = new Random();
        double offsetX = (random.nextDouble() - 0.5) * 25.0;
        double offsetZ = (random.nextDouble() - 0.5) * 25.0;

        double x = target.getX() + offsetX;
        double y = target.getY() + 5;
        double z = target.getZ() + offsetZ;

        Explosion explosion = level.explode(boss, x, y, z, 25.0F, Level.ExplosionInteraction.NONE);
        if (explosion != null) {
            explosion.finalizeExplosion(true);
        }

        level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
    }
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(entity -> entity.getType() == GoMEntities.MAGIC_GUARDIAN.get());
    }
}