package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GoMEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ExtraBossRush.MOD_ID);

    public static final RegistryObject<EntityType<GoMEntity>> MAGIC_GUARDIAN =
            ENTITIES.register("magic_guardian",
                    () -> EntityType.Builder.of(GoMEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .build(new ResourceLocation(ExtraBossRush.MOD_ID, "magic_guardian").toString())
            );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}