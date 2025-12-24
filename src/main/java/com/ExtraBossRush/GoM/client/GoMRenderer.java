package com.ExtraBossRush.GoM.client;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Entity.GoMEntity;
import com.ExtraBossRush.GoM.Entity.GoMEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GoMRenderer {
    @SubscribeEvent
    public static void registerEntityRenderers(RegisterRenderers event) {
        if (GoMEntities.MAGIC_GUARDIAN.isPresent()) {
            event.registerEntityRenderer(
                    GoMEntities.MAGIC_GUARDIAN.get(),
                    MagicGuardianRenderer::new
            );
        }
    }

    // 独自レンダラー
    public static class MagicGuardianRenderer
            extends MobRenderer<GoMEntity, HumanoidModel<GoMEntity>> {
        public MagicGuardianRenderer(EntityRendererProvider.Context ctx) {
            super(
                    ctx,
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)),
                    0.6F  // 影の大きさ（エンティティ半径）
            );
        }

        @Override
        public ResourceLocation getTextureLocation(GoMEntity entity) {
            // assets/extrabossrush/textures/entity/magic_guardian.png を用意する
            return new ResourceLocation(ExtraBossRush.MOD_ID, "textures/entity/magic_guardian.png");
        }
    }
}