package com.ExtraBossRush.GoM.Item;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Attack.ExplosionStaff;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GoMItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExtraBossRush.MOD_ID);
    public static final RegistryObject<Item> MAGIC_GUARDIAN_EGG =
            ITEMS.register("magic_guardian_egg",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(64)
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