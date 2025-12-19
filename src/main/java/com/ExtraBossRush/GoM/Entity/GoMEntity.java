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
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level.ExplosionInteraction;
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
    private static final int SEARCH_RADIUS_GROUND = 50;
    private static final double HOVER_OFFSET_Y = 20.0;
    private static final double DESIRED_DISTANCE_XZ = 25.0;
    private static final double SPEED_MOVE = 0.5;
    private static final double SPEED_HOVER = 0.12;
    private static final float ROTATION_LERP_FACTOR = 0.15f;

    private Vec3 currentTarget = null;
    private Path currentPath = null;

    private static final Map<Explosion, GoMEntity> explosionSourceMap =
            Collections.synchronizedMap(new WeakHashMap<>());

    public GoMEntity(EntityType<? extends GoMEntity> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
        this.moveControl = new MoveControl(this);
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
                .add(Attributes.FOLLOW_RANGE, 500.0D);
    }
    /**
     * 半径 radius 内で最も高い地面の Y を返す。水面も地面としてカウントする。
     */
    private int findHighestGroundY(ServerLevel level, double cx, double cz, int radius) {
        int cxBlock = Mth.floor(cx);
        int czBlock = Mth.floor(cz);
        int highest = level.getMinBuildHeight(); // 最低値から開始
        int r = radius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue; // 円形領域に限定
                int x = cxBlock + dx;
                int z = czBlock + dz;
                // WORLD_SURFACE は水面を含むことが多い（葉や草の上面も含む）
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (y > highest) highest = y;
            }
        }
        return highest;
    }

    /**
     * 最も近いプレイヤーを返す（ServerLevel.players() の中から）。見つからなければ null。
     */
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

    /**
     * 目標位置へ滑らかに移動する。XZ 方向のみで距離調整し、目標Y は固定。
     * speed は移動速度（例: 0.15）。
     */
    private void moveTowardsTargetXZ(double targetX, double targetY, double targetZ, double speed) {
        Vec3 pos = this.position();
        Vec3 dir = new Vec3(targetX - pos.x, targetY - pos.y, targetZ - pos.z);
        // XZ のみ正規化して speed をかける（垂直成分は目標Y への補正量）
        Vec3 dirXZ = new Vec3(dir.x, 0, dir.z);
        double distXZ = dirXZ.length();
        double vy = dir.y * 0.1; // Y軸への補間係数（滑らかに上がる／下がる）
        double vx = 0, vz = 0;
        if (distXZ > 0.01) {
            double factor = speed / distXZ;
            vx = dirXZ.x * factor;
            vz = dirXZ.z * factor;
        }
        this.moveControl = new MoveControl(this);
        // サーバー→クライアント同期（プレイヤー以外のエンティティでも有効）
        if (this.level() instanceof ServerLevel) {
            for (ServerPlayer p : ((ServerLevel) this.level()).players()) {
                p.connection.send(new ClientboundSetEntityMotionPacket(this));
            }
        }
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, ServerPlayer.class, 16.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            // 1) 周囲半径50の一番高い地面の y を取得して目標 y を決める
            int highest = findHighestGroundY(serverLevel, this.getX(), this.getZ(), 50);
            double targetY = highest + 20.0;

            // 2) 最も近いプレイヤーを見つける
            ServerPlayer nearest = findNearestPlayer(serverLevel);
            if (nearest != null) {
                // XZ 平面のみで、プレイヤーから 25 ブロック離れる位置を計算
                double px = nearest.getX();
                double pz = nearest.getZ();
                double bx = this.getX();
                double bz = this.getZ();

                double dx = bx - px;
                double dz = bz - pz;
                double distXZ = Math.sqrt(dx * dx + dz * dz);

                double targetX;
                double targetZ;
                if (distXZ < 0.001) {
                    // 完全に重なっている場合はランダムな方向へ置く
                    double angle = this.random.nextDouble() * Math.PI * 2.0;
                    targetX = px + Math.cos(angle) * 25.0;
                    targetZ = pz + Math.sin(angle) * 25.0;
                } else {
                    double nx = dx / distXZ;
                    double nz = dz / distXZ;
                    targetX = px + nx * 25.0;
                    targetZ = pz + nz * 25.0;
                }

                // 3) 目標に向けて滑らかに移動（速度は好みに応じて調整）
                moveTowardsTargetXZ(targetX, targetY, targetZ, 0.15);
            } else {
                // プレイヤーが見つからない場合はホバリングを維持するために Y を調整
                double bx = this.getX();
                double bz = this.getZ();
                moveTowardsTargetXZ(bx, targetY, bz, 0.08); // ゆっくり上下合わせる
            }
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;

        for (ServerPlayer player : serverLevel.players()) {
            if (this.distanceTo(player) <= 500.0D) bossEvent.addPlayer(player);
            else bossEvent.removePlayer(player);
        }

        if (this.tickCount % 100 == 0) {
            this.RandomSkill();
        }
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        handleFloatingPlayers(serverLevel);
        // --- ホバリング目標Y と 移動ターゲット更新 ---
        int highestGroundY = findHighestGroundY(serverLevel, this.getX(), this.getZ(), SEARCH_RADIUS_GROUND);
        double targetY = highestGroundY + HOVER_OFFSET_Y;
        ServerPlayer nearest = findNearestPlayer(serverLevel);
        if (nearest != null) {
            Vec3 desired = calcDesiredPositionAroundPlayerXZ(nearest, DESIRED_DISTANCE_XZ, targetY);
            updateMovementTarget(serverLevel, desired);
        } else {
            if (currentTarget == null) currentTarget = new Vec3(this.getX(), targetY, this.getZ());
            else updateMovementTarget(serverLevel, new Vec3(this.getX(), targetY, this.getZ()), SPEED_HOVER);
        }
        performMovementAndRotation(serverLevel);
    }

    private void handleFloatingPlayers(ServerLevel level) {
        Vec3 center = this.position();
        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(
                level, center.x, center.y, center.z, FLOAT_RANGE
        );
        if (nearby.isEmpty()) return;

        for (ServerPlayer player : nearby) {
            if (!player.isAlive()) {
                floatTimers.remove(player.getUUID());
                continue;
            }

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
                int ticksToReset = 200;
                GoMInvulnerabilityManager.requestResetInvulnerableTicks(player, ticksToReset);
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();

                for (int i = 0; i < EXPLOSION_REPEAT; i++) {
                    Explosion exp = level.explode(
                            this, x, y + i * 2, z,
                            EXPLOSION_POWER,
                            ExplosionInteraction.NONE
                    );
                    if (exp != null) {
                        explosionSourceMap.put(exp, this);
                        exp.finalizeExplosion(true);
                    }
                }

                player.setDeltaMovement(0, 0, 0);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));

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

    @Override
    public boolean isPickable() {
        return false;
    }

    private boolean hitboxDisabled = true;

    @Override
    public HitResult pick(double maxDistance, float tickDelta, boolean inFluid) {
        if (hitboxDisabled) {
            return null; // トレースヒットなし → ヒットボックス無効
        }
        return super.pick(maxDistance, tickDelta, inFluid);
    }

    public void RandomSkill() {
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
                double offsetX = (random.nextDouble() - 0.5) * 50.0;
                double offsetZ = (random.nextDouble() - 0.5) * 50.0;
                double targetX = target.getX() + offsetX;
                double targetZ = target.getZ() + offsetZ;
                float[] angles = LU.calculateLookAt(cx, cy, cz, targetX, ty, targetZ);

                MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(this, target));
            }
        }
    }
    public void clearFloatTimerFor(UUID uuid) {
        this.floatTimers.remove(uuid);
    }
    public ServerBossEvent getBossEvent() {
        return bossEvent;
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion exp = event.getExplosion();
        GoMEntity src = explosionSourceMap.remove(exp);
        if (src == null) return;

        // GoMEntity 自身は影響を受けない
        event.getAffectedEntities().removeIf(e -> e instanceof GoMEntity);

        // ここで爆発で影響を受けるプレイヤーを取得して、無敵停止をリクエストする
        int ticksToReset = 40; // 例: 40 tick = 2秒。ここを設定値に置き換えてください
        ServerLevel level = (ServerLevel) event.getLevel();

        for (Entity e : event.getAffectedEntities()) {
            if (e instanceof ServerPlayer player) {
                // プレイヤーごとに tick を加算する（加算方式）
                GoMInvulnerabilityManager.requestResetInvulnerableTicks(player, ticksToReset);
            }
        }
    }
    private Vec3 calcDesiredPositionAroundPlayerXZ(ServerPlayer player, double distance, double targetY) {
        double px = player.getX();
        double pz = player.getZ();
        double bx = this.getX();
        double bz = this.getZ();
        double dx = bx - px;
        double dz = bz - pz;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        double targetX, targetZ;
        if (distXZ < 0.001) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            targetX = px + Math.cos(angle) * distance;
            targetZ = pz + Math.sin(angle) * distance;
        } else {
            double nx = dx / distXZ;
            double nz = dz / distXZ;
            targetX = px + nx * distance;
            targetZ = pz + nz * distance;
        }
        return new Vec3(targetX, targetY, targetZ);
    }

    private void updateMovementTarget(ServerLevel level, Vec3 desired) {
        updateMovementTarget(level, desired, SPEED_MOVE);
    }

    private void updateMovementTarget(ServerLevel level, Vec3 desired, double speed) {
        if (currentTarget == null || currentTarget.distanceToSqr(desired) > 1.0) {
            currentTarget = desired;
            if (!tryPathTo(level, desired, speed)) {
                Vec3 fallback = randomHoverTarget(level, desired, 10, 20);
                tryPathTo(level, fallback, speed);
            }
        }
    }

    private boolean tryPathTo(ServerLevel level, Vec3 dest, double speed) {
        PathNavigation nav = this.getNavigation();
        if (nav == null) return false;
        BlockPos pos = BlockPos.containing(dest.x, dest.y, dest.z);
        Path path = nav.createPath(pos, 0);
        if (path != null && path.canReach()) {
            this.currentPath = path;
            return true;
        }
        return false;
    }

    private Vec3 randomHoverTarget(ServerLevel level, Vec3 around, double minR, double maxR) {
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double r = minR + this.random.nextDouble() * (maxR - minR);
        double x = around.x + Math.cos(angle) * r;
        double z = around.z + Math.sin(angle) * r;
        int highest = findHighestGroundY(level, x, z, 6);
        double y = highest + HOVER_OFFSET_Y;
        return new Vec3(x, y, z);
    }

    private void performMovementAndRotation(ServerLevel level) {
        if (currentPath != null && this.getNavigation() != null) {
            if (!this.getNavigation().moveTo(currentPath, SPEED_MOVE)) {
                currentPath = null;
            } else {
                Vec3 vel = this.getDeltaMovement();
                if (vel.lengthSqr() > 1e-6) {
                    float targetYaw = (float) (Mth.atan2(vel.z, vel.x) * (180F / Math.PI)) - 90F;
                    float currentYaw = this.getYRot();
                    float lerped = lerpAngle(currentYaw, targetYaw, ROTATION_LERP_FACTOR);
                    this.setYRot(lerped);
                    this.setYHeadRot(lerped);
                }
                return;
            }
        }

        if (currentTarget != null) {
            Vec3 pos = this.position();
            Vec3 delta = new Vec3(currentTarget.x - pos.x, currentTarget.y - pos.y, currentTarget.z - pos.z);
            Vec3 deltaXZ = new Vec3(delta.x, 0, delta.z);
            double distXZ = deltaXZ.length();
            if (distXZ > DESIRED_DISTANCE_XZ - 0.1) {
                double vx = 0, vz = 0;
                if (distXZ > 0.01) {
                    double factor = SPEED_MOVE / distXZ;
                    vx = deltaXZ.x * factor;
                    vz = deltaXZ.z * factor;
                }
                double vy = delta.y * 0.12;
                this.setDeltaMovement(vx, vy, vz);
                float targetYaw = (float) (Mth.atan2(vz, vx) * (180F / Math.PI)) - 90F;
                float currentYaw = this.getYRot();
                float lerped = lerpAngle(currentYaw, targetYaw, ROTATION_LERP_FACTOR);
                this.setYRot(lerped);
                this.setYHeadRot(lerped);
            } else {
                Vec3 randomVel = new Vec3(
                        (this.random.nextDouble() - 0.5) * SPEED_HOVER,
                        (currentTarget.y - this.getY()) * 0.05,
                        (this.random.nextDouble() - 0.5) * SPEED_HOVER
                );
                this.setDeltaMovement(randomVel);
                Vec3 lookDir = randomVel.lengthSqr() > 1e-6 ? randomVel : this.getDeltaMovement();
                float targetYaw = (float) (Mth.atan2(lookDir.z, lookDir.x) * (180F / Math.PI)) - 90F;
                float currentYaw = this.getYRot();
                float lerped = lerpAngle(currentYaw, targetYaw, ROTATION_LERP_FACTOR);
                this.setYRot(lerped);
                this.setYHeadRot(lerped);
            }
            syncMotionAndRotationToNearbyPlayers(level, this);
        }
    }

    private static float lerpAngle(float current, float target, float factor) {
        float f = wrapDegrees(target - current);
        return current + f * factor;
    }

    private static float wrapDegrees(float angle) {
        angle = (angle + 180.0F) % 360.0F;
        if (angle < 0.0F) angle += 360.0F;
        return angle - 180.0F;
    }

    private void syncMotionAndRotationToNearbyPlayers(ServerLevel level, Entity ent) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(ent) < 1024.0D) {
                p.connection.send(new ClientboundSetEntityMotionPacket(ent));
            }
        }
    }
}