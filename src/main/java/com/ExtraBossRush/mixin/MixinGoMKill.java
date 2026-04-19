package com.ExtraBossRush.mixin;

import com.ExtraBossRush.GoM.Kill.IGoMKillable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinGoMKill implements IGoMKillable {

    @Unique
    private boolean gom_deathFlag = false;

    @Override
    public void gom_setDeathFlag(boolean flag) { this.gom_deathFlag = flag; }

    @Override
    public boolean gom_getDeathFlag() { return gom_deathFlag; }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void gom_clearMitigation(
            DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!gom_deathFlag) return;
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.level().isClientSide()) return;

        self.invulnerableTime = 0;
        self.setAbsorptionAmount(0.0f);
        gom_deathFlag = false;
    }

    @ModifyVariable(
            method = "hurt",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float gom_maximizeDamage(float amount) {
        if (!gom_deathFlag) return amount;
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.level().isClientSide()) return amount;
        return Float.MAX_VALUE;
    }
}