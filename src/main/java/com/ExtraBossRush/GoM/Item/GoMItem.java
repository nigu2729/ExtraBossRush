package com.ExtraBossRush.GoM.Item;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Attack.ExplosionStaff;
import com.ExtraBossRush.GoM.Entity.GoMEntities;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GoMItem {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExtraBossRush.MOD_ID);
    public static final RegistryObject<ForgeSpawnEggItem> MAGIC_GUARDIAN_SPAWN_EGG =
            ITEMS.register("magic_guardian_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            GoMEntities.MAGIC_GUARDIAN,
                            0x8833EE,
                            0x442288,
                            new Item.Properties()
                    )
            );
    public static final RegistryObject<Item> EXPLOSION_STAFF =
            ITEMS.register("gom_staff",
                    () -> new ExplosionStaff(
                            new Item.Properties()
                            .stacksTo(1)
                            .durability(1000)
                    )
            );
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}