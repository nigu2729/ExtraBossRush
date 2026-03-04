package com.ExtraBossRush.GoM.Entity;
import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Skill.GoMSkillEvent;
import com.ExtraBossRush.GoM.Support.PSU;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import io.netty.channel.ChannelPipeline;
@Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public class GoMEntity extends Monster {
    private static final EntityDataAccessor<String>  ζ0=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   ζ1=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ζ2=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.BOOLEAN);
    private static final List<GoMMasterLogic> _ML=new CopyOnWriteArrayList<>();
    private static final SecureRandom ρ=new SecureRandom();
    private static final ConcurrentHashMap<UUID,byte[]>     Σ_A=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     Σ_B=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     Σ_C=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,KeyPair>    Κ  =new ConcurrentHashMap<>();
    static  final ConcurrentHashMap<UUID,AtomicLong>        Ι  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Integer>    Φ  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Long>       Ρ  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Long>       Ψ  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     Χ  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,AtomicLong> Σ_S=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Byte>       Ω_ST=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,long[]>     Τ  =new ConcurrentHashMap<>();
    private static final byte[] Π_P;
    static{byte[]_p=new byte[32];new SecureRandom().nextBytes(_p);Π_P=_p;}
    private static volatile boolean _Γ_ok=false;
    private static final int    _AF=10;
    private static final long   _SR=6000L,_RL=3L;
    private static final int    _PR=120;
    private static final String _Ν="1";
    static final SimpleChannel Π;
    static{
        Π=NetworkRegistry.newSimpleChannel(new ResourceLocation(ExtraBossRush.MOD_ID,new String(new byte[]{0x67,0x6F,0x6D})),()->_Ν,_Ν::equals,_Ν::equals);
        Π.registerMessage(0,Α.class,Α::μ,Α::ν,Α::ξ);
        Π.registerMessage(1,Β.class,Β::μ,Β::ν,Β::ξ);
        Π.registerMessage(2,Γ.class,Γ::μ,Γ::ν,Γ::ξ);
        Π.registerMessage(3,Δ.class,Δ::μ,Δ::ν,Δ::ξ);
        Π.registerMessage(4,Ε.class,Ε::μ,Ε::ν,Ε::ξ);
    }
    private static boolean φ_rate(UUID u){
        long[]r=Τ.computeIfAbsent(u,k->new long[]{0L,0L});
        long t=System.currentTimeMillis();
        if(t-r[0]>1000L){r[0]=t;r[1]=0L;}
        return++r[1]<=_PR;
    }
    private static void φ_purge(UUID u){
        byte[]a=Σ_A.remove(u),b=Σ_B.remove(u),c=Σ_C.remove(u);
        if(a!=null)Arrays.fill(a,(byte)0);
        if(b!=null)Arrays.fill(b,(byte)0);
        if(c!=null)Arrays.fill(c,(byte)0);
        Κ.remove(u);Ι.remove(u);Φ.remove(u);Ρ.remove(u);
        Ψ.remove(u);Χ.remove(u);Σ_S.remove(u);Ω_ST.remove(u);Τ.remove(u);
    }
    private static final byte[] _KM=new String(new byte[]{(byte)0xE6,(byte)0x94,(byte)0xBE,(byte)0xE9,(byte)0x80,(byte)0xA3},StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
    private static void φ_kick(ServerPlayer sp,byte[]sk){
        if(sk!=null)Arrays.fill(sk,(byte)0);
        φ_purge(sp.getUUID());
        sp.connection.disconnect(Component.literal(new String(_KM,StandardCharsets.UTF_8)));
    }
    private static byte[] φ_sk(UUID u){
        byte[]a=Σ_A.get(u),b=Σ_B.get(u),c=Σ_C.get(u);
        if(a==null||b==null||c==null)return null;
        byte[]r=new byte[32];
        for(int i=0;i<32;i++)r[i]=(byte)(a[i]^b[i]^c[i]);
        return r;
    }
    public GoMEntity(EntityType<? extends GoMEntity> t,Level l){super(t,l);this.setNoGravity(true);this.noPhysics=true;}
    @Override
    protected void defineSynchedData(){
        super.defineSynchedData();
        this.entityData.define(ζ0,"");
        this.entityData.define(ζ1,1.0F);
        this.entityData.define(ζ2,false);
    }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?>k){super.onSyncedDataUpdated(k);}
    String  ɦ0()        {return this.entityData.get(ζ0);}
    void    ɦ0(String v){this.entityData.set(ζ0,v);}
    float   ɦ1()        {return this.entityData.get(ζ1);}
    boolean ɦ2()        {return this.entityData.get(ζ2);}
    float   ɦ3()        {return 1.0F-ɦ1();}
    public static AttributeSupplier.Builder createAttributes(){
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,1000.0D)
                .add(Attributes.ATTACK_DAMAGE,15.0D)
                .add(Attributes.FOLLOW_RANGE,1024.0D);
    }
    @Override public boolean isAttackable(){return false;}
    @Override public boolean canBeCollidedWith(){return false;}
    @Override public boolean isPickable(){return false;}
    @Override public void tick(){}
    @Override public boolean hurt(@NotNull DamageSource s,float a){return false;}
    @Override public boolean canBeAffected(net.minecraft.world.effect.@NotNull MobEffectInstance e){return false;}
    void ι0(){super.discard();}
    static String φ0(){
        for(;;){
            StringBuilder b=new StringBuilder();
            for(int i=0;i<10;i++)b.append(ρ.nextInt(10));
            String c=b.toString();
            if(_ML.stream().noneMatch(m->m._id.equals(c)))return c;
        }
    }
    static final class Ξ {
        static final byte[]_IH;
        static{
            byte[]h;
            try{
                String cn=Ξ.class.getName().replace('.','/') +".class";
                java.io.InputStream is=Ξ.class.getClassLoader().getResourceAsStream(cn);
                h=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36}))
                        .digest(is!=null?is.readAllBytes():new byte[0]);
                if(is!=null)is.close();
            }catch(Exception e){h=new byte[32];}
            _IH=h;
        }
        static boolean _tc(){
            try{
                String cn=Ξ.class.getName().replace('.','/') +".class";
                java.io.InputStream is=Ξ.class.getClassLoader().getResourceAsStream(cn);
                if(is==null)return true;
                byte[]cur=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36})).digest(is.readAllBytes());
                is.close();
                int d=0;
                for(int i=0;i<32;i++)d|=(cur.length>i&&_IH.length>i)?(cur[i]^_IH[i]):1;
                return d!=0;
            }catch(Exception e){return true;}
        }
        private static final String _a0=new String(new byte[]{0x45,0x43});
        private static final String _a1=new String(new byte[]{0x73,0x65,0x63,0x70,0x32,0x35,0x36,0x72,0x31});
        private static final String _a2=new String(new byte[]{0x45,0x43,0x44,0x48});
        private static final String _a3=new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36});
        private static final String _a4=new String(new byte[]{0x41,0x45,0x53,0x2F,0x47,0x43,0x4D,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static final String _a5=new String(new byte[]{0x41,0x45,0x53});
        private static final String _a6=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30,0x2D,0x50,0x6F,0x6C,0x79,0x31,0x33,0x30,0x35});
        private static final String _a7=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30});
        private static final String _aE=new String(new byte[]{0x41,0x45,0x53,0x2F,0x45,0x43,0x42,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static int _ω0(int v,int s){return(v<<(s&31))|(v>>>(32-(s&31)));}
        private static long _ω1(long v){return Long.reverse(v)^(v>>>7)^(v<<13);}
        static byte[]κ0(byte[]sk,byte info){
            try{
                MessageDigest md=MessageDigest.getInstance(_a3);
                byte[]blk=new byte[64];
                System.arraycopy(sk,0,blk,0,Math.min(sk.length,64));
                byte[]ip=new byte[64],op=new byte[64];
                for(int i=0;i<64;i++){ip[i]=(byte)(blk[i]^0x36);op[i]=(byte)(blk[i]^0x5C);}
                int _v=_ω0(info&0xFF,3);
                ip[_v&63]^=(byte)(info^0xA3);
                op[(_v^31)&63]^=(byte)(info^0x5B);
                md.update(ip);byte[]r1=md.digest(sk);
                md.reset();md.update(op);
                return md.digest(r1);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]κ1(byte[]sk,byte[]pepper){
            byte[]t=κ0(sk,(byte)0xC3);
            for(int i=0;i<32;i++)t[i]^=pepper[i&(pepper.length-1)];
            return κ0(t,(byte)0x9E);
        }
        static KeyPair ε0(){
            try{
                KeyPairGenerator g=KeyPairGenerator.getInstance(_a0);
                g.initialize(new ECGenParameterSpec(_a1),new SecureRandom());
                return g.generateKeyPair();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]ε1(PrivateKey prv,byte[]pub){
            try{
                PublicKey pk=KeyFactory.getInstance(_a0).generatePublic(new X509EncodedKeySpec(pub));
                KeyAgreement ka=KeyAgreement.getInstance(_a2);
                ka.init(prv);ka.doPhase(pk,true);
                return MessageDigest.getInstance(_a3).digest(ka.generateSecret());
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]ε2(byte[]key,byte[]plain,byte[]aad){
            try{
                byte[]iv=new byte[12];new SecureRandom().nextBytes(iv);
                Cipher c=Cipher.getInstance(_a4);
                c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,_a5),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);
                byte[]ct=c.doFinal(plain);
                return ByteBuffer.allocate(12+ct.length).put(iv).put(ct).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        private static volatile int _φv=0;
        static byte[]ε2c(byte[]key,byte[]plain,byte[]aad,AtomicLong ctr){
            try{
                int _q=(int)(System.nanoTime()&0x7FFFFFFE);
                if((_φv&1)!=0){Arrays.fill(key,(byte)0xFF);return new byte[0];}
                _φv=_q;
                if(_ω0(_q,7)<0&&Integer.bitCount(_q)==32){throw new RuntimeException();}
                byte[]k1=κ0(key,(byte)0x01),k2=κ0(key,(byte)0x02);
                long cnt=ctr.getAndIncrement();
                byte[]iv1=new byte[12];
                System.arraycopy(k1,0,iv1,0,4);
                ByteBuffer.wrap(iv1,4,8).putLong(cnt);
                Cipher c1=Cipher.getInstance(_a4);
                c1.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k1,_a5),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);
                byte[]inner=c1.doFinal(plain);
                byte[]wrapped=ByteBuffer.allocate(12+inner.length).put(iv1).put(inner).array();
                byte[]iv2=new byte[12];new SecureRandom().nextBytes(iv2);
                Cipher c2=Cipher.getInstance(_a6);
                c2.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k2,_a7),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);
                byte[]outer=c2.doFinal(wrapped);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);
                return ByteBuffer.allocate(12+outer.length).put(iv2).put(outer).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]ε3(byte[]key,byte[]data,byte[]aad){
            try{
                long _t=System.nanoTime()^data.length;
                if(Long.bitCount(_t)==0&&_ω1(_t)!=0){return null;}
                byte[]k1=κ0(key,(byte)0x01),k2=κ0(key,(byte)0x02);
                ByteBuffer ob=ByteBuffer.wrap(data);
                byte[]iv2=new byte[12];ob.get(iv2);
                byte[]oCt=new byte[ob.remaining()];ob.get(oCt);
                Cipher c2=Cipher.getInstance(_a6);
                c2.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k2,_a7),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);
                byte[]wrapped=c2.doFinal(oCt);
                ByteBuffer ib=ByteBuffer.wrap(wrapped);
                byte[]iv1=new byte[12];ib.get(iv1);
                byte[]iCt=new byte[ib.remaining()];ib.get(iCt);
                Cipher c1=Cipher.getInstance(_a4);
                c1.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k1,_a5),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);
                byte[]r=c1.doFinal(iCt);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);
                return r;
            }catch(Exception e){return null;}
        }
        static byte[]ε3s(byte[]key,byte[]data,byte[]aad){
            try{
                ByteBuffer bb=ByteBuffer.wrap(data);
                byte[]iv=new byte[12];bb.get(iv);
                byte[]ct=new byte[bb.remaining()];bb.get(ct);
                Cipher c=Cipher.getInstance(_a4);
                c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,_a5),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);
                return c.doFinal(ct);
            }catch(Exception e){return null;}
        }
        static byte δ0(double v){
            boolean _i=(v==Math.floor(v))&&!Double.isInfinite(v)&&v>=Integer.MIN_VALUE&&v<=Integer.MAX_VALUE;
            if(_i)return 0x01;
            if(Math.abs(v)<=32767.0)return 0x00;
            if(Math.abs(v)<=922337203685.0)return 0x02;
            return 0x03;
        }
        static int δ2(byte t){return(t==0x00||t==0x01)?4:8;}
        static void δ3(ByteBuffer bb,double v,byte t){
            switch(t){case 0x00->bb.putFloat((float)v);case 0x01->bb.putInt((int)v);case 0x02->bb.putLong(Math.round(v*10000.0));case 0x03->bb.putDouble(v);}
        }
        static double δ1(ByteBuffer bb,int t){
            return switch(t){case 0x00->bb.getFloat();case 0x01->bb.getInt();case 0x02->bb.getLong()/10000.0;case 0x03->bb.getDouble();default->throw new IllegalArgumentException();};
        }
        static byte[]φe(byte[]key,float v){
            try{
                byte[]blk=new byte[16];
                int bits=Float.floatToRawIntBits(v);
                blk[0]=(byte)(bits>>24);blk[1]=(byte)(bits>>16);blk[2]=(byte)(bits>>8);blk[3]=(byte)bits;
                new SecureRandom().nextBytes(Arrays.copyOfRange(blk,4,16));
                blk[4]=(byte)(bits^blk[0]^0xA5);
                blk[5]=(byte)(bits^(blk[1]<<1)^0x3C);
                blk[12]=(byte)~blk[0];blk[13]=(byte)~blk[1];blk[14]=(byte)~blk[2];blk[15]=(byte)~blk[3];
                Cipher c=Cipher.getInstance(_aE);
                c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,_a5));
                return c.doFinal(blk);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static float φd(byte[]key,byte[]ci){
            if(ci==null||ci.length<16)return 0f;
            try{
                Cipher c=Cipher.getInstance(_aE);
                c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,_a5));
                byte[]blk=c.doFinal(ci);
                int bits=((blk[0]&0xFF)<<24)|((blk[1]&0xFF)<<16)|((blk[2]&0xFF)<<8)|(blk[3]&0xFF);
                return Float.intBitsToFloat(bits);
            }catch(Exception e){return 0f;}
        }
        private static final Set<String>_PP=new HashSet<>(Arrays.asList(
                new String(new byte[]{0x74,0x69,0x6D,0x65,0x6F,0x75,0x74}),
                new String(new byte[]{0x6C,0x65,0x67,0x61,0x63,0x79,0x5F,0x71,0x75,0x65,0x72,0x79}),
                new String(new byte[]{0x66,0x72,0x61,0x6D,0x65,0x72}),
                new String(new byte[]{0x64,0x65,0x63,0x6F,0x64,0x65,0x72}),
                new String(new byte[]{0x70,0x72,0x65,0x70,0x65,0x6E,0x64,0x65,0x72}),
                new String(new byte[]{0x65,0x6E,0x63,0x6F,0x64,0x65,0x72}),
                new String(new byte[]{0x70,0x61,0x63,0x6B,0x65,0x74,0x5F,0x68,0x61,0x6E,0x64,0x6C,0x65,0x72}),
                new String(new byte[]{0x6E,0x65,0x74,0x77,0x6F,0x72,0x6B,0x5F,0x74,0x72,0x61,0x66,0x66,0x69,0x63}),
                new String(new byte[]{0x66,0x6D,0x6C,0x5F,0x6E,0x65,0x74,0x77,0x6F,0x72,0x6B,0x5F,0x66,0x69,0x6C,0x74,0x65,0x72}),
                new String(new byte[]{0x66,0x6D,0x6C,0x5F,0x6E,0x65,0x74,0x77,0x6F,0x72,0x6B,0x5F,0x63,0x68,0x65,0x63,0x6B})
        ));
        static boolean μp(ServerPlayer sp){
            try{
                ChannelPipeline pipe=sp.connection.connection.channel().pipeline();
                for(String n:pipe.names()){
                    String lo=n.toLowerCase(java.util.Locale.ROOT);
                    boolean ok=false;
                    for(String s:_PP){if(lo.contains(s)){ok=true;break;}}
                    if(!ok){
                        String cls=pipe.get(n).getClass().getName().toLowerCase(java.util.Locale.ROOT);
                        if(!cls.contains(new String(new byte[]{0x6D,0x69,0x6E,0x65,0x63,0x72,0x61,0x66,0x74}))
                                &&!cls.contains(new String(new byte[]{0x6D,0x69,0x6E,0x65,0x66,0x6F,0x72,0x67,0x65}))
                                &&!cls.contains(new String(new byte[]{0x6E,0x65,0x74,0x74,0x79}))
                                &&!cls.contains(new String(new byte[]{0x66,0x6F,0x72,0x67,0x65})))
                            return true;
                    }
                }
                return false;
            }catch(Exception e){return false;}
        }
        static boolean μa(){
            try{
                List<String>args=java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
                for(String a:args){
                    String lo=a.toLowerCase(java.util.Locale.ROOT);
                    if(lo.contains(new String(new byte[]{0x6A,0x61,0x76,0x61,0x61,0x67,0x65,0x6E,0x74}))
                            ||lo.contains(new String(new byte[]{0x61,0x67,0x65,0x6E,0x74,0x6C,0x69,0x62}))
                            ||lo.contains(new String(new byte[]{0x6A,0x64,0x77,0x70})))
                        return true;
                }
                return false;
            }catch(Exception e){return true;}
        }
        static boolean μt(){
            long t0=System.nanoTime();int x=0;
            for(int i=0;i<10000;i++)x^=i;
            return(x!=-1)&&(System.nanoTime()-t0)>500_000_000L;
        }
    }
    static final class Α{
        final byte[]κ;
        Α(byte[]k){κ=k;}
        private static final int _M=256;
        static void μ(Α p,FriendlyByteBuf b){b.writeByteArray(p.κ);}
        static Α   ν(FriendlyByteBuf b){return new Α(b.readByteArray(_M));}
        static void ξ(Α p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->Ω.σ0(p.κ));
            ctx.get().setPacketHandled(true);
        }
    }
    static final class Β{
        final byte[]κ;
        Β(byte[]k){κ=k;}
        private static final int _M=256;
        static void μ(Β p,FriendlyByteBuf b){b.writeByteArray(p.κ);}
        static Β   ν(FriendlyByteBuf b){return new Β(b.readByteArray(_M));}
        static void ξ(Β p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();
                if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!φ_rate(uid)){φ_kick(sp,null);return;}
                if(p.κ==null||p.κ.length>_M){Φ.merge(uid,1,Integer::sum);return;}
                Byte st=Ω_ST.get(uid);
                if(st==null||st!=(byte)0){
                    int f=Φ.merge(uid,1,Integer::sum);
                    if(f>=_AF)φ_kick(sp,null);
                    return;
                }
                if(Σ_A.containsKey(uid))return;
                KeyPair kp=Κ.remove(uid);
                if(kp==null)return;
                byte[]sk;
                try{sk=Ξ.ε1(kp.getPrivate(),p.κ);}catch(Exception e){return;}
                byte[]a=new byte[32],c=new byte[32];
                ρ.nextBytes(a);ρ.nextBytes(c);
                byte[]bb2=new byte[32];
                for(int i=0;i<32;i++)bb2[i]=(byte)(sk[i]^a[i]^c[i]);
                Arrays.fill(sk,(byte)0);
                Σ_A.put(uid,a);Σ_B.put(uid,bb2);Σ_C.put(uid,c);
                Ι.put(uid,new AtomicLong(0L));
                Φ.put(uid,0);
                Ρ.put(uid,sp.serverLevel().getGameTime());
                byte[]n0=new byte[8];ρ.nextBytes(n0);
                Χ.put(uid,n0);
                Σ_S.put(uid,new AtomicLong(0L));
                Ω_ST.put(uid,(byte)1);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class Γ{
        final byte[]ψ;
        Γ(byte[]d){ψ=d;}
        private static final int _M=256;
        static void μ(Γ p,FriendlyByteBuf b){b.writeByteArray(p.ψ);}
        static Γ   ν(FriendlyByteBuf b){return new Γ(b.readByteArray(_M));}
        static void ξ(Γ p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();
                if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!φ_rate(uid)){φ_kick(sp,null);return;}
                Byte st=Ω_ST.get(uid);
                if(st==null||st!=(byte)1)return;
                long now=sp.serverLevel().getGameTime();
                Long lΓ=Ψ.get(uid);
                if(lΓ!=null&&now-lΓ<_RL)return;
                Ψ.put(uid,now);
                byte[]sk=φ_sk(uid);
                if(sk==null)return;
                Long lR=Ρ.get(uid);
                if(lR!=null&&now-lR>=_SR){
                    Ρ.put(uid,now);
                    byte[]_a=Σ_A.remove(uid),_b=Σ_B.remove(uid),_c=Σ_C.remove(uid);
                    if(_a!=null)Arrays.fill(_a,(byte)0);
                    if(_b!=null)Arrays.fill(_b,(byte)0);
                    if(_c!=null)Arrays.fill(_c,(byte)0);
                    Ι.remove(uid);Ψ.remove(uid);Χ.remove(uid);Σ_S.remove(uid);
                    Ω_ST.put(uid,(byte)0);
                    Arrays.fill(sk,(byte)0);
                    KeyPair kp=Ξ.ε0();
                    Κ.put(uid,kp);
                    Π.send(PacketDistributor.PLAYER.with(()->sp),new Α(kp.getPublic().getEncoded()));
                    return;
                }
                if(Ξ.μp(sp)){Arrays.fill(sk,(byte)0);φ_kick(sp,null);return;}
                if(Ξ._tc()){
                    Arrays.fill(sk,(byte)0);
                    Σ_A.clear();Σ_B.clear();Σ_C.clear();Κ.clear();Ι.clear();
                    Φ.clear();Ρ.clear();Ψ.clear();Χ.clear();Σ_S.clear();Ω_ST.clear();
                    Component _r=Component.literal(new String(_KM,StandardCharsets.UTF_8));
                    for(ServerPlayer x:sp.serverLevel().getServer().getPlayerList().getPlayers())x.connection.disconnect(_r);
                    return;
                }
                byte[]aad=uid.toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=Ξ.ε3(sk,p.ψ,aad);
                if(plain==null||plain.length<21){
                    int f=Φ.merge(uid,1,Integer::sum);
                    if(f>=_AF)φ_kick(sp,sk);
                    else Arrays.fill(sk,(byte)0);
                    return;
                }
                Φ.put(uid,0);
                ByteBuffer bb=ByteBuffer.wrap(plain);
                byte[]rxN=new byte[8];bb.get(rxN);
                byte[]expN=Χ.get(uid);
                if(expN==null){Arrays.fill(sk,(byte)0);return;}
                int _nd=0;
                for(int i=0;i<8;i++)_nd|=(rxN[i]^expN[i]);
                if(_nd!=0){
                    int f=Φ.merge(uid,1,Integer::sum);
                    if(f>=_AF)φ_kick(sp,sk);
                    else Arrays.fill(sk,(byte)0);
                    return;
                }
                byte[]nN=new byte[8];ρ.nextBytes(nN);
                Χ.put(uid,nN);
                byte[]rxT=new byte[8];bb.get(rxT);
                int tid=bb.getInt();
                int nt=(int)(sp.serverLevel().getGameTime()&0xFFFFFFFFL);
                if(Math.abs(nt-tid)>20){Arrays.fill(sk,(byte)0);return;}
                byte fl=bb.get();
                double hx,hy,hz;
                try{
                    hx=Ξ.δ1(bb,(fl)&0x03);
                    hy=Ξ.δ1(bb,(fl>>2)&0x03);
                    hz=Ξ.δ1(bb,(fl>>4)&0x03);
                }catch(Exception e){Arrays.fill(sk,(byte)0);return;}
                Vec3 hp=new Vec3(hx,hy,hz);
                for(GoMMasterLogic m:_ML){
                    if(m.getLevel()!=sp.serverLevel())continue;
                    byte[]_ts=ByteBuffer.allocate(8).putInt(m._de.getId()).putInt(tid).array();
                    byte[]_tk=Ξ.κ0(sk,(byte)0x7F);
                    for(int i=0;i<16;i++)_tk=Ξ.κ0(_tk,_ts[i&7]);
                    int _nd2=0;
                    for(int i=0;i<8;i++)_nd2|=(rxT[i]^_tk[i]);
                    if(_nd2!=0)continue;
                    if(hp.distanceTo(m._pos)>2.5){Arrays.fill(sk,(byte)0);return;}
                    _Γ_ok=true;
                    m.applyHit(sp);
                    _Γ_ok=false;
                    break;
                }
                Arrays.fill(sk,(byte)0);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class Δ{
        final byte[]ψ;
        Δ(byte[]d){ψ=d;}
        private static final int _M=128;
        static void μ(Δ p,FriendlyByteBuf b){b.writeByteArray(p.ψ);}
        static Δ   ν(FriendlyByteBuf b){return new Δ(b.readByteArray(_M));}
        static void ξ(Δ p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->Ω.σ5(p.ψ));
            ctx.get().setPacketHandled(true);
        }
        static byte[]revoke(byte[]sk,int eid){
            try{
                byte[]pl=new byte[]{(byte)0xFF,(byte)(eid>>24),(byte)(eid>>16),(byte)(eid>>8),(byte)eid};
                return Ξ.ε2(sk,pl,new byte[]{(byte)0xD4,(byte)0xFF});
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]build(byte[]sk,int eid,String mid){
            try{
                byte[]mb=mid.getBytes(StandardCharsets.UTF_8);
                ByteBuffer bb=ByteBuffer.allocate(4+mb.length);bb.putInt(eid);bb.put(mb);
                return Ξ.ε2(sk,bb.array(),new byte[]{(byte)0xD4,(byte)(eid&0xFF)});
            }catch(Exception e){throw new RuntimeException(e);}
        }
    }
    static final class Ε{
        final byte[]ψ;
        Ε(byte[]d){ψ=d;}
        private static final int _M=128;
        static void μ(Ε p,FriendlyByteBuf b){b.writeByteArray(p.ψ);}
        static Ε   ν(FriendlyByteBuf b){return new Ε(b.readByteArray(_M));}
        static void ξ(Ε p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->Ω.σ6(p.ψ));
            ctx.get().setPacketHandled(true);
        }
        static byte[]build(byte[]sk,UUID uid,int eid,float hr,boolean hurt,double x,double y,double z,byte[]nonce,long seq){
            try{
                ByteBuffer bb=ByteBuffer.allocate(50);
                bb.put((byte)0x01);bb.putLong(seq);bb.putInt(eid);
                bb.put(nonce);bb.putFloat(hr);bb.put((byte)(hurt?1:0));
                bb.putDouble(x);bb.putDouble(y);bb.putDouble(z);
                return Ξ.ε2(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]remove(byte[]sk,UUID uid,int eid,long seq){
            try{
                ByteBuffer bb=ByteBuffer.allocate(13);
                bb.put((byte)0x02);bb.putLong(seq);bb.putInt(eid);
                return Ξ.ε2(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));
            }catch(Exception e){throw new RuntimeException(e);}
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
    static final class Ω{
        private static boolean _s0=false;
        private static javax.crypto.SecretKey _s1=null;
        private static AtomicLong _s2=new AtomicLong(0L);
        static final Set<Integer>               λ4=Collections.synchronizedSet(new HashSet<>());
        static final ConcurrentHashMap<Integer,Vec3> λ5=new ConcurrentHashMap<>();
        private static volatile long   _s6=-1L;
        private static volatile byte[] _s7=null;
        private static final double _R=4.5;
        @OnlyIn(Dist.CLIENT)
        static void σ0(byte[]spub){
            try{
                KeyPair kp=Ξ.ε0();
                byte[]raw=Ξ.ε1(kp.getPrivate(),spub);
                if(_s1!=null){try{((javax.security.auth.Destroyable)_s1).destroy();}catch(Exception ignored){}}
                _s1=new SecretKeySpec(raw,new String(new byte[]{0x41,0x45,0x53}));
                Arrays.fill(raw,(byte)0);
                _s2.set(0L);_s6=-1L;_s7=null;
                Π.sendToServer(new Β(kp.getPublic().getEncoded()));
            }catch(Exception ignored){}
        }
        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        static void σ1(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.START)return;
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null||mc.level==null||_s1==null)return;
            try{if(((javax.security.auth.Destroyable)_s1).isDestroyed())return;}catch(Exception ignored){}
            boolean sw=mc.player.swinging;
            if(sw&!_s0)σ2(mc);
            _s0=sw;
        }
        @OnlyIn(Dist.CLIENT)
        private static void σ2(Minecraft mc){
            Vec3 eye=mc.player.getEyePosition(1.0F);
            Vec3 end=eye.add(mc.player.getLookAngle().scale(_R));
            for(net.minecraft.world.entity.Entity ent:mc.level.entitiesForRendering()){
                if(!(ent instanceof GoMEntity g))continue;
                if(!λ4.contains(g.getId()))continue;
                Vec3 ap=λ5.get(g.getId());
                if(ap==null)continue;
                AABB box=new AABB(ap.x-0.4,ap.y,ap.z-0.4,ap.x+0.4,ap.y+1.8,ap.z+0.4);
                Optional<Vec3>hit=box.clip(eye,end);
                if(hit.isEmpty())continue;
                Vec3 hp=hit.get();
                if(eye.distanceToSqr(hp)>(_R+0.1)*(_R+0.1))continue;
                boolean surf=box.inflate(0.05).contains(hp)&&!box.deflate(0.05).contains(hp);
                if(!surf)continue;
                BlockHitResult bhr=mc.level.clip(new ClipContext(eye,hp,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
                if(bhr.getType()!=HitResult.Type.MISS)continue;
                σ3(g,hp,mc);
                return;
            }
        }
        @OnlyIn(Dist.CLIENT)
        private static void σ3(GoMEntity g,Vec3 hp,Minecraft mc){
            if(_s1==null)return;
            byte[]nc=_s7;
            if(nc==null)return;
            try{
                int tick=(int)(mc.level.getGameTime()&0xFFFFFFFFL);
                byte[]rk=_s1.getEncoded();
                byte tx=Ξ.δ0(hp.x),ty=Ξ.δ0(hp.y),tz=Ξ.δ0(hp.z);
                byte fl=(byte)((tx&0x03)|((ty&0x03)<<2)|((tz&0x03)<<4));
                byte[]_ts=ByteBuffer.allocate(8).putInt(g.getId()).putInt(tick).array();
                byte[]_tk=Ξ.κ0(rk,(byte)0x7F);
                for(int i=0;i<16;i++)_tk=Ξ.κ0(_tk,_ts[i&7]);
                byte[]token=Arrays.copyOf(_tk,8);
                int sz=21+Ξ.δ2(tx)+Ξ.δ2(ty)+Ξ.δ2(tz);
                ByteBuffer bb=ByteBuffer.allocate(sz);
                bb.put(nc);bb.put(token);bb.putInt(tick);bb.put(fl);
                Ξ.δ3(bb,hp.x,tx);Ξ.δ3(bb,hp.y,ty);Ξ.δ3(bb,hp.z,tz);
                byte[]aad=mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8);
                byte[]ci=Ξ.ε2c(rk,bb.array(),aad,_s2);
                Π.sendToServer(new Γ(ci));
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void σ5(byte[]data){
            if(_s1==null||data==null||data.length<28)return;
            try{
                byte[]rk=_s1.getEncoded();
                byte fc=(data.length>12)?data[12]:0;
                byte[]plain=Ξ.ε3s(rk,data,new byte[]{(byte)0xD4,fc});
                if(plain==null||plain.length<4)return;
                if(plain.length>=5&&(plain[0]&0xFF)==0xFF){
                    λ4.remove(ByteBuffer.wrap(plain,1,4).getInt());
                }else if(plain.length>=4){
                    λ4.add(ByteBuffer.wrap(plain).getInt());
                }
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void σ6(byte[]data){
            if(_s1==null||data==null)return;
            try{
                byte[]rk=_s1.getEncoded();
                Minecraft mc=Minecraft.getInstance();
                if(mc.player==null)return;
                byte[]aad=mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=Ξ.ε3s(rk,data,aad);
                if(plain==null||plain.length<13)return;
                ByteBuffer bb=ByteBuffer.wrap(plain);
                byte type=bb.get();
                long seq=bb.getLong();
                if(seq<=_s6)return;
                _s6=seq;
                int eid=bb.getInt();
                if(type==(byte)0x02){λ4.remove(eid);λ5.remove(eid);return;}
                if(type!=(byte)0x01||plain.length<50)return;
                byte[]rxN=new byte[8];bb.get(rxN);
                _s7=rxN;
                bb.getFloat();bb.get();
                double ax=bb.getDouble(),ay=bb.getDouble(),az=bb.getDouble();
                λ5.put(eid,new Vec3(ax,ay,az));
            }catch(Exception ignored){}
        }
        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        static void σ4(LevelEvent.Unload e){
            if(!e.getLevel().isClientSide())return;
            _s0=false;
            if(_s1!=null){try{((javax.security.auth.Destroyable)_s1).destroy();}catch(Exception ignored){}
                _s1=null;}
            _s2.set(0L);λ4.clear();λ5.clear();_s6=-1L;_s7=null;
        }
    }
    @SubscribeEvent
    public static void ε0(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;
        UUID uid=sp.getUUID();
        φ_purge(uid);
        Ω_ST.put(uid,(byte)0);
        KeyPair kp=Ξ.ε0();
        Κ.put(uid,kp);
        Π.send(PacketDistributor.PLAYER.with(()->sp),new Α(kp.getPublic().getEncoded()));
    }
    @SubscribeEvent
    public static void ε1(PlayerEvent.PlayerLoggedOutEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;
        φ_purge(sp.getUUID());
    }
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        if(!(event.level instanceof ServerLevel level))return;
        _ML.removeIf(master->{
            if(master.getLevel()!=level)return false;
            master.tickMaster();
            if(master.isDead()){master.discard();return true;}
            return false;
        });
    }
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event){
        if(event.getLevel().isClientSide())return;
        _ML.forEach(GoMMasterLogic::discard);
        _ML.clear();
    }
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event){
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("gomentity")
                        .requires(s->s.hasPermission(2))
                        .executes(ctx->{
                            ServerPlayer p=ctx.getSource().getPlayerOrException();
                            Vec3 sp=p.position().add(p.getLookAngle().scale(5));
                            _ML.add(new GoMMasterLogic(φ0(),p.serverLevel(),sp));
                            return 1;
                        })
        );
    }
    interface IΗ{byte[]γ();void γ(byte[]c);}
    static final class Η implements IΗ{
        private byte[]_c;
        public byte[]γ(){return _c==null?null:Arrays.copyOf(_c,_c.length);}
        public void γ(byte[]c){_c=c==null?null:Arrays.copyOf(c,c.length);}
    }
    static final class GoMMasterLogic{
        final String       _id;
        private final ServerLevel     _lv;
        private final ServerBossEvent _be;
        final GoMEntity               _de;
        Vec3 _pos;
        private static int _dc=0;
        private static final int   _MD=46;
        private static final float _BH=65536.0F;
        private final byte[]_ik=new byte[16];
        private final IΗ   _ih=new Η();
        private float _mh;
        private byte[]_hk(){byte[]k=Ξ.κ0(_ik,(byte)0x42);for(int i=0;i<16;i++)k[i]^=Π_P[i];return Arrays.copyOf(k,16);}
        private float _gH(){return Ξ.φd(_hk(),_ih.γ());}
        private void  _sH(float v){_ih.γ(Ξ.φe(_hk(),v));}
        private static final double _RS=1024.0*1024.0;
        private int _tc=0,_l2=0,_l3=0,_ht=0;
        private long _lh=0L;
        private static final long _HC=10L;
        private static final Random _rnd=new Random();
        GoMMasterLogic(String id,ServerLevel lv,Vec3 pos){
            _id=id;_lv=lv;_pos=pos;
            _be=new ServerBossEvent(
                    Component.literal(new String(new byte[]{
                            (byte)0xE9,(byte)0xAD,(byte)0x94,(byte)0xE8,(byte)0xA1,(byte)0x93,
                            (byte)0xE3,(byte)0x81,(byte)0xAE,(byte)0xE5,(byte)0xAE,(byte)0x88,
                            (byte)0xE8,(byte)0xAD,(byte)0xB7,(byte)0xE8,(byte)0x80,(byte)0x85
                    },StandardCharsets.UTF_8)),
                    BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
            _be.setVisible(true);
            _mh=_BH;
            new SecureRandom().nextBytes(_ik);
            _sH(_mh);
            _de=new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(),lv);
            _de.setPos(pos.x,pos.y,pos.z);
            _de.ɦ0(id);
            _de.entityData.set(ζ1,1.0F);
            _de.entityData.set(ζ2,false);
        }
        String      _id()     {return _id;}
        ServerLevel getLevel(){return _lv;}
        boolean     isDead() {return _gH()<=0;}
        void tickMaster(){
            if(_gH()<=0)return;
            _be.setVisible(true);
            if((_tc&0xFF)==0){
                float _cur=_gH();
                new SecureRandom().nextBytes(_ik);
                _sH(_cur);
            }
            if(_ht>0&&--_ht==0)_de.entityData.set(ζ2,false);
            float _er=1.0F-(_gH()/_mh);
            int _pp=(int)Math.ceil(1.0F+_er*9.0F);
            for(int i=0;i<_pp;i++){_rL();_tc++;}
            _sync();
            _be.setProgress(_gH()/_mh);
        }
        private void _rL(){
            List<ServerPlayer>near=PSU.getPlayersWithinRadius(_lv,_pos.x,_pos.y,_pos.z,1024.0);
            if(near.isEmpty()&&!_lv.players().isEmpty())
                _pos=_lv.players().get(_rnd.nextInt(_lv.players().size())).position().add(0,10,0);
            if(_tc-_l2>=100){_l2=_tc;_fire(2);}
            if(_tc-_l3>=600){_l3=_tc;_fire(3);}
        }
        private void _fire(int sid){
            PSU.getPlayersWithinRadius(_lv,_pos.x,_pos.y,_pos.z,1024.0)
                    .forEach(p->MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(_de,p,sid)));
        }
        void applyHit(ServerPlayer sp){
            if(!_Γ_ok)return;
            _Γ_ok=false;
            long now=_lv.getGameTime();
            if(now-_lh<_HC)return;
            _lh=now;
            net.minecraft.world.item.ItemStack wep=sp.getMainHandItem();
            float _d0=(float)sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float dmg=_d0*0.2F;
            int _sh=net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,wep);
            if(_sh>0)dmg+=(_sh*0.5F+0.5F)*0.2F;
            dmg=Math.min(Math.max(dmg,0.0F),1000.0F);
            _sH(Math.max(0.0F,_gH()-dmg));
            _de.entityData.set(ζ2,true);
            _de.entityData.set(ζ1,_gH()/_mh);
            if(_gH()<=0){
                if(_dc<_MD)_dc++;
                _be.setProgress(0.0F);_be.removeAllPlayers();
                return;
            }
            _ht=20;
        }
        private void _sync(){
            _de.setPos(_pos.x,_pos.y,_pos.z);
            _de.entityData.set(ζ1,_gH()/_mh);
            ClientboundAddEntityPacket    _ap=new ClientboundAddEntityPacket(_de);
            ClientboundSetEntityDataPacket _dp=new ClientboundSetEntityDataPacket(
                    _de.getId(),_de.getEntityData().getNonDefaultValues());
            float _hr=_gH()/_mh;
            boolean _hurt=_de.getEntityData().get(ζ2);
            for(ServerPlayer p:_lv.players()){
                if(p.distanceToSqr(_pos)<_RS){
                    byte[]_ha=Σ_A.get(p.getUUID()),_hb=Σ_B.get(p.getUUID()),_hc=Σ_C.get(p.getUUID());
                    AtomicLong _sc=Σ_S.get(p.getUUID());
                    byte[]_cn=Χ.get(p.getUUID());
                    if(_ha!=null&&_hb!=null&&_hc!=null&&_sc!=null&&_cn!=null){
                        byte[]_sk=new byte[32];
                        for(int i=0;i<32;i++)_sk[i]=(byte)(_ha[i]^_hb[i]^_hc[i]);
                        long seq=_sc.getAndIncrement();
                        Π.send(PacketDistributor.PLAYER.with(()->p),new Δ(Δ.build(_sk,_de.getId(),_id)));
                        p.connection.send(_ap);
                        p.connection.send(_dp);
                        Π.send(PacketDistributor.PLAYER.with(()->p),new Ε(Ε.build(_sk,p.getUUID(),_de.getId(),_hr,_hurt,_pos.x,_pos.y,_pos.z,_cn,seq)));
                        _be.addPlayer(p);
                        Arrays.fill(_sk,(byte)0);
                    }else{
                        p.connection.send(_ap);
                        p.connection.send(_dp);
                        _be.addPlayer(p);
                    }
                }else{
                    p.connection.send(new ClientboundRemoveEntitiesPacket(_de.getId()));
                    _be.removePlayer(p);
                    byte[]_ha=Σ_A.get(p.getUUID()),_hb=Σ_B.get(p.getUUID()),_hc=Σ_C.get(p.getUUID());
                    AtomicLong _sc=Σ_S.get(p.getUUID());
                    if(_ha!=null&&_hb!=null&&_hc!=null&&_sc!=null){
                        byte[]_sk=new byte[32];
                        for(int i=0;i<32;i++)_sk[i]=(byte)(_ha[i]^_hb[i]^_hc[i]);
                        long seq=_sc.getAndIncrement();
                        Π.send(PacketDistributor.PLAYER.with(()->p),new Ε(Ε.remove(_sk,p.getUUID(),_de.getId(),seq)));
                        Arrays.fill(_sk,(byte)0);
                    }
                }
            }
            _de.getEntityData().packDirty();
        }
        void discard(){
            _be.removeAllPlayers();
            int did=_de.getId();
            ClientboundRemoveEntitiesPacket rm=new ClientboundRemoveEntitiesPacket(did);
            for(ServerPlayer p:_lv.players()){
                p.connection.send(rm);
                byte[]_ha=Σ_A.get(p.getUUID()),_hb=Σ_B.get(p.getUUID()),_hc=Σ_C.get(p.getUUID());
                AtomicLong _sc=Σ_S.get(p.getUUID());
                if(_ha!=null&&_hb!=null&&_hc!=null&&_sc!=null){
                    byte[]_sk=new byte[32];
                    for(int i=0;i<32;i++)_sk[i]=(byte)(_ha[i]^_hb[i]^_hc[i]);
                    long seq=_sc.getAndIncrement();
                    Π.send(PacketDistributor.PLAYER.with(()->p),new Δ(Δ.revoke(_sk,did)));
                    Π.send(PacketDistributor.PLAYER.with(()->p),new Ε(Ε.remove(_sk,p.getUUID(),did,seq)));
                    Arrays.fill(_sk,(byte)0);
                }
            }
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class ClientSetup{
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event){
            event.registerEntityRenderer(GoMEntities.MAGIC_GUARDIAN.get(),MagicGuardianRenderer::new);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class MagicGuardianRenderer extends MobRenderer<GoMEntity,HumanoidModel<GoMEntity>>{
        private static final ResourceLocation TX=new ResourceLocation(ExtraBossRush.MOD_ID,"textures/entity/magic_guardian.png");
        private static final Random RD=new Random();
        public MagicGuardianRenderer(EntityRendererProvider.Context ctx){
            super(ctx,new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)),0.6F);
        }
        @Override public @NotNull ResourceLocation getTextureLocation(@NotNull GoMEntity e){return TX;}
        @Override
        public void render(GoMEntity entity,float yaw,float pt,@NotNull PoseStack ps,@NotNull MultiBufferSource buf,int light){
            super.render(entity,yaw,pt,ps,buf,light);
            if(entity.ɦ2())_rl(entity,ps,buf,pt,light,1F,0F,0F,0.2F,true);
            float ef=entity.ɦ3();
            float off=ef*0.04F-0.02F;
            if(off>0.001F){
                Random r=new Random((long)entity.getId()*98765L+entity.tickCount*12345L);
                _rg(entity,ps,buf,pt,light,off,r,1F,0F,0F,ef*0.3F);
                _rg(entity,ps,buf,pt,light,off,r,0F,1F,1F,ef*0.3F);
            }
        }
        private void _rg(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float off,Random r,float rc,float gc,float bc,float a){
            ps.pushPose();
            float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1F,-1F,1F);
            ps.translate(0F,-1.501F,0F);
            ps.translate((RD.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (RD.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (RD.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(TX)),light,OverlayTexture.NO_OVERLAY,rc,gc,bc,a);
            ps.popPose();
        }
        private void _rl(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float r,float g,float b,float a,boolean hurt){
            ps.pushPose();
            float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1.0002F,-1.0002F,1.0002F);
            ps.translate(0F,-1.501F,0F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(TX)),light,
                    OverlayTexture.pack(OverlayTexture.u(0F),OverlayTexture.v(hurt)),r,g,b,a);
            ps.popPose();
        }
    }
}