package com.ExtraBossRush;

import com.ExtraBossRush.GoM.Entity.GoMEntities;
import com.ExtraBossRush.GoM.Item.GoMItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ExtraBossRush.MOD_ID)
public class ExtraBossRush {
    public static final String MOD_ID = "extrabossrush";
    public ExtraBossRush() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        GoMEntities.register(bus);
        GoMItem.register(bus);
    }
}