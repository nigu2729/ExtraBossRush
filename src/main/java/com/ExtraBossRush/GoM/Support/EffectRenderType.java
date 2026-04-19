package com.ExtraBossRush.GoM.Support;

import com.ExtraBossRush.GoM.client.ARV2;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;

/**
 * EffectDef 用の RenderType 定義ラッパー
 * ネットワーク同期用に ID 管理を行う
 */
public enum EffectRenderType {
    // ID 0: 標準透過（デフォルト）
    TRANSLUCENT(0, tex -> ARV2.getTrans(tex)),

    // ID 1: 加算合成（発光・レーザー用）
    ADDITIVE(1, tex -> ARV2.getAdd(tex)),

    // ID 2: カットアウト（α テスト・ドット絵用）
    CUTOUT(2, tex -> RenderType.entityCutoutNoCull(tex));

    public final int id;
    @OnlyIn(Dist.CLIENT)
    public final Function<ResourceLocation, RenderType> factory;

    EffectRenderType(int id, Function<ResourceLocation, RenderType> factory) {
        this.id = id;
        this.factory = factory;
    }

    // ID から列挙型を取得（デコード用）
    public static EffectRenderType fromId(int id) {
        for (EffectRenderType t : values()) {
            if (t.id == id) return t;
        }
        return TRANSLUCENT; // 未知の ID ならデフォルトにフォールバック
    }
}