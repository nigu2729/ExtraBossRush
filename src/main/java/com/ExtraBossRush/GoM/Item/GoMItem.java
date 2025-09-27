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
    // アイテム用の DeferredRegister
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExtraBossRush.MOD_ID);

    // スポーンエッグ登録
    public static final RegistryObject<ForgeSpawnEggItem> MAGIC_GUARDIAN_SPAWN_EGG =
            ITEMS.register("magic_guardian_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            GoMEntities.MAGIC_GUARDIAN,  // 登録済み EntityType
                            0x8833EE,                          // エッグの胴体色
                            0x442288,// エッグの顔色
                            new Item.Properties()
                    )
            );
    public static final RegistryObject<Item> EXPLOSION_STAFF = ITEMS.register("explosion_staff", // タイプミス修正: exposion → explosion
            () -> new ExplosionStaff(new Item.Properties() // Propertiesを追加（デフォルト設定）
                    .stacksTo(1) // スタック不可（杖らしい）
                    .durability(1000))); // 耐久値1000（オプション、壊れないなら.durability(0)）


    // モッド初期化時に呼び出す
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}