package com.ExtraBossRush.GoM.Entity;

import com.ExtraBossRush.ExtraBossRush;
import com.ExtraBossRush.GoM.Support.Time.TimeChangeEvent;
import com.ExtraBossRush.GoM.Support.Time.TimeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 改良版: 外部から無敵停止を要求できる API を提供し、UUIDごとに残り時間を管理する。
 */
@Mod.EventBusSubscriber(
        modid = ExtraBossRush.MOD_ID,
        bus   = Mod.EventBusSubscriber.Bus.FORGE
)
public class GoMEventSubscriber {

    // UUID -> 残り停止時間（tick単位）
    private static final ConcurrentMap<UUID, Integer> resetInvRemainingTicks = new ConcurrentHashMap<>();

    // ---------- 外部から呼べる API ----------

    /**
     * 指定プレイヤーに対して、無敵時間を seconds 秒間 0 にする処理を追加する。
     * 既に停止中なら残りに加算する（加算方式）。
     */
    public static void requestResetInvulnerableTicks(ServerPlayer player, int ticks) {
        if (player == null || ticks <= 0) return;
        UUID id = player.getUUID();
        resetInvRemainingTicks.merge(id, ticks, Integer::sum);
        player.hurtMarked = true;
        player.invulnerableTime = 0;
        player.getPersistentData().putBoolean("ResetInvTime", true);
        player.getPersistentData().putInt("ResetInvTimeTick", resetInvRemainingTicks.get(id));
    }

    public static void requestResetInvulnerableTicks(UUID playerUuid, int ticks, MinecraftServer server) {
        if (playerUuid == null || server == null || ticks <= 0) return;
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) return;
        requestResetInvulnerableTicks(player, ticks);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        UUID id = player.getUUID();

        Integer remaining = resetInvRemainingTicks.get(id);
        if (remaining != null && remaining > 0) {
            // 無敵停止中：プレイヤーの invulnerableTime を常に 0 に維持して残りをデクリメント
            player.invulnerableTime = 0;
            remaining = remaining - 1;
            if (remaining <= 0) {
                // 終了処理: 通常の無敵時間に戻す（20tick）
                player.invulnerableTime = 20;
                resetInvRemainingTicks.remove(id);
                player.getPersistentData().remove("ResetInvTime");
                player.getPersistentData().remove("ResetInvTimeTick");
            } else {
                // 更新
                resetInvRemainingTicks.put(id, remaining);
                player.getPersistentData().putBoolean("ResetInvTime", true);
                player.getPersistentData().putInt("ResetInvTimeTick", remaining);
            }
        } else {
            // mapに存在しないか0のときは何もしない
            // （必要であればここで persistent 側のクリーンアップも行う）
            if (player.getPersistentData().getBoolean("ResetInvTime")) {
                player.getPersistentData().remove("ResetInvTime");
                player.getPersistentData().remove("ResetInvTimeTick");
            }
        }
    }

    @SubscribeEvent
    public static void onTimeChange(TimeChangeEvent event) {
        System.out.println("無敵時間が " + event.getNewTime() + " tick に変更されました");
    }

    // ---------- ユーティリティ ----------

    /**
     * 指定UUIDの残り停止時間（tick単位）を取得する。指定が無ければ0を返す。
     */
    public static int getRemainingResetTicks(UUID uuid) {
        return resetInvRemainingTicks.getOrDefault(uuid, 0);
    }

    /**
     * 指定UUIDの残り停止時間（秒単位）を取得する。小数切り捨て。
     */
    public static int getRemainingResetSeconds(UUID uuid) {
        return getRemainingResetTicks(uuid) / 20;
    }
}