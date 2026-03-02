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

@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.FORGE
)
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
            this.level = level;
            this.start = start;
            this.dir = dir.normalize();
            this.maxLife = maxLife;
            this.warmup = warmup;
            this.beamSize = beamSize;
        }
        // 年齢を進める
        public void incrementAge() {
            age++;
        }

        // 終了判定
        public boolean isExpired() {
            return age >= maxLife;
        }

        // warmup中か
        public boolean isWarmingUp() {
            return age < warmup;
        }

        // 現在の長さ計算
        public float getCurrentLen() {
            float speed = 32.0f;
            float maxLen = 512.0f;
            return Math.min(maxLen, speed * Math.max(0, age - warmup));
        }

        // lastProcessedLen の getter / setter
        public float getLastProcessedLen() {
            return lastProcessedLen;
        }

        public void setLastProcessedLen(float value) {
            lastProcessedLen = value;
        }

        // ブロック破壊処理（public）
        public void processTipImpact(Vec3 tip, float radius) {
            int r = (int) Math.ceil(radius);
            int margin = 2;
            int rSq = (int) (radius * radius);
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
        public void damageEntitiesAtTip(Vec3 tip, float radius) {
            AABB aabb = new AABB(
                    tip.x - radius, tip.y - radius, tip.z - radius,
                    tip.x + radius, tip.y + radius, tip.z + radius
            );
            double rSq = (double) radius * radius;
            level.getEntities((Entity) null, aabb, entity -> {
                double dx = Math.max(0, Math.abs(entity.getX() - tip.x) - entity.getBbWidth() * 0.5);
                double dy = Math.max(0, Math.abs(entity.getY() + entity.getBbHeight() * 0.5 - tip.y) - entity.getBbHeight() * 0.5);
                double dz = Math.max(0, Math.abs(entity.getZ() - tip.z) - entity.getBbWidth() * 0.5);
                return dx*dx + dy*dy + dz*dz <= rSq;
            }).forEach(entity -> {
                if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
                if (entity instanceof ServerPlayer player &&
                        (player.isCreative() || player.isSpectator())) return;
                if (entity instanceof ServerPlayer player) {
                    if (GoMEventSubscriber.getRemainingResetTicks(player.getUUID()) < 5) {
                        GoMEventSubscriber.requestResetInvulnerableTicks(player, 200);
                    }
                } else {
                    living.invulnerableTime = 0;
                }
                living.hurt(level.damageSources().magic(), Integer.MAX_VALUE);
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0));
            });
        }

        // 新規追加：エンティティが円柱内にあるか判定（簡易中心点ベース）
        public boolean isEntityInCylinder(Entity entity, float currentLen) {
            Vec3 entityPos = entity.position();
            Vec3 center = entityPos.add(0, entity.getBbHeight() * 0.5, 0);  // 中心調整

            Vec3 toCenter = center.subtract(start);
            double proj = toCenter.dot(dir);

            if (proj < 0 || proj > currentLen) return false;

            double distSq = toCenter.lengthSqr() - proj * proj;
            double radiusSq = (double) beamSize * beamSize;

            return distSq <= radiusSq;
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
                () -> "1",
                "1"::equals,
                "1"::equals
        );
        NETWORK.registerMessage(
                0,
                SkillEffectPacket.class,
                SkillEffectPacket::encode,
                SkillEffectPacket::decode,
                SkillEffectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    private static class SkillEffectPacket {
        private final Vec3 pos;
        private final int skillId;

        public SkillEffectPacket(Vec3 pos, int skillId) {
            this.pos = pos;
            this.skillId = skillId;
        }

        public static void encode(SkillEffectPacket msg, FriendlyByteBuf buf) {
            buf.writeDouble(msg.pos.x);
            buf.writeDouble(msg.pos.y);
            buf.writeDouble(msg.pos.z);
            buf.writeInt(msg.skillId);
        }

        public static SkillEffectPacket decode(FriendlyByteBuf buf) {
            return new SkillEffectPacket(
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readInt()
            );
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
    private static Vec3 calculateBeamDir(float nP, float nY, float nR,
                                         float NP, float NY, float NR) {
        Quaternionf baseRot = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(nP),
                (float) Math.toRadians(nY),
                (float) Math.toRadians(nR)
        );
        Quaternionf addRot = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(NP),
                (float) Math.toRadians(NY),
                (float) Math.toRadians(NR)
        );
        baseRot.mul(addRot);
        Vector3f forward = new Vector3f(0, 0, 1);
        baseRot.transform(forward);
        return new Vec3(forward.x(), forward.y(), forward.z()).normalize();
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
                    new ARV2.Builder(sqmodel, mhtex, pos.add(0, (10 * i - 1), 0)).setSize(size + (10 * i)).setAlphaAnim(new float[]{0, 0, 1, 1, 0}, new int[]{0, kan * i, kan * i + (kan / 2), Life - 10, Life}).setRotAnim(new float[]{90,90}, new float[]{0,0}, new float[]{0, (float) ((1080 * (random.nextBoolean() ? 1 : -1) + (random.nextDouble() * 2 - 1) * 720))}, new int[]{0, Life}).setMaxLife(Life).spawn();
                }
                new ARV2.Builder(spmodel, whitex, pos.add(0, -10, 0)).setRot(90, 0, 0).setAlphaAnim(new float[]{1, 1, 0}, new int[]{0, Life - 10, Life}).setSizeAnim(new float[]{0, 5}, new int[]{0, kan2}).setMaxLife(Life).spawn();
                new ARV2.Builder(bemodel, whitex, pos.add(0, -10, 0)).setRot(90, 0, 0).asBeam2(32.0f, 512.0f, 32.0f).setAlphaAnim(new float[]{0, 0, 1, 1, 0}, new int[]{0, kan2, kan2, Life - 10, Life}).setSize(5).setMaxLife(Life).spawn();
            }
        }
    }

    // ====================== GoMSkillEvent処理 ======================
    @SubscribeEvent
    public static void onGoMSkill(GoMSkillEvent event) {
        GoMEntity boss      = event.getBoss();
        ServerPlayer target = event.getTarget();
        Level level         = target.level();
        int skillId         = event.getSkillId();
        long delayTicks = 100;
        Vec3 beamDir    = null;
        switch (skillId) {
            case 1 -> {
                PE(boss, target, level);
            }
            case 2 -> {
                float er = 1024;
                List<Entity> targets = SU.getEntitiesWithinRadius(
                        (ServerLevel) level, boss.getX(), boss.getY(), boss.getZ(), er);
                for (Entity entity : targets) {
                    if (entity == boss || entity instanceof GoMEntity) continue;
                    if (entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) continue;
                    AK(boss, entity, level);
                }
            }
            case 3 -> {
                Vec3 center  = new Vec3(boss.getX(), 150, boss.getZ());
                float radius = 256;
                delayTicks = 0;
                float nP = 0.0f;
                float nY = 0.0f;
                float nR = 0.0f;
                float NP = 90.0f;
                float NY = 0.0f;
                float NR = 0.0f;
                beamDir = calculateBeamDir(nP, nY, nR, NP, NY, NR);
                for (int i = 0; i < 50; i++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double r = radius * Math.sqrt(random.nextDouble());
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Vec3 spawnPos = new Vec3(center.x + x, 150, center.z + z);
                    if (!level.isClientSide) {
                        addSkillTask(level, target.getUUID(), level.getGameTime() + delayTicks, skillId, spawnPos, beamDir);
                    }
                }
                radius = 1024;
                for (int i = 0; i < 100; i++) {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double r = radius * Math.sqrt(random.nextDouble());
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Vec3 spawnPos = new Vec3(center.x + x, 150, center.z + z);
                    if (!level.isClientSide) {
                        addSkillTask(level, target.getUUID(), level.getGameTime() + delayTicks, skillId, spawnPos, beamDir);
                    }
                }
            }
        }
    }

    // ====================== Tick処理 ======================
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        ServerLevel level = (ServerLevel) event.level;
        long currentTime = level.getGameTime();
        Iterator<DelayedSkillTask> it = SERVER_TASKS.iterator();
        while (it.hasNext()) {
            DelayedSkillTask task = it.next();
            if (!event.level.dimension().equals(task.dimensionKey)) continue;
            if (currentTime < task.executeAt) continue;
            if (!event.level.isLoaded(BlockPos.containing(task.pos))) {
                it.remove();
                continue;
            }
            executeServerLogic(event.level, task);
            sendEffectToClients(event.level, task.pos, task.skillId);
            it.remove();
        }
        Iterator<BeamInstance> beamIt = ACTIVE_BEAMS.iterator();
        while (beamIt.hasNext()) {
            BeamInstance beam = beamIt.next();

            if (beam.level != level) {  // ← event.level → level
                beamIt.remove();
                continue;
            }

            beam.incrementAge();

            if (beam.isExpired()) {
                beamIt.remove();
                continue;
            }
            if (beam.isWarmingUp()) continue;
            float currentLen = beam.getCurrentLen();
            if (ARV2Config.SERVER_SPEC.isLoaded() && ARV2Config.ENABLE_BEAM_BREAK.get()) {
                float scanStartBlock = beam.getLastProcessedLen();
                float step = Math.max(1.0f, beam.beamSize - 0.5f);
                for (float d = scanStartBlock; d < currentLen; d += step) {
                    Vec3 tip = beam.start.add(beam.dir.scale(d));
                    beam.processTipImpact(tip, beam.beamSize);
                }
            }
            Vec3 end = beam.start.add(beam.dir.scale(currentLen));
            double margin = beam.beamSize + 2.0;
            AABB cylinderAABB = new AABB(
                    Math.min(beam.start.x, end.x) - margin, Math.min(beam.start.y, end.y) - margin, Math.min(beam.start.z, end.z) - margin,
                    Math.max(beam.start.x, end.x) + margin, Math.max(beam.start.y, end.y) + margin, Math.max(beam.start.z, end.z) + margin
            );
            level.getEntities((Entity) null, cylinderAABB, e -> e instanceof LivingEntity)
                    .forEach(entity -> {
                        LivingEntity living = (LivingEntity) entity;  // ← キャストOKになる
                        if (living instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) return;
                        if (beam.isEntityInCylinder(entity, currentLen)) {
                            if (living instanceof ServerPlayer player) {
                                if (GoMEventSubscriber.getRemainingResetTicks(player.getUUID()) < 5) {
                                    GoMEventSubscriber.requestResetInvulnerableTicks(player, 200);
                                }
                            } else {
                                living.invulnerableTime = 0;
                            }

                            living.hurt(level.damageSources().magic(), 20.0f);  // ← int → float に修正（1.20+対応）
                            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        }
                    });

            beam.setLastProcessedLen(currentLen);
        }
    }
    // ====================== サーバーロジック ======================
    private static void executeServerLogic(Level level, DelayedSkillTask task) {
        switch (task.skillId) {
            case 1 -> {
            }
            case 2 -> {
            }
            case 3 -> {
                if (task.beamDir != null) {
                    addBeam(level, task.pos, task.beamDir, 400, 200, 5.0f);
                }
            }
        }
    }
    // ====================== タスク登録ヘルパー ======================
    private static void addSkillTask(Level level, UUID casterId, long executeAt,
                                     int skillId, Vec3 pos, Vec3 beamDir) {
        SERVER_TASKS.add(new DelayedSkillTask(
                executeAt,
                level.dimension(),
                pos,
                skillId,
                casterId,
                beamDir
        ));
    }

    // ====================== パケット送信 ======================
    private static void sendEffectToClients(Level level, Vec3 pos, int skillId) {
        NETWORK.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(BlockPos.containing(pos))),
                new SkillEffectPacket(pos, skillId)
        );
    }

    // ====================== PE / AK ======================
    private static void PE(GoMEntity boss, ServerPlayer target, Level level) {
        Random rand = new Random();
        double offsetX = (rand.nextDouble() - 0.5) * 25.0;
        double offsetZ = (rand.nextDouble() - 0.5) * 25.0;
        double x = target.getX() + offsetX;
        double y = target.getY() + 5.0;
        double z = target.getZ() + offsetZ;
        Explosion explosion = level.explode(boss, x, y, z, 25.0F, ExplosionInteraction.NONE);
        if (explosion != null) explosion.finalizeExplosion(false);
        target.setDeltaMovement(0, 0, 0);
        target.connection.send(new ClientboundSetEntityMotionPacket(target));
    }
    private static void AK(GoMEntity boss, Entity entity, Level level) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
        float killRatio = Math.min(0.9999f,
                (float)(Math.log(1 + mobKillCount) / Math.log(1 + 10000)));
        float maxHpInWorld = ((ServerLevel) level).players().stream()
                .map(p -> p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH))
                .map(Double::floatValue)
                .max(Float::compareTo)
                .orElse(20.0f);
        float newHealth = living.getHealth() - (living.getMaxHealth() * 0.1f + Math.max(maxHpInWorld, living.getMaxHealth()) * killRatio * 0.9f);
        living.setHealth(Math.max(0.0f, newHealth));
        living.animateHurt(0.0f);
        if (newHealth <= 0) living.kill();
    }
}