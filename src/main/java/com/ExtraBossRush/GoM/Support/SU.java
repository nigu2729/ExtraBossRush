package com.ExtraBossRush.GoM.Support;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Collectors;

public class SU {
    /**
     * 指定座標を中心に、半径 Er ブロック内の「全エンティティ」を取得する
     */
    public static List<Entity> getEntitiesWithinRadius(
            ServerLevel world,
            double x, double y, double z,
            double Er
    ) {
        // 1. AABBで検索範囲を作成
        AABB box = new AABB(
                x - Er, y - Er, z - Er,
                x + Er, y + Er, z + Er
        );

        // 2. AABB内の全エンティティを取得
        // 第1引数は除外するエンティティ（nullでOK）、第2引数は範囲
        List<Entity> list = world.getEntities((Entity)null, box);

        // 3. 距離判定で「球状」に絞り込む（高精度）
        Vec3 center = new Vec3(x, y, z);
        double ErSq = Er * Er; // 半径の2乗

        return list.stream()
                .filter(e -> {
                    // 足元ではなく体の中心で判定
                    Vec3 entityCenter = e.position().add(0, e.getBbHeight() * 0.5, 0);
                    return entityCenter.distanceToSqr(center) <= ErSq;
                })
                .collect(Collectors.toList());
    }
}
