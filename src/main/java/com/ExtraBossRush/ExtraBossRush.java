package com.ExtraBossRush;

import com.ExtraBossRush.GoM.ARV2Config;
import com.ExtraBossRush.GoM.Entity.GoMEntities;
import com.ExtraBossRush.GoM.Entity.SkillEventHandler;
import com.ExtraBossRush.GoM.Item.GoMItem;
import com.ExtraBossRush.GoM.Item.GoMTabs;
//import com.ExtraBossRush.GoM.client.RandomKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod(ExtraBossRush.MOD_ID)
public class ExtraBossRush {
    public static final String MOD_ID = "extrabossrush";

    public ExtraBossRush() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ARV2Config.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ARV2Config.CLIENT_SPEC);
        GoMEntities.register(bus);
        GoMItem.register(bus);
        GoMTabs.register(bus);
        SkillEventHandler.register();
    }
}