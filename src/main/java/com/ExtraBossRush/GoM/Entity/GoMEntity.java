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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoMEntity extends Monster {
    private static final List<GoMEntity> GHOST_ENTITIES = new CopyOnWriteArrayList<>();
    private final ServerBossEvent bossEvent;
    private static final double SYNC_RANGE_SQ = 1024.0 * 1024.0;
    private static final Random random = new Random();
    private static final EntityDataAccessor<Float> DATA_HEALTH_RATIO = SynchedEntityData.defineId(GoMEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_DUMMY = SynchedEntityData.defineId(GoMEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_HURT = SynchedEntityData.defineId(GoMEntity.class, EntityDataSerializers.BOOLEAN);
    private UUID linkedId;
    private GoMEntity dummyRef;
    private long lastHurtTick = 0;
    private static final long HURT_COOLDOWN_TICKS = 10;
    private long lastMasterCheckTick = 0;
    private static final long MASTER_CHECK_INTERVAL = 20;
    private int masterHurtTime = 0;

    public GoMEntity(EntityType<? extends GoMEntity> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.linkedId = UUID.randomUUID();
        this.bossEvent = new ServerBossEvent(
                Component.literal("魔術の守護者"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );
        this.bossEvent.setVisible(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_DUMMY, false);
        this.entityData.define(DATA_HEALTH_RATIO, 1.0f);
        this.entityData.define(DATA_IS_HURT, false);
    }

    public boolean isDummy() {
        return this.entityData.get(DATA_IS_DUMMY);
    }

    public void setDummy(boolean isDummy) {
        this.entityData.set(DATA_IS_DUMMY, isDummy);
    }

    public boolean isHurt() {
        return this.entityData.get(DATA_IS_HURT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_IS_DUMMY.equals(key)) {
            this.refreshDimensions();
        }
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 1024.0D);
    }

    public float getErosionFactor() {
        return 1.0F - this.entityData.get(DATA_HEALTH_RATIO);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            for (GoMEntity ghost : GHOST_ENTITIES) {
                ghost.bossEvent.removeAllPlayers();
                if (ghost.dummyRef != null) ghost.dummyRef.discard();
                ghost.remove(RemovalReason.UNLOADED_WITH_PLAYER);
            }
            GHOST_ENTITIES.clear();
        }
    }
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gomentity").requires(s -> s.hasPermission(2)).executes(c -> {
            spawnGhost(c.getSource().getPlayerOrException());
            return 1;
        }));
    }

    private static void spawnGhost(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        GoMEntity master = new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(), level);
        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(5));
        master.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        master.setDummy(false);
        GHOST_ENTITIES.add(master);
        master.sendSpawnPacket(player);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            for (GoMEntity ghost : GHOST_ENTITIES) {
                if (ghost.isDummy() || ghost.getHealth() <= 0) continue;
                if (ghost.masterHurtTime > 0) {
                    ghost.masterHurtTime--;
                    if (ghost.masterHurtTime <= 0) {
                        ghost.entityData.set(DATA_IS_HURT, false);
                    }
                }
                int processingPower = (int) Math.ceil(1.0F + (ghost.getErosionFactor() * 9.0F));
                for (int i = 0; i < processingPower; i++) {
                    ghost.tickGhost(serverLevel);
                }
            }
        }
    }

    public void tickGhost(ServerLevel level) {
        if (this.dummyRef == null || !this.dummyRef.isAlive() || this.dummyRef.isRemoved()) {
            GoMEntity newDummy = new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(), level);
            newDummy.setPos(this.getX(), this.getY(), this.getZ());
            newDummy.setDummy(true);
            newDummy.linkedId = this.linkedId;
            newDummy.setInvisible(true);
            level.addFreshEntity(newDummy);
            this.dummyRef = newDummy;
            this.dummyRef.refreshDimensions();
        }
        if (this.dummyRef != null) {
            this.dummyRef.setPos(this.getX(), this.getY(), this.getZ());
            this.hurtTime = this.dummyRef.hurtTime;
            this.dummyRef.invulnerableTime = this.invulnerableTime;
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < SYNC_RANGE_SQ) {
                this.dummyRef.sendSpawnPacket(player);
            }
        }
        List<ServerPlayer> nearbyPlayers = PSU.getPlayersWithinRadius(level, this.getX(), this.getY(), this.getZ(), 1024.0);
        if (nearbyPlayers.isEmpty()) {
            List<ServerPlayer> allPlayers = level.players();
            if (!allPlayers.isEmpty()) {
                ServerPlayer target = allPlayers.get(random.nextInt(allPlayers.size()));
                this.setPos(target.getX(), target.getY() + 10, target.getZ());
            }
        }
        if (this.getHealth() <= 0) {
            this.remove(RemovalReason.KILLED);
            return;
        }
        if (this.tickCount % 100 == 0) {
            //this.RandomSkill(level,1);
            this.RandomSkill(level,2);
        }
        if (this.tickCount % 600 == 0) {
            this.RandomSkill(level,3);
        }
        this.syncToNearbyPlayers(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.tickCount++;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDummy()) {
            long currentTick = this.level().getGameTime();
            if (currentTick - this.lastMasterCheckTick >= MASTER_CHECK_INTERVAL) {
                this.lastMasterCheckTick = currentTick;
                boolean masterExists = GHOST_ENTITIES.stream().anyMatch(m -> m.linkedId.equals(this.linkedId));
                if (!masterExists) this.discard();
            }
        }
    }

    @Override
    public boolean isAttackable() {
        return this.isDummy();
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isDummy();
    }

    @Override
    public boolean isPickable() {
        return this.isDummy();
    }

    @Override public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        if (this.isDummy()) {
            return EntityDimensions.scalable(0.6F, 1.8F);
        } return super.getDimensions(pose);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof GoMEntity) {
            return false;
        }
        if (this.isDummy()) {
            for (GoMEntity master : GHOST_ENTITIES) {
                if (master.linkedId.equals(this.linkedId)) {
                    master.proxyHurt(source, amount);
                    this.level().broadcastEntityEvent(this, (byte) 2);
                    return true;
                }
            }
        } else if (this.dummyRef != null) {
            return this.dummyRef.hurt(source, amount);
        }
        return false;
    }
    public void proxyHurt(DamageSource source, float amount) {
        long currentTick = this.level().getGameTime();
        if (currentTick - this.lastHurtTick < HURT_COOLDOWN_TICKS) return;
        this.lastHurtTick = currentTick;
        float finalDamage = Math.min(amount * 0.2F, 10.0F);
        this.setHealth(this.getHealth() - finalDamage);
        this.masterHurtTime = 20;
        this.entityData.set(DATA_IS_HURT, true);
        float ratio = Math.max(0.0F, this.getHealth() / this.getMaxHealth());
        this.entityData.set(DATA_HEALTH_RATIO, ratio);
        if (this.dummyRef != null) {
            this.dummyRef.hurtTime = 20;
            this.dummyRef.invulnerableTime = 10;
        }
        if (this.getHealth() <= 0) {
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.@NotNull MobEffectInstance effect) {
        return false;
    }

    public void sendSpawnPacket(ServerPlayer player) {
        player.connection.send(new ClientboundAddEntityPacket(this));
        player.connection.send(new ClientboundSetEntityDataPacket(this.getId(), this.getEntityData().getNonDefaultValues()));
    }

    private void syncToNearbyPlayers(ServerLevel level) {
        if (this.isDummy()) return;

        ClientboundTeleportEntityPacket tpPacket = new ClientboundTeleportEntityPacket(this);
        var dirtyData = this.getEntityData().packDirty();
        List<SynchedEntityData.DataValue<?>> forcedData = new ArrayList<>();
        forcedData.add(new SynchedEntityData.DataValue<>(
                DATA_IS_HURT.getId(),
                EntityDataSerializers.BOOLEAN,
                this.entityData.get(DATA_IS_HURT)
        ));
        if (dirtyData != null) {
            forcedData.addAll(dirtyData);
        }
        // またはもっとシンプルに：dirtyがなくても forcedData だけ送る
        // List<SynchedEntityData.DataValue<?>> dataToSend = forcedData.isEmpty() ? dirtyData : forcedData;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < SYNC_RANGE_SQ) {
                p.connection.send(tpPacket);

                // データ送信（forcedData を優先的に含める）
                p.connection.send(new ClientboundSetEntityDataPacket(
                        this.getId(),
                        forcedData.isEmpty() && dirtyData != null ? dirtyData : forcedData
                ));

                bossEvent.addPlayer(p);
            } else {
                bossEvent.removePlayer(p);
            }
        }
    }

    public void RandomSkill(ServerLevel level, int SkillID) {
        List<ServerPlayer> targets = PSU.getPlayersWithinRadius(level, this.getX(), this.getY(), this.getZ(), 1024.0);
        for (ServerPlayer target : targets) {
            MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(this, target, SkillID));
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!this.isDummy() && !this.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            ClientboundRemoveEntitiesPacket removePacket =
                    new ClientboundRemoveEntitiesPacket(this.getId());
            for (ServerPlayer p : serverLevel.players()) {
                p.connection.send(removePacket);
            }
        }
        super.remove(reason);
        if (!this.isDummy()) {
            if (dummyRef != null) dummyRef.discard();
            GHOST_ENTITIES.remove(this);
            bossEvent.removeAllPlayers();
        }
    }

    // --- Renderer ---
    @Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(GoMEntities.MAGIC_GUARDIAN.get(), MagicGuardianRenderer::new);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MagicGuardianRenderer extends MobRenderer<GoMEntity, HumanoidModel<GoMEntity>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/entity/magic_guardian.png");

        public MagicGuardianRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.6F);
        }
        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull GoMEntity entity) {
            return TEXTURE;
        }
        @Override
        public void render(GoMEntity entity, float yaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int light) {
            if (entity.isDummy()) return;
            super.render(entity, yaw, partialTicks, poseStack, buffer, light);

            if (entity.isHurt()) {
                poseStack.pushPose();
                float fh = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
                this.setupRotations(entity, poseStack, (float) entity.tickCount + partialTicks, fh, partialTicks);
                poseStack.scale(-1.0002F, -1.0002F, 1.0002F);
                poseStack.translate(0.0F, -1.501F, 0.0F);
                VertexConsumer vcHurt = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
                int overlayHurt = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true));
                this.model.renderToBuffer(poseStack, vcHurt, light, overlayHurt, 1.0F, 0.0F, 0.0F, 0.2F);
                poseStack.popPose();
            }

            // ブレ処理
            float offset = entity.getErosionFactor() * 0.04F - 0.02F;
            if (offset > 0.001F) {
                poseStack.pushPose();
                float f = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
                this.setupRotations(entity, poseStack, (float) entity.tickCount + partialTicks, f, partialTicks);
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(0.0F, -1.501F, 0.0F);
                Random rnd = new Random(entity.getId() * 98765L + entity.tickCount * 12345L);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
                int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(entity.hurtTime > 0));
                poseStack.pushPose();
                poseStack.translate(((random.nextFloat() * 2.0F - 1.0F) * offset + ((rnd.nextFloat() - 0.5F) * 0.01F)), ((random.nextFloat() * 2.0F - 1.0F) * offset + ((rnd.nextFloat() - 0.5F) * 0.01F)), ((random.nextFloat() * 2.0F - 1.0F) * offset + ((rnd.nextFloat() - 0.5F) * 0.01F)));
                this.model.renderToBuffer(poseStack, vc, light, overlay, 1.0F, 0.0F, 0.0F, entity.getErosionFactor() * 0.3F);
                poseStack.popPose();
                poseStack.pushPose();
                poseStack.translate(((random.nextFloat() * 2.0F - 1.0F) * offset + ((rnd.nextFloat() - 0.5F) * 0.01F)), ((random.nextFloat() * 2.0F - 1.0F) * offset + ((rnd.nextFloat() - 0.5F) * 0.01F)), ((random.nextFloat() * 2.0F - 1.0F) * offset + ((rnd.nextFloat() - 0.5F) * 0.01F)));
                this.model.renderToBuffer(poseStack, vc, light, overlay, 0.0F, 1.0F, 1.0F, entity.getErosionFactor() * 0.3F);
                poseStack.popPose();
                poseStack.popPose();
            }
        }
    }
}