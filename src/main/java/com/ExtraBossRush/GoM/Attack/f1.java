package com.ExtraBossRush.GoM.Attack;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class f1 {

    // プレイヤーごとのタイマー管理
    private static final Map<UUID, PlayerFloatTimer> playerTimers = new HashMap<>();

    // PlayerFloatTimerクラス
    public static class PlayerFloatTimer {
        private int ticks = 0;

        public int getTicks() {
            return ticks;
        }

        public void setTicks(int ticks) {
            this.ticks = ticks;
        }

        public void incrementTicks() {
            ticks++;
        }
    }

    // プレイヤーのタイマーを取得
    public PlayerFloatTimer getTimer(Player player) {
        return playerTimers.computeIfAbsent(player.getUUID(), k -> new PlayerFloatTimer());
    }

    // PlayerTickEventのハンドラ内で呼び出す（サーバー側）
    public void checkAndDetonatePlayer(Player player) {
        if (player.level().isClientSide()) return; // クライアント側では実行しない

        // プレイヤーのタイマーを取得
        PlayerFloatTimer timer = getTimer(player);

        // プレイヤーのゲームモードを取得
        GameType gameMode = player instanceof ServerPlayer serverPlayer ? serverPlayer.gameMode.getGameModeForPlayer() : GameType.SURVIVAL;

        // サバイバルモードまたはアドベンチャーモードでのみ処理を実行
        if (gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE) {

            // 浮遊状態の判定とタイマーの更新
            if (player.onGround()) {
                timer.setTicks(0);
            } else {
                timer.incrementTicks();

                if (timer.getTicks() >= 300) { // 300ティック = 15秒

                    // 座標を変数x, y, zとして設定
                    double x = player.getX();
                    double y = player.getY();
                    double z = player.getZ();

                    Level level = player.level();

                    // 1ティック内で10回爆発を発生させる
                    for (int i = 0; i < 10; i++) {
                        // 爆風なしの爆発（ブロックを破壊しない）
                        level.explode(null, x, y, z, 25.0F, Level.ExplosionInteraction.NONE);

                        // 爆発パーティクルを追加
                        level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
                    }

                    // 検出後、タイマーをリセット
                    timer.setTicks(0);
                }
            }
        } else {
            // 対象外のゲームモード（クリエイティブやスペクテイター）の場合、タイマーをリセット
            timer.setTicks(0);
        }
    }
}