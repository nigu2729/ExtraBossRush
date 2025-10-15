package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Support.PSU;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.FORGE
)
public class GoMEntity extends Monster {
    private final ServerBossEvent bossEvent;

    // 浮遊ペナルティ用タイマー
    private final Map<UUID, Integer> floatTimers = new HashMap<>();
    private static final int    MAX_FLOAT_TICKS        = 300;
    private static final double FLOAT_RANGE             = 100.0;

    // 小爆発用設定
    private static final int   SMALL_EXPLOSION_COUNT    = 10;
    private static final float SMALL_EXPLOSION_STRENGTH = 5.0F;

    // Explosion → 発生元ボス を記録するマップ（相打ち防止用）
    private static final Map<Explosion, GoMEntity> explosionSourceMap =
            Collections.synchronizedMap(new WeakHashMap<>());

    public GoMEntity(EntityType<? extends GoMEntity> type, Level world) {
        super(type, world);
        bossEvent = new ServerBossEvent(
                Component.literal("魔術の守護者"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossEvent.setVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,     512.0D)
                .add(Attributes.MOVEMENT_SPEED,   0.5D)
                .add(Attributes.ATTACK_DAMAGE,   15.0D)
                .add(Attributes.FOLLOW_RANGE,   100.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, ServerPlayer.class, 16.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) return;

        // ── ボスバー表示／非表示 ──
        for (ServerPlayer player : serverLevel.players()) {
            if (this.distanceTo(player) < 500.0D) bossEvent.addPlayer(player);
            else                                    bossEvent.removePlayer(player);
        }

        // ── 定期スキル発動 ──
        if (this.tickCount % 100 == 0) {
            this.RandomSkill();
        }
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        // ── 浮遊ペナルティ処理 ──
        handleFloatingPlayers(serverLevel);
    }

    /**
     * 300Tick超過プレイヤーに対し、
     * (1) メイン爆発
     * (2) 小爆発を複数回
     * (3) ノックバックをゼロ化＆同期
     */
    private void handleFloatingPlayers(ServerLevel level) {
        Vec3 center = this.position();
        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(
                level, center.x, center.y, center.z, FLOAT_RANGE
        );
        if (nearby.isEmpty()) return;

        Random rand = new Random();

        for (ServerPlayer player : nearby) {
            GameType mode = player.gameMode.getGameModeForPlayer();
            if (mode != GameType.SURVIVAL && mode != GameType.CREATIVE) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            if (player.onGround()) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            int ticks = floatTimers.getOrDefault(player.getUUID(), 0) + 1;
            floatTimers.put(player.getUUID(), ticks);

            if (ticks >= MAX_FLOAT_TICKS) {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                // (1) メイン爆発
                Explosion mainExp = level.explode(
                        /* exploder = */ this,
                        x, y, z,
                        25.0F,
                        ExplosionInteraction.NONE
                );
                if (mainExp != null) {
                    explosionSourceMap.put(mainExp, this);
                    mainExp.finalizeExplosion(true);
                }

                // (2) 小爆発
                for (int i = 0; i < SMALL_EXPLOSION_COUNT; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 25.0;
                    double oy = (rand.nextDouble() - 0.5) * 25.0;
                    double oz = (rand.nextDouble() - 0.5) * 25.0;
                    double sx = x + ox;
                    double sy = y + oy;
                    double sz = z + oz;

                    Explosion smallExp = level.explode(
                            this,
                            sx, sy, sz,
                            SMALL_EXPLOSION_STRENGTH,
                            ExplosionInteraction.NONE
                    );
                    if (smallExp != null) {
                        explosionSourceMap.put(smallExp, this);
                        smallExp.finalizeExplosion(true);
                    }

                    level.sendParticles(
                            ParticleTypes.EXPLOSION,
                            sx, sy, sz,
                            1, 0, 0, 0, 0
                    );
                }

                // (3) ノックバックゼロ化＆クライアント同期
                player.setDeltaMovement(0, 0, 0);
                ServerGamePacketListenerImpl conn = player.connection;
                conn.send(new ClientboundSetEntityMotionPacket(player));

                // メイン爆発パーティクル
                level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        x, y, z,
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && attacker.getClass() == this.getClass()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossEvent.setVisible(false);
        bossEvent.removeAllPlayers();
    }

    public void RandomSkill() {
        // 既存スキル処理…
    }

    public ServerBossEvent getBossEvent() {
        return bossEvent;
    }

    /**
     * ExplosionEvent.Detonate で、
     * GoMEntity 発生元の爆発から他の GoMEntity への影響を除外
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion exp = event.getExplosion();
        GoMEntity src = explosionSourceMap.remove(exp);
        if (src == null) return;

        event.getAffectedEntities().removeIf(e -> e instanceof GoMEntity);
    }
}