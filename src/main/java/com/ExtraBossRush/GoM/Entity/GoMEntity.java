package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Support.PSU;
import com.ExtraBossRush.GoM.Support.LU;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * GoMEntity（ボス本体）。
 * ・300Tick以上空中にいるプレイヤーに対し毎Tick爆発＋ノックバック0
 * ・同種同士の爆発相打ちを防止
 */
@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.FORGE
)
public class GoMEntity extends Monster {
    private final ServerBossEvent bossEvent;

    // 浮遊ペナルティ用タイマー
    private final Map<UUID, Integer> floatTimers = new HashMap<>();
    private static final int    MAX_FLOAT_TICKS = 300;
    private static final double FLOAT_RANGE      = 100.0;

    /** Explosion → 発生元ボス を記録するマップ */
    private static final Map<Explosion, GoMEntity> explosionSourceMap = Collections.synchronizedMap(new WeakHashMap<>());

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
                .add(Attributes.MAX_HEALTH,    512.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.ATTACK_DAMAGE,  15.0D)
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

        // ── ボスバー登録 ──
        for (ServerPlayer player : serverLevel.players()) {
            if (this.distanceTo(player) < 500.0D) bossEvent.addPlayer(player);
            else                                    bossEvent.removePlayer(player);
        }

        // ── 定期スキル ──
        if (this.tickCount % 100 == 0) {
            this.RandomSkill();
        }
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        // ── 浮遊ペナルティ ──
        handleFloatingPlayers(serverLevel);
    }

    /**
     * 範囲内プレイヤーの空中滞在をカウントし、
     * MAX_FLOAT_TICKS超過後は毎Tick爆発＋ノックバックをゼロ化
     */
    private void handleFloatingPlayers(ServerLevel level) {
        Vec3 center = this.position();
        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(
                level, center.x, center.y, center.z, FLOAT_RANGE
        );
        if (nearby.isEmpty()) return;

        for (ServerPlayer player : nearby) {
            // ゲームモードチェック
            GameType mode = player.gameMode.getGameModeForPlayer();
            if (mode != GameType.SURVIVAL && mode != GameType.CREATIVE) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            // 地上着地 → リセット
            if (player.onGround()) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            // 空中Tick加算
            int ticks = floatTimers.getOrDefault(player.getUUID(), 0) + 1;
            floatTimers.put(player.getUUID(), ticks);

            // 300Tick超過でペナルティ爆発
            if (ticks >= MAX_FLOAT_TICKS) {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                // (1) 爆発を起こす
                Explosion exp = level.explode(
                        /* exploder = */ this,
                        x, y, z,
                        25.0F,
                        ExplosionInteraction.NONE
                );
                if (exp != null) {
                    // 発生元を記録
                    explosionSourceMap.put(exp, this);
                    exp.finalizeExplosion(true);
                }

                // (2) サーバー内で速度をゼロに
                player.setDeltaMovement(0, 0, 0);

                // (3) クライアントに速度同期パケットを送信
                ServerGamePacketListenerImpl conn = player.connection;
                conn.send(new ClientboundSetEntityMotionPacket(player));

                // (4) パーティクルだけ表示
                level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        x, y, z, 1, 0, 0, 0, 0
                );
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 自分と同クラスからのダメージを無効化
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
        if (!(this.level() instanceof ServerLevel world)) return;

        double cx     = this.getX();
        double cy     = this.getY();
        double cz     = this.getZ();
        double radius = 100.0;

        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(world, cx, cy, cz, radius);
        if (nearby.isEmpty()) return;

        Collections.shuffle(nearby, new Random());
        Random random = new Random();
        int timesPerPlayer = 4;

        for (ServerPlayer target : nearby) {
            double ty = target.getY() + target.getEyeHeight() * 0.5;
            for (int j = 0; j < timesPerPlayer; j++) {
                double offsetX = (random.nextDouble() - 0.5) * 50.0;
                double offsetZ = (random.nextDouble() - 0.5) * 50.0;
                double targetX = target.getX() + offsetX;
                double targetZ = target.getZ() + offsetZ;
                LU.calculateLookAt(cx, cy, cz, targetX, ty, targetZ);

                MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(this, target));
            }
        }
    }

    public ServerBossEvent getBossEvent() {
        return bossEvent;
    }

    /**
     * ExplosionEvent.Detonate で、
     * GoMEntity 発生元の爆発に対しては他の GoMEntity への影響を無効化
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion exp = event.getExplosion();
        GoMEntity src = explosionSourceMap.remove(exp);
        if (src == null) return;

        // 発生元が GoMEntity の場合、対象リストから GoMEntity をすべて除外
        event.getAffectedEntities().removeIf(e -> e instanceof GoMEntity);
    }
}