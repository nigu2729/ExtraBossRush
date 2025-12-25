package com.ExtraBossRush.GoM.client;

import com.ExtraBossRush.ExtraBossRush;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class RandomKey {
    public RandomKey() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }
    private void clientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeyShuffler());
    }
    public static class KeyShuffler {
        private static final boolean INCLUDE_MODIFIERS = false; // 修飾キーを候補に含めるか
        private static final int MIN_TICKS = 1200;
        private static final int MAX_TICKS = 6000;
        private static long tickCounter = 0L;
        private static long nextRandomizeTick = -1L;
        private static boolean hasJoinedWorld = false;
        private static final Random RAND = new Random();
        private static final List<InputConstants.Key> POSSIBLE_SINGLE = new ArrayList<>();
        static {
            // letters a - z
            for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) {
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(k));
            }
            // digits 0 - 9
            for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) {
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(k));
            }
            // punctuation: , . / ; ' [ ] - = \ `
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_COMMA));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PERIOD));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SLASH));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SEMICOLON));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_APOSTROPHE));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_MINUS));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_EQUAL));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_BACKSLASH));
            POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_GRAVE_ACCENT));

            if (INCLUDE_MODIFIERS) {
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SHIFT));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_TAB));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_BACKSPACE));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_ENTER));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_CONTROL));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_ALT));
                POSSIBLE_SINGLE.add(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_MENU));
            }
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            tickCounter++;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;

            // ワールド入室検出
            if (!hasJoinedWorld && mc.level != null) {
                hasJoinedWorld = true;
                performRandomizeNow();
                scheduleNextRandomizeTicks();
                return;
            }

            // ワールド離脱検出
            if (hasJoinedWorld && mc.level == null) {
                hasJoinedWorld = false;
                nextRandomizeTick = -1L;
                return;
            }

            // 定期実行
            if (nextRandomizeTick >= 0 && tickCounter >= nextRandomizeTick) {
                performRandomizeNow();
                scheduleNextRandomizeTicks();
            }
        }

        private static void scheduleNextRandomizeTicks() {
            int delay = MIN_TICKS + RAND.nextInt(MAX_TICKS - MIN_TICKS + 1);
            nextRandomizeTick = tickCounter + delay;
        }

        public static void performRandomizeNow() {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.options == null) return;
            Options options = mc.options;
            KeyMapping[] mappings = options.keyMappings;
            if (mappings == null || mappings.length == 0) return;

            for (KeyMapping mapping : mappings) {
                if (mapping == null) continue;
                String name = mapping.getName();
                // 必要なら除外キーをここでチェック
                if ("key.attack".equals(name) || "key.use".equals(name)) continue;

                // GUI が開いているときは変更を避ける
                if (mc.screen != null) continue;

                InputConstants.Key newKey = randomSingle();
                // 公開 API を使って安全に設定（MAP の更新を含む）
                try {
                    mapping.setKeyModifierAndCode(null, newKey);
                } catch (Throwable ignored) {
                    // フォールバック: setKey + resetMapping（最小限）
                    try {
                        mapping.setKey(newKey);
                        KeyMapping.resetMapping();
                    } catch (Throwable ignored2) {
                        // 失敗しても何もしない（安全に無視）
                    }
                }
            }

            // オプション保存（必要なら有効にする）
            try {
                options.save();
            } catch (Throwable ignored) {}
        }

        private static InputConstants.Key randomSingle() {
            return POSSIBLE_SINGLE.get(RAND.nextInt(POSSIBLE_SINGLE.size()));
        }
    }
}