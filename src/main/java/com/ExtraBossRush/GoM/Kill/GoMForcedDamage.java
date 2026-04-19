package com.ExtraBossRush.GoM.Kill;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * GoM ビーム専用の「防げない20ダメージ」マーカー。
 *
 * GoMLethalDamage と違い、hurt() をキャンセルしない。
 * MixinLivingEntityHurt が HEAD で invulnerableTime を 0 に強制した後、
 * hurt() をそのまま通過させるため:
 *   - 赤いダメージ表示
 *   - 画面揺れ (攻撃アニメーション)
 *   - ノックバック
 * などのアニメーションが正常に発生する。
 *
 * hurt() 通過後、呼び出し側がバックアップとして setHealth() で
 * 直接 -20 減算も行う。
 */
public final class GoMForcedDamage {

    public static final String MSG_ID = "\u0000gom_forced\u0000";

    public static DamageSource create(Level level) {
        return new GoMForcedDamageSource(level.damageSources().magic().typeHolder());
    }

    public static boolean isGoMForced(DamageSource source) {
        return source instanceof GoMForcedDamageSource;
    }

    private static final class GoMForcedDamageSource extends DamageSource {
        GoMForcedDamageSource(Holder<DamageType> holder) {
            super(holder);
        }
        @Override public String toString() { return "GoMForcedDamage"; }
    }
}