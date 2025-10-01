package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Attack.f1;
import com.ExtraBossRush.GoM.Support.Time.TimeChangeEvent;
import com.ExtraBossRush.GoM.Support.Time.TimeConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * EntityAttributeCreationEvent でボスエンティティの属性を登録するクラス
 */
@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.FORGE
)
public class GoMEventSubscriber {
    @SubscribeEvent
    public static void onExplosionDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypes.EXPLOSION)) return;  // isExplosion() → is(DamageTypes.EXPLOSION)

        player.hurtMarked = true;
        int Time = 0;
        player.invulnerableTime = Time;
        player.getPersistentData().putBoolean("ResetInvTime", true);
        player.getPersistentData().putInt("ResetInvTimeTick", 20);
    }
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        if (player.getPersistentData().getBoolean("ResetInvTime")) {
            int tick = player.getPersistentData().getInt("ResetInvTimeTick");
            tick++;

            if (tick >= TimeConfig.TimeLong) {
                player.invulnerableTime = 20; // 通常の無敵時間に戻す
                player.getPersistentData().remove("ResetInvTime");
                player.getPersistentData().remove("ResetInvTimeTick");
            } else {
                player.getPersistentData().putInt("ResetInvTimeTick", tick);
            }
        }
    }
    @SubscribeEvent
    public static void onTimeChange(TimeChangeEvent event) {
        System.out.println("無敵時間が " + event.getNewTime() + " tick に変更されました");
    }
}