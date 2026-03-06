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
    private static final EntityDataAccessor<String>  i11li11i=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float>   i11li1i1=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> i11li1ii=SynchedEntityData.defineId(GoMEntity.class,EntityDataSerializers.BOOLEAN);
    private static final List<i11ilill> i11i11ii=new CopyOnWriteArrayList<>();
    private static final SecureRandom i11l1l1l=new SecureRandom();
    private static final ConcurrentHashMap<UUID,byte[]>     i11iill1=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     i11il111=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     i11iilli=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,KeyPair>    i11l1lii  =new ConcurrentHashMap<>();
    static  final ConcurrentHashMap<UUID,AtomicLong>        i11l1lil  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Integer>    i11l1li1  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Long>       i11iliii  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Long>       i11ilil1  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,byte[]>     i11ilili  =new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,AtomicLong> i11iilll=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,Byte>       i11i1i11=new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,long[]>     i11l1ill  =new ConcurrentHashMap<>();
    private static final byte[]i11il1ll;
    static{byte[]i11ii111=new byte[32];new SecureRandom().nextBytes(i11ii111);i11il1ll=i11ii111;}
    private static volatile boolean i111l1il=false;
    private static final int    i1111lil=10;
    private static final long   i1111ll1=6000L,i1111li1=3L;
    private static final int    i111li1i=120;
    private static final String i11iii11="1";
    static final SimpleChannel i11l1ll1;
    static{
        i11l1ll1=NetworkRegistry.newSimpleChannel(new ResourceLocation(ExtraBossRush.MOD_ID,new String(new byte[]{0x67,0x6F,0x6D})),()->i11iii11,i11iii11::equals,i11iii11::equals);
        i11l1ll1.registerMessage(0,i11l1ili.class,i11l1ili::i11li1ll,i11l1ili::i11ill11,i11l1ili::i11lii1l);
        i11l1ll1.registerMessage(1,i11l1l11.class,i11l1l11::i11li1ll,i11l1l11::i11ill11,i11l1l11::i11lii1l);
        i11l1ll1.registerMessage(2,i11l11l1.class,i11l11l1::i11li1ll,i11l11l1::i11ill11,i11l11l1::i11lii1l);
        i11l1ll1.registerMessage(3,i11l11li.class,i11l11li::i11li1ll,i11l11li::i11ill11,i11l11li::i11lii1l);
        i11l1ll1.registerMessage(4,i11l11il.class,i11l11il::i11li1ll,i11l11il::i11ill11,i11l11il::i11lii1l);
    }
    private static boolean i11i1i1l(UUID u){
        long[]r=i11l1ill.computeIfAbsent(u,k->new long[]{0L,0L});
        long t=System.currentTimeMillis();
        if(t-r[0]>1000L){r[0]=t;r[1]=0L;}
        return++r[1]<=i111li1i;
    }
    private static void i11i11ll(UUID u){
        byte[]a=i11iill1.remove(u),b=i11il111.remove(u),c=i11iilli.remove(u);
        if(a!=null)Arrays.fill(a,(byte)0);if(b!=null)Arrays.fill(b,(byte)0);if(c!=null)Arrays.fill(c,(byte)0);
        i11l1lii.remove(u);i11l1lil.remove(u);i11l1li1.remove(u);i11iliii.remove(u);i11ilil1.remove(u);i11ilili.remove(u);i11iilll.remove(u);i11i1i11.remove(u);i11l1ill.remove(u);
    }
    private static final byte[]i111liii=new String(new byte[]{(byte)0xE6,(byte)0x94,(byte)0xBE,(byte)0xE9,(byte)0x80,(byte)0xA3},StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
    private static void i11i1i1i(ServerPlayer sp,byte[]sk){
        if(sk!=null)Arrays.fill(sk,(byte)0);i11i11ll(sp.getUUID());
        sp.connection.disconnect(Component.literal(new String(i111liii,StandardCharsets.UTF_8)));
    }
    private static byte[]i11iliil(UUID u){
        byte[]a=i11iill1.get(u),b=i11il111.get(u),c=i11iilli.get(u);if(a==null||b==null||c==null)return null;
        byte[]r=new byte[32];for(int i=0;i<32;i++)r[i]=(byte)(a[i]^b[i]^c[i]);return r;
    }
    public GoMEntity(EntityType<? extends GoMEntity>t,Level l){super(t,l);this.setNoGravity(true);this.noPhysics=true;}
    @Override
    protected void defineSynchedData(){
        super.defineSynchedData();
        this.entityData.define(i11li11i,"");this.entityData.define(i11li1i1,1.0F);this.entityData.define(i11li1ii,false);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?>k){super.onSyncedDataUpdated(k);}
    String  i11il11i()        {return this.entityData.get(i11li11i);}
    void    i11il11i(String v){this.entityData.set(i11li11i,v);}
    float   i11ili1i()        {return this.entityData.get(i11li1i1);}
    boolean i11ili1l()        {return this.entityData.get(i11li1ii);}
    float   i11ilii1()        {return 1.0F-i11ili1i();}
    public static AttributeSupplier.Builder createAttributes(){
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1000.0D).add(Attributes.ATTACK_DAMAGE,15.0D).add(Attributes.FOLLOW_RANGE,1024.0D);
    }
    @Override public boolean isAttackable(){return false;}
    @Override public boolean canBeCollidedWith(){return false;}
    @Override public boolean isPickable(){return false;}
    @Override public void tick(){}
    @Override public boolean hurt(@NotNull DamageSource s,float a){return false;}
    @Override public boolean canBeAffected(net.minecraft.world.effect.@NotNull MobEffectInstance e){return false;}
    void i11il1i1(){super.discard();}
    static String i11l1il1(){
        for(;;){
            StringBuilder b=new StringBuilder();for(int i=0;i<10;i++)b.append(i11l1l1l.nextInt(10));
            String c=b.toString();if(i11i11ii.stream().noneMatch(m->m.i111l1ll.equals(c)))return c;
        }
    }
    static final class i11l1iii{
        static final byte[]i11illl1,i11ill1i,i11illi1,i11ill1l,i11illil,i11illli,i11illll,i11illii;
        private static byte[]i1111lii(String cn){
            try{
                java.io.InputStream is=i11l1iii.class.getClassLoader().getResourceAsStream(cn);
                if(is==null)return new byte[32];
                byte[]r=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36})).digest(is.readAllBytes());
                is.close();return r;
            }catch(Exception e){return new byte[32];}
        }
        static{
            i11illl1=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1iii.class");
            i11ill1i=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11ilill.class");
            i11illi1=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity.class");
            i11ill1l=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11l1.class");
            i11illil=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11li.class");
            i11illli=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11il.class");
            i11illll=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1ili.class");
            i11illii=i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1l11.class");
        }
        static boolean i111i1il(){
            try{
                int d=0;
                byte[][]cur={i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1iii.class"),i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11ilill.class"),
                        i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity.class"),i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11l1.class"),
                        i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11li.class"),i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11il.class"),i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1ili.class"),i1111lii("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1l11.class")};
                byte[][]base={i11illl1,i11ill1i,i11illi1,i11ill1l,i11illil,i11illli,i11illll,i11illii};
                for(int j=0;j<cur.length;j++)for(int i=0;i<32;i++)d|=(cur[j][i]^base[j][i]);
                return d!=0;
            }catch(Exception e){return true;}
        }
        private static final String i11i1iil=new String(new byte[]{0x45,0x43});
        private static final String i11lii11=new String(new byte[]{0x73,0x65,0x63,0x70,0x32,0x35,0x36,0x72,0x31});
        private static final String i111111i=new String(new byte[]{0x45,0x43,0x44,0x48});
        private static final String i11li1il=new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36});
        private static final String i11i1ii1=new String(new byte[]{0x41,0x45,0x53,0x2F,0x47,0x43,0x4D,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static final String i1111ili=new String(new byte[]{0x41,0x45,0x53});
        private static final String i111llll=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30,0x2D,0x50,0x6F,0x6C,0x79,0x31,0x33,0x30,0x35});
        private static final String i11ii1ll=new String(new byte[]{0x43,0x68,0x61,0x43,0x68,0x61,0x32,0x30});
        private static final String i111i11l=new String(new byte[]{0x41,0x45,0x53,0x2F,0x45,0x43,0x42,0x2F,0x4E,0x6F,0x50,0x61,0x64,0x64,0x69,0x6E,0x67});
        private static int i1111lli(int v,int s){return(v<<(s&31))|(v>>>(32-(s&31)));}
        private static long i111i11i(long v){return Long.reverse(v)^(v>>>7)^(v<<13);}
        static byte[]i11il1ii(byte[]sk,byte info){
            try{
                MessageDigest md=MessageDigest.getInstance(i11li1il);
                byte[]blk=new byte[64];System.arraycopy(sk,0,blk,0,Math.min(sk.length,64));
                byte[]ip=new byte[64],op=new byte[64];
                for(int i=0;i<64;i++){ip[i]=(byte)(blk[i]^0x36);op[i]=(byte)(blk[i]^0x5C);}
                int i11lil1i=i1111lli(info&0xFF,3);
                ip[i11lil1i&63]^=(byte)(info^0xA3);op[(i11lil1i^31)&63]^=(byte)(info^0x5B);
                md.update(ip);byte[]r1=md.digest(sk);md.reset();md.update(op);return md.digest(r1);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static KeyPair i11l111i(){
            try{
                KeyPairGenerator g=KeyPairGenerator.getInstance(i11i1iil);
                g.initialize(new ECGenParameterSpec(i11lii11),new SecureRandom());return g.generateKeyPair();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]i11l11ll(PrivateKey prv,byte[]pub){
            try{
                PublicKey pk=KeyFactory.getInstance(i11i1iil).generatePublic(new X509EncodedKeySpec(pub));
                KeyAgreement ka=KeyAgreement.getInstance(i111111i);ka.init(prv);ka.doPhase(pk,true);
                return MessageDigest.getInstance(i11li1il).digest(ka.generateSecret());
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]i11l1i1i(byte[]key,byte[]plain,byte[]aad){
            try{
                byte[]iv=new byte[12];new SecureRandom().nextBytes(iv);
                Cipher c=Cipher.getInstance(i11i1ii1);
                c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,i1111ili),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);byte[]ct=c.doFinal(plain);
                return ByteBuffer.allocate(12+ct.length).put(iv).put(ct).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        private static volatile int i111lil1=0;
        static byte[]i11il1li(byte[]key,byte[]plain,byte[]aad,AtomicLong ctr){
            try{
                int i11iii1l=(int)(System.nanoTime()&0x7FFFFFFE);
                if((i111lil1&1)!=0){Arrays.fill(key,(byte)0xFF);return new byte[0];}
                i111lil1=i11iii1l;
                if(i1111lli(i11iii1l,7)<0&&Integer.bitCount(i11iii1l)==32){throw new RuntimeException();}
                byte[]k1=i11il1ii(key,(byte)0x01),k2=i11il1ii(key,(byte)0x02);
                long cnt=ctr.getAndIncrement();
                byte[]iv1=new byte[12];System.arraycopy(k1,0,iv1,0,4);ByteBuffer.wrap(iv1,4,8).putLong(cnt);
                Cipher c1=Cipher.getInstance(i11i1ii1);
                c1.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k1,i1111ili),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);byte[]inner=c1.doFinal(plain);
                byte[]wrapped=ByteBuffer.allocate(12+inner.length).put(iv1).put(inner).array();
                byte[]iv2=new byte[12];new SecureRandom().nextBytes(iv2);
                Cipher c2=Cipher.getInstance(i111llll);
                c2.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k2,i11ii1ll),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);byte[]outer=c2.doFinal(wrapped);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);
                return ByteBuffer.allocate(12+outer.length).put(iv2).put(outer).array();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]i11l1i1l(byte[]key,byte[]data,byte[]aad){
            try{
                long i11i1lli=System.nanoTime()^data.length;
                if(Long.bitCount(i11i1lli)==0&&i111i11i(i11i1lli)!=0){return null;}
                byte[]k1=i11il1ii(key,(byte)0x01),k2=i11il1ii(key,(byte)0x02);
                ByteBuffer ob=ByteBuffer.wrap(data);
                byte[]iv2=new byte[12];ob.get(iv2);byte[]oCt=new byte[ob.remaining()];ob.get(oCt);
                Cipher c2=Cipher.getInstance(i111llll);
                c2.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k2,i11ii1ll),new IvParameterSpec(iv2));
                if(aad!=null)c2.updateAAD(aad);byte[]wrapped=c2.doFinal(oCt);
                ByteBuffer ib=ByteBuffer.wrap(wrapped);
                byte[]iv1=new byte[12];ib.get(iv1);byte[]iCt=new byte[ib.remaining()];ib.get(iCt);
                Cipher c1=Cipher.getInstance(i11i1ii1);
                c1.init(Cipher.DECRYPT_MODE,new SecretKeySpec(k1,i1111ili),new GCMParameterSpec(128,iv1));
                if(aad!=null)c1.updateAAD(aad);byte[]r=c1.doFinal(iCt);
                Arrays.fill(k1,(byte)0);Arrays.fill(k2,(byte)0);return r;
            }catch(Exception e){return null;}
        }
        static byte[]i11il1il(byte[]key,byte[]data,byte[]aad){
            try{
                ByteBuffer bb=ByteBuffer.wrap(data);byte[]iv=new byte[12];bb.get(iv);
                byte[]ct=new byte[bb.remaining()];bb.get(ct);
                Cipher c=Cipher.getInstance(i11i1ii1);
                c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,i1111ili),new GCMParameterSpec(128,iv));
                if(aad!=null)c.updateAAD(aad);return c.doFinal(ct);
            }catch(Exception e){return null;}
        }
        static byte i11l11i1(double v){
            boolean i11iilil=(v==Math.floor(v))&&!Double.isInfinite(v)&&v>=Integer.MIN_VALUE&&v<=Integer.MAX_VALUE;
            if(i11iilil)return 0x01;if(Math.abs(v)<=32767.0)return 0x00;if(Math.abs(v)<=922337203685.0)return 0x02;return 0x03;
        }
        static int i11il11l(byte t){return(t==0x00||t==0x01)?4:8;}
        static void i11l111l(ByteBuffer bb,double v,byte t){
            switch(t){case 0x00->bb.putFloat((float)v);case 0x01->bb.putInt((int)v);case 0x02->bb.putLong(Math.round(v*10000.0));case 0x03->bb.putDouble(v);}
        }
        static double i11l1111(ByteBuffer bb,int t){
            return switch(t){case 0x00->bb.getFloat();case 0x01->bb.getInt();case 0x02->bb.getLong()/10000.0;case 0x03->bb.getDouble();default->throw new IllegalArgumentException();};
        }
        static byte[]i11l1ii1(byte[]key,float v){
            try{
                byte[]blk=new byte[16];int bits=Float.floatToRawIntBits(v);
                blk[0]=(byte)(bits>>24);blk[1]=(byte)(bits>>16);blk[2]=(byte)(bits>>8);blk[3]=(byte)bits;
                new SecureRandom().nextBytes(Arrays.copyOfRange(blk,4,16));
                blk[4]=(byte)(bits^blk[0]^0xA5);blk[5]=(byte)(bits^(blk[1]<<1)^0x3C);
                blk[12]=(byte)~blk[0];blk[13]=(byte)~blk[1];blk[14]=(byte)~blk[2];blk[15]=(byte)~blk[3];
                Cipher c=Cipher.getInstance(i111i11l);c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,i1111ili));return c.doFinal(blk);
            }catch(Exception e){throw new RuntimeException(e);}
        }
        static float i11l1iil(byte[]key,byte[]ci){
            if(ci==null||ci.length<16)return 0f;
            try{
                Cipher c=Cipher.getInstance(i111i11l);c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,i1111ili));
                byte[]blk=c.doFinal(ci);
                int bits=((blk[0]&0xFF)<<24)|((blk[1]&0xFF)<<16)|((blk[2]&0xFF)<<8)|(blk[3]&0xFF);
                return Float.intBitsToFloat(bits);
            }catch(Exception e){return 0f;}
        }
        private static final Set<String>i111i111=new HashSet<>(Arrays.asList(
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
        static boolean i11111ll(ServerPlayer sp){try{io.netty.channel.ChannelPipeline pipe=sp.connection.connection.channel().pipeline();for(String n:pipe.names()){String lo=n.toLowerCase(java.util.Locale.ROOT);boolean ok=false;for(String s:i111i111){if(lo.contains(s)){ok=true;break;}}if(!ok){String cls=pipe.get(n).getClass().getName().toLowerCase(java.util.Locale.ROOT);if(!cls.contains(new String(new byte[]{0x6D,0x69,0x6E,0x65,0x63,0x72,0x61,0x66,0x74}))&&!cls.contains(new String(new byte[]{0x6D,0x69,0x6E,0x65,0x66,0x6F,0x72,0x67,0x65}))&&!cls.contains(new String(new byte[]{0x6E,0x65,0x74,0x74,0x79}))&&!cls.contains(new String(new byte[]{0x66,0x6F,0x72,0x67,0x65})))System.out.println(new String(new byte[]{0x5B,0x47,0x6F,0x4D,0x5D})+n+new String(new byte[]{0x3A,0x20})+cls);}}}catch(Exception e){}return false;}
    }
    static final class i11l1ili{
        final byte[]i11li111;i11l1ili(byte[]k){i11li111=k;}private static final int i11ii11l=256;
        static void i11li1ll(i11l1ili p,FriendlyByteBuf b){b.writeByteArray(p.i11li111);}
        static i11l1ili i11ill11(FriendlyByteBuf b){return new i11l1ili(b.readByteArray(i11ii11l));}
        static void i11lii1l(i11l1ili p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->i11l1l1i.i111ili1(p.i11li111));ctx.get().setPacketHandled(true);}
    }
    static final class i11l1l11{
        final byte[]i11li111;i11l1l11(byte[]k){i11li111=k;}private static final int i11ii11l=256;
        static void i11li1ll(i11l1l11 p,FriendlyByteBuf b){b.writeByteArray(p.i11li111);}
        static i11l1l11 i11ill11(FriendlyByteBuf b){return new i11l1l11(b.readByteArray(i11ii11l));}
        static void i11lii1l(i11l1l11 p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!i11i1i1l(uid)){i11i1i1i(sp,null);return;}
                if(p.i11li111==null||p.i11li111.length>i11ii11l){i11l1li1.merge(uid,1,Integer::sum);return;}
                Byte st=i11i1i11.get(uid);
                if(st==null||st!=(byte)0){int f=i11l1li1.merge(uid,1,Integer::sum);if(f>=i1111lil)i11i1i1i(sp,null);return;}
                if(i11iill1.containsKey(uid))return;
                KeyPair kp=i11l1lii.remove(uid);if(kp==null)return;
                byte[]sk;try{sk=i11l1iii.i11l11ll(kp.getPrivate(),p.i11li111);}catch(Exception e){return;}
                byte[]a=new byte[32],c=new byte[32];i11l1l1l.nextBytes(a);i11l1l1l.nextBytes(c);
                byte[]bb2=new byte[32];for(int i=0;i<32;i++)bb2[i]=(byte)(sk[i]^a[i]^c[i]);
                Arrays.fill(sk,(byte)0);
                i11iill1.put(uid,a);i11il111.put(uid,bb2);i11iilli.put(uid,c);
                i11l1lil.put(uid,new AtomicLong(0L));i11l1li1.put(uid,0);i11iliii.put(uid,sp.serverLevel().getGameTime());
                byte[]n0=new byte[8];i11l1l1l.nextBytes(n0);i11ilili.put(uid,n0);
                i11iilll.put(uid,new AtomicLong(0L));i11i1i11.put(uid,(byte)1);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class i11l11l1{
        final byte[]i11l1lll;i11l11l1(byte[]d){i11l1lll=d;}private static final int i11ii11l=256;
        static void i11li1ll(i11l11l1 p,FriendlyByteBuf b){b.writeByteArray(p.i11l1lll);}
        static i11l11l1 i11ill11(FriendlyByteBuf b){return new i11l11l1(b.readByteArray(i11ii11l));}
        static void i11lii1l(i11l11l1 p,Supplier<NetworkEvent.Context>ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer sp=ctx.get().getSender();if(sp==null)return;
                UUID uid=sp.getUUID();
                if(!i11i1i1l(uid)){i11i1i1i(sp,null);return;}
                Byte st=i11i1i11.get(uid);if(st==null||st!=(byte)1)return;
                long now=sp.serverLevel().getGameTime();
                Long lΓ=i11ilil1.get(uid);if(lΓ!=null&&now-lΓ<i1111li1)return;i11ilil1.put(uid,now);
                byte[]sk=i11iliil(uid);if(sk==null)return;
                Long lR=i11iliii.get(uid);
                if(lR!=null&&now-lR>=i1111ll1){
                    i11iliii.put(uid,now);
                    byte[]i11iiiii=i11iill1.remove(uid),i11iiil1=i11il111.remove(uid),i11iili1=i11iilli.remove(uid);
                    if(i11iiiii!=null)Arrays.fill(i11iiiii,(byte)0);if(i11iiil1!=null)Arrays.fill(i11iiil1,(byte)0);if(i11iili1!=null)Arrays.fill(i11iili1,(byte)0);
                    i11l1lil.remove(uid);i11ilil1.remove(uid);i11ilili.remove(uid);i11iilll.remove(uid);
                    i11i1i11.put(uid,(byte)0);Arrays.fill(sk,(byte)0);
                    KeyPair kp=i11l1iii.i11l111i();i11l1lii.put(uid,kp);
                    i11l1ll1.send(PacketDistributor.PLAYER.with(()->sp),new i11l1ili(kp.getPublic().getEncoded()));return;
                }
                if(i11l1iii.i11111ll(sp)){Arrays.fill(sk,(byte)0);i11i1i1i(sp,null);return;}
                if(i11l1iii.i111i1il()){
                    Arrays.fill(sk,(byte)0);
                    Arrays.fill(sk,(byte)0);i11i1i1i(sp,null);return;
                }
                byte[]aad=uid.toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=i11l1iii.i11l1i1l(sk,p.i11l1lll,aad);
                if(plain==null||plain.length<21){
                    int f=i11l1li1.merge(uid,1,Integer::sum);if(f>=i1111lil)i11i1i1i(sp,sk);else Arrays.fill(sk,(byte)0);return;
                }
                i11l1li1.put(uid,0);
                ByteBuffer bb=ByteBuffer.wrap(plain);
                byte[]rxN=new byte[8];bb.get(rxN);
                byte[]expN=i11ilili.get(uid);if(expN==null){Arrays.fill(sk,(byte)0);return;}
                int i111lill=0;for(int i=0;i<8;i++)i111lill|=(rxN[i]^expN[i]);
                if(i111lill!=0){int f=i11l1li1.merge(uid,1,Integer::sum);if(f>=i1111lil)i11i1i1i(sp,sk);else Arrays.fill(sk,(byte)0);return;}
                byte[]nN=new byte[8];i11l1l1l.nextBytes(nN);i11ilili.put(uid,nN);
                byte[]rxT=new byte[8];bb.get(rxT);
                int tid=bb.getInt();
                int nt=(int)(sp.serverLevel().getGameTime()&0xFFFFFFFFL);
                if(Math.abs(nt-tid)>20){Arrays.fill(sk,(byte)0);return;}
                byte fl=bb.get();double hx,hy,hz;
                try{hx=i11l1iii.i11l1111(bb,(fl)&0x03);hy=i11l1iii.i11l1111(bb,(fl>>2)&0x03);hz=i11l1iii.i11l1111(bb,(fl>>4)&0x03);}
                catch(Exception e){Arrays.fill(sk,(byte)0);return;}
                Vec3 hp=new Vec3(hx,hy,hz);
                for(i11ilill m:i11i11ii){
                    if(m.getLevel()!=sp.serverLevel())continue;
                    byte[]i111lili=ByteBuffer.allocate(8).putInt(m.i111llil.getId()).putInt(tid).array();
                    byte[]i111ll11=i11l1iii.i11il1ii(sk,(byte)0x7F);
                    for(int i=0;i<16;i++)i111ll11=i11l1iii.i11il1ii(i111ll11,i111lili[i&7]);
                    int i1111iil=0;for(int i=0;i<8;i++)i1111iil|=(rxT[i]^i111ll11[i]);
                    if(i1111iil!=0)continue;
                    int i111i1i1=Math.min(Math.abs(nt-tid),10);
                    double i111i1ll=0.5+i111i1i1*0.2;
                    Vec3 i111i1li=m.i111i1l1[(int)((long)tid&31)];if(i111i1li==null)i111i1li=m.i1111ill;
                    AABB i111iii1=new AABB(i111i1li.x-0.4-i111i1ll,i111i1li.y-i111i1ll,i111i1li.z-0.4-i111i1ll,i111i1li.x+0.4+i111i1ll,i111i1li.y+1.8+i111i1ll,i111i1li.z+0.4+i111i1ll);
                    if(!i111iii1.contains(hp)){Arrays.fill(sk,(byte)0);return;}
                    Vec3 i1111il1=sp.getEyePosition(1.0F);double i111il11=5.5+i111i1i1*0.3;
                    if(i1111il1.distanceToSqr(hp)>i111il11*i111il11){Arrays.fill(sk,(byte)0);return;}
                    BlockHitResult i111l11l=sp.serverLevel().clip(new ClipContext(i1111il1,hp,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,sp));
                    if(i111l11l.getType()!=HitResult.Type.MISS){Arrays.fill(sk,(byte)0);return;}
                    i111l1il=true;m.i11iiiil(sp);i111l1il=false;break;
                }
                Arrays.fill(sk,(byte)0);
            });
            ctx.get().setPacketHandled(true);
        }
    }
    static final class i11l11li{
        final byte[]i11l1lll;i11l11li(byte[]d){i11l1lll=d;}private static final int i11ii11l=128;
        static void i11li1ll(i11l11li p,FriendlyByteBuf b){b.writeByteArray(p.i11l1lll);}
        static i11l11li i11ill11(FriendlyByteBuf b){return new i11l11li(b.readByteArray(i11ii11l));}
        static void i11lii1l(i11l11li p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->i11l1l1i.i11lii1i(p.i11l1lll));ctx.get().setPacketHandled(true);}
        static byte[]revoke(byte[]sk,int eid){
            try{byte[]pl=new byte[]{(byte)0xFF,(byte)(eid>>24),(byte)(eid>>16),(byte)(eid>>8),(byte)eid};
                return i11l1iii.i11l1i1i(sk,pl,new byte[]{(byte)0xD4,(byte)0xFF});}catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]build(byte[]sk,int eid,String mid){
            try{byte[]mb=mid.getBytes(StandardCharsets.UTF_8);ByteBuffer bb=ByteBuffer.allocate(4+mb.length);bb.putInt(eid);bb.put(mb);
                return i11l1iii.i11l1i1i(sk,bb.array(),new byte[]{(byte)0xD4,(byte)(eid&0xFF)});}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    static final class i11l11il{
        final byte[]i11l1lll;i11l11il(byte[]d){i11l1lll=d;}private static final int i11ii11l=128;
        static void i11li1ll(i11l11il p,FriendlyByteBuf b){b.writeByteArray(p.i11l1lll);}
        static i11l11il i11ill11(FriendlyByteBuf b){return new i11l11il(b.readByteArray(i11ii11l));}
        static void i11lii1l(i11l11il p,Supplier<NetworkEvent.Context>ctx){ctx.get().enqueueWork(()->i11l1l1i.i11li1li(p.i11l1lll));ctx.get().setPacketHandled(true);}
        static byte[]build(byte[]sk,UUID uid,int eid,float hr,boolean hurt,double x,double y,double z,byte[]nonce,long seq){
            try{ByteBuffer bb=ByteBuffer.allocate(50);
                bb.put((byte)0x01);bb.putLong(seq);bb.putInt(eid);bb.put(nonce);bb.putFloat(hr);bb.put((byte)(hurt?1:0));
                bb.putDouble(x);bb.putDouble(y);bb.putDouble(z);
                return i11l1iii.i11l1i1i(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}
        }
        static byte[]remove(byte[]sk,UUID uid,int eid,long seq){
            try{ByteBuffer bb=ByteBuffer.allocate(13);bb.put((byte)0x02);bb.putLong(seq);bb.putInt(eid);
                return i11l1iii.i11l1i1i(sk,bb.array(),uid.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
    static final class i11l1l1i{
        private static boolean i11111ii=false;
        private static javax.crypto.SecretKey i111ii1l=null;
        private static AtomicLong i111ii11=new AtomicLong(0L);
        static final Set<Integer>                    i11liii1=Collections.synchronizedSet(new HashSet<>());
        static final ConcurrentHashMap<Integer,Vec3> i111111l=new ConcurrentHashMap<>();
        private static volatile long   i11111i1=-1L;
        private static volatile byte[] i111llli=null;
        private static final double i1111111=4.5;
        @OnlyIn(Dist.CLIENT)
        static void i111ili1(byte[]spub){
            try{
                KeyPair kp=i11l1iii.i11l111i();byte[]raw=i11l1iii.i11l11ll(kp.getPrivate(),spub);
                if(i111ii1l!=null){try{((javax.security.auth.Destroyable)i111ii1l).destroy();}catch(Exception ignored){}}
                i111ii1l=new SecretKeySpec(raw,new String(new byte[]{0x41,0x45,0x53}));
                Arrays.fill(raw,(byte)0);i111ii11.set(0L);i11111i1=-1L;i111llli=null;
                i11l1ll1.sendToServer(new i11l1l11(kp.getPublic().getEncoded()));
            }catch(Exception ignored){}
        }
        @SubscribeEvent @OnlyIn(Dist.CLIENT)
        static void i11li11l(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.START)return;
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null||mc.level==null||i111ii1l==null)return;
            try{if(((javax.security.auth.Destroyable)i111ii1l).isDestroyed())return;}catch(Exception ignored){}
            boolean sw=mc.player.swinging;if(sw&!i11111ii)i1111lll(mc);i11111ii=sw;
        }
        @OnlyIn(Dist.CLIENT)
        private static void i1111lll(Minecraft mc){
            Vec3 eye=mc.player.getEyePosition(1.0F);Vec3 end=eye.add(mc.player.getLookAngle().scale(i1111111));
            for(net.minecraft.world.entity.Entity ent:mc.level.entitiesForRendering()){
                if(!(ent instanceof GoMEntity g))continue;if(!i11liii1.contains(g.getId()))continue;
                Vec3 ap=i111111l.get(g.getId());if(ap==null)continue;
                AABB box=new AABB(ap.x-0.4,ap.y,ap.z-0.4,ap.x+0.4,ap.y+1.8,ap.z+0.4);
                Optional<Vec3>hit=box.clip(eye,end);if(hit.isEmpty())continue;
                Vec3 hp=hit.get();if(eye.distanceToSqr(hp)>(i1111111+0.1)*(i1111111+0.1))continue;
                boolean surf=box.inflate(0.05).contains(hp)&&!box.deflate(0.05).contains(hp);if(!surf)continue;
                BlockHitResult bhr=mc.level.clip(new ClipContext(eye,hp,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
                if(bhr.getType()!=HitResult.Type.MISS)continue;
                i11l11ii(g,hp,mc);return;
            }
        }
        @OnlyIn(Dist.CLIENT)
        private static void i11l11ii(GoMEntity g,Vec3 hp,Minecraft mc){
            if(i111ii1l==null)return;byte[]nc=i111llli;if(nc==null)return;
            try{
                int tick=(int)(mc.level.getGameTime()&0xFFFFFFFFL);byte[]rk=i111ii1l.getEncoded();
                byte tx=i11l1iii.i11l11i1(hp.x),ty=i11l1iii.i11l11i1(hp.y),tz=i11l1iii.i11l11i1(hp.z);
                byte fl=(byte)((tx&0x03)|((ty&0x03)<<2)|((tz&0x03)<<4));
                byte[]i111lili=ByteBuffer.allocate(8).putInt(g.getId()).putInt(tick).array();
                byte[]i111ll11=i11l1iii.i11il1ii(rk,(byte)0x7F);for(int i=0;i<16;i++)i111ll11=i11l1iii.i11il1ii(i111ll11,i111lili[i&7]);
                byte[]token=Arrays.copyOf(i111ll11,8);
                int sz=21+i11l1iii.i11il11l(tx)+i11l1iii.i11il11l(ty)+i11l1iii.i11il11l(tz);ByteBuffer bb=ByteBuffer.allocate(sz);
                bb.put(nc);bb.put(token);bb.putInt(tick);bb.put(fl);
                i11l1iii.i11l111l(bb,hp.x,tx);i11l1iii.i11l111l(bb,hp.y,ty);i11l1iii.i11l111l(bb,hp.z,tz);
                byte[]aad=mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8);
                byte[]ci=i11l1iii.i11il1li(rk,bb.array(),aad,i111ii11);i11l1ll1.sendToServer(new i11l11l1(ci));
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void i11lii1i(byte[]data){
            if(i111ii1l==null||data==null||data.length<28)return;
            try{
                byte[]rk=i111ii1l.getEncoded();byte fc=(data.length>12)?data[12]:0;
                byte[]plain=i11l1iii.i11il1il(rk,data,new byte[]{(byte)0xD4,fc});if(plain==null||plain.length<4)return;
                if(plain.length>=5&&(plain[0]&0xFF)==0xFF){i11liii1.remove(ByteBuffer.wrap(plain,1,4).getInt());}
                else if(plain.length>=4){i11liii1.add(ByteBuffer.wrap(plain).getInt());}
            }catch(Exception ignored){}
        }
        @OnlyIn(Dist.CLIENT)
        static void i11li1li(byte[]data){
            if(i111ii1l==null||data==null)return;
            try{
                byte[]rk=i111ii1l.getEncoded();Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
                byte[]aad=mc.player.getUUID().toString().getBytes(StandardCharsets.UTF_8);
                byte[]plain=i11l1iii.i11il1il(rk,data,aad);if(plain==null||plain.length<13)return;
                ByteBuffer bb=ByteBuffer.wrap(plain);byte type=bb.get();long seq=bb.getLong();
                if(seq<=i11111i1)return;i11111i1=seq;int eid=bb.getInt();
                if(type==(byte)0x02){i11liii1.remove(eid);i111111l.remove(eid);return;}
                if(type!=(byte)0x01||plain.length<50)return;
                byte[]rxN=new byte[8];bb.get(rxN);i111llli=rxN;bb.getFloat();bb.get();
                double ax=bb.getDouble(),ay=bb.getDouble(),az=bb.getDouble();i111111l.put(eid,new Vec3(ax,ay,az));
            }catch(Exception ignored){}
        }
        @SubscribeEvent @OnlyIn(Dist.CLIENT)
        static void i11l1i11(LevelEvent.Unload e){
            if(!e.getLevel().isClientSide())return;i11111ii=false;
            if(i111ii1l!=null){try{((javax.security.auth.Destroyable)i111ii1l).destroy();}catch(Exception ignored){}i111ii1l=null;}
            i111ii11.set(0L);i11liii1.clear();i111111l.clear();i11111i1=-1L;i111llli=null;
        }
    }
    @SubscribeEvent
    public static void i11l111i(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;
        UUID uid=sp.getUUID();i11i11ll(uid);i11i1i11.put(uid,(byte)0);
        KeyPair kp=i11l1iii.i11l111i();i11l1lii.put(uid,kp);i11l1ll1.send(PacketDistributor.PLAYER.with(()->sp),new i11l1ili(kp.getPublic().getEncoded()));
    }
    @SubscribeEvent
    public static void i11l11ll(PlayerEvent.PlayerLoggedOutEvent e){
        if(!(e.getEntity() instanceof ServerPlayer sp))return;i11i11ll(sp.getUUID());
    }
    @SubscribeEvent
    public static void i11i1l1i(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;if(!(event.level instanceof ServerLevel level))return;
        i11i11ii.removeIf(master->{
            if(master.getLevel()!=level)return false;master.i11i11li();
            if(master.i11i11il()){master.discard();return true;}return false;
        });
    }
    @SubscribeEvent
    public static void i11i1l11(LevelEvent.Unload event){
        if(event.getLevel().isClientSide())return;i11i11ii.forEach(i11ilill::discard);i11i11ii.clear();
    }


    @SubscribeEvent
    public static void i11i1ili(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer sp))return;
        if(!sp.getMainHandItem().is(GoMItems.MAGIC_GUARDIAN_EGG.get()))return;
        event.setCanceled(true);
        net.minecraft.core.BlockPos bp=event.getPos().relative(event.getFace());
        Vec3 spawnPos=new Vec3(bp.getX()+0.5,bp.getY(),bp.getZ()+0.5);
        if(!sp.isCreative())sp.getMainHandItem().shrink(1);
        i11i11ii.add(new i11ilill(i11l1il1(),(ServerLevel)event.getLevel(),spawnPos));
    }
    @SubscribeEvent
    public static void i11i1il1(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event){
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer sp))return;
        if(!sp.getMainHandItem().is(GoMItems.MAGIC_GUARDIAN_EGG.get()))return;
        event.setCanceled(true);
        Vec3 spawnPos=sp.getEyePosition(1.0F).add(sp.getLookAngle().scale(3.0));
        if(!sp.isCreative())sp.getMainHandItem().shrink(1);
        i11i11ii.add(new i11ilill(i11l1il1(),(ServerLevel)event.getLevel(),spawnPos));
    }
    @SubscribeEvent
    public static void i11i1iii(RegisterCommandsEvent event){
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("gomentity").requires(s->s.hasPermission(2))

                        .executes(ctx->{
                            net.minecraft.commands.CommandSourceStack src=ctx.getSource();
                            if(!(src.getEntity() instanceof ServerPlayer p)){
                                src.sendFailure(Component.literal("Player only"));return 0;
                            }
                            Vec3 sp2=p.position().add(p.getLookAngle().scale(5));
                            i11i11ii.add(new i11ilill(i11l1il1(),p.serverLevel(),sp2));
                            src.sendSuccess(()->Component.literal("Spawned"),true);return 1;
                        })

                        .then(net.minecraft.commands.Commands.argument("pos",net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3())
                                .executes(ctx->{
                                    net.minecraft.commands.CommandSourceStack src=ctx.getSource();
                                    Vec3 pos2=net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx,"pos");
                                    ServerLevel lv2;
                                    if(src.getEntity() instanceof ServerPlayer p2)lv2=p2.serverLevel();
                                    else lv2=(ServerLevel)src.getLevel();
                                    i11i11ii.add(new i11ilill(i11l1il1(),lv2,pos2));
                                    src.sendSuccess(()->Component.literal("Spawned at "+pos2),true);return 1;
                                })
                        )
        );
    }
    interface i11li1l1{byte[]i11l1lli();void i11l1lli(byte[]c);}
    static final class i11ii1il implements i11li1l1{
        private byte[]i11iili1;
        public byte[]i11l1lli(){return i11iili1==null?null:Arrays.copyOf(i11iili1,i11iili1.length);}
        public void i11l1lli(byte[]c){i11iili1=c==null?null:Arrays.copyOf(c,c.length);}
    }
    static final class i11ilill{
        private static final int i111iiii=0x00,i111illi=0x04,i111iiil=0x08,i111iili=0x0C,i111ilil=0x10,i111iill=0x18;
        private static final byte[]i11liiii,i11liiil,i11liil1,i11liili,i11liill,i111l1li;
        private static final byte[]i111li11;
        private static byte[]i111l111(String cn){
            try{
                java.io.InputStream is=i11ilill.class.getClassLoader().getResourceAsStream(cn);
                if(is==null)return new byte[32];
                byte[]r=MessageDigest.getInstance(new String(new byte[]{0x53,0x48,0x41,0x2D,0x32,0x35,0x36})).digest(is.readAllBytes());
                is.close();return r;
            }catch(Exception e){return new byte[32];}
        }
        static{
            i11liiii   =i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11ilill.class");
            i11liiil=i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1iii.class");
            i11liil1 =i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11l1.class");
            i11liili =i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11li.class");
            i11liill =i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11il.class");
            byte[]ok=i11l1iii.i11il1ii(i11liiii,(byte)0x55);i111l1li=ok;
            byte[]plain={0x00,0x03,0x07,0x01,0x06,0x04,0x05,0x02};
            byte[]ot=new byte[8];for(int i=0;i<8;i++)ot[i]=(byte)(plain[i]^ok[i]);
            i111li11=ot;
        }
        private static int i111illl=0;
        private static final int   i111l11i=46;
        private static final float i111iil1=65536.0F;
        private static final long  i111il1l=10L;
        private static final double i111ilii=1024.0*1024.0;
        private static final Random i111liil=new Random();
        static final ConcurrentHashMap<Integer,Long>i111l1i1=new ConcurrentHashMap<>();
        final String i111l1ll;
        private final ServerLevel i111ill1;
        private final ServerBossEvent i111il1i;
        final GoMEntity i111llil;
        Vec3 i1111ill;
        private final byte[]i1111l1i=new byte[0x20];
        private final byte[]i111lll1=new byte[16];
        private final byte[]i11i111l=new byte[16];
        private final i11li1l1   i11i11i1=new i11ii1il();
        private final long[]i111l1l1=new long[1];
        private final byte[]i11i1111=new byte[8];
        private final byte[]i11i1lii=new byte[8];
        final Vec3[]i111i1l1=new Vec3[32];
        private int i111lii1(int o){int p=o&0xF;return((i111lll1[p]&0xFF)<<24)|((i111lll1[(p+1)&0xF]&0xFF)<<16)|((i111lll1[(p+2)&0xF]&0xFF)<<8)|(i111lll1[(p+3)&0xF]&0xFF);}
        private long i111l1ii(int o){int p=o&0xF;return(((long)i111lll1[p]&0xFF)<<56)|(((long)i111lll1[(p+1)&0xF]&0xFF)<<48)|(((long)i111lll1[(p+2)&0xF]&0xFF)<<40)|(((long)i111lll1[(p+3)&0xF]&0xFF)<<32)|(((long)i111lll1[(p+4)&0xF]&0xFF)<<24)|(((long)i111lll1[(p+5)&0xF]&0xFF)<<16)|(((long)i111lll1[(p+6)&0xF]&0xFF)<<8)|((long)i111lll1[(p+7)&0xF]&0xFF);}
        private int i11i1lil(int o){return ByteBuffer.wrap(i1111l1i,o,4).getInt()^i111lii1(o);}
        private void i11i1l1l(int o,int v){byte[]b=new byte[4];ByteBuffer.wrap(b).putInt(v^i111lii1(o));System.arraycopy(b,0,i1111l1i,o,4);}
        private long i11i1li1(int o){return ByteBuffer.wrap(i1111l1i,o,8).getLong()^i111l1ii(o);}
        private void i11ii1i1(int o,long v){byte[]b=new byte[8];ByteBuffer.wrap(b).putLong(v^i111l1ii(o));System.arraycopy(b,0,i1111l1i,o,8);}
        private float i11ii1ii(int o){return Float.intBitsToFloat(i11i1lil(o));}
        private void i11ii11i(int o,float v){i11i1l1l(o,Float.floatToRawIntBits(v));}
        private byte[]i11i111i(){byte[]k=i11l1iii.i11il1ii(i11i111l,(byte)0x42);for(int i=0;i<16;i++)k[i]^=i11il1ll[i];return Arrays.copyOf(k,16);}
        private float i111lli1(){return i11l1iii.i11l1iil(i11i111i(),i11i11i1.i11l1lli());}
        private void  i111llii(float v){i11i11i1.i11l1lli(i11l1iii.i11l1ii1(i11i111i(),v));}
        private void i11111il(float v){i111l1i1.put(i111llil.getId(),(long)Float.floatToRawIntBits(v)^(i111l1l1[0]&0xFFFFFFFFL));}
        private boolean i11lil1l(){
            Long e=i111l1i1.get(i111llil.getId());if(e==null)return true;
            float s=Float.intBitsToFloat((int)((e^(i111l1l1[0]&0xFFFFFFFFL))&0xFFFFFFFFL));
            return Math.abs(i111lli1()-s)<1.0f;
        }
        private void i1111l11(int sl){
            int op=(i11i1111[sl&7]^i11i1lii[sl&7])&0xFF;
            switch(op){
                case 0x00->i1111i11();case 0x01->i1111l1l();case 0x02->i11111l1();case 0x03->i11111li();
                case 0x04->i1111i1l();case 0x05->i1111i1i();case 0x06->i111ii1i();case 0x07->i1111ii1();
            }
        }
        private void i1111i11(){
            int t0=i11i1lil(i111iiii),t1=i11i1lil(i111illi),t2=i11i1lil(i111iiil),t3=i11i1lil(i111iili);long t4=i11i1li1(i111ilil);float t5=i11ii1ii(i111iill);
            new SecureRandom().nextBytes(i111lll1);
            i11i1l1l(i111iiii,t0);i11i1l1l(i111illi,t1);i11i1l1l(i111iiil,t2);i11i1l1l(i111iili,t3);i11ii1i1(i111ilil,t4);i11ii11i(i111iill,t5);
            float i11iili1=i111lli1();new SecureRandom().nextBytes(i11i111l);i111llii(i11iili1);
            i111l1l1[0]=new SecureRandom().nextLong();i11111il(i11iili1);
            byte[]rk=new byte[8];new SecureRandom().nextBytes(rk);
            for(int i=0;i<8;i++){i11i1111[i]^=rk[i];i11i1lii[i]^=rk[i];}
        }
        private void i1111l1l(){i111llil.entityData.set(i11li1i1,i111lli1()/i11ii1ii(i111iill));}
        private void i11111l1(){PSU.getPlayersWithinRadius(i111ill1,i1111ill.x,i1111ill.y,i1111ill.z,1024.0).forEach(p->MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(i111llil,p,2)));}
        private void i11111li(){PSU.getPlayersWithinRadius(i111ill1,i1111ill.x,i1111ill.y,i1111ill.z,1024.0).forEach(p->MinecraftForge.EVENT_BUS.post(new GoMSkillEvent(i111llil,p,3)));}
        private void i1111i1l(){i111i1l1[(int)(i111ill1.getGameTime()&31)]=i1111ill;}
        private void i1111i1i(){
            if(!i11lil1l()){discard();return;}
            try{
                byte[][]cur={
                        i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11ilill.class"),
                        i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l1iii.class"),
                        i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11l1.class"),
                        i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11li.class"),
                        i111l111("com/ExtraBossRush/GoM/Entity/GoMEntity$i11l11il.class")
                };
                byte[][]base={i11liiii,i11liiil,i11liil1,i11liili,i11liill};
                int d=0;
                for(int j=0;j<cur.length;j++)for(int i=0;i<32;i++)d|=(cur[j][i]^base[j][i]);
                if(d!=0){discard();return;}
            }catch(Exception e){discard();}
        }
        private void i111ii1i(){i111il1i.setProgress(i111lli1()/i11ii1ii(i111iill));}
        private void i1111ii1(){int i11i11l1=i11i1lil(i111iili);if(i11i11l1>0){i11i1l1l(i111iili,i11i11l1-1);if(i11i11l1==1)i111llil.entityData.set(i11li1ii,false);}}
        i11ilill(String id,ServerLevel lv,Vec3 pos){
            i111l1ll=id;i111ill1=lv;i1111ill=pos;
            i111il1i=new ServerBossEvent(
                    Component.literal(new String(new byte[]{
                            (byte)0xE9,(byte)0xAD,(byte)0x94,(byte)0xE8,(byte)0xA1,(byte)0x93,
                            (byte)0xE3,(byte)0x81,(byte)0xAE,(byte)0xE5,(byte)0xAE,(byte)0x88,
                            (byte)0xE8,(byte)0xAD,(byte)0xB7,(byte)0xE8,(byte)0x80,(byte)0x85
                    },StandardCharsets.UTF_8)),
                    BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
            i111il1i.setVisible(true);
            System.arraycopy(i111li11,0,i11i1111,0,8);System.arraycopy(i111l1li,0,i11i1lii,0,8);
            new SecureRandom().nextBytes(i111lll1);new SecureRandom().nextBytes(i11i111l);
            i111l1l1[0]=new SecureRandom().nextLong();
            i11i1l1l(i111iiii,0);i11i1l1l(i111illi,0);i11i1l1l(i111iiil,0);i11i1l1l(i111iili,0);i11ii1i1(i111ilil,0L);i11ii11i(i111iill,i111iil1);
            i111llii(i111iil1);
            Arrays.fill(i111i1l1,pos);
            i111llil=new GoMEntity(GoMEntities.MAGIC_GUARDIAN.get(),lv);
            i11111il(i111iil1);
            i111llil.setPos(pos.x,pos.y,pos.z);i111llil.i11il11i(id);
            i111llil.entityData.set(i11li1i1,1.0F);i111llil.entityData.set(i11li1ii,false);

        }
        String i111l1ll(){return i111l1ll;}
        ServerLevel getLevel(){return i111ill1;}
        boolean i11i11il(){return i111lli1()<=0;}
        void i11i11li(){
            if(i111lli1()<=0)return;
            i111il1i.setVisible(true);
            if((i11i1lil(i111iiii)&0xFF)==0)i1111l11(0);
            i1111l11(2);
            float i111ll1i=1.0F-(i111lli1()/i11ii1ii(i111iill));
            int i11ii1li=(int)Math.ceil(1.0F+i111ll1i*9.0F);
            for(int i=0;i<i11ii1li;i++)i11i1ll1();
            i1111l11(5);i1111l11(4);i1111l11(6);
            i1111iii();
        }
        private void i11i1ll1(){
            int i111i1il=i11i1lil(i111iiii);
            List<ServerPlayer>near=PSU.getPlayersWithinRadius(i111ill1,i1111ill.x,i1111ill.y,i1111ill.z,1024.0);
            if(near.isEmpty()&&!i111ill1.players().isEmpty())
                i1111ill=i111ill1.players().get(i111liil.nextInt(i111ill1.players().size())).position().add(0,10,0);
            if(i111i1il-i11i1lil(i111illi)>=100){i11i1l1l(i111illi,i111i1il);i1111l11(7);}
            if(i111i1il-i11i1lil(i111iiil)>=600){i11i1l1l(i111iiil,i111i1il);i1111l11(1);}
            i11i1l1l(i111iiii,i111i1il+1);
        }
        void i11iiiil(ServerPlayer sp){
            if(!i111l1il)return;i111l1il=false;
            long now=i111ill1.getGameTime();
            if(now-i11i1li1(i111ilil)<i111il1l)return;i11ii1i1(i111ilil,now);
            float i11i1lll=(float)sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float dmg=i11i1lll*0.2F;
            int i11ii1l1=net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,sp.getMainHandItem());
            if(i11ii1l1>0)dmg+=(i11ii1l1*0.5F+0.5F)*0.2F;
            dmg=Math.min(Math.max(dmg,0.0F),1000.0F);
            float nH=Math.max(0.0F,i111lli1()-dmg);
            i111llii(nH);i11111il(nH);
            i111llil.entityData.set(i11li1ii,true);i111llil.entityData.set(i11li1i1,nH/i11ii1ii(i111iill));
            if(nH<=0){
                if(i111illl<i111l11i)i111illl++;
                i111il1i.setProgress(0.0F);i111il1i.removeAllPlayers();i111l1i1.remove(i111llil.getId());return;
            }
            i11i1l1l(i111iili,20);
        }
        private void i1111iii(){
            i111llil.setPos(i1111ill.x,i1111ill.y,i1111ill.z);i1111l11(3);
            ClientboundAddEntityPacket i11iiii1=new ClientboundAddEntityPacket(i111llil);
            ClientboundSetEntityDataPacket i11iiili=new ClientboundSetEntityDataPacket(i111llil.getId(),i111llil.getEntityData().getNonDefaultValues());
            float i11iil11=i111lli1()/i11ii1ii(i111iill);boolean i11lil11=i111llil.getEntityData().get(i11li1ii);
            for(ServerPlayer p:i111ill1.players()){
                if(p.distanceToSqr(i1111ill)<i111ilii){
                    byte[]i11iii1i=i11iill1.get(p.getUUID()),i11iil1i=i11il111.get(p.getUUID()),i1111lii=i11iilli.get(p.getUUID());
                    AtomicLong i11iil1l=i11iilll.get(p.getUUID());byte[]i111i1ii=i11ilili.get(p.getUUID());
                    if(i11iii1i!=null&&i11iil1i!=null&&i1111lii!=null&&i11iil1l!=null&&i111i1ii!=null){
                        byte[]i11iilii=new byte[32];for(int i=0;i<32;i++)i11iilii[i]=(byte)(i11iii1i[i]^i11iil1i[i]^i1111lii[i]);
                        long seq=i11iil1l.getAndIncrement();
                        i11l1ll1.send(PacketDistributor.PLAYER.with(()->p),new i11l11li(i11l11li.build(i11iilii,i111llil.getId(),i111l1ll)));
                        p.connection.send(i11iiii1);p.connection.send(i11iiili);
                        i11l1ll1.send(PacketDistributor.PLAYER.with(()->p),new i11l11il(i11l11il.build(i11iilii,p.getUUID(),i111llil.getId(),i11iil11,i11lil11,i1111ill.x,i1111ill.y,i1111ill.z,i111i1ii,seq)));
                        i111il1i.addPlayer(p);Arrays.fill(i11iilii,(byte)0);
                    }else{p.connection.send(i11iiii1);p.connection.send(i11iiili);i111il1i.addPlayer(p);}
                }else{
                    p.connection.send(new ClientboundRemoveEntitiesPacket(i111llil.getId()));i111il1i.removePlayer(p);
                    byte[]i11iii1i=i11iill1.get(p.getUUID()),i11iil1i=i11il111.get(p.getUUID()),i1111lii=i11iilli.get(p.getUUID());
                    AtomicLong i11iil1l=i11iilll.get(p.getUUID());
                    if(i11iii1i!=null&&i11iil1i!=null&&i1111lii!=null&&i11iil1l!=null){
                        byte[]i11iilii=new byte[32];for(int i=0;i<32;i++)i11iilii[i]=(byte)(i11iii1i[i]^i11iil1i[i]^i1111lii[i]);
                        long seq=i11iil1l.getAndIncrement();
                        i11l1ll1.send(PacketDistributor.PLAYER.with(()->p),new i11l11il(i11l11il.remove(i11iilii,p.getUUID(),i111llil.getId(),seq)));
                        Arrays.fill(i11iilii,(byte)0);
                    }
                }
            }
            i111llil.getEntityData().packDirty();
        }
        void discard(){
            i111il1i.removeAllPlayers();int did=i111llil.getId();i111l1i1.remove(did);
            i111llil.i11il1i1();
            ClientboundRemoveEntitiesPacket rm=new ClientboundRemoveEntitiesPacket(did);
            for(ServerPlayer p:i111ill1.players()){
                p.connection.send(rm);
                byte[]i11iii1i=i11iill1.get(p.getUUID()),i11iil1i=i11il111.get(p.getUUID()),i1111lii=i11iilli.get(p.getUUID());
                AtomicLong i11iil1l=i11iilll.get(p.getUUID());
                if(i11iii1i!=null&&i11iil1i!=null&&i1111lii!=null&&i11iil1l!=null){
                    byte[]i11iilii=new byte[32];for(int i=0;i<32;i++)i11iilii[i]=(byte)(i11iii1i[i]^i11iil1i[i]^i1111lii[i]);
                    long seq=i11iil1l.getAndIncrement();
                    i11l1ll1.send(PacketDistributor.PLAYER.with(()->p),new i11l11li(i11l11li.revoke(i11iilii,did)));
                    i11l1ll1.send(PacketDistributor.PLAYER.with(()->p),new i11l11il(i11l11il.remove(i11iilii,p.getUUID(),did,seq)));
                    Arrays.fill(i11iilii,(byte)0);
                }
            }
        }
    }
    @Mod.EventBusSubscriber(modid=ExtraBossRush.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class i111ll1l{
        @SubscribeEvent
        public static void i11i1ill(EntityRenderersEvent.RegisterRenderers event){
            event.registerEntityRenderer(GoMEntities.MAGIC_GUARDIAN.get(),i111li1l::new);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class i111li1l extends MobRenderer<GoMEntity,HumanoidModel<GoMEntity>>{
        private static final ResourceLocation i11il1l1=new ResourceLocation(ExtraBossRush.MOD_ID,"textures/entity/magic_guardian.png");
        private static final Random i11ili11=new Random();
        public i111li1l(EntityRendererProvider.Context ctx){
            super(ctx,new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)),0.6F);
        }
        @Override public @NotNull ResourceLocation getTextureLocation(@NotNull GoMEntity e){return i11il1l1;}
        @Override
        public void render(GoMEntity entity,float yaw,float pt,@NotNull PoseStack ps,@NotNull MultiBufferSource buf,int light){
            super.render(entity,yaw,pt,ps,buf,light);
            if(entity.i11ili1l())i11i1li1(entity,ps,buf,pt,light,1F,0F,0F,0.2F,true);
            float ef=entity.i11ilii1();float off=ef*0.04F-0.02F;
            if(off>0.001F){
                Random r=new Random((long)entity.getId()*98765L+entity.tickCount*12345L);
                i11iiill(entity,ps,buf,pt,light,off,r,1F,0F,0F,ef*0.3F);
                i11iiill(entity,ps,buf,pt,light,off,r,0F,1F,1F,ef*0.3F);
            }
        }
        private void i11iiill(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float off,Random r,float rc,float gc,float bc,float a){
            ps.pushPose();float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1F,-1F,1F);ps.translate(0F,-1.501F,0F);
            ps.translate((i11ili11.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (i11ili11.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F,
                    (i11ili11.nextFloat()*2F-1F)*off+(r.nextFloat()-.5F)*.01F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(i11il1l1)),light,OverlayTexture.NO_OVERLAY,rc,gc,bc,a);
            ps.popPose();
        }
        private void i11i1li1(GoMEntity e,PoseStack ps,MultiBufferSource buf,float pt,int light,float r,float g,float b,float a,boolean hurt){
            ps.pushPose();float f=Mth.rotLerp(pt,e.yBodyRotO,e.yBodyRot);
            this.setupRotations(e,ps,(float)e.tickCount+pt,f,pt);
            ps.scale(-1.0002F,-1.0002F,1.0002F);ps.translate(0F,-1.501F,0F);
            this.model.renderToBuffer(ps,buf.getBuffer(RenderType.entityTranslucentEmissive(i11il1l1)),light,
                    OverlayTexture.pack(OverlayTexture.u(0F),OverlayTexture.v(hurt)),r,g,b,a);
            ps.popPose();
        }
    }
}