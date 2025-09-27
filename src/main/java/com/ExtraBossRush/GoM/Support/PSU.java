package com.ExtraBossRush.GoM.Support;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.stream.Collectors;

/**
 * プレイヤー検索ユーティリティ
 */
public class PSU {
    /**
     * 指定座標 (x, y, z) を中心に半径 radius ブロック内のプレイヤーを取得する
     *
     * @param world   サーバーワールド
     * @param x       検索中心 X
     * @param y       検索中心 Y
     * @param z       検索中心 Z
     * @param radius  半径（ブロック）
     * @return        範囲内の ServerPlayer リスト
     */
    public static List<ServerPlayer> getPlayersWithinRadius(
            ServerLevel world,
            double x, double y, double z,
            double radius
    ) {
        // AABBで立方体範囲をまず絞り込む
        AABB box = new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );
        // world.players() はワールド上の全プレイヤーを返す
        return world.players()
                .stream()
                // BlockPos.closerThan は平方根不要で速い距離判定
                .filter(player ->
                        player.blockPosition()
                                .closerThan(BlockPos.containing(x, y, z), radius)
                )
                .collect(Collectors.toList());
    }
}
