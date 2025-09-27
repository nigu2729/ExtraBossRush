package com.ExtraBossRush.GoM.Attack;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion; // Explosionクラス
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext; // phys → levelパッケージ
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;

public class ExplosionStaff extends Item {
    public ExplosionStaff(Properties properties) {
        super(properties);
    }

    private int holdTime = 0; // 右クリック持続カウンタ（10秒=200ティック）
    private boolean isHolding = false;
    private BlockPos targetPos; // ブロックヒット位置を保持

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { // 競合解決: 戻り値はInteractionResult (ItemStackなし)
        ItemStack stack = player.getItemInHand(hand);

        if (!isHolding) {
            // 右クリック開始: 持続開始とClipContextでブロック位置設定
            isHolding = true;
            holdTime = 0;

            // ClipContextでブロックヒット位置を取得（目線方向の最大距離、例: 50ブロック）
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getLookAngle();
            Vec3 endPos = eyePos.add(lookVec.scale(100.0)); // 最大50ブロック先まで
            BlockHitResult clipResult = level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (clipResult.getType() == HitResult.Type.BLOCK) {
                targetPos = clipResult.getBlockPos(); // ヒットしたブロックの位置
                // オプション: ブロックの表面に調整（double直接でBlockPosコンストラクタOK、int変換不要）
                Vec3 hitVec = clipResult.getLocation();
                targetPos = BlockPos.containing(hitVec.x, hitVec.y, hitVec.z);
            } else {
                // ヒットなし: デフォルトでendPosを使う（containingでdoubleからBlockPos）
                targetPos = BlockPos.containing(endPos);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHULKER_BOX_CLOSE, SoundSource.PLAYERS, 1.0F, 1.0F); // チャージ音（代用）
            return InteractionResultHolder.success(stack);
        } else {
            // 持続中: カウンタ更新（useが毎ティック呼ばれないので、inventoryTickで管理）
            return InteractionResultHolder.pass(stack);
        }
    }

    private void performExplosion(Level level, Player player) {
        if (targetPos == null || level.isClientSide()) return; // クライアント側スキップ

        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY() + 0.5;
        double z = targetPos.getZ() + 0.5;

        // explode呼び出し（シグネチャ完全一致）
        Explosion explosion = level.explode((Entity) null, x, y, z, 25.0F, Level.ExplosionInteraction.TNT);

        if (explosion != null) {
            // 爆発を最終化（これでダメージ/ノックバック/エフェクト確定）
            explosion.finalizeExplosion(true);
        }

        // オプション: 追加エフェクト
        level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
    }

    // 右クリック離した時の処理
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (entity instanceof Player) {
            isHolding = false;
            holdTime = 0;
        }
    }

    // 持続管理（毎ティックカウンタ更新）
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (selected && entity instanceof Player player && isHolding) {
            holdTime++;
            if (holdTime >= 200) { // 10秒=200ティックで発動
                performExplosion(level, player);
                isHolding = false;
                holdTime = 0;
            }
        }
    }
}