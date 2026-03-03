package com.ExtraBossRush.GoM.Entity;
import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Support.PSU;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoMEntity extends Monster {
    private static final EntityDataAccessor<String>  DATA_MASTER_ID    = SynchedEntityData.defineId(GoMEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   DATA_HEALTH_RATIO = SynchedEntityData.defineId(GoMEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_DUMMY     = SynchedEntityData.defineId(GoMEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_HURT      = SynchedEntityData.defineId(GoMEntity.class, EntityDataSerializers.BOOLEAN);
    private static final List<GoMMasterLogic> MASTER_LIST = new CopyOnWriteArrayList<>();
    private static final Random ID_RANDOM = new Random();
    public GoMEntity(EntityType<? extends GoMEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MASTER_ID,    "");
        this.entityData.define(DATA_HEALTH_RATIO, 1.0F);
        this.entityData.define(DATA_IS_DUMMY,     false);
        this.entityData.define(DATA_IS_HURT,      false);
    }
    public String  getMasterId()          { return this.entityData.get(DATA_MASTER_ID); }
    public void    setMasterId(String id) { this.entityData.set(DATA_MASTER_ID, id); }
    public boolean isDummy()              { return this.entityData.get(DATA_IS_DUMMY); }
    public void    setDummy(boolean v)    { this.entityData.set(DATA_IS_DUMMY, v); }
    public boolean isHurtDisplay()        { return this.entityData.get(DATA_IS_HURT); }
    public float   getErosionFactor()     { return 1.0F - this.entityData.get(DATA_HEALTH_RATIO); }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_IS_DUMMY.equals(key)) this.refreshDimensions();
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    1000.0D)
                .add(Attributes.ATTACK_DAMAGE,   15.0D)
                .add(Attributes.FOLLOW_RANGE,  1024.0D);
    }
    @Override public boolean isAttackable()      { return this.isDummy(); }
    @Override public boolean canBeCollidedWith() { return this.isDummy(); }
    @Override public boolean isPickable()        { return this.isDummy(); }
    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.isDummy()
                ? EntityDimensions.scalable(0.6F, 1.8F)
                : super.getDimensions(pose);
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.@NotNull MobEffectInstance effect) {
        return false;
    }
    @Override
    public void tick() {
    }
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.getEntity() instanceof GoMEntity) return false;
        if (!this.level().isClientSide() && this.isDummy()) {
            String id = this.getMasterId();
            for (GoMMasterLogic master : MASTER_LIST) {
                if (master.getId().equals(id)) {
                    master.applyProxyHurt(source, amount);
                    this.level().broadcastEntityEvent(this, (byte) 2);
                    return true;
                }
            }
        }
        return false;
    }
    public void internalDiscard() {
        super.discard();
    }
    private static String generateUniqueId() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) sb.append(ID_RANDOM.nextInt(10));
            String candidate = sb.toString();
            if (MASTER_LIST.stream().noneMatch(m -> m.getId().equals(candidate))) {
                return candidate;
            }
        }
    }
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("gomentity")
                        .requires(s -> s.hasPermission(2))
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            Vec3 spawnPos = player.position().add(player.getLookAngle().scale(5));
                            String id = generateUniqueId();
                            MASTER_LIST.add(new GoMMasterLogic(id, player.serverLevel(), spawnPos));
                            return 1;
                        })
        );
    }
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        MASTER_LIST.removeIf(master -> {
            if (master.getLevel() != level) return false;
            master.tickMaster();
            if (master.isDead()) {
                master.discard();
                return true;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        MASTER_LIST.forEach(GoMMasterLogic::discard);
        MASTER_LIST.clear();
    }
    public static class GoMMasterLogic {
        private final String id;
        private final ServerLevel level;
        private final ServerBossEvent bossEvent;
        private final GoMEntity displayEntity;
        private final Set<UUID> spawnedPlayers = new HashSet<>();
        private UUID dummyUUID = null;
        private Vec3 pos;
        private float health = 1000.0F;
        private static final float MAX_HEALTH = 1000.0F;
        private static final double SYNC_RANGE_SQ = 1024.0 * 1024.0;
        private int  tickCount      = 0;
        private int  lastSkill2Tick = 0;
        private int  lastSkill3Tick = 0;
        private int  masterHurtTime = 0;
        private long lastHurtTick   = 0;
        private static final long HURT_COOLDOWN_TICKS = 10;
        private int dummyRespawnCount = 0;
        private static final int MAX_DUMMY_RESPAWN = 50;
        private static final Random random = new Random();
        public GoMMasterLogic(String id, ServerLevel level, Vec3 pos) {
            this.id    = id;
            this.level = level;
            this.pos   = pos;
            this.bossEvent = new ServerBossEvent(
                    Component.literal("魔術の守護者"),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            this.bossEvent.setVisible(true);
            this.displayEntity = new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(), level);
            this.displayEntity.setPos(pos.x, pos.y, pos.z);
            this.displayEntity.setDummy(false);
            this.displayEntity.setMasterId(id);
            this.displayEntity.getEntityData().set(DATA_HEALTH_RATIO, 1.0F);
            this.displayEntity.getEntityData().set(DATA_IS_HURT, false);
        }
        public String      getId()    { return id; }
        public ServerLevel getLevel() { return level; }
        public boolean     isDead()   { return health <= 0; }
        public void tickMaster() {
            if (health <= 0) return;
            if (masterHurtTime > 0) {
                masterHurtTime--;
                if (masterHurtTime <= 0) {
                    displayEntity.getEntityData().set(DATA_IS_HURT, false);
                }
            }
            float erosion = 1.0F - (health / MAX_HEALTH);
            int processingPower = (int) Math.ceil(1.0F + erosion * 9.0F);
            for (int i = 0; i < processingPower; i++) {
                runLogic();
                tickCount++;
            }
            updateDummy();
            syncDisplayToPlayers();
            bossEvent.setProgress(health / MAX_HEALTH);
        }
        private void runLogic() {
            List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(level, pos.x, pos.y, pos.z, 1024.0);
            if (nearby.isEmpty() && !level.players().isEmpty()) {
                ServerPlayer target = level.players().get(random.nextInt(level.players().size()));
                pos = target.position().add(0, 10, 0);
            }
            if (tickCount - lastSkill2Tick >= 100) {
                lastSkill2Tick = tickCount;
                triggerSkills(2);
            }
            if (tickCount - lastSkill3Tick >= 600) {
                lastSkill3Tick = tickCount;
                triggerSkills(3);
            }
        }
        private void triggerSkills(int skillId) {
            PSU.getPlayersWithinRadius(level, pos.x, pos.y, pos.z, 1024.0)
                    .forEach(p -> MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(displayEntity, p, skillId)));
        }
        private void updateDummy() {
            GoMEntity dummy = getDummyEntity();
            boolean needsRespawn = dummy == null || dummy.isRemoved() || !dummy.isAlive();
            if (needsRespawn) {
                dummyRespawnCount++;
                if (dummyRespawnCount >= MAX_DUMMY_RESPAWN) {
                    kickAllPlayers();
                    health = 0;
                    return;
                }
                GoMEntity newDummy = new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(), level);
                newDummy.setPos(pos.x, pos.y, pos.z);
                newDummy.setDummy(true);
                newDummy.setMasterId(id);
                newDummy.setInvisible(true);
                level.addFreshEntity(newDummy);
                dummyUUID = newDummy.getUUID();
            } else {
                dummy.setPos(pos.x, pos.y, pos.z);
            }
        }
        private GoMEntity getDummyEntity() {
            if (dummyUUID == null) return null;
            Entity e = level.getEntity(dummyUUID);
            return (e instanceof GoMEntity g) ? g : null;
        }
        private void kickAllPlayers() {
            Component reason = Component.literal("異常を検出しました (´・ω・`)ｳｰﾝ…");
            for (ServerPlayer player : new ArrayList<>(level.getServer().getPlayerList().getPlayers())) {
                player.connection.disconnect(reason);
            }
        }
        private void syncDisplayToPlayers() {
            displayEntity.setPos(pos.x, pos.y, pos.z);
            displayEntity.getEntityData().set(DATA_HEALTH_RATIO, health / MAX_HEALTH);
            ClientboundTeleportEntityPacket tpPacket = new ClientboundTeleportEntityPacket(displayEntity);
            var dirtyData = displayEntity.getEntityData().packDirty();
            List<SynchedEntityData.DataValue<?>> dataToSend = new ArrayList<>();
            dataToSend.add(new SynchedEntityData.DataValue<>(
                    DATA_IS_HURT.getId(),
                    EntityDataSerializers.BOOLEAN,
                    displayEntity.getEntityData().get(DATA_IS_HURT)
            ));
            if (dirtyData != null) dataToSend.addAll(dirtyData);
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(pos) < SYNC_RANGE_SQ) {
                    if (!spawnedPlayers.contains(player.getUUID())) {
                        player.connection.send(new ClientboundAddEntityPacket(displayEntity));
                        player.connection.send(new ClientboundSetEntityDataPacket(
                                displayEntity.getId(),
                                displayEntity.getEntityData().getNonDefaultValues()
                        ));
                        spawnedPlayers.add(player.getUUID());
                    } else {
                        player.connection.send(tpPacket);
                        if (!dataToSend.isEmpty()) {
                            player.connection.send(new ClientboundSetEntityDataPacket(
                                    displayEntity.getId(), dataToSend));
                        }
                    }
                    bossEvent.addPlayer(player);
                } else {
                    if (spawnedPlayers.remove(player.getUUID())) {
                        player.connection.send(new ClientboundRemoveEntitiesPacket(displayEntity.getId()));
                    }
                    bossEvent.removePlayer(player);
                }
            }
        }
        public void applyProxyHurt(DamageSource source, float amount) {
            long currentTick = level.getGameTime();
            if (currentTick - lastHurtTick < HURT_COOLDOWN_TICKS) return;
            lastHurtTick = currentTick;
            float finalDamage = Math.min(amount * 0.2F, 10.0F);
            health = Math.max(0.0F, health - finalDamage);
            displayEntity.getEntityData().set(DATA_IS_HURT, true);
            displayEntity.getEntityData().set(DATA_HEALTH_RATIO, health / MAX_HEALTH);
            if (health <= 0) {
                bossEvent.setProgress(0.0F);
                bossEvent.removeAllPlayers();
                return;
            }
            masterHurtTime = 20;
            GoMEntity dummy = getDummyEntity();
            if (dummy != null) {
                dummy.hurtTime         = 20;
                dummy.invulnerableTime = 10;
            }
        }
        public void discard() {
            bossEvent.removeAllPlayers();
            GoMEntity dummy = getDummyEntity();
            if (dummy != null) dummy.internalDiscard();
            if (!spawnedPlayers.isEmpty()) {
                ClientboundRemoveEntitiesPacket removePacket =
                        new ClientboundRemoveEntitiesPacket(displayEntity.getId());
                for (ServerPlayer player : level.players()) {
                    if (spawnedPlayers.contains(player.getUUID())) {
                        player.connection.send(removePacket);
                    }
                }
                spawnedPlayers.clear();
            }
        }
    }
    @Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(GoMEntities.MAGIC_GUARDIAN.get(), MagicGuardianRenderer::new);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class MagicGuardianRenderer extends MobRenderer<GoMEntity, HumanoidModel<GoMEntity>> {
        private static final ResourceLocation TEXTURE =
                new ResourceLocation(ExtraBossRush.MOD_ID, "textures/entity/magic_guardian.png");
        private static final Random RND = new Random();
        public MagicGuardianRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.6F);
        }
        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull GoMEntity entity) {
            return TEXTURE;
        }
        @Override
        public void render(GoMEntity entity, float yaw, float partialTicks,
                           @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int light) {
            if (entity.isDummy()) return;
            super.render(entity, yaw, partialTicks, poseStack, buffer, light);
            if (entity.isHurtDisplay()) {
                renderLayer(entity, poseStack, buffer, partialTicks, light,
                        1.0F, 0.0F, 0.0F, 0.2F, true);
            }
            float erosionFactor = entity.getErosionFactor();
            float offset = erosionFactor * 0.04F - 0.02F;
            if (offset > 0.001F) {
                long seed = entity.getId() * 98765L + entity.tickCount * 12345L;
                Random rnd = new Random(seed);
                renderGlitch(entity, poseStack, buffer, partialTicks, light, offset, rnd,
                        1.0F, 0.0F, 0.0F, erosionFactor * 0.3F);
                renderGlitch(entity, poseStack, buffer, partialTicks, light, offset, rnd,
                        0.0F, 1.0F, 1.0F, erosionFactor * 0.3F);
            }
        }
        private void renderGlitch(GoMEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                  float partialTicks, int light, float offset, Random rnd,
                                  float r, float g, float b, float alpha) {
            poseStack.pushPose();
            float f = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            this.setupRotations(entity, poseStack, (float) entity.tickCount + partialTicks, f, partialTicks);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            float tx = (RND.nextFloat() * 2.0F - 1.0F) * offset + (rnd.nextFloat() - 0.5F) * 0.01F;
            float ty = (RND.nextFloat() * 2.0F - 1.0F) * offset + (rnd.nextFloat() - 0.5F) * 0.01F;
            float tz = (RND.nextFloat() * 2.0F - 1.0F) * offset + (rnd.nextFloat() - 0.5F) * 0.01F;
            poseStack.translate(tx, ty, tz);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
            this.model.renderToBuffer(poseStack, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
            poseStack.popPose();
        }
        private void renderLayer(GoMEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                 float partialTicks, int light,
                                 float r, float g, float b, float a, boolean isHurt) {
            poseStack.pushPose();
            float f = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            this.setupRotations(entity, poseStack, (float) entity.tickCount + partialTicks, f, partialTicks);
            poseStack.scale(-1.0002F, -1.0002F, 1.0002F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
            int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(isHurt));
            this.model.renderToBuffer(poseStack, vc, light, overlay, r, g, b, a);
            poseStack.popPose();
        }
    }
}