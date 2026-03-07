package com.ExtraBossRush.GoM.Entity;
import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.ARV2Config;
import com.ExtraBossRush.GoM.Entity.GoMEntity;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import com.ExtraBossRush.GoM.Support.SU;
import com.ExtraBossRush.GoM.client.ARV2;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkillEventHandler {

    private static final Random random = new Random();

    // ====================== キルカウント ======================

    private static int mobKillCount = 0;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) mobKillCount++;
    }

    // ====================== サーバー側タスク管理 ======================

    private static final List<DelayedSkillTask> SERVER_TASKS = new ArrayList<>();

    private record DelayedSkillTask(
            long executeAt,
            ResourceKey<Level> dimensionKey,
            Vec3 pos,
            int skillId,
            UUID casterId,
            Vec3 beamDir
    ) {}

    // ====================== ビーム管理 ======================

    private static final List<BeamInstance> ACTIVE_BEAMS = new ArrayList<>();

    public static class BeamInstance {
        private final Level level;
        private final Vec3 start, dir;
        private final int maxLife, warmup;
        private final float beamSize;
        private int age = 0;
        private float lastProcessedLen = 0;
        private final BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        public BeamInstance(Level level, Vec3 start, Vec3 dir, int maxLife, int warmup, float beamSize) {
            this.level  = level;
            this.start  = start;
            this.dir    = dir.normalize();
            this.maxLife = maxLife;
            this.warmup  = warmup;
            this.beamSize = beamSize;
        }

        public void incrementAge() { age++; }
        public boolean isExpired() { return age >= maxLife; }
        public boolean isWarmingUp() { return age < warmup; }

        public float getCurrentLen() {
            float speed  = 32.0f;
            float maxLen = 512.0f;
            return Math.min(maxLen, speed * Math.max(0, age - warmup));
        }

        public float getLastProcessedLen() { return lastProcessedLen; }
        public void setLastProcessedLen(float value) { lastProcessedLen = value; }

        public void processTipImpact(Vec3 tip, float radius) {
            int r       = (int) Math.ceil(radius);
            int margin  = 1;
            int rSq     = (int) (radius * radius);
            int outerRSq = (int) ((radius + margin) * (radius + margin));
            int cx = (int) Math.floor(tip.x);
            int cy = (int) Math.floor(tip.y);
            int cz = (int) Math.floor(tip.z);
            for (int dx = -r - margin; dx <= r + margin; dx++) {
                for (int dy = -r - margin; dy <= r + margin; dy++) {
                    for (int dz = -r - margin; dz <= r + margin; dz++) {
                        mPos.set(cx + dx, cy + dy, cz + dz);
                        if (!level.hasChunkAt(mPos)) continue;
                        BlockState state = level.getBlockState(mPos);
                        if (state.getDestroySpeed(level, mPos) < 0 || state.isAir()) continue;
                        int distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > outerRSq) continue;
                        if (distSq <= rSq) {
                            level.setBlock(mPos, Blocks.AIR.defaultBlockState(), 2 | 16);
                        } else {
                            if (!level.getFluidState(mPos).is(net.minecraft.tags.FluidTags.LAVA)) {
                                level.setBlock(mPos, Blocks.LAVA.defaultBlockState(), 2 | 16);
                            }
                        }
                    }
                }
            }
        }

        public void damageEntitiesInBeam() {
            if (age < warmup) return;
            float currentLen = getCurrentLen();
            if (currentLen <= 0) return;
            Vec3 end = start.add(dir.scale(currentLen));
            double margin = beamSize;
            AABB beamAABB = new AABB(
                    Math.min(start.x, end.x) - margin, Math.min(start.y, end.y) - margin, Math.min(start.z, end.z) - margin,
                    Math.max(start.x, end.x) + margin, Math.max(start.y, end.y) + margin, Math.max(start.z, end.z) + margin
            );
            List<Entity> entities = level.getEntities((Entity) null, beamAABB,
                    entity -> entity instanceof LivingEntity &&
                            !((entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) || entity instanceof GoMEntity)
            );
            for (Entity entity : entities) {
                LivingEntity living = (LivingEntity) entity;
                if (!isEntityInCylinder(entity, currentLen)) continue;
                if (living instanceof ServerPlayer player) {
                    if (GoMEventSubscriber.getRemainingResetTicks(player.getUUID()) < 5) {
                        GoMEventSubscriber.requestResetInvulnerableTicks(player, 200);
                    }
                } else {
                    living.invulnerableTime = 0;
                }
                living.hurt(level.damageSources().magic(), 20.0f);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, false, false));
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         100, 0, false, false, false));
            }
        }

        public boolean isEntityInCylinder(Entity entity, float currentLen) {
            Vec3 entityPos    = entity.position();
            Vec3 entityCenter = new Vec3(entityPos.x, entityPos.y + entity.getBbHeight() * 0.5, entityPos.z);
            Vec3 toEntity     = entityCenter.subtract(start);
            double projection = toEntity.dot(dir);
            if (projection < 0 || projection > currentLen) return false;
            Vec3 closestPoint = start.add(dir.scale(projection));
            double distance   = entityCenter.distanceTo(closestPoint);
            double totalRadius = beamSize + Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.5;
            return distance <= totalRadius;
        }
    }

    public static void addBeam(Level level, Vec3 start, Vec3 dir, int maxLife, int warmup, float beamSize) {
        ACTIVE_BEAMS.add(new BeamInstance(level, start, dir, maxLife, warmup, beamSize));
    }

    // ====================== ネットワーク ======================

    private static SimpleChannel NETWORK;

    public static void register() {
        NETWORK = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ExtraBossRush.MOD_ID, "skill_effects"),
                () -> "1", "1"::equals, "1"::equals
        );
        NETWORK.registerMessage(
                0, SkillEffectPacket.class,
                SkillEffectPacket::encode, SkillEffectPacket::decode, SkillEffectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    private static class SkillEffectPacket {
        private final Vec3 pos;
        private final int skillId;

        public SkillEffectPacket(Vec3 pos, int skillId) { this.pos = pos; this.skillId = skillId; }

        public static void encode(SkillEffectPacket msg, FriendlyByteBuf buf) {
            buf.writeDouble(msg.pos.x); buf.writeDouble(msg.pos.y); buf.writeDouble(msg.pos.z);
            buf.writeInt(msg.skillId);
        }
        public static SkillEffectPacket decode(FriendlyByteBuf buf) {
            return new SkillEffectPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt());
        }
        public static void handle(SkillEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleEffect(msg)));
            ctx.get().setPacketHandled(true);
        }
    }

    private static class ClientPacketHandler {
        public static void handleEffect(SkillEffectPacket msg) {
            Level level = Minecraft.getInstance().level;
            if (level != null) spawnClientEffects(level, msg.pos, msg.skillId);
        }
    }

    // ====================== Quaternionで方向を計算するヘルパー ======================

    private static Vec3 calculateBeamDir(float nP, float nY, float nR, float NP, float NY, float NR) {
        Quaternionf baseRot = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(nP), (float) Math.toRadians(nY), (float) Math.toRadians(nR));
        Quaternionf addRot  = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(NP), (float) Math.toRadians(NY), (float) Math.toRadians(NR));
        baseRot.mul(addRot);
        Vector3f forward = new Vector3f(0, 0, 1);
        baseRot.transform(forward);
        return new Vec3(forward.x(), forward.y(), forward.z()).normalize();
    }

    // ====================== 球状ブロック破壊ヘルパー ======================

    // ====================== 球状ブロック段階破壊 (ProgressiveCarver インライン) ======================

    private static final java.util.Queue<CarveJob> CARVE_JOBS = new java.util.LinkedList<>();

    private static class CarveJob {
        private final ServerLevel level;
        private final Vec3 center;
        private final int maxRadius;
        private final int shellsPerTick;
        private final float blocksPerTickFloat;
        private final boolean removeFluids;
        private double blockCarry = 0.0;
        private int currentRadius = -1;          // -1: まだ r=0 を処理していない
        private int lastDamagedRadius = -1;      // エンティティダメージ済みの最大半径
        private final java.util.Deque<BlockPos> pendingShell = new java.util.ArrayDeque<>();
        private final Random rand = new Random();

        CarveJob(ServerLevel level, Vec3 center, int maxRadius, int shellsPerTick, float blocksPerTickFloat, boolean removeFluids) {
            this.level             = level;
            this.center            = center;
            this.maxRadius         = Math.max(0, maxRadius);
            this.shellsPerTick     = Math.max(1, shellsPerTick);
            this.blocksPerTickFloat = blocksPerTickFloat;
            this.removeFluids      = removeFluids;
        }

        private void prepareNextShell() {
            while (currentRadius < maxRadius && pendingShell.isEmpty()) {
                currentRadius++;
                collectShellPositions(currentRadius, pendingShell);
                if (!pendingShell.isEmpty()) {
                    java.util.List<BlockPos> tmp = new java.util.ArrayList<>(pendingShell);
                    java.util.Collections.shuffle(tmp, rand);
                    pendingShell.clear();
                    for (BlockPos p : tmp) pendingShell.addLast(p);
                }
            }
        }

        private void collectShellPositions(int r, java.util.Deque<BlockPos> out) {
            int cx = (int) Math.floor(center.x);
            int cy = (int) Math.floor(center.y);
            int cz = (int) Math.floor(center.z);
            double rSqLo = (r - 0.5) * (r - 0.5);
            double rSqHi = (r + 0.5) * (r + 0.5);
            int minY = Math.max(level.getMinBuildHeight(), cy - r);
            int maxY = Math.min(level.getMaxBuildHeight(), cy + r);
            for (int x = cx - r; x <= cx + r; x++) {
                int dxSq = (x - cx) * (x - cx);
                for (int y = minY; y <= maxY; y++) {
                    int dySq = (y - cy) * (y - cy);
                    for (int z = cz - r; z <= cz + r; z++) {
                        int distSq = dxSq + dySq + (z - cz) * (z - cz);
                        if (distSq >= rSqLo && distSq <= rSqHi) out.addLast(new BlockPos(x, y, z));
                    }
                }
            }
        }

        boolean tickOnce() {
            for (int i = 0; i < shellsPerTick; i++) prepareNextShell();

            // allowedThisTick は long で上限なし (Integer.MAX_VALUE 制限を撤廃)
            long allowedThisTick;
            if (blocksPerTickFloat > 0.0f) {
                blockCarry += (double) blocksPerTickFloat;
                long al = (long) Math.floor(blockCarry);
                if (al > 0) blockCarry -= al;
                allowedThisTick = Math.max(0L, al);
            } else {
                allowedThisTick = Long.MAX_VALUE;
            }

            long removed = 0;
            int unloadedRetry = 0;
            while (removed < allowedThisTick && !pendingShell.isEmpty()) {
                BlockPos pos = pendingShell.pollFirst();
                if (pos == null) break;
                if (!level.isLoaded(pos)) {
                    // ロードされていないブロックは末尾に再追加して後で再処理
                    if (unloadedRetry < pendingShell.size() + 1) {
                        pendingShell.addLast(pos);
                        unloadedRetry++;
                    }
                    // 全残りが unloaded の場合は無限ループ防止のため抜ける
                    if (unloadedRetry >= 4096) break;
                    continue;
                }
                unloadedRetry = 0;
                if (removeFluids && !level.getFluidState(pos).isEmpty()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    removed++; continue;
                }
                if (level.getBlockState(pos).isAir()) continue;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                removed++;
            }

            // currentRadius が進んだ分だけエンティティにダメージ
            if (currentRadius > lastDamagedRadius) {
                damageEntitiesInSphere(currentRadius);
                lastDamagedRadius = currentRadius;
            }

            return pendingShell.isEmpty() && currentRadius >= maxRadius;
        }

        private void damageEntitiesInSphere(int radius) {
            double radSq = (double) radius * radius;
            AABB box = new AABB(
                    center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);
            java.util.List<Entity> entities = level.getEntities((Entity) null, box,
                    e -> e instanceof LivingEntity
                            && !(e instanceof GoMEntity)
                            && !(e instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())));
            for (Entity e : entities) {
                LivingEntity living = (LivingEntity) e;
                Vec3 ec = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
                if (ec.distanceToSqr(center) > radSq) continue;
                if (living instanceof ServerPlayer player) {
                    if (GoMEventSubscriber.getRemainingResetTicks(player.getUUID()) < 5)
                        GoMEventSubscriber.requestResetInvulnerableTicks(player, 200);
                } else {
                    living.invulnerableTime = 0;
                }
                living.hurt(level.damageSources().magic(), Integer.MAX_VALUE);
            }
        }
    }

    private static void startSphereDestruction(
            ServerLevel level, Vec3 center,
            int radius, int shellsPerTick, float blocksPerTick, boolean removeFluids
    ) {
        synchronized (CARVE_JOBS) {
            CARVE_JOBS.add(new CarveJob(level, center, radius, shellsPerTick, blocksPerTick, removeFluids));
        }
    }

    // ====================== クライアントエフェクト ======================

    public static void spawnClientEffects(Level level, Vec3 pos, int skillId) {
        final ResourceLocation sqmodel = new ResourceLocation(ExtraBossRush.MOD_ID, "models/block/square.obj");
        final ResourceLocation spmodel = new ResourceLocation(ExtraBossRush.MOD_ID, "models/block/sphere.obj");
        final ResourceLocation bemodel = new ResourceLocation(ExtraBossRush.MOD_ID, "models/block/beam1.obj");
        final ResourceLocation mhtex   = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/block/mhz4-1whi.png");
        final ResourceLocation whitex  = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/block/whi.png");
        final ResourceLocation blatex  = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/block/bla.png");
        switch (skillId) {
            case 1 -> {}
            case 2 -> {}
            case 3 -> {
                final int size = 10;
                final int kan  = 20;
                final int kan2 = 200;
                final int Life = 400;
                for (int i = 0; i < 3; i++) {
                    new ARV2.Builder(sqmodel, mhtex, pos.add(0, (10 * i - 1), 0))
                            .setSize(size + (10 * i))
                            .setAlphaAnim(new float[]{0, 0, 1, 1, 0}, new int[]{0, kan * i, kan * i + (kan / 2), Life - 10, Life})
                            .setRotAnim(new float[]{90, 90}, new float[]{0, 0},
                                    new float[]{0, (float) ((1080 * (random.nextBoolean() ? 1 : -1) + (random.nextDouble() * 2 - 1) * 720))},
                                    new int[]{0, Life})
                            .setMaxLife(Life).spawn();
                }
                new ARV2.Builder(spmodel, whitex, pos.add(0, -10, 0)).setRot(90, 0, 0)
                        .setAlphaAnim(new float[]{1, 1, 0}, new int[]{0, Life - 10, Life})
                        .setSizeAnim(new float[]{0, 5}, new int[]{0, kan2})
                        .setMaxLife(Life).spawn();
                new ARV2.Builder(bemodel, whitex, pos.add(0, -10, 0)).setRot(90, 0, 0)
                        .asBeam2(32.0f, 512.0f, 32.0f)
                        .setAlphaAnim(new float[]{0, 0, 1, 1, 0}, new int[]{0, kan2, kan2, Life - 10, Life})
                        .setSize(5).setMaxLife(Life).spawn();
            }
            case 4 -> {}
        }
    }

    // ====================== スキルイベント処理 ======================

    @SubscribeEvent
    public static void onGoMSkill(GoMSkillEvent event) {
        GoMEntity    boss   = event.getBoss();
        ServerPlayer target = event.getTarget();
        Level        level  = target.level();
        int          skillId = event.getSkillId();

        long delayTicks = 100;
        Vec3 beamDir    = null;

        switch (skillId) {
            case 1 -> {
                PE(boss, target, level);
            }
            case 2 -> {
                float er = 1024;
                // SU: AABB + distSq 球状フィルタでエンティティ取得
                List<Entity> targets = SU.getEntitiesWithinRadius(
                        (ServerLevel) level, boss.getX(), boss.getY(), boss.getZ(), er);
                for (Entity entity : targets) {
                    if (entity == boss || entity instanceof GoMEntity) continue;
                    if (entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) continue;
                    AK(boss, entity, level);
                }
            }
            case 3 -> {
                Vec3  center = new Vec3(boss.getX(), 150, boss.getZ());
                float radius = 256;
                delayTicks   = 0;
                beamDir = calculateBeamDir(0.0f, 0.0f, 0.0f, 90.0f, 0.0f, 0.0f);

                for (int i = 0; i < 50; i++) {
                    double angle    = random.nextDouble() * 2 * Math.PI;
                    double r        = radius * Math.sqrt(random.nextDouble());
                    Vec3 spawnPos   = new Vec3(center.x + Math.cos(angle) * r, 150, center.z + Math.sin(angle) * r);
                    if (!level.isClientSide)
                        addSkillTask(level, target.getUUID(), level.getGameTime() + delayTicks, skillId, spawnPos, beamDir);
                }
                radius = 1024;
                for (int i = 0; i < 100; i++) {
                    double angle    = random.nextDouble() * 2 * Math.PI;
                    double r        = radius * Math.sqrt(random.nextDouble());
                    Vec3 spawnPos   = new Vec3(center.x + Math.cos(angle) * r, 150, center.z + Math.sin(angle) * r);
                    if (!level.isClientSide)
                        addSkillTask(level, target.getUUID(), level.getGameTime() + delayTicks, skillId, spawnPos, beamDir);
                }
            }
            case 4 -> {
                // ボス死亡時: 中心座標から半径512ブロックの球を問答無用で破壊
                if (!level.isClientSide) {
                    Vec3 center = new Vec3(boss.getX(), boss.getY(), boss.getZ());
                    startSphereDestruction(
                            (ServerLevel) level,
                            center,
                            512,
                            2,
                            Long.MAX_VALUE,
                            true
                    );
                }
            }
        }
    }

    // ====================== Tick処理 ======================

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        ServerLevel level   = (ServerLevel) event.level;
        long currentTime    = level.getGameTime();

        Iterator<DelayedSkillTask> it = SERVER_TASKS.iterator();
        while (it.hasNext()) {
            DelayedSkillTask task = it.next();
            if (!event.level.dimension().equals(task.dimensionKey)) continue;
            if (currentTime < task.executeAt) continue;
            if (!event.level.isLoaded(BlockPos.containing(task.pos))) { it.remove(); continue; }
            executeServerLogic(event.level, task);
            sendEffectToClients(event.level, task.pos, task.skillId);
            it.remove();
        }

        // CarveJob Tick処理
        synchronized (CARVE_JOBS) {
            java.util.Iterator<CarveJob> cit = CARVE_JOBS.iterator();
            while (cit.hasNext()) {
                CarveJob job = cit.next();
                if (job.level != level) continue;
                try { if (job.tickOnce()) cit.remove(); }
                catch (Throwable t) { cit.remove(); t.printStackTrace(); }
            }
        }

        Iterator<BeamInstance> beamIt = ACTIVE_BEAMS.iterator();
        while (beamIt.hasNext()) {
            BeamInstance beam = beamIt.next();
            if (beam.level != level) continue;
            beam.incrementAge();
            if (beam.isExpired()) { beamIt.remove(); continue; }
            if (beam.isWarmingUp()) continue;
            float currentLen = beam.getCurrentLen();
            if (ARV2Config.SERVER_SPEC.isLoaded() && ARV2Config.ENABLE_BEAM_BREAK.get()) {
                float step = Math.max(1.0f, beam.beamSize - 0.5f);
                for (float d = beam.getLastProcessedLen(); d < currentLen; d += step) {
                    beam.processTipImpact(beam.start.add(beam.dir.scale(d)), beam.beamSize);
                }
            }
            beam.damageEntitiesInBeam();
            beam.setLastProcessedLen(currentLen);
        }
    }

    // ====================== サーバーロジック ======================

    private static void executeServerLogic(Level level, DelayedSkillTask task) {
        switch (task.skillId) {
            case 1 -> {}
            case 2 -> {}
            case 3 -> {
                if (task.beamDir != null)
                    addBeam(level, task.pos, task.beamDir, 400, 200, 5.0f);
            }
        }
    }

    // ====================== タスク登録ヘルパー ======================

    private static void addSkillTask(Level level, UUID casterId, long executeAt,
                                     int skillId, Vec3 pos, Vec3 beamDir) {
        SERVER_TASKS.add(new DelayedSkillTask(executeAt, level.dimension(), pos, skillId, casterId, beamDir));
    }

    // ====================== パケット送信 ======================

    private static void sendEffectToClients(Level level, Vec3 pos, int skillId) {
        NETWORK.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(BlockPos.containing(pos))),
                new SkillEffectPacket(pos, skillId)
        );
    }

    // ====================== スキルロジック ======================

    private static void PE(GoMEntity boss, ServerPlayer target, Level level) {
        double offsetX = (random.nextDouble() - 0.5) * 25.0;
        double offsetZ = (random.nextDouble() - 0.5) * 25.0;
        Explosion explosion = level.explode(boss,
                target.getX() + offsetX, target.getY() + 5.0, target.getZ() + offsetZ,
                25.0F, ExplosionInteraction.NONE);
        if (explosion != null) explosion.finalizeExplosion(false);
        target.setDeltaMovement(0, 0, 0);
        target.connection.send(new ClientboundSetEntityMotionPacket(target));
    }

    private static void AK(GoMEntity boss, Entity entity, Level level) {
        if (!(entity instanceof LivingEntity living)) return;
        float killRatio  = Math.min(0.9999f,
                (float) (Math.log(1 + mobKillCount) / Math.log(1 + 10000)));
        float maxHpInWorld = ((ServerLevel) level).players().stream()
                .map(p -> p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH))
                .map(Double::floatValue)
                .max(Float::compareTo)
                .orElse(20.0f);
        float newHealth = living.getHealth()
                - (living.getMaxHealth() * 0.1f
                + Math.max(maxHpInWorld, living.getMaxHealth()) * killRatio * 0.9f);
        living.setHealth(Math.max(0.0f, newHealth));
        living.animateHurt(0.0f);
        if (newHealth <= 0) living.kill();
    }
}