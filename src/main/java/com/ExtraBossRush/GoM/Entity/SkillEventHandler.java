package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Entity.GoMEntity;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import com.ExtraBossRush.GoM.Support.SU;
import com.ExtraBossRush.GoM.client.ARV2;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
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

        public boolean tick() {
            age++;
            if (age >= maxLife) return true;
            if (age < warmup) return false;
            float speed = 32.0f;
            float maxLen = 512.0f;
            float currentLen = Math.min(maxLen, speed * (age - warmup));
            float scanStart = lastProcessedLen;
            if (scanStart >= maxLen) return false;
            float step = Math.max(1.0f, beamSize * 0.5f);
            for (float d = scanStart; d < currentLen; d += step) {
                Vec3 tip = start.add(dir.scale(d));
                processTipImpact(tip, beamSize);
            }
            lastProcessedLen = currentLen;
            return false;
        }

        private void processTipImpact(Vec3 tip, float radius) {
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
            case 1 -> {
            }
            case 2 -> {
            }
            case 3 -> {
                final int size = 10;
                final int kan  = 20;
                final int kan2 = 200;
                final int Life = 800;
                for (int i = 0; i < 3; i++) {
                    new ARV2.Builder(sqmodel, mhtex, pos.add(0, (10 * i - 1), 0)).setSize(size + (10 * i)).setAlphaAnim(new float[]{0, 0, 1, 1, 0}, new int[]{0, kan * i, kan * i, Life - 10, Life}).setRotAnim(new float[]{0, 0}, new float[]{0, 0}, new float[]{0, (float) ((1080 * (random.nextBoolean() ? 1 : -1) + (random.nextDouble() * 2 - 1) * 720))}, new int[]{0, Life}).setMaxLife(Life).spawn();
                }
                new ARV2.Builder(spmodel, whitex, pos.add(0, -10, 0)).setRot(90, 0, 0).setAlphaAnim(new float[]{1, 1, 0}, new int[]{0, Life - 10, Life}).setSizeAnim(new float[]{0, 5}, new int[]{0, kan2}).setMaxLife(Life).spawn();
                new ARV2.Builder(bemodel, whitex, pos.add(0, -10, 0)).setRot(90, 0, 0).asBeam2(32.0f, 512.0f, 32.0f).setAlphaAnim(new float[]{0, 0, 1, 1, 0}, new int[]{0, kan2, kan2, Life - 10, Life}).setSize(5).setMaxLife(Life).spawn();
            }
        }
    }

    // ====================== GoMSkillEvent処理（SkillEventHandler優先） ======================
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
                float er = 128;
                List<Entity> targets = SU.getEntitiesWithinRadius(
                        (ServerLevel) level, boss.getX(), boss.getY(), boss.getZ(), er);
                for (Entity entity : targets) {
                    if (entity != boss &&
                            (entity instanceof Monster ||
                                    (entity instanceof Mob mob && mob.getTarget() != null) ||
                                    entity instanceof ServerPlayer)) {
                        AK(boss, entity, level);
                    }
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
                    Vec3 spawnPos = new Vec3(
                            center.x + x, 150, center.z + z
                    );
                    if (!level.isClientSide) {
                        addSkillTask(level, target.getUUID(), level.getGameTime() + delayTicks , skillId, spawnPos, beamDir);
                    }
                }
            }
        }
    }

    // ====================== Tick処理 ======================
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.level.isClientSide) return;

        long currentTime = event.level.getGameTime();

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

        ACTIVE_BEAMS.removeIf(beam -> {
            if (beam.level == event.level) {
                return beam.tick();
            }
            return false;
        });
    }

    // ====================== サーバーロジック ======================
    private static void executeServerLogic(Level level, DelayedSkillTask task) {
        switch (task.skillId) {
            case 1 -> {}
            case 2 -> {}
            case 3 -> {
                if (task.beamDir != null) {
                    float beamSize = 5.0f;
                    addBeam(level, task.pos, task.beamDir, 800, 200, beamSize);
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

    // ====================== PE / AK（SkillEventHandler優先） ======================
    private static void PE(GoMEntity boss, ServerPlayer target, Level level) {
        Random rand = new Random();
        double offsetX = (rand.nextDouble() - 0.5) * 25.0;
        double offsetZ = (rand.nextDouble() - 0.5) * 25.0;

        double x = target.getX() + offsetX;
        double y = target.getY() + 5.0;
        double z = target.getZ() + offsetZ;

        Explosion explosion = level.explode(boss, x, y, z, 25.0F, ExplosionInteraction.NONE);
        if (explosion != null) {
            explosion.finalizeExplosion(false);
        }

        target.setDeltaMovement(0, 0, 0);
        ServerGamePacketListenerImpl conn = target.connection;
        conn.send(new ClientboundSetEntityMotionPacket(target));
    }

    private static void AK(GoMEntity boss, Entity entity, Level level) {
        Random rand = new Random();
        double offsetX = (rand.nextDouble() - 0.5) * 25.0;
        double offsetZ = (rand.nextDouble() - 0.5) * 25.0;

        Explosion explosion = level.explode(
                boss,
                entity.getX() + offsetX, entity.getY() + 5.0, entity.getZ() + offsetZ,
                25.0F,
                ExplosionInteraction.NONE
        );
        if (explosion != null) {
            explosion.finalizeExplosion(false);
        }

        entity.setDeltaMovement(0, 0, 0);
        if (entity instanceof ServerPlayer serverPlayer) {
            ServerGamePacketListenerImpl conn = serverPlayer.connection;
            conn.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }
}