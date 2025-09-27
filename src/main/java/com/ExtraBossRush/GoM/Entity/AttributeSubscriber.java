package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.MOD
)

public class AttributeSubscriber {
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