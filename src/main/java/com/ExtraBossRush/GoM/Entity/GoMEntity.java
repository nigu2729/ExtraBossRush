package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Support.LU;
import com.ExtraBossRush.GoM.Support.PSU;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoMEntity extends Monster {

    private final ServerBossEvent bossEvent;

    private final Map<UUID, Integer> floatTimers = new HashMap<>();
    private static final int MAX_FLOAT_TICKS = 300;
    private static final double FLOAT_RANGE = 500.0;
    private static final float EXPLOSION_POWER = 30.0F;
    private static final int EXPLOSION_REPEAT = 4;

    private static final double TELEPORT_DISTANCE_THRESHOLD = 400.0; // 400ブロック以上離れたらワープ
    private static final double TELEPORT_MIN_RADIUS = 50.0;
    private static final double TELEPORT_MAX_RADIUS = 100.0;
    private static final double TELEPORT_HEIGHT_MIN = 30.0;
    private static final double TELEPORT_HEIGHT_MAX = 50.0;

    private static final double SPEED_MOVE = 0.5;
    private static final float ROTATION_LERP_FACTOR = 0.15f;
    private static final double MAX_MOVE_PER_TICK = 5000.0;

    private static final Map<Explosion, GoMEntity> explosionSourceMap =
            Collections.synchronizedMap(new WeakHashMap<>());

    // 前ティック保持用
    private Vec3 prevPos = Vec3.ZERO;
    private Vec3 prevDeltaMovement = Vec3.ZERO;
    private float prevYaw = 0f;
    private float prevHeadYaw = 0f;

    // 計画リスト
    private final List<PlannedUpdate> plannedUpdates = new ArrayList<>();

    private static final class PlannedUpdate {
        final Vec3 deltaMovement;
        final Float yaw;
        final Float headYaw;

        PlannedUpdate(Vec3 deltaMovement, Float yaw, Float headYaw) {
            this.deltaMovement = deltaMovement;
            this.yaw = yaw;
            this.headYaw = headYaw;
        }
    }

    public GoMEntity(EntityType<? extends GoMEntity> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
        this.xpReward = 500;
        bossEvent = new ServerBossEvent(
                Component.literal("魔術の守護者"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossEvent.setVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 512.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 500.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    // ブロック貫通を完全に許可
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public HitResult pick(double maxDistance, float tickDelta, boolean inFluid) {
        return null; // 完全に選択不能・貫通
    }

    private int findHighestGroundY(ServerLevel level, double cx, double cz, int radius) {
        int cxBlock = Mth.floor(cx);
        int czBlock = Mth.floor(cz);
        int highest = level.getMinBuildHeight();
        int r = radius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                int x = cxBlock + dx;
                int z = czBlock + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (y > highest) highest = y;
            }
        }
        return highest;
    }

    private ServerPlayer findNearestPlayer(ServerLevel level) {
        ServerPlayer nearest = null;
        double bestSq = Double.MAX_VALUE;
        Vec3 center = this.position();
        for (ServerPlayer p : level.players()) {
            double d2 = center.distanceToSqr(p.position());
            if (d2 < bestSq) {
                bestSq = d2;
                nearest = p;
            }
        }
        return nearest;
    }

    private void updateBossBar(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (this.distanceTo(player) <= 500.0D) {
                bossEvent.addPlayer(player);
            } else {
                bossEvent.removePlayer(player);
            }
        }
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    private void handleFloatingPlayers(ServerLevel level) {
        Vec3 center = this.position();
        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(level, center.x, center.y, center.z, FLOAT_RANGE);
        if (nearby.isEmpty()) return;

        for (ServerPlayer player : nearby) {
            if (!player.isAlive() || player.onGround()) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            GameType mode = player.gameMode.getGameModeForPlayer();
            if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) {
                floatTimers.remove(player.getUUID());
                continue;
            }

            int ticks = floatTimers.getOrDefault(player.getUUID(), 0) + 1;
            floatTimers.put(player.getUUID(), ticks);

            if (ticks >= MAX_FLOAT_TICKS) {
                double x = player.getX(), y = player.getY(), z = player.getZ();
                for (int i = 0; i < EXPLOSION_REPEAT; i++) {
                    Explosion exp = level.explode(this, x, y + i * 2, z, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
                    if (exp != null) {
                        explosionSourceMap.put(exp, this);
                        exp.finalizeExplosion(true);
                    }
                }
                player.setDeltaMovement(0, 0, 0);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
                floatTimers.remove(player.getUUID());
            }
        }
    }

    // 400ブロック以上離れたらワープ
    private boolean tryTeleportNearPlayer(ServerLevel level, ServerPlayer player) {
        double dist = this.distanceTo(player);
        if (dist < TELEPORT_DISTANCE_THRESHOLD) return false;

        double angle = random.nextDouble() * Math.PI * 2;
        double radius = TELEPORT_MIN_RADIUS + random.nextDouble() * (TELEPORT_MAX_RADIUS - TELEPORT_MIN_RADIUS);
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;

        int groundY = findHighestGroundY(level, player.getX() + offsetX, player.getZ() + offsetZ, 10);
        double teleportY = groundY + TELEPORT_HEIGHT_MIN + random.nextDouble() * (TELEPORT_HEIGHT_MAX - TELEPORT_HEIGHT_MIN);

        Vec3 teleportPos = new Vec3(player.getX() + offsetX, teleportY, player.getZ() + offsetZ);

        // ワープ実行（計画リスト経由でラバーバンド対策）
        Vec3 deltaToTeleport = teleportPos.subtract(this.position());
        plannedUpdates.add(new PlannedUpdate(deltaToTeleport, null, null));

        // パーティクルで演出
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 50, 1, 1, 1, 0.5);
        level.sendParticles(ParticleTypes.PORTAL, teleportPos.x, teleportPos.y, teleportPos.z, 50, 1, 1, 1, 0.5);

        return true;
    }

    private void planMovementAndRotationTowardsPlayer(ServerLevel level, ServerPlayer player, double baseHoverY) {
        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        Vec3 bossPos = this.position();

        double distXZ = bossPos.subtract(playerPos.x, 0, playerPos.z).horizontalDistance();

        // 気分パラメータで動きに変化
        double moodPhase = (this.tickCount * 0.02) % (Math.PI * 2);
        double mood = Math.sin(moodPhase) * 0.5 + 0.5; // 0.0 ~ 1.0

        double desiredDist = 25.0 + (mood * 40.0 - 20.0); // 15 ~ 45
        double heightAdd = 20.0 + mood * 30.0; // +20 ~ +50
        double targetY = baseHoverY + heightAdd;

        Vec3 targetPos;
        if (distXZ < 12.0) {
            // 近すぎたら逃げる
            Vec3 away = bossPos.subtract(playerPos).normalize();
            targetPos = playerPos.add(away.scale(desiredDist));
        } else {
            Vec3 toPlayer = playerPos.subtract(bossPos).normalize();
            Vec3 baseTarget = playerPos.add(toPlayer.scale(desiredDist));

            // 周回っぽい横オフセット
            double sideAngle = this.tickCount * 0.03 + mood * Math.PI;
            double offsetX = Math.cos(sideAngle) * 10.0;
            double offsetZ = Math.sin(sideAngle) * 10.0;

            targetPos = new Vec3(baseTarget.x + offsetX, targetY, baseTarget.z + offsetZ);
        }

        Vec3 direction = targetPos.subtract(bossPos);
        double distance = direction.length();

        double speed = SPEED_MOVE * 0.8;
        if (distance > desiredDist + 12.0) speed *= 2.2;
        else if (distance < desiredDist - 12.0) speed *= 0.6;

        double verticalWave = Math.sin(this.tickCount * 0.1) * 0.15 * (0.5 + mood);

        Vec3 velocity = direction.normalize().scale(speed);
        velocity = new Vec3(velocity.x, velocity.y + verticalWave, velocity.z);

        float targetYaw = (float) (Mth.atan2(velocity.z, velocity.x) * 180F / Math.PI) - 90F;
        float currentYaw = this.getYRot();
        float lerpedYaw = Mth.rotLerp(ROTATION_LERP_FACTOR, currentYaw, targetYaw);

        plannedUpdates.add(new PlannedUpdate(velocity, lerpedYaw, lerpedYaw));
    }

    private void planHoverInPlace(double targetY) {
        Vec3 pos = this.position();
        double vy = (targetY - pos.y) * 0.08;
        double wanderX = (random.nextDouble() - 0.5) * 0.2;
        double wanderZ = (random.nextDouble() - 0.5) * 0.2;
        Vec3 velocity = new Vec3(wanderX, vy, wanderZ);

        if (velocity.lengthSqr() > 1e-6) {
            float targetYaw = (float) (Mth.atan2(velocity.z, velocity.x) * 180F / Math.PI) - 90F;
            float currentYaw = this.getYRot();
            float lerpedYaw = Mth.rotLerp(ROTATION_LERP_FACTOR, currentYaw, targetYaw);
            plannedUpdates.add(new PlannedUpdate(velocity, lerpedYaw, lerpedYaw));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        savePreviousTickStateAndClearPlans();

        ServerPlayer nearest = findNearestPlayer(serverLevel);
        int highestGroundY = findHighestGroundY(serverLevel, this.getX(), this.getZ(), 50);
        double baseHoverY = highestGroundY + 20.0;

        updateBossBar(serverLevel);
        handleFloatingPlayers(serverLevel);

        if (this.tickCount % 100 == 0) {
            RandomSkill();
        }

        if (nearest != null) {
            // 遠すぎたらワープ（計画リストに巨大デルタを追加 → applyで巻き戻しされないよう注意）
            if (tryTeleportNearPlayer(serverLevel, nearest)) {
                // ワープしたらこのティックの移動は終了
                applyPlannedUpdates(serverLevel);
                return;
            }

            planMovementAndRotationTowardsPlayer(serverLevel, nearest, baseHoverY);
        } else {
            planHoverInPlace(baseHoverY);
        }

        applyPlannedUpdates(serverLevel);
    }

    public void RandomSkill() {
        if (!(this.level() instanceof ServerLevel world)) return;

        double cx = this.getX(), cy = this.getY(), cz = this.getZ();
        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(world, cx, cy, cz, 100.0);
        if (nearby.isEmpty()) return;

        Collections.shuffle(nearby, new java.util.Random() {
            @Override
            public int nextInt(int bound) {
                return GoMEntity.this.random.nextInt(bound);
            }
        });

        int timesPerPlayer = 4;

        for (ServerPlayer target : nearby) {
            double ty = target.getY() + target.getEyeHeight() * 0.5;
            for (int j = 0; j < timesPerPlayer; j++) {
                // ここも統一してthis.randomを使う（new Random()は不要）
                double offsetX = (this.random.nextDouble() - 0.5) * 50.0;
                double offsetZ = (this.random.nextDouble() - 0.5) * 50.0;
                double targetX = target.getX() + offsetX;
                double targetZ = target.getZ() + offsetZ;
                float[] angles = LU.calculateLookAt(cx, cy, cz, targetX, ty, targetZ);
                MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(this, target));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && attacker.getClass() == this.getClass()) return false;
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

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion exp = event.getExplosion();
        GoMEntity src = explosionSourceMap.remove(exp);
        if (src == null) return;

        event.getAffectedEntities().removeIf(e -> e instanceof GoMEntity);

        int ticksToReset = 40;
        ServerLevel level = (ServerLevel) event.getLevel();
        for (Entity e : event.getAffectedEntities()) {
            if (e instanceof ServerPlayer player) {
                GoMInvulnerabilityManager.requestResetInvulnerableTicks(player, ticksToReset);
            }
        }
    }

    public void clearFloatTimerFor(UUID uuid) {
        floatTimers.remove(uuid);
    }

    public ServerBossEvent getBossEvent() {
        return bossEvent;
    }

    // === ラバーバンド対策関連 ===
    private void savePreviousTickStateAndClearPlans() {
        this.prevPos = this.position();
        this.prevDeltaMovement = this.getDeltaMovement();
        this.prevYaw = this.getYRot();
        this.prevHeadYaw = this.getYHeadRot();
        this.plannedUpdates.clear();
    }

    private void syncMotionAndRotationToNearbyPlayers(ServerLevel level, Entity ent) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(ent) < 1024.0D) {
                p.connection.send(new ClientboundSetEntityMotionPacket(ent));
            }
        }
    }

    private void applyPlannedUpdates(ServerLevel level) {
        if (plannedUpdates.isEmpty()) return;

        Vec3 sumDelta = Vec3.ZERO;
        Float lastYaw = null;
        Float lastHead = null;

        for (PlannedUpdate pu : plannedUpdates) {
            sumDelta = sumDelta.add(pu.deltaMovement);
            if (pu.yaw != null) lastYaw = pu.yaw;
            if (pu.headYaw != null) lastHead = pu.headYaw;
        }

        Vec3 predictedPos = this.prevPos.add(sumDelta);
        double moved = predictedPos.distanceTo(this.prevPos);

        // ワープ時は巨大移動を許可
        if (moved > MAX_MOVE_PER_TICK && moved < 500.0) { // 500ブロック以内のワープはOK
            // 異常移動として扱わず通常適用
        } else if (moved > MAX_MOVE_PER_TICK) {
            // 本物の異常移動 → 巻き戻し
            this.setPos(prevPos.x, prevPos.y, prevPos.z);
            this.setDeltaMovement(prevDeltaMovement);
            this.setYRot(prevYaw);
            this.setYHeadRot(prevHeadYaw);
            syncMotionAndRotationToNearbyPlayers(level, this);
            plannedUpdates.clear();
            return;
        }

        this.setPos(predictedPos.x, predictedPos.y, predictedPos.z);
        this.setDeltaMovement(sumDelta);
        if (lastYaw != null) {
            float head = lastHead != null ? lastHead : lastYaw;
            this.setYRot(lastYaw);
            this.setYHeadRot(head);
        }

        syncMotionAndRotationToNearbyPlayers(level, this);
        plannedUpdates.clear();
    }
}