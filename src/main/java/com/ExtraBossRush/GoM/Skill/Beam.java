package com.ExtraBossRush.GoM.Skill;

import com.ExtraBossRush.GoM.Support.PSU;
import com.ExtraBossRush.GoM.Support.LU;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;
import java.util.List;

public class Beam extends Entity {
    // どのプレイヤーが生成したか を保持するフィールドは削除

    /**
     * Boss（魔術の守護者）から spawnEntity を呼ぶ際に使うコンストラクタ
     */
    public Beam(EntityType<? extends Beam> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /**
     * ワールド追加時などに呼び出して、
     * 半径100内のプレイヤーを探しビームの向きを合わせる
     */
    public void Beam() {
        if (!(this.level() instanceof ServerLevel world)) return;

        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();
        double radius = 100.0;

        // 範囲内のプレイヤーを取得
        List<ServerPlayer> nearby = PSU.getPlayersWithinRadius(world, cx, cy, cz, radius);
        if (!nearby.isEmpty()) {
            // 最も近いプレイヤーをターゲットに選択
            ServerPlayer target = nearby.stream()
                    .min(Comparator.comparingDouble(p -> this.distanceToSqr(p)))
                    .orElse(nearby.get(0));

            // 目線高さの半分あたりを狙う例
            double ty = target.getY() + target.getEyeHeight() * 0.5;
            float[] angles = LU.calculateLookAt(
                    cx, cy, cz,
                    target.getX(), ty, target.getZ()
            );

            // Setter メソッドでエンティティの向きをセット
            this.setYRot(angles[0]);
            this.setXRot(angles[1]);
            this.setYHeadRot(angles[0]);
        }
    }
}