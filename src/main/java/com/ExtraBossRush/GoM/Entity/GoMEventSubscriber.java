package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
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
}