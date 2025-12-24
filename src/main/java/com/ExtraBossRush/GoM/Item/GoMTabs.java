package com.ExtraBossRush.GoM.Item;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Item.GoMItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class GoMTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExtraBossRush.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GOM_TAB = TABS.register("gom_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.extrabossrush.gom"))
                    .icon(() -> new ItemStack(GoMItem.EXPLOSION_STAFF.get()))
                    .displayItems((params, output) -> {
                        output.accept(GoMItem.MAGIC_GUARDIAN_SPAWN_EGG.get());
                        output.accept(GoMItem.EXPLOSION_STAFF.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}