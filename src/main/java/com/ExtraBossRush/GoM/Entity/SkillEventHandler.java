package com.ExtraBossRush.GoM.Entity;
import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.ARV2Config;
import com.ExtraBossRush.GoM.Entity.GoMEntity;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import com.ExtraBossRush.GoM.Support.EffectRenderType;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraft.world.entity.Entity.RemovalReason;
import com.ExtraBossRush.GoM.Kill.GoMForcedDamage;
import com.ExtraBossRush.GoM.Kill.IGoMKillable;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkillEventHandler {
    private static final Random random = new Random();
    private static final ResourceLocation SQMODEL = new ResourceLocation(ExtraBossRush.MOD_ID, "models/block/square.obj");
    private static final ResourceLocation SPMODEL = new ResourceLocation(ExtraBossRush.MOD_ID, "models/block/sphere.obj");
    private static final ResourceLocation BEMODEL = new ResourceLocation(ExtraBossRush.MOD_ID, "models/block/beam1.obj");
    private static final ResourceLocation MHTEX = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/block/mhz4-1whi.png");
    private static final ResourceLocation WHITEX = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/block/whi.png");
    private static final ResourceLocation BLATEX = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/block/bla.png");
    public static final java.util.concurrent.ConcurrentHashMap<Integer, Entity> TRACKED_ENTITIES = new java.util.concurrent.ConcurrentHashMap<>();
    @SubscribeEvent public static void OnEJ(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (e instanceof GoMEntity) return;
        TRACKED_ENTITIES.put(e.getId(), e);
    }

    @SubscribeEvent public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        TRACKED_ENTITIES.remove(event.getEntity().getId());
    }

    private static int mobKillCount = 0;
    @SubscribeEvent public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) mobKillCount++;
    }

    @FunctionalInterface public interface DynamicVec3 { Vec3 compute(int age, Vec3 startPos, Vec3 currentDir); }
    @FunctionalInterface public interface BeamHitHandler { void onHit(Level level, LivingEntity entity, int age, Vec3 hitPos); }
    @FunctionalInterface public interface BeamBlockHandler { void onBlock(Level level, BlockPos pos, int age); }

    static float bLerp(float[] v, int[] t, float tick) {
        if (v == null || v.length == 0) return 1f;
        if (v.length == 1 || tick <= t[0]) return v[0];
        if (tick >= t[t.length-1]) return v[v.length-1];
        for (int i = 0; i < t.length-1; i++)
            if (tick < t[i+1]) return v[i] + (v[i+1]-v[i])*((tick-t[i])/(float)(t[i+1]-t[i]));
        return v[v.length-1];
    }

    static float[] hsvToRgb(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;
        int hi = (int)(h * 6); float f = h*6 - hi;
        float p = v*(1-s), q = v*(1-f*s), t_ = v*(1-(1-f)*s);
        return switch (hi % 6) {
            case 0 -> new float[]{v, t_, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t_};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t_, p, v};
            default -> new float[]{v, p, q};
        };
    }

    public sealed interface RotBehavior permits RotBehavior.Static, RotBehavior.TrackLocalPlayer, RotBehavior.Spin, RotBehavior.Scaled, RotBehavior.Animated, RotBehavior.TrackEntityWithMaxSpeed {
        ARV2.DynamicVec3 toRotFn(Vec3 origin);
        void encode(FriendlyByteBuf buf);
        @FunctionalInterface interface Decoder { RotBehavior decode(FriendlyByteBuf buf); }
        Map<Integer, Decoder> DECODERS = new LinkedHashMap<>(Map.of(0, Static::decode, 1, TrackLocalPlayer::decode, 2, Spin::decode, 3, Animated::decode, 4, TrackEntityWithMaxSpeed::decode));
        static RotBehavior decode(FriendlyByteBuf buf) {
            int id = buf.readByte();
            return DECODERS.getOrDefault(id, b -> new Static(0, 0, 0)).decode(buf);
        }
        default RotBehavior timeScale(float scale) { return new Scaled(this, scale); }

        record Static(float pitch, float yaw, float roll) implements RotBehavior {
            @Override public ARV2.DynamicVec3 toRotFn(Vec3 o) { return t -> new Vec3(pitch, yaw, roll); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(0); buf.writeFloat(pitch); buf.writeFloat(yaw); buf.writeFloat(roll); }
            static Static decode(FriendlyByteBuf buf) { return new Static(buf.readFloat(), buf.readFloat(), buf.readFloat()); }
        }

        final class TrackLocalPlayer implements RotBehavior {
            public final float degreesPerTick; public final float initPitch, initYaw;
            private final float[] prev; private int lastTick = -1;
            public TrackLocalPlayer(float degreesPerTick, float initPitch, float initYaw) {
                this.degreesPerTick = degreesPerTick; this.initPitch = initPitch; this.initYaw = initYaw;
                this.prev = new float[]{initPitch, initYaw};
            }
            @Override public ARV2.DynamicVec3 toRotFn(Vec3 origin) {
                return t -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return new Vec3(prev[0], prev[1], 0);
                    float partial = t - (int)t;
                    Vec3 eye = mc.player.getEyePosition(partial);
                    Vec3 target = new Vec3(eye.x, mc.player.position().y + mc.player.getEyeHeight() * 0.5, eye.z);
                    double dx = target.x - origin.x, dy = target.y - origin.y, dz = target.z - origin.z;
                    float tY = (float)Math.toDegrees(Math.atan2(dx, dz));
                    float tP = (float)-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
                    int tick = (int)t;
                    if (tick != lastTick) {
                        float diffY = ((tY - prev[1]) % 360 + 540) % 360 - 180;
                        float diffP = ((tP - prev[0]) % 360 + 540) % 360 - 180;
                        prev[1] += Math.max(-degreesPerTick, Math.min(degreesPerTick, diffY));
                        prev[0] += Math.max(-degreesPerTick, Math.min(degreesPerTick, diffP));
                        lastTick = tick;
                    }
                    return new Vec3(prev[0], prev[1], 0);
                };
            }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(1); buf.writeFloat(degreesPerTick); buf.writeFloat(initPitch); buf.writeFloat(initYaw); }
            static TrackLocalPlayer decode(FriendlyByteBuf buf) { return new TrackLocalPlayer(buf.readFloat(), buf.readFloat(), buf.readFloat()); }
        }

        record Spin(float pitchPerTick, float yawPerTick, float rollPerTick) implements RotBehavior {
            @Override public ARV2.DynamicVec3 toRotFn(Vec3 o) { return t -> new Vec3(pitchPerTick*t % 360, yawPerTick*t % 360, rollPerTick*t % 360); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(2); buf.writeFloat(pitchPerTick); buf.writeFloat(yawPerTick); buf.writeFloat(rollPerTick); }
            static Spin decode(FriendlyByteBuf buf) { return new Spin(buf.readFloat(), buf.readFloat(), buf.readFloat()); }
        }

        final class Scaled implements RotBehavior {
            private final RotBehavior base; private final float scale;
            Scaled(RotBehavior base, float scale) { this.base=base; this.scale=scale; }
            @Override public ARV2.DynamicVec3 toRotFn(Vec3 o) { ARV2.DynamicVec3 fn=base.toRotFn(o); return t -> fn.get(t*scale); }
            @Override public void encode(FriendlyByteBuf buf) { base.encode(buf); }
        }

        final class Animated implements RotBehavior {
            private final float[] xk, yk, zk; private final int[] times;
            Animated(float[] xk, float[] yk, float[] zk, int[] times) { this.xk=xk; this.yk=yk; this.zk=zk; this.times=times; }
            @Override public ARV2.DynamicVec3 toRotFn(Vec3 o) { return t -> new Vec3(bLerp(xk,times,t), bLerp(yk,times,t), bLerp(zk,times,t)); }
            @Override public void encode(FriendlyByteBuf buf) {
                buf.writeByte(3); buf.writeInt(xk.length);
                for (float v : xk) buf.writeFloat(v); for (float v : yk) buf.writeFloat(v); for (float v : zk) buf.writeFloat(v);
                buf.writeInt(times.length); for (int t : times) buf.writeInt(t);
            }
            static Animated decode(FriendlyByteBuf buf) {
                int n = buf.readInt(); float[] xk=new float[n], yk=new float[n], zk=new float[n];
                for(int i=0;i<n;i++) xk[i]=buf.readFloat(); for(int i=0;i<n;i++) yk[i]=buf.readFloat(); for(int i=0;i<n;i++) zk[i]=buf.readFloat();
                int tn=buf.readInt(); int[] times=new int[tn]; for(int i=0;i<tn;i++) times[i]=buf.readInt();
                return new Animated(xk, yk, zk, times);
            }
        }
        final class TrackEntityWithMaxSpeed implements RotBehavior {
            public final int targetEntityId;
            public final float maxDegreesPerTick;
            private final float[] prevRot; // [pitch, yaw]
            private int lastTick = -1;
            public TrackEntityWithMaxSpeed(int targetEntityId, float maxDegreesPerTick) {
                this.targetEntityId = targetEntityId;
                this.maxDegreesPerTick = Math.max(0.1f, maxDegreesPerTick);
                this.prevRot = new float[]{0f, 0f};
            }
            @Override
            public ARV2.DynamicVec3 toRotFn(Vec3 origin) {
                return t -> {
                    Level level = Minecraft.getInstance().level;
                    if (level == null) return new Vec3(prevRot[0], prevRot[1], 0);
                    Entity target = level.getEntity(targetEntityId);
                    if (target == null || !target.isAlive()) {
                        return new Vec3(prevRot[0], prevRot[1], 0);
                    }
                    Vec3 eyePos = target.getEyePosition((float)(t % 1f));
                    Vec3 diff = eyePos.subtract(origin);
                    if (diff.lengthSqr() < 1e-6) {
                        return new Vec3(prevRot[0], prevRot[1], 0);
                    }
                    float targetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x));
                    float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
                    int currentTick = (int) t;
                    if (currentTick != lastTick) {
                        float yawDiff = targetYaw - prevRot[1];
                        yawDiff = ((yawDiff % 360) + 540) % 360 - 180;
                        float pitchDiff = targetPitch - prevRot[0];
                        float yawTurn = Math.max(-maxDegreesPerTick, Math.min(maxDegreesPerTick, yawDiff));
                        float pitchTurn = Math.max(-maxDegreesPerTick, Math.min(maxDegreesPerTick, pitchDiff));
                        prevRot[1] += yawTurn;
                        prevRot[0] += pitchTurn;
                        lastTick = currentTick;
                    }
                    return new Vec3(prevRot[0], prevRot[1], 0);
                };
            }
            @Override
            public void encode(FriendlyByteBuf buf) {
                buf.writeByte(4);
                buf.writeInt(targetEntityId);
                buf.writeFloat(maxDegreesPerTick);
            }
            static TrackEntityWithMaxSpeed decode(FriendlyByteBuf buf) {
                return new TrackEntityWithMaxSpeed(buf.readInt(), buf.readFloat());
            }
        }
    }

    public sealed interface SizeBehavior permits SizeBehavior.KeyFramed, SizeBehavior.Fixed, SizeBehavior.Pulse, SizeBehavior.Shrink, SizeBehavior.Composed {
        ARV2.DynamicFloat toSizeFn();
        void encode(FriendlyByteBuf buf);
        @FunctionalInterface interface Decoder { SizeBehavior decode(FriendlyByteBuf buf); }
        Map<Integer, Decoder> DECODERS = new LinkedHashMap<>(Map.of(0, KeyFramed::decode, 1, Fixed::decode, 2, Pulse::decode, 3, Shrink::decode, 4, Composed::decode));
        static SizeBehavior decode(FriendlyByteBuf buf) {
            int id = buf.readByte(); return DECODERS.getOrDefault(id, b -> new Fixed(1f)).decode(buf);
        }
        default SizeBehavior multiply(float factor) { SizeBehavior b=this; return new Composed(b, t->b.toSizeFn().get(t)*factor); }
        default SizeBehavior add(float offset) { SizeBehavior b=this; return new Composed(b, t->b.toSizeFn().get(t)+offset); }
        default SizeBehavior clamp(float min, float max) { SizeBehavior b=this; return new Composed(b, t->Math.max(min,Math.min(max,b.toSizeFn().get(t)))); }
        default SizeBehavior then(SizeBehavior next) { SizeBehavior a=this; return new Composed(a, t->a.toSizeFn().get(t)*next.toSizeFn().get(t)); }
        default SizeBehavior timeScale(float scale) { SizeBehavior b=this; return new Composed(b, t->b.toSizeFn().get(t*scale)); }
        default SizeBehavior reverse(int maxLife) { SizeBehavior b=this; return new Composed(b, t->b.toSizeFn().get(maxLife-t)); }
        default SizeBehavior loop(int period) { SizeBehavior b=this; return new Composed(b, t->b.toSizeFn().get(t%period)); }
        default SizeBehavior noise(float amplitude, long seed) {
            SizeBehavior b=this; Random r=new Random(seed); float[]ns=new float[1024]; for(int i=0;i<1024;i++)ns[i]=r.nextFloat()*2-1;
            return new Composed(b, t->b.toSizeFn().get(t)+ns[((int)t&1023)]*amplitude);
        }
        static SizeBehavior presetExplosion(float start, float peak, int life) { return new KeyFramed(new float[]{start,peak,0.1f},new int[]{0,life/4,life}).clamp(0,peak*1.2f); }
        static SizeBehavior presetBreath(float min, float max, int period) { return new Pulse(min,max,period); }

        record KeyFramed(float[] keys, int[] times) implements SizeBehavior {
            @Override public ARV2.DynamicFloat toSizeFn() { return t->bLerp(keys,times,t); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(0); buf.writeInt(keys.length); for(float k:keys)buf.writeFloat(k); for(int t:times)buf.writeInt(t); }
            static KeyFramed decode(FriendlyByteBuf buf) { int n=buf.readInt(); float[]k=new float[n]; int[]t=new int[n]; for(int i=0;i<n;i++)k[i]=buf.readFloat(); for(int i=0;i<n;i++)t[i]=buf.readInt(); return new KeyFramed(k,t); }
        }
        record Fixed(float value) implements SizeBehavior {
            @Override public ARV2.DynamicFloat toSizeFn() { return t->value; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(1); buf.writeFloat(value); }
            static Fixed decode(FriendlyByteBuf buf) { return new Fixed(buf.readFloat()); }
        }
        record Pulse(float min, float max, float period) implements SizeBehavior {
            @Override public ARV2.DynamicFloat toSizeFn() { return t->min+(max-min)*(0.5f+0.5f*(float)Math.sin(2*Math.PI*t/period)); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(2); buf.writeFloat(min); buf.writeFloat(max); buf.writeFloat(period); }
            static Pulse decode(FriendlyByteBuf buf) { return new Pulse(buf.readFloat(),buf.readFloat(),buf.readFloat()); }
        }
        record Shrink(float start, float end, int duration) implements SizeBehavior {
            @Override public ARV2.DynamicFloat toSizeFn() { return t->start+(end-start)*Math.min(1f,t/(float)duration); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(3); buf.writeFloat(start); buf.writeFloat(end); buf.writeInt(duration); }
            static Shrink decode(FriendlyByteBuf buf) { return new Shrink(buf.readFloat(),buf.readFloat(),buf.readInt()); }
        }
        final class Composed implements SizeBehavior {
            private final SizeBehavior base; private final ARV2.DynamicFloat fn;
            Composed(SizeBehavior base, ARV2.DynamicFloat fn) { this.base=base; this.fn=fn; }
            @Override public ARV2.DynamicFloat toSizeFn() { return fn; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(4); base.encode(buf); }
            static Composed decode(FriendlyByteBuf buf) { return new Composed(SizeBehavior.decode(buf), t->1f); }
        }
    }

    public sealed interface AlphaBehavior permits AlphaBehavior.KeyFramed, AlphaBehavior.Fixed, AlphaBehavior.Pulse, AlphaBehavior.FadeOut, AlphaBehavior.Blink, AlphaBehavior.Composed {
        ARV2.DynamicFloat toAlphaFn();
        void encode(FriendlyByteBuf buf);
        @FunctionalInterface interface Decoder { AlphaBehavior decode(FriendlyByteBuf buf); }
        Map<Integer, Decoder> DECODERS = new LinkedHashMap<>(Map.of(0, KeyFramed::decode, 1, Fixed::decode, 2, Pulse::decode, 3, FadeOut::decode, 4, Blink::decode, 5, Composed::decode));
        static AlphaBehavior decode(FriendlyByteBuf buf) {
            int id = buf.readByte(); return DECODERS.getOrDefault(id, b -> new Fixed(1f)).decode(buf);
        }
        default AlphaBehavior multiply(float factor) { AlphaBehavior b=this; return new Composed(b, t->Math.max(0,Math.min(1,b.toAlphaFn().get(t)*factor))); }
        default AlphaBehavior then(AlphaBehavior next) { AlphaBehavior a=this; return new Composed(a, t->a.toAlphaFn().get(t)*next.toAlphaFn().get(t)); }
        default AlphaBehavior timeScale(float scale) { AlphaBehavior b=this; return new Composed(b, t->b.toAlphaFn().get(t*scale)); }
        default AlphaBehavior reverse(int maxLife) { AlphaBehavior b=this; return new Composed(b, t->b.toAlphaFn().get(maxLife-t)); }
        default AlphaBehavior loop(int period) { AlphaBehavior b=this; return new Composed(b, t->b.toAlphaFn().get(t%period)); }
        static AlphaBehavior presetFadeInOut(int life) { return new KeyFramed(new float[]{0,1,1,0},new int[]{0,life/5,life*4/5,life}); }

        record KeyFramed(float[] keys, int[] times) implements AlphaBehavior {
            @Override public ARV2.DynamicFloat toAlphaFn() { return t->bLerp(keys,times,t); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(0); buf.writeInt(keys.length); for(float k:keys)buf.writeFloat(k); for(int t:times)buf.writeInt(t); }
            static KeyFramed decode(FriendlyByteBuf buf) { int n=buf.readInt(); float[]k=new float[n]; int[]t=new int[n]; for(int i=0;i<n;i++)k[i]=buf.readFloat(); for(int i=0;i<n;i++)t[i]=buf.readInt(); return new KeyFramed(k,t); }
        }
        record Fixed(float value) implements AlphaBehavior {
            @Override public ARV2.DynamicFloat toAlphaFn() { return t->value; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(1); buf.writeFloat(value); }
            static Fixed decode(FriendlyByteBuf buf) { return new Fixed(buf.readFloat()); }
        }
        record Pulse(float min, float max, float period) implements AlphaBehavior {
            @Override public ARV2.DynamicFloat toAlphaFn() { return t->min+(max-min)*(0.5f+0.5f*(float)Math.sin(2*Math.PI*t/period)); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(2); buf.writeFloat(min); buf.writeFloat(max); buf.writeFloat(period); }
            static Pulse decode(FriendlyByteBuf buf) { return new Pulse(buf.readFloat(),buf.readFloat(),buf.readFloat()); }
        }
        record FadeOut(int startTick, int endTick) implements AlphaBehavior {
            @Override public ARV2.DynamicFloat toAlphaFn() { return t->t<=startTick?1f:t>=endTick?0f:1f-(t-startTick)/(float)(endTick-startTick); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(3); buf.writeInt(startTick); buf.writeInt(endTick); }
            static FadeOut decode(FriendlyByteBuf buf) { return new FadeOut(buf.readInt(),buf.readInt()); }
        }
        record Blink(float onTicks, float offTicks) implements AlphaBehavior {
            @Override public ARV2.DynamicFloat toAlphaFn() { return t->(t%(onTicks+offTicks))<onTicks?1f:0f; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(4); buf.writeFloat(onTicks); buf.writeFloat(offTicks); }
            static Blink decode(FriendlyByteBuf buf) { return new Blink(buf.readFloat(),buf.readFloat()); }
        }
        final class Composed implements AlphaBehavior {
            private final AlphaBehavior base; private final ARV2.DynamicFloat fn;
            Composed(AlphaBehavior base, ARV2.DynamicFloat fn) { this.base=base; this.fn=fn; }
            @Override public ARV2.DynamicFloat toAlphaFn() { return fn; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(5); base.encode(buf); }
            static Composed decode(FriendlyByteBuf buf) { return new Composed(AlphaBehavior.decode(buf), t->1f); }
        }
    }

    public sealed interface ColorBehavior permits ColorBehavior.Fixed, ColorBehavior.Pulse, ColorBehavior.Rainbow, ColorBehavior.HsvPulse, ColorBehavior.Composed {
        ARV2.DynamicColor toColorFn();
        void encode(FriendlyByteBuf buf);
        @FunctionalInterface interface Decoder { ColorBehavior decode(FriendlyByteBuf buf); }
        Map<Integer, Decoder> DECODERS = new LinkedHashMap<>(Map.of(0, Fixed::decode, 1, Pulse::decode, 2, Rainbow::decode, 3, HsvPulse::decode, 4, Composed::decode));
        static ColorBehavior decode(FriendlyByteBuf buf) {
            int id = buf.readByte(); return DECODERS.getOrDefault(id, b -> new Fixed(1f,1f,1f)).decode(buf);
        }
        default ColorBehavior multiply(float r, float g, float b) { ColorBehavior base=this; return new Composed(base, t->{float[]c=base.toColorFn().get(t);return new float[]{c[0]*r,c[1]*g,c[2]*b};}); }
        default ColorBehavior then(ColorBehavior next) { ColorBehavior a=this; return new Composed(a, t->{float[]c1=a.toColorFn().get(t),c2=next.toColorFn().get(t);return new float[]{c1[0]*c2[0],c1[1]*c2[1],c1[2]*c2[2]};}); }
        default ColorBehavior timeScale(float scale) { ColorBehavior base=this; return new Composed(base, t->base.toColorFn().get(t*scale)); }

        record Fixed(float r, float g, float b) implements ColorBehavior {
            @Override public ARV2.DynamicColor toColorFn() { return t->new float[]{r,g,b}; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(0); buf.writeFloat(r); buf.writeFloat(g); buf.writeFloat(b); }
            static Fixed decode(FriendlyByteBuf buf) { return new Fixed(buf.readFloat(),buf.readFloat(),buf.readFloat()); }
        }
        record Pulse(float r1, float g1, float b1, float r2, float g2, float b2, float period) implements ColorBehavior {
            @Override public ARV2.DynamicColor toColorFn() { return t->{float f=0.5f+0.5f*(float)Math.sin(2*Math.PI*t/period);return new float[]{r1+(r2-r1)*f,g1+(g2-g1)*f,b1+(b2-b1)*f};}; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(1);buf.writeFloat(r1);buf.writeFloat(g1);buf.writeFloat(b1);buf.writeFloat(r2);buf.writeFloat(g2);buf.writeFloat(b2);buf.writeFloat(period); }
            static Pulse decode(FriendlyByteBuf buf) { return new Pulse(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat()); }
        }
        record Rainbow(float speed) implements ColorBehavior {
            @Override public ARV2.DynamicColor toColorFn() { return t->hsvToRgb(t*speed,1f,1f); }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(2); buf.writeFloat(speed); }
            static Rainbow decode(FriendlyByteBuf buf) { return new Rainbow(buf.readFloat()); }
        }
        record HsvPulse(float hStart, float hEnd, float s, float v, float period) implements ColorBehavior {
            @Override public ARV2.DynamicColor toColorFn() { return t->{float f=0.5f+0.5f*(float)Math.sin(2*Math.PI*t/period);return hsvToRgb(hStart+(hEnd-hStart)*f,s,v);}; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(3);buf.writeFloat(hStart);buf.writeFloat(hEnd);buf.writeFloat(s);buf.writeFloat(v);buf.writeFloat(period); }
            static HsvPulse decode(FriendlyByteBuf buf) { return new HsvPulse(buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat()); }
        }
        final class Composed implements ColorBehavior {
            private final ColorBehavior base; private final ARV2.DynamicColor fn;
            Composed(ColorBehavior base, ARV2.DynamicColor fn) { this.base=base; this.fn=fn; }
            @Override public ARV2.DynamicColor toColorFn() { return fn; }
            @Override public void encode(FriendlyByteBuf buf) { buf.writeByte(4); base.encode(buf); }
            static Composed decode(FriendlyByteBuf buf) { return new Composed(ColorBehavior.decode(buf), t->new float[]{1,1,1}); }
        }
    }

    public static final class EffectDef {
        public final ResourceLocation model, texture;
        public final Vec3 offset;
        public final int maxLife, fixedSize;
        public final boolean asBeam;
        public final float beamTrailSpeed, beamTrailMaxLen, beamTrailSize;
        public final RotBehavior rotBehavior;
        public final SizeBehavior sizeBehavior;
        public final AlphaBehavior alphaBehavior;
        public final ColorBehavior colorBehavior;
        public final int lookTargetId;
        public final float trackingSpeed;
        public final EffectRenderType renderType;

        private EffectDef(Builder b) {
            model=b.model; texture=b.texture; offset=b.offset!=null?b.offset:Vec3.ZERO;
            maxLife=b.maxLife; fixedSize=b.fixedSize; asBeam=b.asBeam;
            beamTrailSpeed=b.beamTrailSpeed; beamTrailMaxLen=b.beamTrailMaxLen; beamTrailSize=b.beamTrailSize;
            rotBehavior=b.rotBehavior; sizeBehavior=b.sizeBehavior; alphaBehavior=b.alphaBehavior; colorBehavior=b.colorBehavior;
            lookTargetId = b.lookTargetId; trackingSpeed = b.trackingSpeed; this.renderType = b.renderType;
        }

        public static class Builder {
            final ResourceLocation model, texture;
            Vec3 offset; int maxLife=20, fixedSize=0;
            boolean asBeam; float beamTrailSpeed, beamTrailMaxLen, beamTrailSize;
            RotBehavior rotBehavior = new RotBehavior.Static(0,0,0);
            SizeBehavior sizeBehavior = new SizeBehavior.Fixed(1f);
            AlphaBehavior alphaBehavior = new AlphaBehavior.Fixed(1f);
            ColorBehavior colorBehavior = new ColorBehavior.Fixed(1f,1f,1f);
            int lookTargetId = -1;
            float trackingSpeed = 1.0f;
            EffectRenderType renderType = EffectRenderType.TRANSLUCENT;

            public Builder(ResourceLocation model, ResourceLocation texture) { this.model=model; this.texture=texture; }
            public Builder offset(double x, double y, double z) { offset=new Vec3(x,y,z); return this; }
            public Builder maxLife(int v) { maxLife=v; return this; }
            public Builder fixedSize(int v) { fixedSize=v; return this; }
            public Builder asBeam(float s, float l, float i) { asBeam=true; beamTrailSpeed=s; beamTrailMaxLen=l; beamTrailSize=i; return this; }
            public Builder rotBehavior(RotBehavior v) { rotBehavior=v; return this; }
            public Builder sizeBehavior(SizeBehavior v) { sizeBehavior=v; return this; }
            public Builder alphaBehavior(AlphaBehavior v) { alphaBehavior=v; return this; }
            public Builder colorBehavior(ColorBehavior v) { colorBehavior=v; return this; }
            public Builder rot(float x, float y, float z) { rotBehavior=new RotBehavior.Static(x,y,z); return this; }
            public Builder rotAnim(float[] xk, float[] yk, float[] zk, int[] t) { rotBehavior=new RotBehavior.Animated(xk,yk,zk,t); return this; }
            public Builder sizeAnim(float[] k, int[] t) { return sizeBehavior(new SizeBehavior.KeyFramed(k,t)); }
            public Builder alphaAnim(float[] k, int[] t) { return alphaBehavior(new AlphaBehavior.KeyFramed(k,t)); }
            public Builder lookAt(Entity entity) { lookTargetId = entity != null ? entity.getId() : -1; return this; }
            public Builder lookAt(Entity entity, float speed) { lookTargetId = entity != null ? entity.getId() : -1; trackingSpeed = Math.max(0.01f, speed); return this; }
            public Builder lookAtWithMaxSpeed(Entity entity, float maxDegreesPerTick) {lookTargetId = entity != null ? entity.getId() : -1; trackingSpeed = -Math.max(0.1f, maxDegreesPerTick); return this; }
            public Builder renderType(EffectRenderType type) { this.renderType = type; return this; }
            public EffectDef build() { return new EffectDef(this); }
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(model); buf.writeResourceLocation(texture);
            buf.writeDouble(offset.x); buf.writeDouble(offset.y); buf.writeDouble(offset.z);
            buf.writeInt(maxLife); buf.writeInt(fixedSize); buf.writeBoolean(asBeam);
            if (asBeam) { buf.writeFloat(beamTrailSpeed); buf.writeFloat(beamTrailMaxLen); buf.writeFloat(beamTrailSize); }
            rotBehavior.encode(buf); sizeBehavior.encode(buf); alphaBehavior.encode(buf); colorBehavior.encode(buf);
            buf.writeInt(lookTargetId); buf.writeFloat(trackingSpeed); buf.writeByte(renderType.id);
        }
        public static EffectDef decode(FriendlyByteBuf buf) {
            Builder b = new Builder(buf.readResourceLocation(), buf.readResourceLocation());
            b.offset(buf.readDouble(), buf.readDouble(), buf.readDouble());
            b.maxLife(buf.readInt()); b.fixedSize(buf.readInt());
            if (buf.readBoolean()) b.asBeam(buf.readFloat(), buf.readFloat(), buf.readFloat());
            b.rotBehavior(RotBehavior.decode(buf)); b.sizeBehavior(SizeBehavior.decode(buf));
            b.alphaBehavior(AlphaBehavior.decode(buf)); b.colorBehavior(ColorBehavior.decode(buf));
            b.lookTargetId = buf.readInt(); b.trackingSpeed = buf.readFloat(); b.renderType(EffectRenderType.fromId(buf.readByte()));
            return b.build();
        }
    }
    public sealed interface ServerAction permits ServerAction.BeamAction, ServerAction.SphereAction, ServerAction.NoAction {
        final class BeamAction implements ServerAction {
            public final Vec3 startPos, initialDir;
            public final int maxLife, warmup;
            public final float beamSize, speed, maxLen;
            public final DynamicVec3 dirFormula;
            public final BeamHitHandler onHit;
            public final BeamBlockHandler onBlock;
            private BeamAction(Builder b) {
                startPos=b.startPos; initialDir=b.initialDir.normalize(); maxLife=b.maxLife; warmup=b.warmup;
                beamSize=b.beamSize; speed=b.speed; maxLen=b.maxLen; dirFormula=b.dirFormula; onHit=b.onHit; onBlock=b.onBlock;
            }
            public static class Builder {
                final Vec3 startPos, initialDir;
                int maxLife=400, warmup=200; float beamSize=5f, speed=32f, maxLen=512f;
                DynamicVec3 dirFormula; BeamHitHandler onHit; BeamBlockHandler onBlock;
                public Builder(Vec3 s, Vec3 d) { startPos=s; initialDir=d; }
                public Builder maxLife(int v) { maxLife=v; return this; }
                public Builder warmup(int v) { warmup=v; return this; }
                public Builder beamSize(float v) { beamSize=v; return this; }
                public Builder speed(float v) { speed=v; return this; }
                public Builder maxLen(float v) { maxLen=v; return this; }
                public Builder dirFormula(DynamicVec3 f) { dirFormula=f; return this; }
                public Builder onHit(BeamHitHandler f) { onHit=f; return this; }
                public Builder onBlock(BeamBlockHandler f){ onBlock=f; return this; }
                public BeamAction build() { return new BeamAction(this); }
            }
        }
        record SphereAction(int radius, int shellsPerTick, boolean removeFluids) implements ServerAction {}
        record NoAction() implements ServerAction {}
    }

    public record GoMSkillContext(GoMEntity boss, ServerPlayer target, Level level, long now, Vec3 bossPos, Map<String, Object> params) {
        public GoMSkillContext(GoMEntity boss, ServerPlayer target, Level level, long now, Vec3 bossPos) {
            this(boss, target, level, now, bossPos, new HashMap<>());
        }
        public float bossHpRatio() { return boss.GetHPR(); }
        public double distToTarget() { return bossPos.distanceTo(target.position()); }
        public float getFloat(String k, float d) { Object v=params.get(k); return v instanceof Number n?n.floatValue():d; }
        public int getInt(String k, int d) { Object v=params.get(k); return v instanceof Number n?n.intValue():d; }
        public double getDouble(String k,double d){ Object v=params.get(k); return v instanceof Number n?n.doubleValue():d; }
        @SuppressWarnings("unchecked") public <T> T get(String k, T d) { Object v=params.get(k); return v!=null?(T)v:d; }
    }

    @FunctionalInterface public interface SkillCondition {
        boolean test(GoMSkillContext ctx);
        default SkillCondition and(SkillCondition o) { return ctx->test(ctx)&&o.test(ctx); }
        default SkillCondition or(SkillCondition o) { return ctx->test(ctx)||o.test(ctx); }
        default SkillCondition negate() { return ctx->!test(ctx); }
        static SkillCondition always() { return ctx->true; }
        static SkillCondition hpBelow(float r) { return ctx->ctx.bossHpRatio()<r; }
        static SkillCondition hpAbove(float r) { return ctx->ctx.bossHpRatio()>r; }
        static SkillCondition chance(float p) { return ctx->random.nextFloat()<p; }
        static SkillCondition distBelow(double b) { return ctx->ctx.distToTarget()<b; }
        static SkillCondition distAbove(double b) { return ctx->ctx.distToTarget()>b; }
        static SkillCondition targetIsAlive() { return ctx->ctx.target().isAlive(); }
        static SkillCondition paramEquals(String k,Object v){ return ctx->v.equals(ctx.params().get(k)); }
    }

    public static final class GoMSkill {
        private final long delayTicks; private final ServerAction action; private final List<EffectDef> effects;
        private final Consumer<GoMSkillContext> customLogic; private final Function<GoMSkillContext,Vec3> posResolver;
        private final List<SkillCondition> conditions; private final List<int[]> chains; private final Map<String,Object> params;

        private GoMSkill(Builder b) {
            delayTicks=b.delayTicks; action=b.action!=null?b.action:new ServerAction.NoAction();
            effects=b.effects!=null?Collections.unmodifiableList(b.effects):Collections.emptyList();
            customLogic=b.customLogic; posResolver=b.posResolver!=null?b.posResolver:ctx->ctx.bossPos();
            conditions=Collections.unmodifiableList(b.conditions); chains=Collections.unmodifiableList(b.chains);
            params=Collections.unmodifiableMap(b.params);
        }

        public void execute(GoMSkillContext ctx, int skillId) {
            if (ctx.level().isClientSide()) return;
            GoMSkillContext enriched = params.isEmpty() ? ctx : new GoMSkillContext(ctx.boss(),ctx.target(),ctx.level(),ctx.now(),ctx.bossPos(),mergeParams(ctx.params(),params));
            for (SkillCondition cond : conditions) if (!cond.test(enriched)) return;
            if (customLogic != null) customLogic.accept(enriched);
            Vec3 pos = posResolver.apply(enriched);
            addSkillTask(ctx.level(),ctx.target().getUUID(),ctx.now()+delayTicks,skillId,pos,action,effects);
            for (int[] chain : chains) {
                Function<GoMSkillContext,GoMSkill> factory = SKILL_REGISTRY.get(chain[0]);
                if (factory==null) continue;
                GoMSkillContext cc = new GoMSkillContext(ctx.boss(),ctx.target(),ctx.level(),ctx.now()+chain[1],pos,enriched.params());
                factory.apply(cc).execute(cc,chain[0]);
            }
        }

        private static Map<String,Object> mergeParams(Map<String,Object> base, Map<String,Object> extra) {
            Map<String,Object> m=new HashMap<>(base); m.putAll(extra); return m;
        }

        public static class Builder {
            long delayTicks=0; ServerAction action; List<EffectDef> effects=new ArrayList<>();
            Consumer<GoMSkillContext> customLogic; Function<GoMSkillContext,Vec3> posResolver;
            List<SkillCondition> conditions=new ArrayList<>(); List<int[]> chains=new ArrayList<>(); Map<String,Object> params=new HashMap<>();
            public Builder delay(long t) { delayTicks=t; return this; }
            public Builder action(ServerAction a) { action=a; return this; }
            public Builder effect(EffectDef e) { effects.add(e); return this; }
            public Builder effect(Consumer<EffectDef.Builder> cfg, ResourceLocation m, ResourceLocation t) { EffectDef.Builder b=new EffectDef.Builder(m,t); cfg.accept(b); effects.add(b.build()); return this; }
            public Builder effects(List<EffectDef> list) { effects.addAll(list); return this; }
            public Builder customLogic(Consumer<GoMSkillContext> f) { customLogic=f; return this; }
            public Builder pos(Function<GoMSkillContext,Vec3> f) { posResolver=f; return this; }
            public Builder condition(SkillCondition c) { conditions.add(c); return this; }
            public Builder chain(int skillId, long delayTicks) { chains.add(new int[]{skillId,(int)delayTicks}); return this; }
            public Builder param(String key, Object value) { params.put(key,value); return this; }
            public GoMSkill build() { return new GoMSkill(this); }
        }
    }

    private static final Map<Integer, Function<GoMSkillContext, GoMSkill>> SKILL_REGISTRY = new HashMap<>();

    static {
        SKILL_REGISTRY.put(1, ctx -> new GoMSkill.Builder().customLogic(c -> PE(c.boss(), c.target(), c.level())).build());
        SKILL_REGISTRY.put(2, ctx -> new GoMSkill.Builder().customLogic(c -> {
            List<Entity> targets = SU.getEntitiesWithinRadius((ServerLevel)c.level(), c.boss().getX(), c.boss().getY(), c.boss().getZ(), 1024);
            for (Entity e : targets) {
                if (e==c.boss() || e instanceof GoMEntity) continue;
                if (e instanceof ServerPlayer p && (p.isCreative()||p.isSpectator())) continue;
                AK(c.boss(), e, c.level());
            }
        }).build());
        SKILL_REGISTRY.put(3, ctx -> new GoMSkill.Builder().customLogic(c -> {
            Vec3 beamDir = calcDir(0,0,0,90,0,0);
            Vec3 center = new Vec3(c.boss().getX(), 150, c.boss().getZ());
            for (int i=0; i<50; i++) {
                double ang=random.nextDouble()*2*Math.PI, r=256*Math.sqrt(random.nextDouble());
                Vec3 sp=new Vec3(center.x+Math.cos(ang)*r, 150, center.z+Math.sin(ang)*r);
                addSkillTask(c.level(),c.target().getUUID(),c.now(),3,sp,buildBeamAction(sp,beamDir,null),buildBeamEffects());
            }
            for (int i=0; i<100; i++) {
                double ang=random.nextDouble()*2*Math.PI, r=1024*Math.sqrt(random.nextDouble());
                Vec3 sp=new Vec3(center.x+Math.cos(ang)*r, 150, center.z+Math.sin(ang)*r);
                addSkillTask(c.level(),c.target().getUUID(),c.now(),3,sp,buildBeamAction(sp,beamDir,null),buildBeamEffects());
            }
        }).build());
        SKILL_REGISTRY.put(4, ctx -> new GoMSkill.Builder().delay(100L).action(new ServerAction.SphereAction(512, 4, true)).build());
        SKILL_REGISTRY.put(5, ctx -> {
            Vec3 bossPos=ctx.bossPos(); double dist=bossPos.distanceTo(ctx.target().position());
            float factor=1.0f/(1.0f+(float)(dist/48.0));
            return new GoMSkill.Builder().customLogic(c -> {
                Vec3 delta=c.target().position().subtract(bossPos);
                Vec3 knockDir=delta.length()>0.01?delta.normalize():new Vec3(0,1,0);
                float kbStr=3.78f*factor;
                c.target().setDeltaMovement(c.target().getDeltaMovement().add(knockDir.x*kbStr, Math.max(knockDir.y*kbStr,0.35f*factor), knockDir.z*kbStr));
                c.target().connection.send(new ClientboundSetEntityMotionPacket(c.target()));
                float dmg=12.0f*factor;
                if (dmg>0.3f) c.target().hurt(c.level().damageSources().magic(), dmg);
                if (factor>0.5f) { int dur=(int)(40*factor); int amp=factor>0.75f?1:0; c.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,dur,amp)); }
            }).effect(b->b.rot(90,0,0).sizeAnim(new float[]{1,40},new int[]{0,30}).alphaAnim(new float[]{0.8f,0},new int[]{0,30}).maxLife(30).renderType(EffectRenderType.ADDITIVE), SPMODEL, WHITEX).build();
        });
        SKILL_REGISTRY.put(6, ctx -> new GoMSkill.Builder().effect(b->b.rot(90,0,0).sizeAnim(new float[]{0,8},new int[]{0,10}).alphaAnim(new float[]{1,1,0},new int[]{0,10,20}).maxLife(20).renderType(EffectRenderType.ADDITIVE), SPMODEL, WHITEX).build());
        /*SKILL_REGISTRY.put(7, ctx -> new GoMSkill.Builder().customLogic(c -> {
            final int skillDuration = 100;
            final int warmupTicks   = 5;
            final float trackMaxLen = 200f;
            final float trackSize   = 2.5f;
            final ServerPlayer tgt  = c.target();
            final Level level       = c.level();

            for (int bi = 0; bi < 6; bi++) {
                double rad = 32 + random.nextDouble() * 96;
                double az  = random.nextDouble() * 2 * Math.PI;
                double el  = (random.nextDouble() - 0.5) * Math.PI * 0.8;
                Vec3 sp = new Vec3(
                        c.boss().getX() + Math.cos(el) * Math.cos(az) * rad,
                        c.boss().getY() + 1 + Math.sin(el) * rad,
                        c.boss().getZ() + Math.cos(el) * Math.sin(az) * rad
                );
                final float MAX_DEGREES_PER_TICK = 0.2f;
                DynamicVec3 trackingFormula = (age, startPos, currentDir) -> {
                    if (tgt == null || !tgt.isAlive()) return currentDir;
                    Vec3 targetEye = tgt.position().add(0, tgt.getBbHeight() * 0.5, 0);
                    Vec3 desired = targetEye.subtract(startPos).normalize();
                    double dot = currentDir.dot(desired);
                    dot = Math.min(Math.max(dot, -1.0), 1.0);
                    double angleDiffDeg = Math.toDegrees(Math.acos(dot));
                    if (angleDiffDeg < 0.1) return currentDir;
                    double fraction = Math.min(1.0, MAX_DEGREES_PER_TICK / angleDiffDeg);
                    return currentDir.lerp(desired, fraction).normalize();
                };
                ServerAction.BeamAction action = new ServerAction.BeamAction.Builder(
                        sp, tgt.position().subtract(sp).normalize())
                        .maxLife(skillDuration)
                        .warmup(warmupTicks)
                        .speed(20f)
                        .maxLen(trackMaxLen)
                        .beamSize(trackSize)
                        .dirFormula(trackingFormula)
                        .build();

                List<EffectDef> efx = new ArrayList<>();
                efx.add(new EffectDef.Builder(BEMODEL, WHITEX)
                        .asBeam(20f, trackMaxLen, trackSize)
                        .lookAtWithMaxSpeed(tgt, 0.2f)
                        .maxLife(skillDuration)
                        .build());

                addSkillTask(level, tgt.getUUID(), c.now(), 7, sp, action, efx);
            }
        }).build());*/
        SKILL_REGISTRY.put(8, ctx -> new GoMSkill.Builder().condition(SkillCondition.hpBelow(0.5f)).customLogic(c -> {
            ServerLevel serverLevel = (ServerLevel) c.level();
            Vec3 bossCenter = c.bossPos();
            final int warmup = 20; final int beamDuration = 100; final int totalLife = warmup + beamDuration;
            List<Entity> allEntities = SU.getEntitiesWithinRadius(serverLevel, bossCenter.x, bossCenter.y, bossCenter.z, 1024);
            allEntities.removeIf(e -> e instanceof GoMEntity || (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())));
            List<Vec3> targetPositions = new ArrayList<>();
            for (Entity e : allEntities) {
                double eyeOff = e instanceof LivingEntity le ? le.getEyeHeight() * 0.5 : 0;
                targetPositions.add(e.position().add(0, eyeOff, 0));
            }
            Vec3 fallback = c.target().position().add(0, c.target().getEyeHeight() * 0.5, 0);
            for (int i = 0; i < 200; i++) {
                double ang = random.nextDouble() * 2 * Math.PI;
                double r = 32 + random.nextDouble() * 992;
                double yOff = (random.nextDouble() - 0.5) * 512.0;
                Vec3 spherePos = new Vec3(bossCenter.x + Math.cos(ang) * r, bossCenter.y + yOff, bossCenter.z + Math.sin(ang) * r);
                Vec3 nearest = fallback;
                double minDistSq = Double.MAX_VALUE;
                for (Vec3 tPos : targetPositions) {
                    double d = tPos.distanceToSqr(spherePos);
                    if (d < minDistSq) { minDistSq = d; nearest = tPos; }
                }
                Vec3 beamDir = nearest.subtract(spherePos).normalize();
                float yaw = (float) Math.toDegrees(Math.atan2(beamDir.x, beamDir.z));
                float pitch = (float) Math.toDegrees(Math.atan2(-beamDir.y, Math.hypot(beamDir.x, beamDir.z)));
                ServerAction.BeamAction beamAction = new ServerAction.BeamAction.Builder(spherePos, beamDir).maxLife(totalLife).warmup(warmup).beamSize(2f).speed(32f).maxLen(1024f).build();
                List<EffectDef> effects = new ArrayList<>();
                effects.add(new EffectDef.Builder(SPMODEL, WHITEX)
                        .offset(0, 0, 0)
                        .rotBehavior(new RotBehavior.Static(pitch, yaw, 0))
                        .maxLife(totalLife).build());

                effects.add(new EffectDef.Builder(SQMODEL, MHTEX)
                        .offset(0, 0, -1.2)
                        .fixedSize(14)
                        .rotBehavior(new RotBehavior.Static(pitch, yaw, 0))
                        .maxLife(totalLife).build());

                effects.add(new EffectDef.Builder(BEMODEL, WHITEX)
                        .offset(0, 0, 0)
                        .asBeam(32f, 1024f, 2.2f)
                        .rotBehavior(new RotBehavior.Static(pitch, yaw, 0))
                        .maxLife(totalLife).build());
                addSkillTask(c.level(), c.target().getUUID(), c.now(), 8, spherePos, beamAction, effects);
            }
        }).build());
    }

    private record DelayedSkillTask(long executeAt, ResourceKey<Level> dimensionKey, Vec3 pos, int skillId, UUID casterId, ServerAction action, List<EffectDef> effects) {}

    private static final List<DelayedSkillTask> SERVER_TASKS = new ArrayList<>();

    public static void addSkillTask(Level level, UUID casterId, long executeAt, int skillId, Vec3 pos, ServerAction action, List<EffectDef> effects) {
        SERVER_TASKS.add(new DelayedSkillTask(executeAt, level.dimension(), pos, skillId, casterId, action, effects!=null?effects:Collections.emptyList()));
    }
    public static void addSkillTask(Level level, UUID casterId, long executeAt, int skillId, Vec3 pos, ServerAction action) { addSkillTask(level,casterId,executeAt,skillId,pos,action,null); }
    public static void addSkillTask(Level level, UUID casterId, long executeAt, int skillId, Vec3 pos, List<EffectDef> effects) { addSkillTask(level,casterId,executeAt,skillId,pos,new ServerAction.NoAction(),effects); }

    private static final List<BeamInstance> ACTIVE_BEAMS = new ArrayList<>();

    public static class BeamInstance {
        private final Level level; private final ServerAction.BeamAction def;
        private int age=0; private Vec3 currentDir; private float lastProcessedLen=0;
        private final BlockPos.MutableBlockPos mPos=new BlockPos.MutableBlockPos();
        public BeamInstance(Level level, ServerAction.BeamAction def) { this.level=level; this.def=def; this.currentDir=def.initialDir; }
        public boolean tick() {
            age++;
            if (age>=def.maxLife) return true;
            if (age<def.warmup) return false;
            if (def.dirFormula!=null) { Vec3 nd=def.dirFormula.compute(age,def.startPos,currentDir); if(nd.lengthSqr()>1e-6)currentDir=nd.normalize(); }
            float currentLen=Math.min(def.maxLen, def.speed*(age-def.warmup));
            float scanFrom=(def.dirFormula!=null)?0f:lastProcessedLen;
            float step=Math.max(1.0f, def.beamSize*0.5f);
            if (ARV2Config.SERVER_SPEC.isLoaded()&&ARV2Config.ENABLE_BEAM_BREAK.get())
                for (float d=scanFrom; d<currentLen; d+=step) processBlock(def.startPos.add(currentDir.scale(d)),def.beamSize,age);
            for (float d=0; d<currentLen; d+=step) hitEntitiesAt(def.startPos.add(currentDir.scale(d)),def.beamSize,age);
            lastProcessedLen=currentLen; return false;
        }
        private void processBlock(Vec3 tip, float radius, int age) {
            int r=(int)Math.ceil(radius), margin=1;
            int rSq=(int)(radius*radius), outerRSq=(int)((radius+margin)*(radius+margin));
            int cx=(int)Math.floor(tip.x), cy=(int)Math.floor(tip.y), cz=(int)Math.floor(tip.z);
            for (int dx=-r-margin; dx<=r+margin; dx++) for (int dy=-r-margin; dy<=r+margin; dy++) for (int dz=-r-margin; dz<=r+margin; dz++) {
                mPos.set(cx+dx,cy+dy,cz+dz);
                if (!level.hasChunkAt(mPos)) continue;
                BlockState state=level.getBlockState(mPos);
                if (state.getDestroySpeed(level,mPos)<0||state.isAir()) continue;
                int dSq=dx*dx+dy*dy+dz*dz; if(dSq>outerRSq)continue;
                BlockPos bp=mPos.immutable();
                if (def.onBlock!=null) def.onBlock.onBlock(level,bp,age);
                else if (dSq<=rSq) level.setBlock(bp,Blocks.AIR.defaultBlockState(),2|16);
                else if (!level.getFluidState(bp).is(net.minecraft.tags.FluidTags.LAVA)) level.setBlock(bp,Blocks.LAVA.defaultBlockState(),2|16);
            }
        }
        private void hitEntitiesAt(Vec3 tip, float radius, int age) {
            AABB area=new AABB(tip.x-radius,tip.y-radius,tip.z-radius,tip.x+radius,tip.y+radius,tip.z+radius);
            List<Entity> entities=level.getEntities((Entity)null, area, e->e instanceof LivingEntity&&!(e instanceof GoMEntity)&&!(e instanceof ServerPlayer p&&(p.isCreative()||p.isSpectator())));
            for (Entity e : entities) {
                if (e.distanceToSqr(tip.x,tip.y,tip.z)>radius*radius) continue;
                LivingEntity living=(LivingEntity)e;
                if (def.onHit!=null) def.onHit.onHit(level,living,age,tip); else defaultHit(living);
            }
        }
        private void defaultHit(LivingEntity living) {
            living.invulnerableTime=0; living.hurt(GoMForcedDamage.create(level),20.0f);
            float forcedHp=Math.max(0.0f,living.getHealth()-20.0f); living.setHealth(forcedHp); living.animateHurt(0.0f);
            if (forcedHp<=0.0f) ((IGoMKillable)living).gom_setDeathFlag(true);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,4,false,false,false));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100,0,false,false,false));
        }
    }

    private static final java.util.Queue<CarveJob> CARVE_JOBS = new java.util.LinkedList<>();

    private static class CarveJob {
        private final ServerLevel level; private final Vec3 center;
        private final int maxRadius, shellsPerTick; private final boolean removeFluids;
        private int currentRadius=0; private boolean started=false; private int lastDamagedRadius=-1;
        CarveJob(ServerLevel level, Vec3 center, int maxRadius, int shellsPerTick, boolean removeFluids) {
            this.level=level; this.center=center; this.maxRadius=Math.max(0,maxRadius); this.shellsPerTick=Math.max(1,shellsPerTick); this.removeFluids=removeFluids;
        }
        boolean tickOnce() {
            int start=started?currentRadius+1:0, end=Math.min(start+shellsPerTick-1,maxRadius);
            started=true; if(start>maxRadius)return true;
            int cx=(int)Math.floor(center.x), cy=(int)Math.floor(center.y), cz=(int)Math.floor(center.z);
            java.util.Map<Long,java.util.List<BlockPos>> chunkMap=new java.util.LinkedHashMap<>();
            for (int r=start; r<=end; r++) {
                double rSqLo=(r-0.5)*(r-0.5), rSqHi=(r+0.5)*(r+0.5);
                int minY=Math.max(level.getMinBuildHeight(),cy-r), maxY=Math.min(level.getMaxBuildHeight(),cy+r);
                for (int x=cx-r; x<=cx+r; x++) {
                    int dxSq=(x-cx)*(x-cx); if(dxSq>rSqHi)continue;
                    for (int y=minY; y<=maxY; y++) {
                        int dySq=(y-cy)*(y-cy); if(dxSq+dySq>rSqHi)continue;
                        for (int z=cz-r; z<=cz+r; z++) {
                            double dSq=dxSq+dySq+(double)(z-cz)*(z-cz);
                            if(dSq<rSqLo||dSq>rSqHi)continue;
                            chunkMap.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(x>>4,z>>4),k->new java.util.ArrayList<>()).add(new BlockPos(x,y,z));
                        }
                    }
                }
            }
            for (java.util.Map.Entry<Long,java.util.List<BlockPos>> entry:chunkMap.entrySet()) {
                int chX=net.minecraft.world.level.ChunkPos.getX(entry.getKey()), chZ=net.minecraft.world.level.ChunkPos.getZ(entry.getKey());
                level.setChunkForced(chX,chZ,true);
                try { for(BlockPos bp:entry.getValue()){if(removeFluids&&!level.getFluidState(bp).isEmpty()){level.setBlock(bp,Blocks.AIR.defaultBlockState(),3);continue;}if(!level.getBlockState(bp).isAir())level.setBlock(bp,Blocks.AIR.defaultBlockState(),3);} }
                finally { level.setChunkForced(chX,chZ,false); }
            }
            currentRadius=end;
            if (currentRadius>lastDamagedRadius) {
                SU.getEntitiesWithinRadius(level,center.x,center.y,center.z,currentRadius).forEach(e->{
                    if(e instanceof GoMEntity)return;
                    if(e instanceof LivingEntity living){if(e instanceof ServerPlayer p&&(p.isCreative()||p.isSpectator()))return;living.invulnerableTime=0;living.hurt(level.damageSources().genericKill(),Integer.MAX_VALUE);}
                    else{e.remove(Entity.RemovalReason.KILLED);}
                });
                lastDamagedRadius=currentRadius;
            }
            return currentRadius>=maxRadius;
        }
    }

    private static SimpleChannel NETWORK;
    public static void register() {
        NETWORK=NetworkRegistry.newSimpleChannel(new ResourceLocation(ExtraBossRush.MOD_ID,"skill_effects"),()->"2","2"::equals,"2"::equals);
        NETWORK.registerMessage(0,SkillEffectPacket.class,SkillEffectPacket::encode,SkillEffectPacket::decode,SkillEffectPacket::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static class SkillEffectPacket {
        final Vec3 pos; final int skillId; final List<EffectDef> effects;
        SkillEffectPacket(Vec3 p, int s, List<EffectDef> e) { pos=p; skillId=s; effects=e; }
        static void encode(SkillEffectPacket m, FriendlyByteBuf buf) {
            buf.writeDouble(m.pos.x);buf.writeDouble(m.pos.y);buf.writeDouble(m.pos.z);buf.writeInt(m.skillId);buf.writeInt(m.effects.size());for(EffectDef e:m.effects)e.encode(buf);
        }
        static SkillEffectPacket decode(FriendlyByteBuf buf) {
            Vec3 pos=new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble());int sid=buf.readInt(),ec=buf.readInt();
            List<EffectDef> efx=new ArrayList<>(ec);for(int i=0;i<ec;i++)efx.add(EffectDef.decode(buf));return new SkillEffectPacket(pos,sid,efx);
        }
        static void handle(SkillEffectPacket m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientHandler.handleEffect(m)));ctx.get().setPacketHandled(true);
        }
    }

    private static class ClientHandler {
        static void handleEffect(SkillEffectPacket msg) {
            Level level=Minecraft.getInstance().level; if(level==null)return;
            for (EffectDef ef:msg.effects) spawnEffect(ef,msg.pos);
        }
        static void spawnEffect(EffectDef ef, Vec3 worldPos) {
            Vec3 p=worldPos.add(ef.offset);
            ARV2.Builder b=new ARV2.Builder(ef.model,ef.texture,p);
            if (ef.renderType != null && ef.renderType.factory != null) {
                b.setRenderType(ef.renderType.factory.apply(ef.texture));
            }
            if (ef.lookTargetId != -1) {
                b.setDynamicRot(createTrackingRot(p, ef.lookTargetId, ef.trackingSpeed));
            } else {
                b.setDynamicRot(ef.rotBehavior.toRotFn(p));
            }
            if(ef.fixedSize>0)b.setSize(ef.fixedSize); else b.setDynamicSize(ef.sizeBehavior.toSizeFn());
            b.setDynamicAlpha(ef.alphaBehavior.toAlphaFn()); b.setDynamicRGB(ef.colorBehavior.toColorFn());
            if(ef.asBeam)b.asBeam2(ef.beamTrailSpeed,ef.beamTrailMaxLen,ef.beamTrailSize);
            b.setMaxLife(ef.maxLife).spawn();
        }
        private static ARV2.DynamicVec3 createTrackingRot(Vec3 origin, int targetEntityId, float trackingSpeed) {
            final Vec3[] lastDirection = new Vec3[]{null};   // mutable
            final int[] lastTick = {-1};
            return t -> {
                Level level = Minecraft.getInstance().level;
                if (level == null) {
                    return lastDirection[0] != null ? lastDirection[0] : new Vec3(0, 1, 0);
                }
                Entity target = level.getEntity(targetEntityId);
                if (target == null || !target.isAlive()) {
                    return lastDirection[0] != null ? lastDirection[0] : new Vec3(0, 1, 0);
                }
                Vec3 targetEye = target.getEyePosition((float)(t % 1f));
                Vec3 desired = targetEye.subtract(origin).normalize();
                Vec3 current = (lastDirection[0] != null) ? lastDirection[0] : desired;
                double dot = current.dot(desired);
                dot = Math.min(Math.max(dot, -1.0), 1.0);
                double angleDiffDeg = Math.toDegrees(Math.acos(dot));

                if (angleDiffDeg < 0.1) {
                    lastDirection[0] = desired;
                    return desired;
                }
                float maxDegPerTick = Math.abs(trackingSpeed);
                double fraction = Math.min(1.0, maxDegPerTick / angleDiffDeg);
                Vec3 newDir = current.lerp(desired, fraction).normalize();
                lastDirection[0] = newDir;
                return newDir;
            };
        }
    }

    private static Vec3 calcDir(float nP, float nY, float nR, float NP, float NY, float NR) {
        Quaternionf baseRot=new Quaternionf().rotationXYZ((float)Math.toRadians(nP),(float)Math.toRadians(nY),(float)Math.toRadians(nR));
        Quaternionf addRot=new Quaternionf().rotationXYZ((float)Math.toRadians(NP),(float)Math.toRadians(NY),(float)Math.toRadians(NR));
        baseRot.mul(addRot); Vector3f fwd=new Vector3f(0,0,1); baseRot.transform(fwd);
        return new Vec3(fwd.x(),fwd.y(),fwd.z()).normalize();
    }

    @SubscribeEvent public static void onGoMSkill(GoMSkillEvent event) {
        if (event.getTarget().level().isClientSide()) return;
        GoMSkillContext ctx=new GoMSkillContext(event.getBoss(),event.getTarget(),event.getTarget().level(),event.getTarget().level().getGameTime(),new Vec3(event.getBoss().getX(),event.getBoss().getY(),event.getBoss().getZ()));
        Function<GoMSkillContext,GoMSkill> factory=SKILL_REGISTRY.get(event.getSkillId());
        if (factory!=null) factory.apply(ctx).execute(ctx,event.getSkillId());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.level;
        long now = level.getGameTime();

        // ==================== 既存の処理（変更なし） ====================
        Iterator<DelayedSkillTask> it = SERVER_TASKS.iterator();
        while (it.hasNext()) {
            DelayedSkillTask task = it.next();
            if (!event.level.dimension().equals(task.dimensionKey())) continue;
            if (now < task.executeAt()) continue;
            if (!event.level.isLoaded(BlockPos.containing(task.pos()))) {
                it.remove();
                continue;
            }
            executeServerLogic(event.level, task);
            if (!task.effects().isEmpty()) {
                NETWORK.send(PacketDistributor.TRACKING_CHUNK.with(
                                () -> event.level.getChunkAt(BlockPos.containing(task.pos()))),
                        new SkillEffectPacket(task.pos(), task.skillId(), task.effects()));
            }
            it.remove();
        }

        synchronized (CARVE_JOBS) {
            java.util.Iterator<CarveJob> cit = CARVE_JOBS.iterator();
            while (cit.hasNext()) {
                CarveJob job = cit.next();
                if (job.level != level) continue;
                try {
                    if (job.tickOnce()) cit.remove();
                } catch (Throwable t) {
                    cit.remove();
                    t.printStackTrace();
                }
            }
        }

        ACTIVE_BEAMS.removeIf(beam -> {
            if (beam.level != level) return false;
            try {
                return beam.tick();
            } catch (Throwable t) {
                t.printStackTrace();
                return true;
            }
        });
    }

    private static void executeServerLogic(Level level, DelayedSkillTask task) {
        if (task.action() instanceof ServerAction.BeamAction beam) ACTIVE_BEAMS.add(new BeamInstance(level,beam));
        else if (task.action() instanceof ServerAction.SphereAction sphere) synchronized(CARVE_JOBS){CARVE_JOBS.add(new CarveJob((ServerLevel)level,task.pos(),sphere.radius(),sphere.shellsPerTick(),sphere.removeFluids()));}
    }

    private static ServerAction.BeamAction buildBeamAction(Vec3 start, Vec3 dir, DynamicVec3 formula) {
        return new ServerAction.BeamAction.Builder(start,dir).maxLife(400).warmup(200).beamSize(5f).speed(32f).maxLen(512f).dirFormula(formula).build();
    }

    private static List<EffectDef> buildBeamEffects() {
        final int Life=400, kan=20, kan2=200; List<EffectDef> efx=new ArrayList<>();
        for (int i=0; i<3; i++) {
            float rZE=(float)(1080*(random.nextBoolean()?1:-1)+(random.nextDouble()*2-1)*720);
            efx.add(new EffectDef.Builder(SQMODEL,MHTEX).offset(0,10*i-1,0).fixedSize(10+10*i).alphaAnim(new float[]{0,0,1,1,0},new int[]{0,kan*i,kan*i+kan/2,Life-10,Life}).rotAnim(new float[]{90,90},new float[]{0,0},new float[]{0,rZE},new int[]{0,Life}).maxLife(Life).build());
        }
        efx.add(new EffectDef.Builder(SPMODEL,WHITEX).offset(0,-10,0).rot(90,0,0).alphaAnim(new float[]{1,1,0},new int[]{0,Life-10,Life}).sizeAnim(new float[]{0,5},new int[]{0,kan2}).maxLife(Life).build());
        efx.add(new EffectDef.Builder(BEMODEL,WHITEX).offset(0,-10,0).rot(90,0,0).asBeam(32f,512f,32f).alphaAnim(new float[]{0,0,1,1,0},new int[]{0,kan2,kan2,Life-10,Life}).fixedSize(5).maxLife(Life).build());
        return efx;
    }

    private static void PE(GoMEntity boss, ServerPlayer target, Level level) {
        double ox=(random.nextDouble()-0.5)*25.0, oz=(random.nextDouble()-0.5)*25.0;
        Explosion ex=level.explode(boss,target.getX()+ox,target.getY()+5.0,target.getZ()+oz,25.0F,ExplosionInteraction.NONE);
        if(ex!=null)ex.finalizeExplosion(false);
        target.setDeltaMovement(0,0,0); target.connection.send(new ClientboundSetEntityMotionPacket(target));
    }

    private static void AK(GoMEntity boss, Entity entity, Level level) {
        if(!(entity instanceof LivingEntity living))return;
        float killRatio=Math.min(0.9999f,(float)(Math.log(1+mobKillCount)/Math.log(1+10000)));
        float maxHp=((ServerLevel)level).players().stream().map(p->p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)).map(Double::floatValue).max(Float::compareTo).orElse(20.0f);
        float newHp=living.getHealth()-(living.getMaxHealth()*0.1f+Math.max(maxHp,living.getMaxHealth())*killRatio*0.9f);
        living.setHealth(Math.max(0.0f,newHp)); living.animateHurt(0.0f);
        if(newHp<=0)living.kill();
    }
}