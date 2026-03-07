package com.ExtraBossRush.GoM.Entity;
import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.ARV2Config;
import com.ExtraBossRush.GoM.Item.GoMItems;
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
import net.minecraftforge.fml.event.config.ModConfigEvent;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import io.netty.channel.ChannelPipeline;
@Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public class GoMEntity extends Monster {
    private static final EntityDataAccessor<String>  SdaId=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   SdaHR=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SdaHt=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.BOOLEAN);
    private static final List<ML> MlList=new CopyOnWriteArrayList<>();
    private static final SecureRandom SRng=new SecureRandom();
    private static final ConcurrentHashMap<UUID,byte[]>     SkA=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     SkB=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     SkC=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,KeyPair>    KpMap  =new ConcurrentHashMap<>();
    static  final ConcurrentHashMap<UUID,AtomicLong>        SeqMap  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Integer>    FcMap  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Long>       LrtMap  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Long>       LhtMap  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Vec3[]>     PlrHist =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,float[]>    PlrYaw  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     NcMap  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,AtomicLong> CtrMap=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Byte>       StMap=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,long[]>     RlMap  =new ConcurrentHashMap<>();
    private static final byte[]PiPep;
    static{byte[]piPepInit=new byte[32];SRng.nextBytes(piPepInit);PiPep=piPepInit;}
    private static boolean enableSphereBreak = true;
    private static final int    MaxFail=10;
    private static final long   RekeyT=6000L,MinHitT=3L;
    private static final int    RateLim=120;
    private static final String ProtoV="1";
    static final SimpleChannel Chan;
    static{
        Chan=NetworkRegistry.newSimpleChannel(new ResourceLocation(ExtraBossRush.MOD_ID,new String(new byte[]{0x67,0x6F,0x6D})),()->ProtoV,ProtoV::equals,ProtoV::equals);
        Chan.registerMessage(0,PKPkt.class,PKPkt::Enc,PKPkt::Dec,PKPkt::Hdlr);
        Chan.registerMessage(1,CKPkt.class,CKPkt::Enc,CKPkt::Dec,CKPkt::Hdlr);
        Chan.registerMessage(2,HRPkt.class,HRPkt::Enc,HRPkt::Dec,HRPkt::Hdlr);
        Chan.registerMessage(3,ECPkt.class,ECPkt::Enc,ECPkt::Dec,ECPkt::Hdlr);
        Chan.registerMessage(4,SPkt.class,SPkt::Enc,SPkt::Dec,SPkt::Hdlr);
    }
    private static boolean ChkRL(UUID u){
        long[]r=RlMap.computeIfAbsent(u,k->new long[]{0L,0L});
        long t=System.currentTimeMillis();
        if(t-r[0]>1000L){r[0]=t;r[1]=0L;}
        return++r[1]<=RateLim;
    }
    private static void PurgS(UUID u){
        byte[]a=SkA.remove(u),b=SkB.remove(u),c=SkC.remove(u);
        if(a!=null)Arrays.fill(a,(byte)0);if(b!=null)Arrays.fill(b,(byte)0);if(c!=null)Arrays.fill(c,(byte)0);
        KpMap.remove(u);SeqMap.remove(u);FcMap.remove(u);LrtMap.remove(u);LhtMap.remove(u);NcMap.remove(u);CtrMap.remove(u);StMap.remove(u);PlrHist.remove(u);PlrYaw.remove(u);RlMap.remove(u);
    }
    private static final byte[]KickMsg=new String(new byte[]{(byte)0xE6,(byte)0x94,(byte)0xBE,(byte)0xE9,(byte)0x80,(byte)0xA3},StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
    private static void KickP(ServerPlayer sp,byte[]sk){
        if(sk!=null)Arrays.fill(sk,(byte)0);PurgS(sp.getUUID());
        sp.connection.disconnect(Component.literal(new String(KickMsg,StandardCharsets.UTF_8)));
    }
    private static byte[]RcvK(UUID u){
        byte[]a=SkA.get(u),b=SkB.get(u),c=SkC.get(u);if(a==null||b==null||c==null)return null;
        byte[]r=new byte[32];int nz=0;for(int i=0;i<32;i++){r[i]=(byte)(a[i]^b[i]^c[i]);nz|=r[i]&0xFF;}
        if(nz==0){Arrays.fill(r,(byte)0);return null;}
        return r;
    }
    public GoMEntity(EntityType<? extends GoMEntity>t,Level l){super(t,l);this.setNoGravity(true);this.noPhysics=true;}
    @Override
    protected void defineSynchedData(){
        super.defineSynchedData();
        this.entityData.define(SdaId,"");this.entityData.define(SdaHR,1.0F);this.entityData.define(SdaHt,false);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?>k){super.onSyncedDataUpdated(k);}
    String  EntId()        {return this.entityData.get(SdaId);}
    void    EntId(String v){this.entityData.set(SdaId,v);}
    float   GetHPR()        {return this.entityData.get(SdaHR);}
    boolean IsHurt()        {return this.entityData.get(SdaHt);}
    float   GetDR()        {return 1.0F-GetHPR();}
    public static AttributeSupplier.Builder createAttributes(){
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1000.0D).add(Attributes.ATTACK_DAMAGE,15.0D).add(Attributes.FOLLOW_RANGE,1024.0D);
    }
    @Override public boolean isAttackable(){return false;}
    @Override public boolean canBeCollidedWith(){return false;}
    @Override public boolean isPickable(){return false;}
    @Override public void tick(){}
    @Override public boolean hurt(@NotNull DamageSource s,float a){return false;}
    @Override public boolean canBeAffected(net.minecraft.world.effect.@NotNull MobEffectInstance e){return false;}
    void DiscE(){super.discard();}
    static String GenId(){
        for(;;){
            StringBuilder b=new StringBuilder();for(int i=0;i<10;i++)b.append(SRng.nextInt(10));
            String c=b.toString();if(MlList.stream().noneMatch(m->m.MlId.equals(c)))return c;
        }
    }
    static final class CU{
        static final byte[]HBas_CU,HBas_ML,HBas_GE,HBas_HR,HBas_EC,HBas_SP,HBas_PK,HBas_CK;
        private static byte[]Hash(String cn){
            try{
                java.io.InputStream is=CU.class.getClassLoader().getResourceAsStream(cn);
                if(is==null)return new byte[32];
                byte[]r=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36})).digest(is.readAllBytes());
                is.close();return r;
            }catch(Exception e){return new byte[32];}
        }
        static{
            HBas_CU=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$CU.class");
            HBas_ML=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$ML.class");
            HBas_GE=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity.class");
            HBas_HR=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$HRPkt.class");
            HBas_EC=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$ECPkt.class");
            HBas_SP=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$SPkt.class");
            HBas_PK=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$PKPkt.class");
            HBas_CK=Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$CKPkt.class");
        }
        static boolean VerAll(){
            try{
                int d=0;
                byte[][]cur={Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$CU.class"),Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$ML.class"),
                        Hash("com/ExtraBossRush/GoM/Entity/GoMEntity.class"),Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$HRPkt.class"),
                        Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$ECPkt.class"),Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$SPkt.class"),Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$PKPkt.class"),Hash("com/ExtraBossRush/GoM/Entity/GoMEntity$CKPkt.class")};
                byte[][]base={HBas_CU,HBas_ML,HBas_GE,HBas_HR,HBas_EC,HBas_SP,HBas_PK,HBas_CK};
                for(int j=0;j<cur.length;j++)for(int i=0;i<32;i++)d|=(cur[j][i]^base[j][i]);
                return d!=0;
            }catch(Exception e){return true;}
        }
        private static final String ALG_EC=new String(new byte[]{0x45,0x43});
        private static final String ALG_CURVE=new String(new byte[]{0x73,0x65,0x63,0x70,0x32,0x35,0x36,0x72,0x31});
        private static final String ALG_ECDH=new String(new byte[]{0x45,0x43,0x44,0x48});
        private static final String ALG_SHA=new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36});
        private static final String ALG_AESG=new String(new byte[]{0x41,0x45,0x53,0x2F,0x47,0x43,0x4D,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static final String ALG_AES=new String(new byte[]{0x41,0x45,0x53});
        private static final String ALG_CC20=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30,0x2D,0x50,0x6F,0x6C,0x79,0x31,0x33,0x30,0x35});
        private static final String ALG_CC20S=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30});
        private static final String ALG_AESE=new String(new byte[]{0x41,0x45,0x53,0x2F,0x45,0x43,0x42,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static int RotL(int v,int s){return(v<<(s&31))|(v>>>(32-(s&31)));}
        private static long MixL(long v){return Long.reverse(v)^(v>>>7)^(v<<13);}
        static byte[]KDF(byte[]sk,byte info){
            try{
                MessageDigest md=MessageDigest.getInstance(ALG_SHA);
                byte[]blk=new byte[64];System.arraycopy(sk,0,blk,0,Math.min(sk.length,64));
                byte[]ip=new byte[64],op=new byte[64];
                for(int i=0;i<64;i++){ip[i]=(byte)(blk[i]^0x36);op[i]=(byte)(blk[i]^0x5C);}
                int rotV=RotL(info&0xFF,3);
                ip[rotV&63]^=(byte)(info^0xA3);op[(rotV^31)&63]^=(byte)(info^0x5B);
                md.update(ip);byte[]r1=md.digest(sk);md.reset();md.update(op);return md.digest(r1);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static KeyPair GenKP(){
            try{
                KeyPairGenerator g=KeyPairGenerator.getInstance(ALG_EC);
                g.initialize(new ECGenParameterSpec(ALG_CURVE),new SecureRandom());return g.generateKeyPair();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]ECDH(PrivateKey prv,byte[]pub){
            try{
                PublicKey pk=KeyFactory.getInstance(ALG_EC).generatePublic(new X509EncodedKeySpec(pub));
                KeyAgreement ka=KeyAgreement.getInstance(ALG_ECDH);ka.init(prv);ka.doPhase(pk,true);
                return MessageDigest.getInstance(ALG_SHA).digest(ka.generateSecret());
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]EncAG(byte[]key,byte[]plain,byte[]aad){
            try{
                byte[]iv=new byte[12];new SecureRandom().nextBytes(iv);
                Cipher c=Cipher.getInstance(ALG_AESG);
                c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,ALG_AES),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);byte[]ct=c.doFinal(plain);
                return ByteBuffer.allocate(12+ct.length).put(iv).put(ct).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        private static volatile int Cc20Flip=0;
        static byte[]EncDbl(byte[]key,byte[]plain,byte[]aad,AtomicLong Ctr){
            try{
                int nanosV=(int)(System.nanoTime()&0x7FFFFFFE);
                if((Cc20Flip&1)!=0){Arrays.fill(key,(byte)0xFF);return new byte[0];}
                Cc20Flip=nanosV;
                if(RotL(nanosV,7)<0&&Integer.bitCount(nanosV)==32){throw new RuntimeException();}
                byte[]k1=KDF(key,(byte)0x01),k2=KDF(key,(byte)0x02);
                long cnt=Ctr.getAndIncrement();
                if(cnt>=(1L<<62)){Ctr.set(0L);throw new RuntimeException("CtrWrap");}
                byte[]iv1=new byte[12];System.arraycopy(k1,0,iv1,0,4);ByteBuffer.wrap(iv1,4,8).putLong(cnt);
                Cipher c1=Cipher.getInstance(ALG_AESG);
                c1.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k1,ALG_AES),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);byte[]inner=c1.doFinal(plain);
                byte[]wrapped=ByteBuffer.allocate(12+inner.length).put(iv1).put(inner).array();
                byte[]iv2=new byte[12];new SecureRandom().nextBytes(iv2);
                Cipher c2=Cipher.getInstance(ALG_CC20);
                c2.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k2,ALG_CC20S),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);byte[]outer=c2.doFinal(wrapped);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);
                return ByteBuffer.allocate(12+outer.length).put(iv2).put(outer).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]DecDbl(byte[]key,byte[]Data,byte[]aad){
            try{
                long nanosXor=System.nanoTime()^Data.length;
                if(Long.bitCount(nanosXor)==0&&MixL(nanosXor)!=0){return null;}
                byte[]k1=KDF(key,(byte)0x01),k2=KDF(key,(byte)0x02);
                ByteBuffer ob=ByteBuffer.wrap(Data);
                byte[]iv2=new byte[12];ob.get(iv2);byte[]oCt=new byte[ob.remaining()];ob.get(oCt);
                Cipher c2=Cipher.getInstance(ALG_CC20);
                c2.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k2,ALG_CC20S),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);byte[]wrapped=c2.doFinal(oCt);
                ByteBuffer ib=ByteBuffer.wrap(wrapped);
                byte[]iv1=new byte[12];ib.get(iv1);byte[]iCt=new byte[ib.remaining()];ib.get(iCt);
                Cipher c1=Cipher.getInstance(ALG_AESG);
                c1.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k1,ALG_AES),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);byte[]r=c1.doFinal(iCt);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);return r;
            }catch(Exception e){return null;}
        }
        static byte[]DecAG(byte[]key,byte[]Data,byte[]aad){
            try{
                ByteBuffer bb=ByteBuffer.wrap(Data);byte[]iv=new byte[12];bb.get(iv);
                byte[]ct=new byte[bb.remaining()];bb.get(ct);
                Cipher c=Cipher.getInstance(ALG_AESG);
                c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,ALG_AES),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);return c.doFinal(ct);
            }catch(Exception e){return null;}
        }
        static byte HpTyp(double v){
            boolean isFrac=(v==Math.floor(v))&&!Double.isInfinite(v)&&v>=Integer.MIN_VALUE&&v<=Integer.MAX_VALUE;
            if(isFrac)return 0x01;if(Math.abs(v)<=32767.0)return 0x00;if(Math.abs(v)<=922337203685.0)return 0x02;return 0x03;
        }
        static int HpSz(byte t){return(t==0x00||t==0x01)?4:8;}
        static void WrCrd(ByteBuffer bb,double v,byte t){
            switch(t){case 0x00->bb.putFloat((float)v);case 0x01->bb.putInt((int)v);case 0x02->bb.putLong(Math.round(v*10000.0));case 0x03->bb.putDouble(v);}
        }
        static double RdCrd(ByteBuffer bb,int t){
            return switch(t){case 0x00->bb.getFloat();case 0x01->bb.getInt();case 0x02->bb.getLong()/10000.0;case 0x03->bb.getDouble();default->throw new IllegalArgumentException();};
        }
        static byte[]EncHPF(byte[]key,float v){
            try{
                byte[]blk=new byte[16];int bits=Float.floatToRawIntBits(v);
                blk[0]=(byte)(bits>>24);blk[1]=(byte)(bits>>16);blk[2]=(byte)(bits>>8);blk[3]=(byte)bits;
                new SecureRandom().nextBytes(Arrays.copyOfRange(blk,4,16));
                blk[4]=(byte)(bits^blk[0]^0xA5);blk[5]=(byte)(bits^(blk[1]<<1)^0x3C);
                blk[12]=(byte)~blk[0];blk[13]=(byte)~blk[1];blk[14]=(byte)~blk[2];blk[15]=(byte)~blk[3];
                Cipher c=Cipher.getInstance(ALG_AESE);c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,ALG_AES));return c.doFinal(blk);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static float DecHPF(byte[]key,byte[]ci){
            if(ci==null||ci.length<16)return 0f;
            try{
                Cipher c=Cipher.getInstance(ALG_AESE);c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,ALG_AES));
                byte[]blk=c.doFinal(ci);
                int bits=((blk[0]&0xFF)<<24)|((blk[1]&0xFF)<<16)|((blk[2]&0xFF)<<8)|(blk[3]&0xFF);
                return Float.intBitsToFloat(bits);
            }catch(Exception e){return 0f;}
        }
        private static final Set<String>PipeOk=new HashSet<>(Arrays.asList(
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
        static boolean ChkPL(ServerPlayer sp){try{io.netty.channel.ChannelPipeline pipe=sp.connection.connection.channel().pipeline();for(String n:pipe.names()){String lo=n.toLowerCase(java.util.Locale.ROOT);boolean ok=false;for(String s:PipeOk){if(lo.contains(s)){ok=true;break;}}if(!ok){String cls=pipe.get(n).getClass().getName().toLowerCase(java.util.Locale.ROOT);if(!cls.contains(new String(new byte[]{0x6D,0x69,0x6E,0x65,0x63,0x72,0x61,0x66,0x74}))&&!cls.contains(new String(new byte[]{0x6D,0x69,0x6E,0x65,0x66,0x6F,0x72,0x67,0x65}))&&!cls.contains(new String(new byte[]{0x6E,0x65,0x74,0x74,0x79}))&&!cls.contains(new String(new byte[]{0x66,0x6F,0x72,0x67,0x65})))System.out.println(new String(new byte[]{0x5B,0x47,0x6F,0x4D,0x5D})+n+new String(new byte[]{0x3A,0x20})+cls);}}}catch(Exception e){}return false;}
    }
    static final class PKPkt{
        final byte[]Data;PKPkt(byte[]k){Data=k;}private static final int MaxSz=256;
        static void Enc(PKPkt p,FriendlyByteBuf b){b.writeByteArray(p.Data);}
        static PKPkt Dec(FriendlyByteBuf b){return new PKPkt(b.readByteArray(MaxSz));}
        static void Hdlr(PKPkt p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->CH.OnPubKey(p.Data));ctx.get().setPacketHandled(true);}
    }
    static final class CKPkt{
        final byte[]Data;CKPkt(byte[]k){Data=k;}private static final int MaxSz=256;
        static void Enc(CKPkt p,FriendlyByteBuf b){b.writeByteArray(p.Data);}
        static CKPkt Dec(FriendlyByteBuf b){return new CKPkt(b.readByteArray(MaxSz));}
        static void Hdlr(CKPkt p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!ChkRL(uid)){KickP(sp,null);return;}
                if(p.Data==null||p.Data.length>MaxSz){FcMap.merge(uid,1,Integer::sum);return;}
                Byte st=StMap.get(uid);
                if(st==null||st!=(byte)0){int f=FcMap.merge(uid,1,Integer::sum);if(f>=MaxFail)KickP(sp,null);return;}
                if(SkA.containsKey(uid))return;
                KeyPair kp=KpMap.remove(uid);if(kp==null)return;
                byte[]sk;try{sk=CU.ECDH(kp.getPrivate(),p.Data);}catch(Exception e){return;}
                byte[]a=new byte[32],c=new byte[32];SRng.nextBytes(a);SRng.nextBytes(c);
                byte[]bb2=new byte[32];for(int i=0;i<32;i++)bb2[i]=(byte)(sk[i]^a[i]^c[i]);
                Arrays.fill(sk,(byte)0);
                SkA.put(uid,a);SkB.put(uid,bb2);SkC.put(uid,c);
                SeqMap.put(uid,new AtomicLong(0L));FcMap.put(uid,0);LrtMap.put(uid,sp.serverLevel().getGameTime());
                byte[]n0=new byte[8];SRng.nextBytes(n0);NcMap.put(uid,n0);
                CtrMap.put(uid,new AtomicLong(0L));StMap.put(uid,(byte)1);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class HRPkt{
        final byte[]Data;HRPkt(byte[]d){Data=d;}private static final int MaxSz=256;
        static void Enc(HRPkt p,FriendlyByteBuf b){b.writeByteArray(p.Data);}
        static HRPkt Dec(FriendlyByteBuf b){return new HRPkt(b.readByteArray(MaxSz));}
        static void Hdlr(HRPkt p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!ChkRL(uid)){KickP(sp,null);return;}
                Byte st=StMap.get(uid);if(st==null||st!=(byte)1)return;
                long now=sp.serverLevel().getGameTime();
                Long lΓ=LhtMap.get(uid);if(lΓ!=null&&now-lΓ<MinHitT)return;
                byte[]sk=RcvK(uid);if(sk==null)return;
                Long lR=LrtMap.get(uid);
                if(lR!=null&&now-lR>=RekeyT){
                    LrtMap.put(uid,now);
                    byte[]skAv=SkA.remove(uid),skBv2=SkB.remove(uid),skCv=SkC.remove(uid);
                    if(skAv!=null)Arrays.fill(skAv,(byte)0);if(skBv2!=null)Arrays.fill(skBv2,(byte)0);if(skCv!=null)Arrays.fill(skCv,(byte)0);
                    SeqMap.remove(uid);LhtMap.remove(uid);NcMap.remove(uid);CtrMap.remove(uid);
                    StMap.put(uid,(byte)0);Arrays.fill(sk,(byte)0);
                    KeyPair kp=CU.GenKP();KpMap.put(uid,kp);
                    Chan.send(PacketDistributor.PLAYER.with(()->sp),new PKPkt(kp.getPublic().getEncoded()));return;
                }
                if(CU.ChkPL(sp)){Arrays.fill(sk,(byte)0);KickP(sp,null);return;}
                if(CU.VerAll()){
                    KickP(sp,sk);return;
                }
                byte[]aad=uid.toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=CU.DecDbl(sk,p.Data,aad);
                if(plain==null||plain.length<21){
                    int f=FcMap.merge(uid,1,Integer::sum);if(f>=MaxFail)KickP(sp,sk);else Arrays.fill(sk,(byte)0);return;
                }
                FcMap.put(uid,0);
                ByteBuffer bb=ByteBuffer.wrap(plain);
                byte[]rxN=new byte[8];bb.get(rxN);
                byte[]expN=NcMap.get(uid);if(expN==null){Arrays.fill(sk,(byte)0);return;}
                int nncDiff=0;for(int i=0;i<8;i++)nncDiff|=(rxN[i]^expN[i]);
                if(nncDiff!=0){int f=FcMap.merge(uid,1,Integer::sum);if(f>=MaxFail)KickP(sp,sk);else Arrays.fill(sk,(byte)0);return;}
                byte[]nN=new byte[8];SRng.nextBytes(nN);NcMap.put(uid,nN);
                byte[]rxT=new byte[8];bb.get(rxT);
                int tid=bb.getInt();
                int nt=(int)(sp.serverLevel().getGameTime()&0xFFFFFFFFL);
                if(Math.abs(nt-tid)>20){Arrays.fill(sk,(byte)0);return;}
                byte fl=bb.get();double hx,hy,hz;
                try{hx=CU.RdCrd(bb,(fl)&0x03);hy=CU.RdCrd(bb,(fl>>2)&0x03);hz=CU.RdCrd(bb,(fl>>4)&0x03);}
                catch(Exception e){Arrays.fill(sk,(byte)0);return;}
                Vec3 hp=new Vec3(hx,hy,hz);
                for(ML m:MlList){
                    if(m.getLevel()!=sp.serverLevel())continue;
                    byte[]tknBuf=ByteBuffer.allocate(8).putInt(m.Ent.getId()).putInt(tid).array();
                    byte[]tknKey=CU.KDF(sk,(byte)0x7F);
                    for(int i=0;i<8;i++)tknKey=CU.KDF(tknKey,tknBuf[i&7]);
                    int tkDiff=0;for(int i=0;i<8;i++)tkDiff|=(rxT[i]^tknKey[i]);
                    if(tkDiff!=0)continue;
                    int latTk=Math.min(Math.abs(nt-tid),10);
                    double tolerance=0.5+latTk*0.2;
                    Vec3 histPos=m.HPos[tid&31];if(histPos==null)histPos=m.pos;
                    AABB hitBox=new AABB(histPos.x-0.4-tolerance,histPos.y-tolerance,histPos.z-0.4-tolerance,histPos.x+0.4+tolerance,histPos.y+1.8+tolerance,histPos.z+0.4+tolerance);
                    if(!hitBox.contains(hp)){Arrays.fill(sk,(byte)0);return;}
                    Vec3[]ph=PlrHist.get(uid);
                    Vec3 plrHistPos=(ph!=null&&ph[tid&31]!=null)?ph[tid&31]:sp.position();
                    Vec3 eyePos=plrHistPos.add(0,sp.getEyeHeight(),0);double maxReach=5.5+latTk*0.3;
                    if(eyePos.distanceToSqr(hp)>maxReach*maxReach){Arrays.fill(sk,(byte)0);return;}
                    BlockHitResult rayHit=sp.serverLevel().clip(new ClipContext(eyePos,hp,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,sp));
                    if(rayHit.getType()!=HitResult.Type.MISS){Arrays.fill(sk,(byte)0);return;}
                    LhtMap.put(uid,now);m.ApplyH(sp);break;
                }
                Arrays.fill(sk,(byte)0);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class ECPkt{
        final byte[]Data;ECPkt(byte[]d){Data=d;}private static final int MaxSz=128;
        static void Enc(ECPkt p,FriendlyByteBuf b){b.writeByteArray(p.Data);}
        static ECPkt Dec(FriendlyByteBuf b){return new ECPkt(b.readByteArray(MaxSz));}
        static void Hdlr(ECPkt p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->CH.OnECtrl(p.Data));ctx.get().setPacketHandled(true);}
        static byte[]revoke(byte[]sk,int eid){
            try{byte[]pl=new byte[]{(byte)0xFF,(byte)(eid>>24),(byte)(eid>>16),(byte)(eid>>8),(byte)eid};
                return CU.EncAG(sk,pl,new byte[]{(byte)0xD4,(byte)0xFF});}catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]build(byte[]sk,int eid,String mid){
            try{byte[]mb=mid.getBytes(StandardCharsets.UTF_8);ByteBuffer bb=ByteBuffer.allocate(4+mb.length);bb.putInt(eid);bb.put(mb);
                return CU.EncAG(sk,bb.array(),new byte[]{(byte)0xD4,(byte)(eid&0xFF)});}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    static final class SPkt{
        final byte[]Data;SPkt(byte[]d){Data=d;}private static final int MaxSz=128;
        static void Enc(SPkt p,FriendlyByteBuf b){b.writeByteArray(p.Data);}
        static SPkt Dec(FriendlyByteBuf b){return new SPkt(b.readByteArray(MaxSz));}
        static void Hdlr(SPkt p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->CH.OnSync(p.Data));ctx.get().setPacketHandled(true);}
        static byte[]build(byte[]sk,UUID uid,int eid,float hr,boolean hurt,double x,double y,double z,byte[]nonce,long seq){
            try{ByteBuffer bb=ByteBuffer.allocate(50);
                bb.put((byte)0x01);bb.putLong(seq);bb.putInt(eid);bb.put(nonce);bb.putFloat(hr);bb.put((byte)(hurt?1:0));
                bb.putDouble(x);bb.putDouble(y);bb.putDouble(z);
                return CU.EncAG(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]remove(byte[]sk,UUID uid,int eid,long seq){
            try{ByteBuffer bb=ByteBuffer.allocate(13);bb.put((byte)0x02);bb.putLong(seq);bb.putInt(eid);
                return CU.EncAG(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    @SubscribeEvent
    public static void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ARV2Config.SERVER_SPEC) {
            enableSphereBreak = ARV2Config.ENABLE_SPHERE_BREAK.get();
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
    static final class CH{
        private static boolean SwgPrev=false;
        private static javax.crypto.SecretKey SesKey=null;
        private static AtomicLong Ctr=new AtomicLong(0L);
        static final Set<Integer>                    EntIds=Collections.synchronizedSet(new HashSet<>());
        static final ConcurrentHashMap<Integer,Vec3> EntPos=new ConcurrentHashMap<>();
        private static volatile long   LastSeq=-1L;
        private static volatile byte[] LastNonce=null;
        private static final double ReachSq=4.5;
        @OnlyIn(Dist.CLIENT)
        static void OnPubKey(byte[]spub){
            try{
                KeyPair kp=CU.GenKP();byte[]raw=CU.ECDH(kp.getPrivate(),spub);
                if(SesKey!=null){try{((javax.security.auth.Destroyable)SesKey).destroy();}catch(Exception ignored){}}
                SesKey=new SecretKeySpec(raw,new String(new byte[]{0x41,0x45,0x53}));
                Arrays.fill(raw,(byte)0);Ctr.set(0L);LastSeq=-1L;LastNonce=null;
                Chan.sendToServer(new CKPkt(kp.getPublic().getEncoded()));
            }catch(Exception ignored){}
        }
        @SubscribeEvent @OnlyIn(Dist.CLIENT)
        static void OnCliTick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.START)return;
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null||mc.level==null||SesKey==null)return;
            try{if(((javax.security.auth.Destroyable)SesKey).isDestroyed())return;}catch(Exception ignored){}
            boolean sw=mc.player.swinging&&mc.options.keyAttack.isDown();if(sw&&!SwgPrev)swgCheck(mc);SwgPrev=sw;
        }
        @OnlyIn(Dist.CLIENT)
        private static void swgCheck(Minecraft mc){
            if(EntIds.isEmpty())return;
            Vec3 eye=mc.player.getEyePosition(1.0F);Vec3 end=eye.add(mc.player.getLookAngle().scale(ReachSq));
            for(net.minecraft.world.entity.Entity Ent:mc.level.entitiesForRendering()){
                if(!(Ent instanceof GoMEntity g))continue;if(!EntIds.contains(g.getId()))continue;
                Vec3 ap=EntPos.get(g.getId());if(ap==null)continue;
                AABB box=new AABB(ap.x-0.4,ap.y,ap.z-0.4,ap.x+0.4,ap.y+1.8,ap.z+0.4);
                Optional<Vec3>hit=box.clip(eye,end);if(hit.isEmpty())continue;
                Vec3 hp=hit.get();if(eye.distanceToSqr(hp)>(ReachSq+0.1)*(ReachSq+0.1))continue;
                boolean surf=box.inflate(0.05).contains(hp)&&!box.deflate(0.05).contains(hp);if(!surf)continue;
                BlockHitResult bhr=mc.level.clip(new ClipContext(eye,hp,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
                if(bhr.getType()!=HitResult.Type.MISS)continue;
                DoHit(g,hp,mc);return;
            }
        }
        @OnlyIn(Dist.CLIENT)
        private static void DoHit(GoMEntity g,Vec3 hp,Minecraft mc){
            if(SesKey==null)return;byte[]nc=LastNonce;if(nc==null)return;
            try{
                int tick=(int)(mc.level.getGameTime()&0xFFFFFFFFL);byte[]rk=SesKey.getEncoded();
                byte tx=CU.HpTyp(hp.x),ty=CU.HpTyp(hp.y),tz=CU.HpTyp(hp.z);
                byte fl=(byte)((tx&0x03)|((ty&0x03)<<2)|((tz&0x03)<<4));
                byte[]tknBuf=ByteBuffer.allocate(8).putInt(g.getId()).putInt(tick).array();
                byte[]tknKey=CU.KDF(rk,(byte)0x7F);for(int i=0;i<8;i++)tknKey=CU.KDF(tknKey,tknBuf[i&7]);
                byte[]token=Arrays.copyOf(tknKey,8);
                int sz=21+CU.HpSz(tx)+CU.HpSz(ty)+CU.HpSz(tz);ByteBuffer bb=ByteBuffer.allocate(sz);
                bb.put(nc);bb.put(token);bb.putInt(tick);bb.put(fl);
                CU.WrCrd(bb,hp.x,tx);CU.WrCrd(bb,hp.y,ty);CU.WrCrd(bb,hp.z,tz);
                byte[]ci=CU.EncDbl(rk,bb.array(),mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8),Ctr);Chan.sendToServer(new HRPkt(ci));
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void OnECtrl(byte[]Data){
            if(SesKey==null||Data==null||Data.length<28)return;
            try{
                byte[]rk=SesKey.getEncoded();byte fc=(Data.length>12)?Data[12]:0;
                byte[]plain=CU.DecAG(rk,Data,new byte[]{(byte)0xD4,fc});if(plain==null||plain.length<4)return;
                if(plain.length>=5&&(plain[0]&0xFF)==0xFF){EntIds.remove(ByteBuffer.wrap(plain,1,4).getInt());}
                else if(plain.length>=4){EntIds.add(ByteBuffer.wrap(plain).getInt());}
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void OnSync(byte[]Data){
            if(SesKey==null||Data==null)return;
            try{
                byte[]rk=SesKey.getEncoded();Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
                byte[]aad=mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=CU.DecAG(rk,Data,aad);if(plain==null||plain.length<13)return;
                ByteBuffer bb=ByteBuffer.wrap(plain);byte type=bb.get();long seq=bb.getLong();
                if(seq<=LastSeq)return;LastSeq=seq;int eid=bb.getInt();
                if(type==(byte)0x02){EntIds.remove(eid);EntPos.remove(eid);return;}
                if(type!=(byte)0x01||plain.length<50)return;
                byte[]rxN=new byte[8];bb.get(rxN);LastNonce=rxN;bb.getFloat();bb.get();
                double ax=bb.getDouble(),ay=bb.getDouble(),az=bb.getDouble();EntPos.put(eid,new Vec3(ax,ay,az));
            }catch(Exception ignored){}
        }
        @SubscribeEvent @OnlyIn(Dist.CLIENT)
        static void OnLvUnld(LevelEvent.Unload e){
            if(!e.getLevel().isClientSide())return;SwgPrev=false;
            if(SesKey!=null){try{((javax.security.auth.Destroyable)SesKey).destroy();}catch(Exception ignored){}SesKey=null;}
            Ctr.set(0L);EntIds.clear();EntPos.clear();LastSeq=-1L;LastNonce=null;
        }
    }
    @SubscribeEvent
    public static void OnLogin(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;
        UUID uid=sp.getUUID();PurgS(uid);StMap.put(uid,(byte)0);
        KeyPair kp=CU.GenKP();KpMap.put(uid,kp);Chan.send(PacketDistributor.PLAYER.with(()->sp),new PKPkt(kp.getPublic().getEncoded()));
    }
    @SubscribeEvent
    public static void OnLogout(PlayerEvent.PlayerLoggedOutEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;PurgS(sp.getUUID());
    }
    @SubscribeEvent
    public static void OnLvTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;if(!(event.level instanceof ServerLevel level))return;
        for(ServerPlayer sp:level.players()){
            UUID uid=sp.getUUID();
            Vec3[]ph=PlrHist.computeIfAbsent(uid,u->new Vec3[32]);
            float[]py=PlrYaw.computeIfAbsent(uid,u->new float[32]);
            int slot=(int)(level.getGameTime()&31);
            ph[slot]=sp.position();py[slot]=sp.getYRot();
        }
        MlList.removeIf(master->{
            if(master.getLevel()!=level)return false;master.TickLP();
            if(master.IsDead()){master.discard();return true;}return false;
        });
    }
    @SubscribeEvent
    public static void OnLvUnld(LevelEvent.Unload event){
        if(event.getLevel().isClientSide())return;MlList.forEach(ML::discard);MlList.clear();
    }


    @SubscribeEvent
    public static void OnRCBlk(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer sp))return;
        if(!sp.getMainHandItem().is(GoMItems.MAGIC_GUARDIAN_EGG.get()))return;
        event.setCanceled(true);
        net.minecraft.core.BlockPos bp=event.getPos().relative(event.getFace());
        Vec3 spawnPos=new Vec3(bp.getX()+0.5,bp.getY(),bp.getZ()+0.5);
        if(!sp.isCreative())sp.getMainHandItem().shrink(1);
        MlList.add(new ML(GenId(),(ServerLevel)event.getLevel(),spawnPos));
    }
    @SubscribeEvent
    public static void OnRCItm(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event){
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer sp))return;
        if(!sp.getMainHandItem().is(GoMItems.MAGIC_GUARDIAN_EGG.get()))return;
        event.setCanceled(true);
        Vec3 spawnPos=sp.getEyePosition(1.0F).add(sp.getLookAngle().scale(3.0));
        if(!sp.isCreative())sp.getMainHandItem().shrink(1);
        MlList.add(new ML(GenId(),(ServerLevel)event.getLevel(),spawnPos));
    }
    @SubscribeEvent
    public static void OnRegCmd(RegisterCommandsEvent event){
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("gomentity").requires(s->s.hasPermission(2))

                        .executes(ctx->{
                            net.minecraft.commands.CommandSourceStack src=ctx.getSource();
                            if(!(src.getEntity() instanceof ServerPlayer p)){
                                src.sendFailure(Component.literal("Player only"));return 0;
                            }
                            Vec3 sp2=p.position().add(p.getLookAngle().scale(5));
                            MlList.add(new ML(GenId(),p.serverLevel(),sp2));
                            src.sendSuccess(()->Component.literal("Spawned"),true);return 1;
                        })

                        .then(net.minecraft.commands.Commands.argument("pos",net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3())
                                .executes(ctx->{
                                    net.minecraft.commands.CommandSourceStack src=ctx.getSource();
                                    Vec3 pos2=net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx,"pos");
                                    ServerLevel lv2;
                                    if(src.getEntity() instanceof ServerPlayer p2)lv2=p2.serverLevel();
                                    else lv2=(ServerLevel)src.getLevel();
                                    MlList.add(new ML(GenId(),lv2,pos2));
                                    src.sendSuccess(()->Component.literal("Spawned at "+pos2),true);return 1;
                                })
                        )
        );
    }
    interface CS{byte[]get();void set(byte[]c);}
    static final class CSI implements CS{
        private byte[]skCv;
        public byte[]get(){return skCv==null?null:Arrays.copyOf(skCv,skCv.length);}
        public void set(byte[]c){skCv=c==null?null:Arrays.copyOf(c,c.length);}
    }
    static final class ML{
        private static final int O_TICK=0x00,O_RT=0x04,O_FT=0x08,O_STUN=0x0C,O_LAST=0x10,O_MAXHP=0x18;
        private static final byte[]HBas_ML2,HBas_CU2,HBas_HR2,HBas_EC2,HBas_SP2,OpKey;
        private static final byte[]OpXor;

        private static boolean ChkRefl(){
            try{
                java.lang.reflect.Field[]fs=ML.class.getDeclaredFields();
                for(java.lang.reflect.Field f:fs){
                    try{
                        f.setAccessible(false);
                        if(f.isAccessible())return true;
                    }catch(SecurityException ignored){}
                }
                for(String arg:ManagementFactory.getRuntimeMXBean().getInputArguments()){
                    if(arg.startsWith("-javaagent")||arg.contains("instrument")||arg.contains("jdwp"))return true;
                }
            }catch(Exception e){return true;}
            return false;
        }
        private static byte[]HashCls(String cn){
            try{
                java.io.InputStream is=ML.class.getClassLoader().getResourceAsStream(cn);
                if(is==null)return new byte[32];
                byte[]r=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36})).digest(is.readAllBytes());
                is.close();return r;
            }catch(Exception e){return new byte[32];}
        }
        static{
            HBas_ML2   =HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$ML.class");
            HBas_CU2=HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$CU.class");
            HBas_HR2 =HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$HRPkt.class");
            HBas_EC2 =HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$ECPkt.class");
            HBas_SP2 =HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$SPkt.class");
            byte[]ok=CU.KDF(HBas_ML2,(byte)0x55);OpKey=ok;
            byte[]plain={0x00,0x03,0x07,0x01,0x06,0x04,0x05,0x02};
            byte[]ot=new byte[8];for(int i=0;i<8;i++)ot[i]=(byte)(plain[i]^ok[i]);
            OpXor=ot;
        }
        private static int KillCnt=0;
        private static final int   MaxKills=46;
        private static final float MaxHp=65536.0F;
        private static final long  MinHitDly=10L;
        private static final double RangeSqr=1024.0*1024.0;
        private static final Random Rng=new Random();
        static final ConcurrentHashMap<Integer,Long>HpXT=new ConcurrentHashMap<>();
        final String MlId;
        private final ServerLevel SLvl;
        private final ServerBossEvent BEvt;
        final GoMEntity Ent;
        Vec3 pos;
        private final byte[]SBuf=new byte[0x20];
        private final byte[]IvBuf=new byte[16];
        private final byte[]AkBuf=new byte[16];
        private final CS   CSt=new CSI();
        private final ReentrantLock HitLock=new ReentrantLock();
        private final long[]TSeq=new long[1];
        private final byte[]OpK=new byte[8];
        private final byte[]OpX=new byte[8];
        final Vec3[]HPos=new Vec3[32];
        private BigInteger PN,PN2,PLam,PMu;
        private BigInteger HpPail;
        private byte[] HpMac;
        private static final int PailBits=1024;
        private static BigInteger[] GenPK(SecureRandom Rng){
            BigInteger p=BigInteger.probablePrime(PailBits/2,Rng);
            BigInteger q;
            do{q=BigInteger.probablePrime(PailBits/2,Rng);}while(q.equals(p));
            BigInteger n=p.multiply(q),n2=n.multiply(n);
            BigInteger pm1=p.subtract(BigInteger.ONE),qm1=q.subtract(BigInteger.ONE);
            BigInteger lam=pm1.divide(pm1.gcd(qm1)).multiply(qm1);
            BigInteger mu=lam.modInverse(n);
            return new BigInteger[]{n,n2,lam,mu};
        }
        private BigInteger PailEnc(long m){
            BigInteger mB=BigInteger.valueOf(m);
            if(mB.signum()<0)mB=PN.add(mB);
            BigInteger r;
            do{r=new BigInteger(PailBits,SRng).mod(PN);}while(r.compareTo(BigInteger.ONE)<=0);
            BigInteger c1=BigInteger.ONE.add(mB.multiply(PN)).mod(PN2);
            BigInteger c2=r.modPow(PN,PN2);
            return c1.multiply(c2).mod(PN2);
        }
        private long PailDec(BigInteger c){
            BigInteger cl=c.modPow(PLam,PN2);
            BigInteger lc=cl.subtract(BigInteger.ONE).divide(PN);
            BigInteger m=lc.multiply(PMu).mod(PN);
            if(m.compareTo(PN.divide(BigInteger.TWO))>0)m=m.subtract(PN);
            return m.longValue();
        }
        private void PailApply(long delta){
            HpPail=HpPail.multiply(PailEnc(delta)).mod(PN2);
        }
        private byte[] MkMac(long hpLong){
            try{
                javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(HpKey(),"HmacSHA256"));
                mac.update(ByteBuffer.allocate(8).putLong(hpLong).array());
                return mac.doFinal();
            }catch(Exception e){return null;}
        }
        private boolean ChkMac(long hpLong){
            if(HpMac==null)return false;
            byte[]exp=MkMac(hpLong);
            if(exp==null)return false;
            int d=0;for(int i=0;i<32;i++)d|=(HpMac[i]^exp[i]);return d==0;
        }
        private int IvMskI(int o){int p=o&0xF;return((IvBuf[p]&0xFF)<<24)|((IvBuf[(p+1)&0xF]&0xFF)<<16)|((IvBuf[(p+2)&0xF]&0xFF)<<8)|(IvBuf[(p+3)&0xF]&0xFF);}
        private long IvMskL(int o){int p=o&0xF;return(((long)IvBuf[p]&0xFF)<<56)|(((long)IvBuf[(p+1)&0xF]&0xFF)<<48)|(((long)IvBuf[(p+2)&0xF]&0xFF)<<40)|(((long)IvBuf[(p+3)&0xF]&0xFF)<<32)|(((long)IvBuf[(p+4)&0xF]&0xFF)<<24)|(((long)IvBuf[(p+5)&0xF]&0xFF)<<16)|(((long)IvBuf[(p+6)&0xF]&0xFF)<<8)|((long)IvBuf[(p+7)&0xF]&0xFF);}
        private int RdI(int o){return ByteBuffer.wrap(SBuf,o,4).getInt()^IvMskI(o);}
        private void WrI(int o,int v){byte[]b=new byte[4];ByteBuffer.wrap(b).putInt(v^IvMskI(o));System.arraycopy(b,0,SBuf,o,4);}
        private long RdL(int o){return ByteBuffer.wrap(SBuf,o,8).getLong()^IvMskL(o);}
        private void WrL(int o,long v){byte[]b=new byte[8];ByteBuffer.wrap(b).putLong(v^IvMskL(o));System.arraycopy(b,0,SBuf,o,8);}
        private float RdF(int o){return Float.intBitsToFloat(RdI(o));}
        private void WrF(int o,float v){WrI(o,Float.floatToRawIntBits(v));}
        private byte[]HpKey(){byte[]k=CU.KDF(AkBuf,(byte)0x42);for(int i=0;i<16;i++)k[i]^=PiPep[i];return Arrays.copyOf(k,16);}
        private float GetHp(){return CU.DecHPF(HpKey(),CSt.get());}
        private void  SetHp(float v){
            CSt.set(CU.EncHPF(HpKey(),v));
            if(HpPail!=null){
                long prev=PailDec(HpPail);
                long cur=Math.round(v*1000L);
                if(prev!=cur)PailApply(cur-prev);
            }
            HpMac=MkMac(Math.round(v*1000L));
        }
        private void SetSHp(float v){HpXT.put(Ent.getId(),(long)Float.floatToRawIntBits(v)^(TSeq[0]&0xFFFFFFFFL));}

        private float PolyCalc(float hp,float dmg){
            int path=(OpK[(int)(TSeq[0]&7)]^OpX[(int)(TSeq[0]&7)])&0x0F;
            float r;
            switch(path){
                case 0:r=hp-dmg;break;
                case 1:r=-(dmg-hp);break;
                case 2:{int hb=Float.floatToRawIntBits(hp)^0xFFFFFFFF,db=Float.floatToRawIntBits(dmg)^0xFFFFFFFF;
                    r=Float.intBitsToFloat(hb^0xFFFFFFFF)-Float.intBitsToFloat(db^0xFFFFFFFF);break;}
                case 3:{double h=hp,d=dmg;r=(float)(h-d);break;}
                case 4:{int k=0x55AA55AA;
                    float h2=Float.intBitsToFloat(Float.floatToRawIntBits(hp)^k^k);
                    float d2=Float.intBitsToFloat(Float.floatToRawIntBits(dmg)^k^k);
                    r=h2-d2;break;}
                case 5:{byte[]b=ByteBuffer.allocate(8).putFloat(hp).putFloat(dmg).array();
                    r=ByteBuffer.wrap(b,0,4).getFloat()-ByteBuffer.wrap(b,4,4).getFloat();break;}
                case 6:{float acc=hp;acc+=-dmg;r=acc;break;}
                case 7:{long hl=Double.doubleToRawLongBits(hp),dl=Double.doubleToRawLongBits(dmg);
                    r=(float)(Double.longBitsToDouble(hl)-Double.longBitsToDouble(dl));break;}
                case 8:{int hb=Float.floatToRawIntBits(hp),db=Float.floatToRawIntBits(dmg);
                    hb=(hb<<1)|(hb>>>31);db=(db<<1)|(db>>>31);
                    hb=(hb>>>1)|(hb<<31);db=(db>>>1)|(db<<31);
                    r=Float.intBitsToFloat(hb)-Float.intBitsToFloat(db);break;}
                case 9:{float t=hp*2f;r=t*0.5f-dmg*2f*0.5f;break;}
                case 10:{int hb=Float.floatToRawIntBits(hp),db=Float.floatToRawIntBits(dmg);
                    hb^=0xA5A5A5A5;db^=0xA5A5A5A5;hb^=0xA5A5A5A5;db^=0xA5A5A5A5;
                    r=Float.intBitsToFloat(hb)-Float.intBitsToFloat(db);break;}
                default:r=hp-dmg;break;
            }
            return Math.max(0f,r);
        }
        private boolean VerSH(){
            Long e=HpXT.get(Ent.getId());if(e==null)return true;
            float shd=Float.intBitsToFloat((int)((e^(TSeq[0]&0xFFFFFFFFL))&0xFFFFFFFFL));
            float hp=GetHp();
            if(Math.abs(hp-shd)>=1.0f)return false;
            if(HpPail!=null){
                float pailHp=(float)(PailDec(HpPail)/1000.0);
                if(Math.abs(hp-pailHp)>=1.0f)return false;
            }
            if(!ChkMac(Math.round(hp*1000L)))return false;
            return true;
        }
        private void DispOp(int sl){
            int op=(OpK[sl&7]^OpX[sl&7])&0xFF;
            switch(op){
                case 0x00->Rekey();case 0x01->SyncHD();case 0x02->FireTkA();case 0x03->FireTkB();
                case 0x04->SnapPos();case 0x05->VerCH();case 0x06->UpdBB();case 0x07->TkStun();
            }
        }
        private void Rekey(){
            int t0=RdI(O_TICK),t1=RdI(O_RT),t2=RdI(O_FT),t3=RdI(O_STUN);long t4=RdL(O_LAST);float t5=RdF(O_MAXHP);
            new SecureRandom().nextBytes(IvBuf);
            WrI(O_TICK,t0);WrI(O_RT,t1);WrI(O_FT,t2);WrI(O_STUN,t3);WrL(O_LAST,t4);WrF(O_MAXHP,t5);
            float skCv=GetHp();new SecureRandom().nextBytes(AkBuf);SetHp(skCv);
            TSeq[0]=new SecureRandom().nextLong();SetSHp(skCv);
            byte[]rk=new byte[8];new SecureRandom().nextBytes(rk);
            for(int i=0;i<8;i++){OpK[i]^=rk[i];OpX[i]^=rk[i];}
        }
        private void SyncHD(){Ent.entityData.set(SdaHR,GetHp()/RdF(O_MAXHP));}
        private void FireTkA(){PSU.getPlayersWithinRadius(SLvl,pos.x,pos.y,pos.z,1024.0).forEach(p->MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(Ent,p,2)));}
        private void FireTkB(){PSU.getPlayersWithinRadius(SLvl,pos.x,pos.y,pos.z,1024.0).forEach(p->MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(Ent,p,3)));}
        private void SnapPos(){HPos[(int)(SLvl.getGameTime()&31)]=pos;}
        private void VerCH(){
            if(ChkRefl()){discard();return;}
            if(!VerSH()){discard();return;}
            try{
                byte[][]cur={
                        HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$ML.class"),
                        HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$CU.class"),
                        HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$HRPkt.class"),
                        HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$ECPkt.class"),
                        HashCls("com/ExtraBossRush/GoM/Entity/GoMEntity$SPkt.class")
                };
                byte[][]base={HBas_ML2,HBas_CU2,HBas_HR2,HBas_EC2,HBas_SP2};
                int d=0;
                for(int j=0;j<cur.length;j++)for(int i=0;i<32;i++)d|=(cur[j][i]^base[j][i]);
                if(d!=0){discard();return;}
            }catch(Exception e){discard();}
        }
        private void UpdBB(){BEvt.setProgress(GetHp()/RdF(O_MAXHP));}
        private void TkStun(){int stunV2=RdI(O_STUN);if(stunV2>0){WrI(O_STUN,stunV2-1);if(stunV2==1)Ent.entityData.set(SdaHt,false);}}
        ML(String id,ServerLevel lv,Vec3 pos){
            MlId=id;SLvl=lv;this.pos=pos;
            BigInteger[]pk=GenPK(SRng);
            PN=pk[0];PN2=pk[1];PLam=pk[2];PMu=pk[3];
            BEvt=new ServerBossEvent(
                    Component.literal(new String(new byte[]{
                            (byte)0xE9,(byte)0xAD,(byte)0x94,(byte)0xE8,(byte)0xA1,(byte)0x93,
                            (byte)0xE3,(byte)0x81,(byte)0xAE,(byte)0xE5,(byte)0xAE,(byte)0x88,
                            (byte)0xE8,(byte)0xAD,(byte)0xB7,(byte)0xE8,(byte)0x80,(byte)0x85
                    },StandardCharsets.UTF_8)),
                    BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
            BEvt.setVisible(true);
            System.arraycopy(OpXor,0,OpK,0,8);System.arraycopy(OpKey,0,OpX,0,8);
            SRng.nextBytes(IvBuf);SRng.nextBytes(AkBuf);
            TSeq[0]=SRng.nextLong();
            WrI(O_TICK,0);WrI(O_RT,0);WrI(O_FT,0);WrI(O_STUN,0);WrL(O_LAST,0L);WrF(O_MAXHP,MaxHp);
            SetHp(MaxHp);
            HpPail=PailEnc(Math.round(MaxHp*1000L));
            HpMac=MkMac(Math.round(MaxHp*1000L));
            Arrays.fill(HPos,pos);
            Ent=new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(),lv);
            SetSHp(MaxHp);
            Ent.setPos(pos.x,pos.y,pos.z);Ent.EntId(id);
            Ent.entityData.set(SdaHR,1.0F);Ent.entityData.set(SdaHt,false);

        }
        String MlId(){return MlId;}
        ServerLevel getLevel(){return SLvl;}
        boolean IsDead(){return GetHp()<=0;}
        void TickLP(){
            if(GetHp()<=0)return;
            BEvt.setVisible(true);
            if((RdI(O_TICK)&0xFF)==0)DispOp(0);
            DispOp(2);
            float dmgRatio=1.0F-(GetHp()/RdF(O_MAXHP));
            int subTks=(int)Math.ceil(1.0F+dmgRatio*9.0F);
            for(int i=0;i<subTks;i++)TkInr();
            DispOp(5);DispOp(4);DispOp(6);
            Bcast();
        }
        private void TkInr(){
            int TkCnt=RdI(O_TICK);
            List<ServerPlayer>near=PSU.getPlayersWithinRadius(SLvl,pos.x,pos.y,pos.z,1024.0);
            if(near.isEmpty()&&!SLvl.players().isEmpty())
                pos=SLvl.players().get(Rng.nextInt(SLvl.players().size())).position().add(0,10,0);
            if(TkCnt-RdI(O_RT)>=100){WrI(O_RT,TkCnt);DispOp(7);}
            if(TkCnt-RdI(O_FT)>=600){WrI(O_FT,TkCnt);DispOp(1);}
            WrI(O_TICK,TkCnt+1);
        }
        void ApplyH(ServerPlayer sp){
            if(!HitLock.tryLock())return;
            try{
                long now=SLvl.getGameTime();
                if(now-RdL(O_LAST)<MinHitDly)return;WrL(O_LAST,now);
                float atkDmg=(float)sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float dmg=atkDmg*0.2F;
                int sharpLvl=net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,sp.getMainHandItem());
                if(sharpLvl>0)dmg+=(sharpLvl*0.5F+0.5F)*0.2F;
                dmg=Math.min(Math.max(dmg,0.0F),1000.0F);
                float nH=PolyCalc(GetHp(),dmg);
                SetHp(nH);SetSHp(nH);
                Ent.entityData.set(SdaHt,true);Ent.entityData.set(SdaHR,nH/RdF(O_MAXHP));
                if(nH<=0){
                    if(KillCnt<MaxKills)KillCnt++;
                    BEvt.setProgress(0.0F);BEvt.removeAllPlayers();HpXT.remove(Ent.getId());
                    Component deathMsg=Component.translatable("gomentity.boss.death");
                    if (enableSphereBreak){MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(Ent, sp, 4));}
                    for(ServerPlayer p:SLvl.players()){
                        p.sendSystemMessage(deathMsg);
                    }
                    return;
                }
                WrI(O_STUN,20);
            }finally{HitLock.unlock();}
        }
        private void Bcast(){
            Ent.setPos(pos.x,pos.y,pos.z);DispOp(3);
            ClientboundAddEntityPacket addPkt=new ClientboundAddEntityPacket(Ent);
            ClientboundSetEntityDataPacket dataPkt=new ClientboundSetEntityDataPacket(Ent.getId(),Ent.getEntityData().getNonDefaultValues());
            float hpRatio=GetHp()/RdF(O_MAXHP);boolean hurtF=Ent.getEntityData().get(SdaHt);
            for(ServerPlayer p:SLvl.players()){
                if(p.distanceToSqr(pos)<RangeSqr){
                    byte[]skAv2=SkA.get(p.getUUID()),skBv=SkB.get(p.getUUID()),Hash=SkC.get(p.getUUID());
                    AtomicLong seqCtr=CtrMap.get(p.getUUID());byte[]chkN=NcMap.get(p.getUUID());
                    if(skAv2!=null&&skBv!=null&&Hash!=null&&seqCtr!=null&&chkN!=null){
                        byte[]sk3=new byte[32];for(int i=0;i<32;i++)sk3[i]=(byte)(skAv2[i]^skBv[i]^Hash[i]);
                        long seq=seqCtr.getAndIncrement();
                        Chan.send(PacketDistributor.PLAYER.with(()->p),new ECPkt(ECPkt.build(sk3,Ent.getId(),MlId)));
                        p.connection.send(addPkt);p.connection.send(dataPkt);
                        Chan.send(PacketDistributor.PLAYER.with(()->p),new SPkt(SPkt.build(sk3,p.getUUID(),Ent.getId(),hpRatio,hurtF,pos.x,pos.y,pos.z,chkN,seq)));
                        BEvt.addPlayer(p);Arrays.fill(sk3,(byte)0);
                    }else{p.connection.send(addPkt);p.connection.send(dataPkt);BEvt.addPlayer(p);}
                }else{
                    p.connection.send(new ClientboundRemoveEntitiesPacket(Ent.getId()));BEvt.removePlayer(p);
                    byte[]skAv2=SkA.get(p.getUUID()),skBv=SkB.get(p.getUUID()),Hash=SkC.get(p.getUUID());
                    AtomicLong seqCtr=CtrMap.get(p.getUUID());
                    if(skAv2!=null&&skBv!=null&&Hash!=null&&seqCtr!=null){
                        byte[]sk3=new byte[32];for(int i=0;i<32;i++)sk3[i]=(byte)(skAv2[i]^skBv[i]^Hash[i]);
                        long seq=seqCtr.getAndIncrement();
                        Chan.send(PacketDistributor.PLAYER.with(()->p),new SPkt(SPkt.remove(sk3,p.getUUID(),Ent.getId(),seq)));
                        Arrays.fill(sk3,(byte)0);
                    }
                }
            }
            Ent.getEntityData().packDirty();
        }
        void discard(){
            BEvt.removeAllPlayers();int did=Ent.getId();HpXT.remove(did);
            Ent.DiscE();
            ClientboundRemoveEntitiesPacket rm=new ClientboundRemoveEntitiesPacket(did);
            for(ServerPlayer p:SLvl.players()){
                p.connection.send(rm);
                byte[]skAv2=SkA.get(p.getUUID()),skBv=SkB.get(p.getUUID()),Hash=SkC.get(p.getUUID());
                AtomicLong seqCtr=CtrMap.get(p.getUUID());
                if(skAv2!=null&&skBv!=null&&Hash!=null&&seqCtr!=null){
                    byte[]sk3=new byte[32];for(int i=0;i<32;i++)sk3[i]=(byte)(skAv2[i]^skBv[i]^Hash[i]);
                    long seq=seqCtr.getAndIncrement();
                    Chan.send(PacketDistributor.PLAYER.with(()->p),new ECPkt(ECPkt.revoke(sk3,did)));
                    Chan.send(PacketDistributor.PLAYER.with(()->p),new SPkt(SPkt.remove(sk3,p.getUUID(),did,seq)));
                    Arrays.fill(sk3,(byte)0);
                }
            }
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class RR{
        @SubscribeEvent
        public static void OnRegRend(EntityRenderersEvent.RegisterRenderers event){
            event.registerEntityRenderer(GoMEntities.MAGIC_GUARDIAN.get(),GR::new);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class GR extends MobRenderer<GoMEntity,HumanoidModel<GoMEntity>>{
        private static final ResourceLocation TexRL=new ResourceLocation(ExtraBossRush.MOD_ID,"textures/entity/magic_guardian.png");
        private static final Random RngR=new Random();
        public GR(EntityRendererProvider.Context ctx){
            super(ctx,new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)),0.6F);
        }
        @Override public @NotNull ResourceLocation getTextureLocation(@NotNull GoMEntity e){return TexRL;}
        @Override
        public void render(GoMEntity entity,float yaw,float pt,@NotNull PoseStack ps,@NotNull MultiBufferSource buf,int light){
            super.render(entity,yaw,pt,ps,buf,light);
            if(entity.IsHurt())RdL(entity,ps,buf,pt,light,1F,0F,0F,0.2F,true);
            float ef=entity.GetDR();float off=ef*0.04F-0.02F;
            if(off>0.001F){
                Random r=new Random((long)entity.getId()*98765L+entity.tickCount*12345L);
                RenderGlow(entity,ps,buf,pt,light,off,r,1F,0F,0F,ef*0.3F);
                RenderGlow(entity,ps,buf,pt,light,off,r,0F,1F,1F,ef*0.3F);
            }
        }
        private void RenderGlow(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float off,Random r,float rc,float gc,float bc,float a){
            ps.pushPose();float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1F,-1F,1F);ps.translate(0F,-1.501F,0F);
            ps.translate((RngR.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (RngR.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (RngR.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(TexRL)),light,OverlayTexture.NO_OVERLAY,rc,gc,bc,a);
            ps.popPose();
        }
        private void RdL(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float r,float g,float b,float a,boolean hurt){
            ps.pushPose();float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1.0002F,-1.0002F,1.0002F);ps.translate(0F,-1.501F,0F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(TexRL)),light,
                    OverlayTexture.pack(OverlayTexture.u(0F),OverlayTexture.v(hurt)),r,g,b,a);
            ps.popPose();
        }
    }
}