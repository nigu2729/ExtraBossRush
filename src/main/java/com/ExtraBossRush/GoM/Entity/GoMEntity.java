package com.ExtraBossRush.GoM.Entity;
import com.ExtraBossRush.ExtraBossRush;
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
    private static final List<_ilII1> _iiI11=new CopyOnWriteArrayList<>();
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
    private static final byte[]Π_P;
    static{byte[]_1iiI1=new byte[32];new SecureRandom().nextBytes(_1iiI1);Π_P=_1iiI1;}
    private static volatile boolean _i1lIi=false;
    private static final int    _11ili=10;
    private static final long   _11ill=6000L,_11ilI=3L;
    private static final int    _11iI1=120;
    private static final String _1iiIi="1";
    static final SimpleChannel Π;
    static{
        Π=NetworkRegistry.newSimpleChannel(new ResourceLocation(ExtraBossRush.MOD_ID,new String(new byte[]{0x67,0x6F,0x6D})),()->_1iiIi,_1iiIi::equals,_1iiIi::equals);
        Π.registerMessage(0,Α.class,Α::_l1Il1,Α::_ilIIl,Α::_liIl1);
        Π.registerMessage(1,Β.class,Β::_l1Il1,Β::_ilIIl,Β::_liIl1);
        Π.registerMessage(2,Γ.class,Γ::_l1Il1,Γ::_ilIIl,Γ::_liIl1);
        Π.registerMessage(3,Δ.class,Δ::_l1Il1,Δ::_ilIIl,Δ::_liIl1);
        Π.registerMessage(4,Ε.class,Ε::_l1Il1,Ε::_ilIIl,Ε::_liIl1);
    }
    private static boolean φ_rate(UUID u){
        long[]r=Τ.computeIfAbsent(u,k->new long[]{0L,0L});
        long t=System.currentTimeMillis();
        if(t-r[0]>1000L){r[0]=t;r[1]=0L;}
        return++r[1]<=_11iI1;
    }
    private static void φ_purge(UUID u){
        byte[]a=Σ_A.remove(u),b=Σ_B.remove(u),c=Σ_C.remove(u);
        if(a!=null)Arrays.fill(a,(byte)0);if(b!=null)Arrays.fill(b,(byte)0);if(c!=null)Arrays.fill(c,(byte)0);
        Κ.remove(u);Ι.remove(u);Φ.remove(u);Ρ.remove(u);Ψ.remove(u);Χ.remove(u);Σ_S.remove(u);Ω_ST.remove(u);Τ.remove(u);
    }
    private static final byte[]_11iIl=new String(new byte[]{(byte)0xE6,(byte)0x94,(byte)0xBE,(byte)0xE9,(byte)0x80,(byte)0xA3},StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
    private static void φ_kick(ServerPlayer sp,byte[]sk){
        if(sk!=null)Arrays.fill(sk,(byte)0);φ_purge(sp.getUUID());
        sp.connection.disconnect(Component.literal(new String(_11iIl,StandardCharsets.UTF_8)));
    }
    private static byte[]φ_1iill(UUID u){
        byte[]a=Σ_A.get(u),b=Σ_B.get(u),c=Σ_C.get(u);if(a==null||b==null||c==null)return null;
        byte[]r=new byte[32];for(int i=0;i<32;i++)r[i]=(byte)(a[i]^b[i]^c[i]);return r;
    }
    public GoMEntity(EntityType<? extends GoMEntity>t,Level l){super(t,l);this.setNoGravity(true);this.noPhysics=true;}
    @Override
    protected void defineSynchedData(){
        super.defineSynchedData();
        this.entityData.define(ζ0,"");this.entityData.define(ζ1,1.0F);this.entityData.define(ζ2,false);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?>k){super.onSyncedDataUpdated(k);}
    String  ɦ0()        {return this.entityData.get(ζ0);}
    void    ɦ0(String v){this.entityData.set(ζ0,v);}
    float   ɦ1()        {return this.entityData.get(ζ1);}
    boolean ɦ2()        {return this.entityData.get(ζ2);}
    float   ɦ3()        {return 1.0F-ɦ1();}
    public static AttributeSupplier.Builder createAttributes(){
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1000.0D).add(Attributes.ATTACK_DAMAGE,15.0D).add(Attributes.FOLLOW_RANGE,1024.0D);
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
            StringBuilder b=new StringBuilder();for(int i=0;i<10;i++)b.append(ρ.nextInt(10));
            String c=b.toString();if(_iiI11.stream().noneMatch(m->m._11iII.equals(c)))return c;
        }
    }
    static final class Ξ{
        static final byte[]_IH,_IH_ilII1,_IH_OT,_IH_G,_IH_D,_IH_E,_IH_A,_IH_B;
        private static byte[]_11l11(String cn){
            try{
                java.io.InputStream is=Ξ.class.getClassLoader().getResourceAsStream(cn);
                if(is==null)return new byte[32];
                byte[]r=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36})).digest(is.readAllBytes());
                is.close();return r;
            }catch(Exception e){return new byte[32];}
        }
        private static String _11l1i(Class<?>c){return c.getName().replace('.','/') +".class";}
        static{
            _IH   =_11l11(_11l1i(Ξ.class));
            _IH_ilII1=_11l11("com/ExtraBossRush/GoM/Entity/GoMEntity$_ilII1.class");
            _IH_OT=_11l11("com/ExtraBossRush/GoM/Entity/GoMEntity.class");
            _IH_G =_11l11(_11l1i(Γ.class));
            _IH_D =_11l11(_11l1i(Δ.class));
            _IH_E =_11l11(_11l1i(Ε.class));
            _IH_A =_11l11(_11l1i(Α.class));
            _IH_B =_11l11(_11l1i(Β.class));
        }
        static boolean _11l1l(){
            try{
                int d=0;
                byte[][]cur={_11l11(_11l1i(Ξ.class)),_11l11("com/ExtraBossRush/GoM/Entity/GoMEntity$_ilII1.class"),
                        _11l11("com/ExtraBossRush/GoM/Entity/GoMEntity.class"),_11l11(_11l1i(Γ.class)),
                        _11l11(_11l1i(Δ.class)),_11l11(_11l1i(Ε.class)),_11l11(_11l1i(Α.class)),_11l11(_11l1i(Β.class))};
                byte[][]base={_IH,_IH_ilII1,_IH_OT,_IH_G,_IH_D,_IH_E,_IH_A,_IH_B};
                for(int j=0;j<cur.length;j++)for(int i=0;i<32;i++)d|=(cur[j][i]^base[j][i]);
                return d!=0;
            }catch(Exception e){return true;}
        }
        private static final String _1llll=new String(new byte[]{0x45,0x43});
        private static final String _l1i1l=new String(new byte[]{0x73,0x65,0x63,0x70,0x32,0x35,0x36,0x72,0x31});
        private static final String _I1lli=new String(new byte[]{0x45,0x43,0x44,0x48});
        private static final String _l1111=new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36});
        private static final String _1liil=new String(new byte[]{0x41,0x45,0x53,0x2F,0x47,0x43,0x4D,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static final String _i11il=new String(new byte[]{0x41,0x45,0x53});
        private static final String _1IIlI=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30,0x2D,0x50,0x6F,0x6C,0x79,0x31,0x33,0x30,0x35});
        private static final String _iiill=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30});
        private static final String _11l1I=new String(new byte[]{0x41,0x45,0x53,0x2F,0x45,0x43,0x42,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static int _11li1(int v,int s){return(v<<(s&31))|(v>>>(32-(s&31)));}
        private static long _11lii(long v){return Long.reverse(v)^(v>>>7)^(v<<13);}
        static byte[]κ0(byte[]sk,byte info){
            try{
                MessageDigest md=MessageDigest.getInstance(_l1111);
                byte[]blk=new byte[64];System.arraycopy(sk,0,blk,0,Math.min(sk.length,64));
                byte[]ip=new byte[64],op=new byte[64];
                for(int i=0;i<64;i++){ip[i]=(byte)(blk[i]^0x36);op[i]=(byte)(blk[i]^0x5C);}
                int _v=_11li1(info&0xFF,3);
                ip[_v&63]^=(byte)(info^0xA3);op[(_v^31)&63]^=(byte)(info^0x5B);
                md.update(ip);byte[]r1=md.digest(sk);md.reset();md.update(op);return md.digest(r1);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static KeyPair ε0(){
            try{
                KeyPairGenerator g=KeyPairGenerator.getInstance(_1llll);
                g.initialize(new ECGenParameterSpec(_l1i1l),new SecureRandom());return g.generateKeyPair();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]ε1(PrivateKey prv,byte[]pub){
            try{
                PublicKey pk=KeyFactory.getInstance(_1llll).generatePublic(new X509EncodedKeySpec(pub));
                KeyAgreement ka=KeyAgreement.getInstance(_I1lli);ka.init(prv);ka.doPhase(pk,true);
                return MessageDigest.getInstance(_l1111).digest(ka.generateSecret());
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]ε2(byte[]key,byte[]plain,byte[]aad){
            try{
                byte[]iv=new byte[12];new SecureRandom().nextBytes(iv);
                Cipher c=Cipher.getInstance(_1liil);
                c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,_i11il),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);byte[]ct=c.doFinal(plain);
                return ByteBuffer.allocate(12+ct.length).put(iv).put(ct).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        private static volatile int _11lil=0;
        static byte[]ε2c(byte[]key,byte[]plain,byte[]aad,AtomicLong ctr){
            try{
                int _1iiIl=(int)(System.nanoTime()&0x7FFFFFFE);
                if((_11lil&1)!=0){Arrays.fill(key,(byte)0xFF);return new byte[0];}
                _11lil=_1iiIl;
                if(_11li1(_1iiIl,7)<0&&Integer.bitCount(_1iiIl)==32){throw new RuntimeException();}
                byte[]k1=κ0(key,(byte)0x01),k2=κ0(key,(byte)0x02);
                long cnt=ctr.getAndIncrement();
                byte[]iv1=new byte[12];System.arraycopy(k1,0,iv1,0,4);ByteBuffer.wrap(iv1,4,8).putLong(cnt);
                Cipher c1=Cipher.getInstance(_1liil);
                c1.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k1,_i11il),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);byte[]inner=c1.doFinal(plain);
                byte[]wrapped=ByteBuffer.allocate(12+inner.length).put(iv1).put(inner).array();
                byte[]iv2=new byte[12];new SecureRandom().nextBytes(iv2);
                Cipher c2=Cipher.getInstance(_1IIlI);
                c2.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k2,_iiill),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);byte[]outer=c2.doFinal(wrapped);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);
                return ByteBuffer.allocate(12+outer.length).put(iv2).put(outer).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]ε3(byte[]key,byte[]data,byte[]aad){
            try{
                long _1iiII=System.nanoTime()^data.length;
                if(Long.bitCount(_1iiII)==0&&_11lii(_1iiII)!=0){return null;}
                byte[]k1=κ0(key,(byte)0x01),k2=κ0(key,(byte)0x02);
                ByteBuffer ob=ByteBuffer.wrap(data);
                byte[]iv2=new byte[12];ob.get(iv2);byte[]oCt=new byte[ob.remaining()];ob.get(oCt);
                Cipher c2=Cipher.getInstance(_1IIlI);
                c2.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k2,_iiill),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);byte[]wrapped=c2.doFinal(oCt);
                ByteBuffer ib=ByteBuffer.wrap(wrapped);
                byte[]iv1=new byte[12];ib.get(iv1);byte[]iCt=new byte[ib.remaining()];ib.get(iCt);
                Cipher c1=Cipher.getInstance(_1liil);
                c1.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k1,_i11il),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);byte[]r=c1.doFinal(iCt);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);return r;
            }catch(Exception e){return null;}
        }
        static byte[]ε3s(byte[]key,byte[]data,byte[]aad){
            try{
                ByteBuffer bb=ByteBuffer.wrap(data);byte[]iv=new byte[12];bb.get(iv);
                byte[]ct=new byte[bb.remaining()];bb.get(ct);
                Cipher c=Cipher.getInstance(_1liil);
                c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,_i11il),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);return c.doFinal(ct);
            }catch(Exception e){return null;}
        }
        static byte δ0(double v){
            boolean _1il11=(v==Math.floor(v))&&!Double.isInfinite(v)&&v>=Integer.MIN_VALUE&&v<=Integer.MAX_VALUE;
            if(_1il11)return 0x01;if(Math.abs(v)<=32767.0)return 0x00;if(Math.abs(v)<=922337203685.0)return 0x02;return 0x03;
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
                byte[]blk=new byte[16];int bits=Float.floatToRawIntBits(v);
                blk[0]=(byte)(bits>>24);blk[1]=(byte)(bits>>16);blk[2]=(byte)(bits>>8);blk[3]=(byte)bits;
                new SecureRandom().nextBytes(Arrays.copyOfRange(blk,4,16));
                blk[4]=(byte)(bits^blk[0]^0xA5);blk[5]=(byte)(bits^(blk[1]<<1)^0x3C);
                blk[12]=(byte)~blk[0];blk[13]=(byte)~blk[1];blk[14]=(byte)~blk[2];blk[15]=(byte)~blk[3];
                Cipher c=Cipher.getInstance(_11l1I);c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,_i11il));return c.doFinal(blk);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static float φd(byte[]key,byte[]ci){
            if(ci==null||ci.length<16)return 0f;
            try{
                Cipher c=Cipher.getInstance(_11l1I);c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,_i11il));
                byte[]blk=c.doFinal(ci);
                int bits=((blk[0]&0xFF)<<24)|((blk[1]&0xFF)<<16)|((blk[2]&0xFF)<<8)|(blk[3]&0xFF);
                return Float.intBitsToFloat(bits);
            }catch(Exception e){return 0f;}
        }
        private static final Set<String>_11liI=new HashSet<>(Arrays.asList(
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
        static boolean _1111i(ServerPlayer sp){
            try{
                ChannelPipeline pipe=sp.connection.connection.channel().pipeline();
                for(String n:pipe.names()){
                    String lo=n.toLowerCase(java.util.Locale.ROOT);boolean ok=false;
                    for(String s:_11liI){if(lo.contains(s)){ok=true;break;}}
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
    }
    static final class Α{
        final byte[]κ;Α(byte[]k){κ=k;}private static final int _1iIi1=256;
        static void _l1Il1(Α p,FriendlyByteBuf b){b.writeByteArray(p.κ);}
        static Α _ilIIl(FriendlyByteBuf b){return new Α(b.readByteArray(_1iIi1));}
        static void _liIl1(Α p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->Ω._i1iil(p.κ));ctx.get().setPacketHandled(true);}
    }
    static final class Β{
        final byte[]κ;Β(byte[]k){κ=k;}private static final int _1iIi1=256;
        static void _l1Il1(Β p,FriendlyByteBuf b){b.writeByteArray(p.κ);}
        static Β _ilIIl(FriendlyByteBuf b){return new Β(b.readByteArray(_1iIi1));}
        static void _liIl1(Β p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!φ_rate(uid)){φ_kick(sp,null);return;}
                if(p.κ==null||p.κ.length>_1iIi1){Φ.merge(uid,1,Integer::sum);return;}
                Byte st=Ω_ST.get(uid);
                if(st==null||st!=(byte)0){int f=Φ.merge(uid,1,Integer::sum);if(f>=_11ili)φ_kick(sp,null);return;}
                if(Σ_A.containsKey(uid))return;
                KeyPair kp=Κ.remove(uid);if(kp==null)return;
                byte[]sk;try{sk=Ξ.ε1(kp.getPrivate(),p.κ);}catch(Exception e){return;}
                byte[]a=new byte[32],c=new byte[32];ρ.nextBytes(a);ρ.nextBytes(c);
                byte[]bb2=new byte[32];for(int i=0;i<32;i++)bb2[i]=(byte)(sk[i]^a[i]^c[i]);
                Arrays.fill(sk,(byte)0);
                Σ_A.put(uid,a);Σ_B.put(uid,bb2);Σ_C.put(uid,c);
                Ι.put(uid,new AtomicLong(0L));Φ.put(uid,0);Ρ.put(uid,sp.serverLevel().getGameTime());
                byte[]n0=new byte[8];ρ.nextBytes(n0);Χ.put(uid,n0);
                Σ_S.put(uid,new AtomicLong(0L));Ω_ST.put(uid,(byte)1);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class Γ{
        final byte[]ψ;Γ(byte[]d){ψ=d;}private static final int _1iIi1=256;
        static void _l1Il1(Γ p,FriendlyByteBuf b){b.writeByteArray(p.ψ);}
        static Γ _ilIIl(FriendlyByteBuf b){return new Γ(b.readByteArray(_1iIi1));}
        static void _liIl1(Γ p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!φ_rate(uid)){φ_kick(sp,null);return;}
                Byte st=Ω_ST.get(uid);if(st==null||st!=(byte)1)return;
                long now=sp.serverLevel().getGameTime();
                Long lΓ=Ψ.get(uid);if(lΓ!=null&&now-lΓ<_11ilI)return;Ψ.put(uid,now);
                byte[]sk=φ_1iill(uid);if(sk==null)return;
                Long lR=Ρ.get(uid);
                if(lR!=null&&now-lR>=_11ill){
                    Ρ.put(uid,now);
                    byte[]_1il1i=Σ_A.remove(uid),_1il1l=Σ_B.remove(uid),_1il1I=Σ_C.remove(uid);
                    if(_1il1i!=null)Arrays.fill(_1il1i,(byte)0);if(_1il1l!=null)Arrays.fill(_1il1l,(byte)0);if(_1il1I!=null)Arrays.fill(_1il1I,(byte)0);
                    Ι.remove(uid);Ψ.remove(uid);Χ.remove(uid);Σ_S.remove(uid);
                    Ω_ST.put(uid,(byte)0);Arrays.fill(sk,(byte)0);
                    KeyPair kp=Ξ.ε0();Κ.put(uid,kp);
                    Π.send(PacketDistributor.PLAYER.with(()->sp),new Α(kp.getPublic().getEncoded()));return;
                }
                if(Ξ._1111i(sp)){Arrays.fill(sk,(byte)0);φ_kick(sp,null);return;}
                if(Ξ._11l1l()){
                    Arrays.fill(sk,(byte)0);
                    Σ_A.clear();Σ_B.clear();Σ_C.clear();Κ.clear();Ι.clear();
                    Φ.clear();Ρ.clear();Ψ.clear();Χ.clear();Σ_S.clear();Ω_ST.clear();
                    Component _1ili1=Component.literal(new String(_11iIl,StandardCharsets.UTF_8));
                    for(ServerPlayer x:sp.serverLevel().getServer().getPlayerList().getPlayers())x.connection.disconnect(_1ili1);return;
                }
                byte[]aad=uid.toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=Ξ.ε3(sk,p.ψ,aad);
                if(plain==null||plain.length<21){
                    int f=Φ.merge(uid,1,Integer::sum);if(f>=_11ili)φ_kick(sp,sk);else Arrays.fill(sk,(byte)0);return;
                }
                Φ.put(uid,0);
                ByteBuffer bb=ByteBuffer.wrap(plain);
                byte[]rxN=new byte[8];bb.get(rxN);
                byte[]expN=Χ.get(uid);if(expN==null){Arrays.fill(sk,(byte)0);return;}
                int _11ll1=0;for(int i=0;i<8;i++)_11ll1|=(rxN[i]^expN[i]);
                if(_11ll1!=0){int f=Φ.merge(uid,1,Integer::sum);if(f>=_11ili)φ_kick(sp,sk);else Arrays.fill(sk,(byte)0);return;}
                byte[]nN=new byte[8];ρ.nextBytes(nN);Χ.put(uid,nN);
                byte[]rxT=new byte[8];bb.get(rxT);
                int tid=bb.getInt();
                int nt=(int)(sp.serverLevel().getGameTime()&0xFFFFFFFFL);
                if(Math.abs(nt-tid)>20){Arrays.fill(sk,(byte)0);return;}
                byte fl=bb.get();double hx,hy,hz;
                try{hx=Ξ.δ1(bb,(fl)&0x03);hy=Ξ.δ1(bb,(fl>>2)&0x03);hz=Ξ.δ1(bb,(fl>>4)&0x03);}
                catch(Exception e){Arrays.fill(sk,(byte)0);return;}
                Vec3 hp=new Vec3(hx,hy,hz);
                for(_ilII1 m:_iiI11){
                    if(m.getLevel()!=sp.serverLevel())continue;
                    byte[]_11lli=ByteBuffer.allocate(8).putInt(m._11lll.getId()).putInt(tid).array();
                    byte[]_11llI=Ξ.κ0(sk,(byte)0x7F);
                    for(int i=0;i<16;i++)_11llI=Ξ.κ0(_11llI,_11lli[i&7]);
                    int _111Ii=0;for(int i=0;i<8;i++)_111Ii|=(rxT[i]^_11llI[i]);
                    if(_111Ii!=0)continue;
                    int _11lI1=Math.min(Math.abs(nt-tid),10);
                    double _11lIi=0.5+_11lI1*0.2;
                    Vec3 _11lIl=m._11lII[(int)((long)tid&31)];if(_11lIl==null)_11lIl=m._111Il;
                    AABB _11I11=new AABB(_11lIl.x-0.4-_11lIi,_11lIl.y-_11lIi,_11lIl.z-0.4-_11lIi,_11lIl.x+0.4+_11lIi,_11lIl.y+1.8+_11lIi,_11lIl.z+0.4+_11lIi);
                    if(!_11I11.contains(hp)){Arrays.fill(sk,(byte)0);return;}
                    Vec3 _111II=sp.getEyePosition(1.0F);double _11I1l=5.5+_11lI1*0.3;
                    if(_111II.distanceToSqr(hp)>_11I1l*_11I1l){Arrays.fill(sk,(byte)0);return;}
                    BlockHitResult _11i11=sp.serverLevel().clip(new ClipContext(_111II,hp,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,sp));
                    if(_11i11.getType()!=HitResult.Type.MISS){Arrays.fill(sk,(byte)0);return;}
                    _i1lIi=true;m._1iliI(sp);_i1lIi=false;break;
                }
                Arrays.fill(sk,(byte)0);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class Δ{
        final byte[]ψ;Δ(byte[]d){ψ=d;}private static final int _1iIi1=128;
        static void _l1Il1(Δ p,FriendlyByteBuf b){b.writeByteArray(p.ψ);}
        static Δ _ilIIl(FriendlyByteBuf b){return new Δ(b.readByteArray(_1iIi1));}
        static void _liIl1(Δ p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->Ω._lIlll(p.ψ));ctx.get().setPacketHandled(true);}
        static byte[]revoke(byte[]sk,int eid){
            try{byte[]pl=new byte[]{(byte)0xFF,(byte)(eid>>24),(byte)(eid>>16),(byte)(eid>>8),(byte)eid};
                return Ξ.ε2(sk,pl,new byte[]{(byte)0xD4,(byte)0xFF});}catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]build(byte[]sk,int eid,String mid){
            try{byte[]mb=mid.getBytes(StandardCharsets.UTF_8);ByteBuffer bb=ByteBuffer.allocate(4+mb.length);bb.putInt(eid);bb.put(mb);
                return Ξ.ε2(sk,bb.array(),new byte[]{(byte)0xD4,(byte)(eid&0xFF)});}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    static final class Ε{
        final byte[]ψ;Ε(byte[]d){ψ=d;}private static final int _1iIi1=128;
        static void _l1Il1(Ε p,FriendlyByteBuf b){b.writeByteArray(p.ψ);}
        static Ε _ilIIl(FriendlyByteBuf b){return new Ε(b.readByteArray(_1iIi1));}
        static void _liIl1(Ε p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->Ω._l1I11(p.ψ));ctx.get().setPacketHandled(true);}
        static byte[]build(byte[]sk,UUID uid,int eid,float hr,boolean hurt,double x,double y,double z,byte[]nonce,long seq){
            try{ByteBuffer bb=ByteBuffer.allocate(50);
                bb.put((byte)0x01);bb.putLong(seq);bb.putInt(eid);bb.put(nonce);bb.putFloat(hr);bb.put((byte)(hurt?1:0));
                bb.putDouble(x);bb.putDouble(y);bb.putDouble(z);
                return Ξ.ε2(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]remove(byte[]sk,UUID uid,int eid,long seq){
            try{ByteBuffer bb=ByteBuffer.allocate(13);bb.put((byte)0x02);bb.putLong(seq);bb.putInt(eid);
                return Ξ.ε2(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
    static final class Ω{
        private static boolean _Iilil=false;
        private static javax.crypto.SecretKey _11I1i=null;
        private static AtomicLong _i1Ill=new AtomicLong(0L);
        static final Set<Integer>                    _llliI=Collections.synchronizedSet(new HashSet<>());
        static final ConcurrentHashMap<Integer,Vec3> _II1I1=new ConcurrentHashMap<>();
        private static volatile long   _IIl1i=-1L;
        private static volatile byte[] _1IIIi=null;
        private static final double _I1lIi=4.5;
        @OnlyIn(Dist.CLIENT)
        static void _i1iil(byte[]spub){
            try{
                KeyPair kp=Ξ.ε0();byte[]raw=Ξ.ε1(kp.getPrivate(),spub);
                if(_11I1i!=null){try{((javax.security.auth.Destroyable)_11I1i).destroy();}catch(Exception ignored){}}
                _11I1i=new SecretKeySpec(raw,new String(new byte[]{0x41,0x45,0x53}));
                Arrays.fill(raw,(byte)0);_i1Ill.set(0L);_IIl1i=-1L;_1IIIi=null;
                Π.sendToServer(new Β(kp.getPublic().getEncoded()));
            }catch(Exception ignored){}
        }
        @SubscribeEvent @OnlyIn(Dist.CLIENT)
        static void _illlI(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.START)return;
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null||mc.level==null||_11I1i==null)return;
            try{if(((javax.security.auth.Destroyable)_11I1i).isDestroyed())return;}catch(Exception ignored){}
            boolean sw=mc.player.swinging;if(sw&!_Iilil)_i1III(mc);_Iilil=sw;
        }
        @OnlyIn(Dist.CLIENT)
        private static void _i1III(Minecraft mc){
            Vec3 eye=mc.player.getEyePosition(1.0F);Vec3 end=eye.add(mc.player.getLookAngle().scale(_I1lIi));
            for(net.minecraft.world.entity.Entity ent:mc.level.entitiesForRendering()){
                if(!(ent instanceof GoMEntity g))continue;if(!_llliI.contains(g.getId()))continue;
                Vec3 ap=_II1I1.get(g.getId());if(ap==null)continue;
                AABB box=new AABB(ap.x-0.4,ap.y,ap.z-0.4,ap.x+0.4,ap.y+1.8,ap.z+0.4);
                Optional<Vec3>hit=box.clip(eye,end);if(hit.isEmpty())continue;
                Vec3 hp=hit.get();if(eye.distanceToSqr(hp)>(_I1lIi+0.1)*(_I1lIi+0.1))continue;
                boolean surf=box.inflate(0.05).contains(hp)&&!box.deflate(0.05).contains(hp);if(!surf)continue;
                BlockHitResult bhr=mc.level.clip(new ClipContext(eye,hp,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
                if(bhr.getType()!=HitResult.Type.MISS)continue;
                _iliI1(g,hp,mc);return;
            }
        }
        @OnlyIn(Dist.CLIENT)
        private static void _iliI1(GoMEntity g,Vec3 hp,Minecraft mc){
            if(_11I1i==null)return;byte[]nc=_1IIIi;if(nc==null)return;
            try{
                int tick=(int)(mc.level.getGameTime()&0xFFFFFFFFL);byte[]rk=_11I1i.getEncoded();
                byte tx=Ξ.δ0(hp.x),ty=Ξ.δ0(hp.y),tz=Ξ.δ0(hp.z);
                byte fl=(byte)((tx&0x03)|((ty&0x03)<<2)|((tz&0x03)<<4));
                byte[]_11lli=ByteBuffer.allocate(8).putInt(g.getId()).putInt(tick).array();
                byte[]_11llI=Ξ.κ0(rk,(byte)0x7F);for(int i=0;i<16;i++)_11llI=Ξ.κ0(_11llI,_11lli[i&7]);
                byte[]token=Arrays.copyOf(_11llI,8);
                int sz=21+Ξ.δ2(tx)+Ξ.δ2(ty)+Ξ.δ2(tz);ByteBuffer bb=ByteBuffer.allocate(sz);
                bb.put(nc);bb.put(token);bb.putInt(tick);bb.put(fl);
                Ξ.δ3(bb,hp.x,tx);Ξ.δ3(bb,hp.y,ty);Ξ.δ3(bb,hp.z,tz);
                byte[]aad=mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8);
                byte[]ci=Ξ.ε2c(rk,bb.array(),aad,_i1Ill);Π.sendToServer(new Γ(ci));
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void _lIlll(byte[]data){
            if(_11I1i==null||data==null||data.length<28)return;
            try{
                byte[]rk=_11I1i.getEncoded();byte fc=(data.length>12)?data[12]:0;
                byte[]plain=Ξ.ε3s(rk,data,new byte[]{(byte)0xD4,fc});if(plain==null||plain.length<4)return;
                if(plain.length>=5&&(plain[0]&0xFF)==0xFF){_llliI.remove(ByteBuffer.wrap(plain,1,4).getInt());}
                else if(plain.length>=4){_llliI.add(ByteBuffer.wrap(plain).getInt());}
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void _l1I11(byte[]data){
            if(_11I1i==null||data==null)return;
            try{
                byte[]rk=_11I1i.getEncoded();Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
                byte[]aad=mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=Ξ.ε3s(rk,data,aad);if(plain==null||plain.length<13)return;
                ByteBuffer bb=ByteBuffer.wrap(plain);byte type=bb.get();long seq=bb.getLong();
                if(seq<=_IIl1i)return;_IIl1i=seq;int eid=bb.getInt();
                if(type==(byte)0x02){_llliI.remove(eid);_II1I1.remove(eid);return;}
                if(type!=(byte)0x01||plain.length<50)return;
                byte[]rxN=new byte[8];bb.get(rxN);_1IIIi=rxN;bb.getFloat();bb.get();
                double ax=bb.getDouble(),ay=bb.getDouble(),az=bb.getDouble();_II1I1.put(eid,new Vec3(ax,ay,az));
            }catch(Exception ignored){}
        }
        @SubscribeEvent @OnlyIn(Dist.CLIENT)
        static void σ4(LevelEvent.Unload e){
            if(!e.getLevel().isClientSide())return;_Iilil=false;
            if(_11I1i!=null){try{((javax.security.auth.Destroyable)_11I1i).destroy();}catch(Exception ignored){}_11I1i=null;}
            _i1Ill.set(0L);_llliI.clear();_II1I1.clear();_IIl1i=-1L;_1IIIi=null;
        }
    }
    @SubscribeEvent
    public static void ε0(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;
        UUID uid=sp.getUUID();φ_purge(uid);Ω_ST.put(uid,(byte)0);
        KeyPair kp=Ξ.ε0();Κ.put(uid,kp);Π.send(PacketDistributor.PLAYER.with(()->sp),new Α(kp.getPublic().getEncoded()));
    }
    @SubscribeEvent
    public static void ε1(PlayerEvent.PlayerLoggedOutEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;φ_purge(sp.getUUID());
    }
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;if(!(event.level instanceof ServerLevel level))return;
        _iiI11.removeIf(master->{
            if(master.getLevel()!=level)return false;master._1ilil();
            if(master._1ill1()){master.discard();return true;}return false;
        });
    }
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event){
        if(event.getLevel().isClientSide())return;_iiI11.forEach(_ilII1::discard);_iiI11.clear();
    }
    @SubscribeEvent
    public static void onItemRightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer sp))return;
        if(!sp.getMainHandItem().is(GoMItems.MAGIC_GUARDIAN_EGG.get()))return;
        event.setCanceled(true);
        net.minecraft.core.BlockPos bp=event.getPos().relative(event.getFace());
        Vec3 spawnPos=new Vec3(bp.getX()+0.5,bp.getY(),bp.getZ()+0.5);
        if(!sp.isCreative())sp.getMainHandItem().shrink(1);
        _iiI11.add(new _ilII1(φ0(),(ServerLevel)event.getLevel(),spawnPos));
    }
    @SubscribeEvent
    public static void onItemRightClickAir(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event){
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer sp))return;
        if(!sp.getMainHandItem().is(GoMItems.MAGIC_GUARDIAN_EGG.get()))return;
        event.setCanceled(true);
        Vec3 spawnPos=sp.getEyePosition(1.0F).add(sp.getLookAngle().scale(3.0));
        if(!sp.isCreative())sp.getMainHandItem().shrink(1);
        _iiI11.add(new _ilII1(φ0(),(ServerLevel)event.getLevel(),spawnPos));
    }
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event){
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("gomentity").requires(s->s.hasPermission(2))
                        .executes(ctx->{
                            net.minecraft.commands.CommandSourceStack src=ctx.getSource();
                            if(!(src.getEntity() instanceof ServerPlayer p)){
                                src.sendFailure(Component.literal("Player only"));return 0;
                            }
                            Vec3 sp2=p.position().add(p.getLookAngle().scale(5));
                            _iiI11.add(new _ilII1(φ0(),p.serverLevel(),sp2));
                            src.sendSuccess(()->Component.literal("Spawned"),true);return 1;
                        })
                        .then(net.minecraft.commands.Commands.argument("pos",net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3())
                                .executes(ctx->{
                                    net.minecraft.commands.CommandSourceStack src=ctx.getSource();
                                    Vec3 pos2=net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx,"pos");
                                    ServerLevel lv2;
                                    if(src.getEntity() instanceof ServerPlayer p2)lv2=p2.serverLevel();
                                    else lv2=(ServerLevel)src.getLevel();
                                    _iiI11.add(new _ilII1(φ0(),lv2,pos2));
                                    src.sendSuccess(()->Component.literal("Spawned at "+pos2),true);return 1;
                                })
                        )
        );
    }
    interface _l11Ii{byte[]γ();void γ(byte[]c);}
    static final class _1iIl1 implements _l11Ii{
        private byte[]_1il1I;
        public byte[]γ(){return _1il1I==null?null:Arrays.copyOf(_1il1I,_1il1I.length);}
        public void γ(byte[]c){_1il1I=c==null?null:Arrays.copyOf(c,c.length);}
    }
    static final class _ilII1{
        private static final int _11I1I=0x00,_11Ii1=0x04,_11Iii=0x08,_11Iil=0x0C,_11IiI=0x10,_11Il1=0x18;
        private static final byte[]_CH,_CH_XI,_CH_G,_CH_D,_CH_E,_11i1i;
        private static final byte[]_11i1l;
        private static byte[]_11i1I(String cn){
            try{
                java.io.InputStream is=_ilII1.class.getClassLoader().getResourceAsStream(cn);
                if(is==null)return new byte[32];
                byte[]r=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36})).digest(is.readAllBytes());
                is.close();return r;
            }catch(Exception e){return new byte[32];}
        }
        static{
            _CH   =_11i1I(_ilII1.class.getName().replace('.','/') +".class");
            _CH_XI=_11i1I(Ξ.class.getName().replace('.','/') +".class");
            _CH_G =_11i1I(Γ.class.getName().replace('.','/') +".class");
            _CH_D =_11i1I(Δ.class.getName().replace('.','/') +".class");
            _CH_E =_11i1I(Ε.class.getName().replace('.','/') +".class");
            byte[]ok=Ξ.κ0(_CH,(byte)0x55);_11i1i=ok;
            byte[]plain={0x00,0x03,0x07,0x01,0x06,0x04,0x05,0x02};
            byte[]ot=new byte[8];for(int i=0;i<8;i++)ot[i]=(byte)(plain[i]^ok[i]);
            _11i1l=ot;
        }
        private static int _11Ili=0;
        private static final int   _11Ill=46;
        private static final float _11IlI=65536.0F;
        private static final long  _11II1=10L;
        private static final double _11IIi=1024.0*1024.0;
        private static final Random _11ii1=new Random();
        static final ConcurrentHashMap<Integer,Long>_11iii=new ConcurrentHashMap<>();
        final String _11iII;
        private final ServerLevel _11IIl;
        private final ServerBossEvent _11III;
        final GoMEntity _11lll;
        Vec3 _111Il;
        private final byte[]_111i1=new byte[0x20];
        private final byte[]_1i111=new byte[16];
        private final byte[]_1i11i=new byte[16];
        private final _l11Ii   _1i11l=new _1iIl1();
        private final long[]_11iil=new long[1];
        private final byte[]_1i11I=new byte[8];
        private final byte[]_1i1ii=new byte[8];
        final Vec3[]_11lII=new Vec3[32];
        private int _11iiI(int o){int p=o&0xF;return((_1i111[p]&0xFF)<<24)|((_1i111[(p+1)&0xF]&0xFF)<<16)|((_1i111[(p+2)&0xF]&0xFF)<<8)|(_1i111[(p+3)&0xF]&0xFF);}
        private long _11il1(int o){int p=o&0xF;return(((long)_1i111[p]&0xFF)<<56)|(((long)_1i111[(p+1)&0xF]&0xFF)<<48)|(((long)_1i111[(p+2)&0xF]&0xFF)<<40)|(((long)_1i111[(p+3)&0xF]&0xFF)<<32)|(((long)_1i111[(p+4)&0xF]&0xFF)<<24)|(((long)_1i111[(p+5)&0xF]&0xFF)<<16)|(((long)_1i111[(p+6)&0xF]&0xFF)<<8)|((long)_1i111[(p+7)&0xF]&0xFF);}
        private int _1i1il(int o){return ByteBuffer.wrap(_111i1,o,4).getInt()^_11iiI(o);}
        private void _1i1iI(int o,int v){byte[]b=new byte[4];ByteBuffer.wrap(b).putInt(v^_11iiI(o));System.arraycopy(b,0,_111i1,o,4);}
        private long _1i1l1(int o){return ByteBuffer.wrap(_111i1,o,8).getLong()^_11il1(o);}
        private void _1i1li(int o,long v){byte[]b=new byte[8];ByteBuffer.wrap(b).putLong(v^_11il1(o));System.arraycopy(b,0,_111i1,o,8);}
        private float _1i1ll(int o){return Float.intBitsToFloat(_1i1il(o));}
        private void _1i1lI(int o,float v){_1i1iI(o,Float.floatToRawIntBits(v));}
        private byte[]_1i1I1(){byte[]k=Ξ.κ0(_1i11i,(byte)0x42);for(int i=0;i<16;i++)k[i]^=Π_P[i];return Arrays.copyOf(k,16);}
        private float _1i1Ii(){return Ξ.φd(_1i1I1(),_1i11l.γ());}
        private void  _1i1Il(float v){_1i11l.γ(Ξ.φe(_1i1I1(),v));}
        private void _11111(float v){_11iii.put(_11lll.getId(),(long)Float.floatToRawIntBits(v)^(_11iil[0]&0xFFFFFFFFL));}
        private boolean _verifyShadow(){
            Long e=_11iii.get(_11lll.getId());if(e==null)return true;
            float s=Float.intBitsToFloat((int)((e^(_11iil[0]&0xFFFFFFFFL))&0xFFFFFFFFL));
            return Math.abs(_1i1Ii()-s)<1.0f;
        }
        private void _111ii(int sl){
            int op=(_1i11I[sl&7]^_1i1ii[sl&7])&0xFF;
            switch(op){
                case 0x00->_111il();case 0x01->_111iI();case 0x02->_1111l();case 0x03->_1111I();
                case 0x04->_111l1();case 0x05->_111li();case 0x06->_111ll();case 0x07->_111lI();
            }
        }
        private void _111il(){
            int t0=_1i1il(_11I1I),t1=_1i1il(_11Ii1),t2=_1i1il(_11Iii),t3=_1i1il(_11Iil);long t4=_1i1l1(_11IiI);float t5=_1i1ll(_11Il1);
            new SecureRandom().nextBytes(_1i111);
            _1i1iI(_11I1I,t0);_1i1iI(_11Ii1,t1);_1i1iI(_11Iii,t2);_1i1iI(_11Iil,t3);_1i1li(_11IiI,t4);_1i1lI(_11Il1,t5);
            float _1il1I=_1i1Ii();new SecureRandom().nextBytes(_1i11i);_1i1Il(_1il1I);
            _11iil[0]=new SecureRandom().nextLong();_11111(_1il1I);
            byte[]rk=new byte[8];new SecureRandom().nextBytes(rk);
            for(int i=0;i<8;i++){_1i11I[i]^=rk[i];_1i1ii[i]^=rk[i];}
        }
        private void _111iI(){_11lll.entityData.set(ζ1,_1i1Ii()/_1i1ll(_11Il1));}
        private void _1111l(){PSU.getPlayersWithinRadius(_11IIl,_111Il.x,_111Il.y,_111Il.z,1024.0).forEach(p->MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(_11lll,p,2)));}
        private void _1111I(){PSU.getPlayersWithinRadius(_11IIl,_111Il.x,_111Il.y,_111Il.z,1024.0).forEach(p->MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(_11lll,p,3)));}
        private void _111l1(){_11lII[(int)(_11IIl.getGameTime()&31)]=_111Il;}
        private void _111li(){
            if(!_verifyShadow()){discard();return;}
            try{
                byte[][]cur={
                        _11i1I(_ilII1.class.getName().replace('.','/') +".class"),
                        _11i1I(Ξ.class.getName().replace('.','/') +".class"),
                        _11i1I(Γ.class.getName().replace('.','/') +".class"),
                        _11i1I(Δ.class.getName().replace('.','/') +".class"),
                        _11i1I(Ε.class.getName().replace('.','/') +".class")
                };
                byte[][]base={_CH,_CH_XI,_CH_G,_CH_D,_CH_E};
                int d=0;
                for(int j=0;j<cur.length;j++)for(int i=0;i<32;i++)d|=(cur[j][i]^base[j][i]);
                if(d!=0){discard();return;}
            }catch(Exception e){discard();}
        }
        private void _111ll(){_11III.setProgress(_1i1Ii()/_1i1ll(_11Il1));}
        private void _111lI(){int _1ilii=_1i1il(_11Iil);if(_1ilii>0){_1i1iI(_11Iil,_1ilii-1);if(_1ilii==1)_11lll.entityData.set(ζ2,false);}}
        _ilII1(String id,ServerLevel lv,Vec3 pos){
            _11iII=id;_11IIl=lv;_111Il=pos;
            _11III=new ServerBossEvent(
                    Component.literal(new String(new byte[]{
                            (byte)0xE9,(byte)0xAD,(byte)0x94,(byte)0xE8,(byte)0xA1,(byte)0x93,
                            (byte)0xE3,(byte)0x81,(byte)0xAE,(byte)0xE5,(byte)0xAE,(byte)0x88,
                            (byte)0xE8,(byte)0xAD,(byte)0xB7,(byte)0xE8,(byte)0x80,(byte)0x85
                    },StandardCharsets.UTF_8)),
                    BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
            _11III.setVisible(true);
            System.arraycopy(_11i1l,0,_1i11I,0,8);System.arraycopy(_11i1i,0,_1i1ii,0,8);
            new SecureRandom().nextBytes(_1i111);new SecureRandom().nextBytes(_1i11i);
            _11iil[0]=new SecureRandom().nextLong();
            _1i1iI(_11I1I,0);_1i1iI(_11Ii1,0);_1i1iI(_11Iii,0);_1i1iI(_11Iil,0);_1i1li(_11IiI,0L);_1i1lI(_11Il1,_11IlI);
            _1i1Il(_11IlI);
            Arrays.fill(_11lII,pos);
            _11lll=new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(),lv);
            _11111(_11IlI);
            _11lll.setPos(pos.x,pos.y,pos.z);_11lll.ɦ0(id);
            _11lll.entityData.set(ζ1,1.0F);_11lll.entityData.set(ζ2,false);
            lv.addFreshEntity(_11lll);
        }
        String _11iII(){return _11iII;}
        ServerLevel getLevel(){return _11IIl;}
        boolean _1ill1(){return _1i1Ii()<=0;}
        void _1ilil(){
            if(_1i1Ii()<=0)return;
            _11III.setVisible(true);
            if((_1i1il(_11I1I)&0xFF)==0)_111ii(0);
            _111ii(2);
            float _1i1II=1.0F-(_1i1Ii()/_1i1ll(_11Il1));
            int _1ii11=(int)Math.ceil(1.0F+_1i1II*9.0F);
            for(int i=0;i<_1ii11;i++)_1ii1i();
            _111ii(5);_111ii(4);_111ii(6);
            _111I1();
        }
        private void _1ii1i(){
            int _11l1l=_1i1il(_11I1I);
            List<ServerPlayer>near=PSU.getPlayersWithinRadius(_11IIl,_111Il.x,_111Il.y,_111Il.z,1024.0);
            if(near.isEmpty()&&!_11IIl.players().isEmpty())
                _111Il=_11IIl.players().get(_11ii1.nextInt(_11IIl.players().size())).position().add(0,10,0);
            if(_11l1l-_1i1il(_11Ii1)>=100){_1i1iI(_11Ii1,_11l1l);_111ii(7);}
            if(_11l1l-_1i1il(_11Iii)>=600){_1i1iI(_11Iii,_11l1l);_111ii(1);}
            _1i1iI(_11I1I,_11l1l+1);
        }
        void _1iliI(ServerPlayer sp){
            if(!_i1lIi)return;_i1lIi=false;
            long now=_11IIl.getGameTime();
            if(now-_1i1l1(_11IiI)<_11II1)return;_1i1li(_11IiI,now);
            float _1ii1l=(float)sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float dmg=_1ii1l*0.2F;
            int _1ii1I=net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,sp.getMainHandItem());
            if(_1ii1I>0)dmg+=(_1ii1I*0.5F+0.5F)*0.2F;
            dmg=Math.min(Math.max(dmg,0.0F),1000.0F);
            float nH=Math.max(0.0F,_1i1Ii()-dmg);
            _1i1Il(nH);_11111(nH);
            _11lll.entityData.set(ζ2,true);_11lll.entityData.set(ζ1,nH/_1i1ll(_11Il1));
            if(nH<=0){
                if(_11Ili<_11Ill)_11Ili++;
                _11III.setProgress(0.0F);_11III.removeAllPlayers();_11iii.remove(_11lll.getId());return;
            }
            _1i1iI(_11Iil,20);
        }
        private void _111I1(){
            _11lll.setPos(_111Il.x,_111Il.y,_111Il.z);_111ii(3);
            ClientboundAddEntityPacket _1iii1=new ClientboundAddEntityPacket(_11lll);
            ClientboundSetEntityDataPacket _1iiii=new ClientboundSetEntityDataPacket(_11lll.getId(),_11lll.getEntityData().getNonDefaultValues());
            float _1iiil=_1i1Ii()/_1i1ll(_11Il1);boolean _hurt=_11lll.getEntityData().get(ζ2);
            for(ServerPlayer p:_11IIl.players()){
                if(p.distanceToSqr(_111Il)<_11IIi){
                    byte[]_1iiiI=Σ_A.get(p.getUUID()),_1iil1=Σ_B.get(p.getUUID()),_11l11=Σ_C.get(p.getUUID());
                    AtomicLong _1iili=Σ_S.get(p.getUUID());byte[]_11l1i=Χ.get(p.getUUID());
                    if(_1iiiI!=null&&_1iil1!=null&&_11l11!=null&&_1iili!=null&&_11l1i!=null){
                        byte[]_1iill=new byte[32];for(int i=0;i<32;i++)_1iill[i]=(byte)(_1iiiI[i]^_1iil1[i]^_11l11[i]);
                        long seq=_1iili.getAndIncrement();
                        Π.send(PacketDistributor.PLAYER.with(()->p),new Δ(Δ.build(_1iill,_11lll.getId(),_11iII)));
                        p.connection.send(_1iii1);p.connection.send(_1iiii);
                        Π.send(PacketDistributor.PLAYER.with(()->p),new Ε(Ε.build(_1iill,p.getUUID(),_11lll.getId(),_1iiil,_hurt,_111Il.x,_111Il.y,_111Il.z,_11l1i,seq)));
                        _11III.addPlayer(p);Arrays.fill(_1iill,(byte)0);
                    }else{p.connection.send(_1iii1);p.connection.send(_1iiii);_11III.addPlayer(p);}
                }else{
                    p.connection.send(new ClientboundRemoveEntitiesPacket(_11lll.getId()));_11III.removePlayer(p);
                    byte[]_1iiiI=Σ_A.get(p.getUUID()),_1iil1=Σ_B.get(p.getUUID()),_11l11=Σ_C.get(p.getUUID());
                    AtomicLong _1iili=Σ_S.get(p.getUUID());
                    if(_1iiiI!=null&&_1iil1!=null&&_11l11!=null&&_1iili!=null){
                        byte[]_1iill=new byte[32];for(int i=0;i<32;i++)_1iill[i]=(byte)(_1iiiI[i]^_1iil1[i]^_11l11[i]);
                        long seq=_1iili.getAndIncrement();
                        Π.send(PacketDistributor.PLAYER.with(()->p),new Ε(Ε.remove(_1iill,p.getUUID(),_11lll.getId(),seq)));
                        Arrays.fill(_1iill,(byte)0);
                    }
                }
            }
            _11lll.getEntityData().packDirty();
        }
        void discard(){
            _11III.removeAllPlayers();int did=_11lll.getId();_11iii.remove(did);
            _11lll.ι0();
            ClientboundRemoveEntitiesPacket rm=new ClientboundRemoveEntitiesPacket(did);
            for(ServerPlayer p:_11IIl.players()){
                p.connection.send(rm);
                byte[]_1iiiI=Σ_A.get(p.getUUID()),_1iil1=Σ_B.get(p.getUUID()),_11l11=Σ_C.get(p.getUUID());
                AtomicLong _1iili=Σ_S.get(p.getUUID());
                if(_1iiiI!=null&&_1iil1!=null&&_11l11!=null&&_1iili!=null){
                    byte[]_1iill=new byte[32];for(int i=0;i<32;i++)_1iill[i]=(byte)(_1iiiI[i]^_1iil1[i]^_11l11[i]);
                    long seq=_1iili.getAndIncrement();
                    Π.send(PacketDistributor.PLAYER.with(()->p),new Δ(Δ.revoke(_1iill,did)));
                    Π.send(PacketDistributor.PLAYER.with(()->p),new Ε(Ε.remove(_1iill,p.getUUID(),did,seq)));
                    Arrays.fill(_1iill,(byte)0);
                }
            }
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class _1i1i1{
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event){
            event.registerEntityRenderer(GoMEntities.MAGIC_GUARDIAN.get(),_11iIi::new);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class _11iIi extends MobRenderer<GoMEntity,HumanoidModel<GoMEntity>>{
        private static final ResourceLocation TX=new ResourceLocation(ExtraBossRush.MOD_ID,"textures/entity/magic_guardian.png");
        private static final Random RD=new Random();
        public _11iIi(EntityRendererProvider.Context ctx){
            super(ctx,new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)),0.6F);
        }
        @Override public @NotNull ResourceLocation getTextureLocation(@NotNull GoMEntity e){return TX;}
        @Override
        public void render(GoMEntity entity,float yaw,float pt,@NotNull PoseStack ps,@NotNull MultiBufferSource buf,int light){
            super.render(entity,yaw,pt,ps,buf,light);
            if(entity.ɦ2())_1i1l1(entity,ps,buf,pt,light,1F,0F,0F,0.2F,true);
            float ef=entity.ɦ3();float off=ef*0.04F-0.02F;
            if(off>0.001F){
                Random r=new Random((long)entity.getId()*98765L+entity.tickCount*12345L);
                _1iilI(entity,ps,buf,pt,light,off,r,1F,0F,0F,ef*0.3F);
                _1iilI(entity,ps,buf,pt,light,off,r,0F,1F,1F,ef*0.3F);
            }
        }
        private void _1iilI(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float off,Random r,float rc,float gc,float bc,float a){
            ps.pushPose();float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1F,-1F,1F);ps.translate(0F,-1.501F,0F);
            ps.translate((RD.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (RD.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (RD.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(TX)),light,OverlayTexture.NO_OVERLAY,rc,gc,bc,a);
            ps.popPose();
        }
        private void _1i1l1(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float r,float g,float b,float a,boolean hurt){
            ps.pushPose();float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1.0002F,-1.0002F,1.0002F);ps.translate(0F,-1.501F,0F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(TX)),light,
                    OverlayTexture.pack(OverlayTexture.u(0F),OverlayTexture.v(hurt)),r,g,b,a);
            ps.popPose();
        }
    }
}