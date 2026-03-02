package com.ExtraBossRush.GoM.client;
import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.ARV2Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import java.io.*;
import java.lang.Math;
import java.util.*;
@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ARV2 {
    private static final List<ARV2> R_L = new ArrayList<>();
    private static final Map<ResourceLocation, ObjLoadResult> M_C = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> A_C = new HashMap<>(), T_C = new HashMap<>();
    private static float cFov = 70f;
    private static float cAspect = 1.777f;
    private static final Vector3d C_P = new Vector3d();
    private static final Vector3f C_R = new Vector3f();
    private static final Quaternionf C_Q = new Quaternionf();
    private static final float[] C_C = new float[3];
    @FunctionalInterface public interface DynamicVec3 { Vec3 get(float t); }
    @FunctionalInterface public interface DynamicFloat { float get(float t); }
    @FunctionalInterface public interface DynamicColor { float[] get(float t); }
    private final ObjLoadResult mD; private final RenderType rT; private final int mL;
    private int tE = 0; private final Vec3Anim pA, rA;
    private final MultiKeyAnim sA, aA; private final ColorAnim cA;
    private final boolean isB, isS; private final float bS, bL, bI;
    private ARV2(ObjLoadResult d, RenderType t, int m, Vec3Anim p, Vec3Anim r, MultiKeyAnim s, ColorAnim c, MultiKeyAnim a, boolean b, boolean s2, float bs, float bl, float bi) {
        mD = d; rT = t; mL = m; pA = p; rA = r; sA = s; cA = c; aA = a; isB = b; isS = s2; bS = bs; bL = bl; bI = Math.max(0.001f, bi);
    }
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || R_L.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance(); MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        Camera cam = mc.gameRenderer.getMainCamera(); PoseStack ps = e.getPoseStack();
        Map<RenderType, List<ARV2>> batches = new HashMap<>();
        for (ARV2 r : R_L) batches.computeIfAbsent(r.rT, k -> new ArrayList<>()).add(r);
        batches.forEach((type, list) -> {
            VertexConsumer vc = bs.getBuffer(type);
            for (ARV2 r : list) r.rb(ps, vc, cam, e.getPartialTick());
        });
        bs.endBatch();
    }
    private void rb(PoseStack ps, VertexConsumer vc, Camera cam, float pt) {
        float ct = (float) tE + pt, a = aA.get(ct); if (a <= 0.0001f) return;
        pA.fill(ct, C_P); float s = sA.get(ct), rad = mD.maxRadius() * s;
        if (isB || isS) rad += Math.min(bL, bS * ct);
        if (!iv(cam, C_P, rad)) return;
        Vec3 cp = cam.getPosition(); rA.fill(ct, C_R); cA.fill(ct, C_C);
        ps.pushPose();
        ps.translate(C_P.x - cp.x, C_P.y - cp.y, C_P.z - cp.z);
        ps.mulPose(C_Q.rotationYXZ(C_R.y * 0.01745f, C_R.x * 0.01745f, C_R.z * 0.01745f));
        if (isS) ps.scale(s, s, Math.min(bL, bS * ct) / bI);
        else ps.scale(s, s, s);
        Matrix4f m = ps.last().pose(); Matrix3f n = ps.last().normal();
        if (isB) {
            float l = Math.min(bL, bS * ct); int c = (int) Math.ceil(l / bI);
            for (int i = 0; i < c; i++) wm(m, n, vc, C_C, a, i * bI);
        } else wm(m, n, vc, C_C, a, 0);
        ps.popPose();
    }
    private void wm(Matrix4f m, Matrix3f n, VertexConsumer vc, float[] c, float a, float z) {
        for (RawVertex[] f : mD.faces) {
            if (f.length == 3) { av(vc, m, n, f[0], c, a, z); av(vc, m, n, f[1], c, a, z); av(vc, m, n, f[2], c, a, z); }
            else if (f.length == 4) {
                av(vc, m, n, f[0], c, a, z); av(vc, m, n, f[1], c, a, z); av(vc, m, n, f[2], c, a, z);
                av(vc, m, n, f[0], c, a, z); av(vc, m, n, f[2], c, a, z); av(vc, m, n, f[3], c, a, z);
            }
        }
    }
    private void av(VertexConsumer vc, Matrix4f m, Matrix3f n, RawVertex v, float[] c, float a, float z) {
        vc.vertex(m, v.p.x(), v.p.y(), v.p.z() + z).color(c[0], c[1], c[2], a).uv(v.u, v.v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(n, v.n.x(), v.n.y(), v.n.z()).endVertex();
    }
    private boolean iv(Camera c, Vector3d p, float r) {
        if (!ARV2Config.CLIENT_SPEC.isLoaded() || !ARV2Config.ENABLE_CULLING.get()) return true;
        Vec3 cp = c.getPosition(); double dx = p.x-cp.x, dy = p.y-cp.y, dz = p.z-cp.z, d2 = dx*dx+dy*dy+dz*dz;
        if (ARV2Config.ENABLE_DISTANCE_CULLING.get()) { long dist = ARV2Config.CULLING_DISTANCE.get(); if (d2 > (double)dist*dist) return false; }
        if (!ARV2Config.ENABLE_FRUSTUM_CULLING.get()) return true;
        if (d2 < r*r) return true;
        double dl = Math.sqrt(d2); Vector3f l = c.getLookVector();
        double vHalf = Math.toRadians(cFov * 0.5f);
        double hHalf = Math.atan(Math.tan(vHalf) * cAspect);
        double diagHalf = Math.sqrt(vHalf * vHalf + hHalf * hHalf);
        return (dx*l.x()+dy*l.y()+dz*l.z())/dl > Math.cos(diagHalf + Math.asin(Math.min(1.0, r/dl)));
    }
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e) { cFov = (float) e.getFOV(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) { if (e.phase == TickEvent.Phase.END) R_L.removeIf(r -> ++r.tE >= r.mL); }
    public static RenderType getAdd(ResourceLocation t) { return A_C.computeIfAbsent(t, l -> ct("arv2:add", l, () -> { RenderSystem.enableBlend(); RenderSystem.blendFunc(770, 1); })); }
    public static RenderType getTrans(ResourceLocation t) { return T_C.computeIfAbsent(t, l -> ct("arv2:trans", l, () -> { RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); })); }
    private static RenderType ct(String n, ResourceLocation t, Runnable s) {
        return RenderType.create(n, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.TRIANGLES, 256, false, false,
                RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(t, false, false))
                        .setTransparencyState(new RenderStateShard.TransparencyStateShard("transparency", s, RenderSystem::disableBlend))
                        .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515)).setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, true))
                        .setCullState(new RenderStateShard.CullStateShard(false)).setLightmapState(new RenderStateShard.LightmapStateShard(true)).createCompositeState(false));
    }
    public static ObjLoadResult loadObj(ResourceLocation l) {
        return M_C.computeIfAbsent(l, loc -> {
            List<RawVertex[]> fs = new ArrayList<>(); float d2 = 0;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(loc).get().open()))) {
                List<Vector3f> vs = new ArrayList<>(), ns = new ArrayList<>(); List<Vector2f> uvs = new ArrayList<>(); String ln;
                while ((ln = r.readLine()) != null) {
                    String[] p = ln.trim().split("\\s+");
                    if (p[0].equals("v")) { float x=f(p[1]), y=f(p[2]), z=f(p[3]); vs.add(new Vector3f(x,y,z)); d2=Math.max(d2,x*x+y*y+z*z); }
                    else if (p[0].equals("vt")) uvs.add(new Vector2f(f(p[1]), 1-f(p[2])));
                    else if (p[0].equals("vn")) ns.add(new Vector3f(f(p[1]),f(p[2]),f(p[3])));
                    else if (p[0].equals("f")) {
                        RawVertex[] f = new RawVertex[p.length-1];
                        for (int j=0; j<f.length; j++) {
                            String[] s = p[j+1].split("/"); int vi=Integer.parseInt(s[0])-1;
                            float u=s.length>1&&!s[1].isEmpty()?uvs.get(Integer.parseInt(s[1])-1).x:0, v=s.length>1&&!s[1].isEmpty()?uvs.get(Integer.parseInt(s[1])-1).y:0;
                            f[j]=new RawVertex(vs.get(vi), u, v, s.length>2?ns.get(Integer.parseInt(s[2])-1):new Vector3f(0,1,0));
                        }
                        fs.add(f);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); } return new ObjLoadResult(fs, (float)Math.sqrt(d2));
        });
    }
    private static float f(String s) { return Float.parseFloat(s); }
    public static class Builder {
        private final ResourceLocation m; private RenderType t; private int mL = 200;
        private boolean isB, isS; private float bS=32, bL=1024, bI=1, pX[], pY[], pZ[], rP[], rY[], rR[], sV[], cR[], cG[], cB[], aV[];
        private int pT[], rT[], sT[], cT[], aT[]; private DynamicVec3 dP, dR; private DynamicFloat dS, dA; private DynamicColor dC;
        public Builder(ResourceLocation m, ResourceLocation tex, Vec3 start) {
            this.m = m; this.t = getTrans(tex);
            setPos(start.x, start.y, start.z); setRot(0, 0, 0); setSize(1); setRGB(1, 1, 1); setAlpha(1);
        }
        public Builder setRenderType(RenderType rt) { this.t = rt; return this; }
        public Builder asBeam(float s, float l, float i) { this.isB = true; this.bS = s; this.bL = l; this.bI = i; return this; }
        public Builder asBeam2(float s, float l, float i) { this.isS = true; this.bS = s; this.bL = l; this.bI = i; return this; }
        public Builder setMaxLife(int m) { this.mL = m; return this; }
        public Builder setPosAnim(float[] x, float[] y, float[] z, int[] t) { pX=x; pY=y; pZ=z; pT=t; dP=null; return this; }
        public Builder setRotAnim(float[] p, float[] y, float[] r, int[] t) { rP=p; rY=y; rR=r; rT=t; dR=null; return this; }
        public Builder setSizeAnim(float[] v, int[] t) { sV=v; sT=t; dS=null; return this; }
        public Builder setRGBAnim(float[] r, float[] g, float[] b, int[] t) { cR=r; cG=g; cB=b; cT=t; dC=null; return this; }
        public Builder setAlphaAnim(float[] v, int[] t) { aV=v; aT=t; dA=null; return this; }
        public Builder setPos(double x, double y, double z) { return setPosAnim(new float[]{(float)x}, new float[]{(float)y}, new float[]{(float)z}, new int[]{0}); }
        public Builder setRot(double p, double y, double r) { return setRotAnim(new float[]{(float)p}, new float[]{(float)y}, new float[]{(float)r}, new int[]{0}); }
        public Builder setSize(float s) { return setSizeAnim(new float[]{s}, new int[]{0}); }
        public Builder setRGB(float r, float g, float b) { return setRGBAnim(new float[]{r}, new float[]{g}, new float[]{b}, new int[]{0}); }
        public Builder setAlpha(float a) { return setAlphaAnim(new float[]{a}, new int[]{0}); }
        public Builder setDynamicPos(DynamicVec3 v) { dP=v; return this; }
        public Builder setDynamicRot(DynamicVec3 v) { dR=v; return this; }
        public Builder setDynamicSize(DynamicFloat v) { dS=v; return this; }
        public Builder setDynamicRGB(DynamicColor v) { dC=v; return this; }
        public Builder setDynamicAlpha(DynamicFloat v) { dA=v; return this; }
        public void spawn() {
            R_L.add(new ARV2(loadObj(m), t, mL, dP!=null ? new Vec3Anim(dP) : new Vec3Anim(pX, pY, pZ, pT),
                    dR!=null ? new Vec3Anim(dR) : new Vec3Anim(rP, rY, rR, rT), dS!=null ? new MultiKeyAnim(dS) : new MultiKeyAnim(sV, sT),
                    dC!=null ? new ColorAnim(dC) : new ColorAnim(cR, cG, cB, cT), dA!=null ? new MultiKeyAnim(dA) : new MultiKeyAnim(aV, aT), isB, isS, bS, bL, bI));
        }
    }
    private static class MultiKeyAnim {
        private final float[] v; private final int[] t; private final DynamicFloat d;
        MultiKeyAnim(float[] v, int[] t) { this.v = v; this.t = t; this.d = null; }
        MultiKeyAnim(DynamicFloat d) { this.v = null; this.t = null; this.d = d; }
        float get(float tick) {
            if (d != null) return d.get(tick);
            if (v.length == 1 || tick <= t[0]) return v[0];
            if (tick >= t[t.length - 1]) return v[v.length - 1];
            for (int i = 0; i < t.length - 1; i++)
                if (tick < t[i + 1]) return v[i] + (v[i + 1] - v[i]) * ((tick - t[i]) / (float)(t[i + 1] - t[i]));
            return v[v.length - 1];
        }
    }
    private static class Vec3Anim {
        private final MultiKeyAnim x, y, z; private final DynamicVec3 d;
        Vec3Anim(float[] xv, float[] yv, float[] zv, int[] t) { x = new MultiKeyAnim(xv, t); y = new MultiKeyAnim(yv, t); z = new MultiKeyAnim(zv, t); d = null; }
        Vec3Anim(DynamicVec3 d) { this.x = null; this.y = null; this.z = null; this.d = d; }
        void fill(float t, Vector3d o) { if (d != null) { Vec3 v = d.get(t); o.set(v.x, v.y, v.z); } else o.set(x.get(t), y.get(t), z.get(t)); }
        void fill(float t, Vector3f o) { if (d != null) { Vec3 v = d.get(t); o.set((float)v.x, (float)v.y, (float)v.z); } else o.set(x.get(t), y.get(t), z.get(t)); }
    }
    private static class ColorAnim {
        private final MultiKeyAnim r, g, b; private final DynamicColor d;
        ColorAnim(float[] rv, float[] gv, float[] bv, int[] t) { r = new MultiKeyAnim(rv, t); g = new MultiKeyAnim(gv, t); b = new MultiKeyAnim(bv, t); d = null; }
        ColorAnim(DynamicColor d) { this.r = null; this.g = null; this.b = null; this.d = d; }
        void fill(float t, float[] o) { if (d != null) { float[] rs = d.get(t); if (rs != null) { o[0] = rs[0]; o[1] = rs[1]; o[2] = rs[2]; } } else { o[0] = r.get(t); o[1] = g.get(t); o[2] = b.get(t); } }
    }
    private record RawVertex(Vector3f p, float u, float v, Vector3f n) {}
    public record ObjLoadResult(List<RawVertex[]> faces, float maxRadius) {}
}