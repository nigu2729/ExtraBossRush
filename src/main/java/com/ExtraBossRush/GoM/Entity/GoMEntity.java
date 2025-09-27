package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.GoM.Support.PSU;
import com.ExtraBossRush.GoM.Support.LU;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Collections; // シャッフル用
import java.util.Random; // 乱数用

public class GoMEntity extends Monster {
    private ServerBossEvent bossEvent;

    public GoMEntity(EntityType<? extends GoMEntity> type, Level world) {
        super(type, world);
        if (world instanceof ServerLevel) {
            bossEvent = new ServerBossEvent(
                    Component.literal("魔術の守護者"), // 表示名
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            bossEvent.setVisible(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 100.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, ServerPlayer.class, 16.0F));
    }

    @Override
    protected void defineSynchedData() {
        // これが無いと体力等の同期用データが登録されず NPE になる
        super.defineSynchedData();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) return;

        for (ServerPlayer player : serverLevel.players()) {
            if (this.distanceTo(player) < 500.0D) {
                bossEvent.addPlayer(player);
            } else {
                bossEvent.removePlayer(player);
            }
        }
        this.RandomSkill();
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 攻撃時のボスバー表示追加処理…
        return super.hurt(source, amount);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (bossEvent != null) bossEvent.removeAllPlayers();
    }

    public void RandomSkill() {  // クラス内メソッドとして
        if (!(this.level() instanceof ServerLevel world)) return;

        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();
        double radius = 100.0;

        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(world, cx, cy, cz, radius);
        if (nearby.isEmpty()) return;

        Collections.shuffle(nearby, new Random());

        Random random = new Random();
        int timesPerPlayer = 4;

        for (ServerPlayer target : nearby) {
            double ty = target.getY() + target.getEyeHeight() * 0.5;

            for (int j = 0; j < timesPerPlayer; j++) {
                double offsetX = (random.nextDouble() - 0.5) * 20.0;
                double offsetZ = (random.nextDouble() - 0.5) * 20.0;

                double targetX = target.getX() + offsetX;
                double targetZ = target.getZ() + offsetZ;

                float[] angles = LU.calculateLookAt(cx, cy, cz, targetX, ty, targetZ);

                // 🔥 イベント発火
                MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(this, target));
            }
        }
    }
    public ServerBossEvent getBossEvent() {
        return bossEvent;
    }
}