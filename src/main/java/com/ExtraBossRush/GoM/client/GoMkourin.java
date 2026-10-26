/*package com.ExtraBossRush.GoM.client;

import com.ExtraBossRush.ExtraBossRush;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GoMkourin {
    private static final ResourceLocation modelLoc = new ResourceLocation(ExtraBossRush.MOD_ID, "models/block/square.obj");
    private static final ResourceLocation texLoc = new ResourceLocation(ExtraBossRush.MOD_ID, "textures/block/mhz4-1whi.png");
    @SubscribeEvent
    public static void onClientSetUpEffect(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            new ARV2.Builder(modelLoc, texLoc, player.position())
                    .setMaxLife(2000000000)
                    .setSize(5.0f)
                    .setDynamicPos(time -> {
                        float partialTick = time % 1.0f;
                        Vec3 lerpedPos = player.getPosition(partialTick);
                        net.minecraft.world.phys.Vec3 lookVec = player.getViewVector(partialTick);
                        double distanceBehind = 1.5;
                        double heightOffset = 1.0;
                        double finalX = lerpedPos.x - (lookVec.x * distanceBehind);
                        double finalY = lerpedPos.y + heightOffset;
                        double finalZ = lerpedPos.z - (lookVec.z * distanceBehind);
                        return new Vec3(finalX, finalY, finalZ);
                    })
                    .setDynamicRot(time -> {
                        float partialTick = time % 1.0f;
                        net.minecraft.world.phys.Vec3 lookVec = player.getViewVector(partialTick);
                        float yaw = (float) Math.toDegrees(Math.atan2(lookVec.x, lookVec.z));
                        float spinSpeed = 5.0f;
                        float spinAngle = time * spinSpeed;
                        return new Vec3(0.0, yaw, spinAngle);
                    })
                    .spawn();
        }
    }
}*/