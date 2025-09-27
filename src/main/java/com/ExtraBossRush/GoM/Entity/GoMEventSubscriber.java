package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;

import com.ExtraBossRush.GoM.Support.Time.TimeChangeEvent;
import com.ExtraBossRush.GoM.Support.Time.TimeConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



/**
 * EntityAttributeCreationEvent でボスエンティティの属性を登録するクラス
 */
@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.MOD
)
public class GoMEventSubscriber {
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        // RegistryObject が実際に登録済みかをチェック
        GoMEntities.MAGIC_GUARDIAN.ifPresent(entityType -> {
            event.put(
                    entityType,
                    GoMEntity.createAttributes().build()
            );
        });
    }
    @SubscribeEvent
    public static void onExplosionDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypes.EXPLOSION)) return;  // isExplosion() → is(DamageTypes.EXPLOSION)

        player.hurtMarked = true;
        TimeConfig.Time = 0;
        player.invulnerableTime = TimeConfig.Time;
    }
    @SubscribeEvent
    public static void onTimeChange(TimeChangeEvent event) {
        System.out.println("無敵時間が " + event.getNewTime() + " tick に変更されました");
    }

}