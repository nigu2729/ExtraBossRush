package com.ExtraBossRush.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin(Level.class)
public class LevelMixin {
    @Unique
    private static boolean extrabossrush$isCustomEntity(Entity entity) {
        if (entity == null) return false;
        ResourceKey<EntityType<?>> key = entity.getType().builtInRegistryHolder().key();
        return key != null && key.location().getNamespace().equals("extrabossrush");
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("TAIL"), cancellable = true)
    private void filterCustomEntities(Entity center, AABB area, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        List<Entity> originalList = cir.getReturnValue();
        if (originalList != null && !originalList.isEmpty()) {
            List<Entity> filteredList = originalList.stream()
                    .filter(entity -> !extrabossrush$isCustomEntity(entity))
                    .collect(Collectors.toList());
            cir.setReturnValue(filteredList);
        }
    }
}