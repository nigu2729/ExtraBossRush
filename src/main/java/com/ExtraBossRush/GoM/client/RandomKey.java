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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ExtraBossRush.MOD_ID , bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RandomKey {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
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
            System.out.println("[KeyShuffler] static init - building POSSIBLE_SINGLE list");
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
            System.out.println("[KeyShuffler] POSSIBLE_SINGLE size = " + POSSIBLE_SINGLE.size());
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            tickCounter++;
            if (tickCounter % 1000 == 0) {
                System.out.println("[KeyShuffler] tickCounter = " + tickCounter);
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                System.out.println("[KeyShuffler] Minecraft instance is null - skipping tick");
                return;
            }

            // ワールド入室検出
            if (!hasJoinedWorld && mc.level != null) {
                hasJoinedWorld = true;
                System.out.println("[KeyShuffler] Detected world join at tick " + tickCounter);
                performRandomizeNow();
                scheduleNextRandomizeTicks();
                return;
            }

            // ワールド離脱検出
            if (hasJoinedWorld && mc.level == null) {
                hasJoinedWorld = false;
                nextRandomizeTick = -1L;
                System.out.println("[KeyShuffler] Detected world leave at tick " + tickCounter);
                return;
            }

            // 定期実行
            if (nextRandomizeTick >= 0 && tickCounter >= nextRandomizeTick) {
                System.out.println("[KeyShuffler] Scheduled randomize triggered at tick " + tickCounter + " (nextRandomizeTick=" + nextRandomizeTick + ")");
                performRandomizeNow();
                scheduleNextRandomizeTicks();
            }
        }

        private static void scheduleNextRandomizeTicks() {
            int delay = MIN_TICKS + RAND.nextInt(MAX_TICKS - MIN_TICKS + 1);
            nextRandomizeTick = tickCounter + delay;
            System.out.println("[KeyShuffler] Next randomize scheduled in " + delay + " ticks (at tick " + nextRandomizeTick + ")");
        }

        public static void performRandomizeNow() {

            System.out.println("[KeyShuffler] performRandomizeNow start (tick=" + tickCounter + ")");
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                System.out.println("[KeyShuffler] Minecraft instance null - aborting randomize");
                return;
            }
            if (mc.options == null) {
                System.out.println("[KeyShuffler] mc.options is null - aborting randomize");
                return;
            }

            Options options = mc.options;
            KeyMapping[] mappings = options.keyMappings;
            if (mappings == null) {
                System.out.println("[KeyShuffler] options.keyMappings is null");
                return;
            }
            if (mappings.length == 0) {
                System.out.println("[KeyShuffler] options.keyMappings is empty");
                return;
            }

            System.out.println("[KeyShuffler] Found " + mappings.length + " key mappings");

            for (KeyMapping mapping : mappings) {
                if (mapping == null) {
                    System.out.println("[KeyShuffler] encountered null mapping - skipping");
                    continue;
                }
                String name = mapping.getName();
                System.out.println("[KeyShuffler] processing mapping: " + name);

                // 必要なら除外キーをここでチェック
                if ("key.attack".equals(name) || "key.use".equals(name)) {
                    System.out.println("[KeyShuffler] skipping excluded mapping: " + name);
                    continue;
                }

                // GUI が開いているときは変更を避ける
                if (mc.screen != null) {
                    System.out.println("[KeyShuffler] GUI open - skipping mapping " + name + " this tick");
                    continue;
                }

                InputConstants.Key newKey = randomSingle();
                System.out.println("[KeyShuffler] chosen new key for " + name + ": " + newKey.getValue());

                // 公開 API を使って安全に設定（MAP の更新を含む）
                boolean success = false;
                try {
                    // まず公開 API を試す（setKey が存在する場合）
                    try {
                        mapping.setKey(newKey);
                        KeyMapping.resetMapping();
                        success = true;
                        System.out.println("[KeyShuffler] setKey succeeded for " + name + " -> " + newKey.getValue());
                    } catch (Throwable t1) {
                        System.out.println("[KeyShuffler] setKey failed for " + name + ": " + t1);
                        // フォールバック: リフレクションで setKeyModifierAndCode を試す
                        try {
                            Method m = mapping.getClass().getMethod("setKeyModifierAndCode", InputConstants.Key.class, InputConstants.Key.class);
                            m.setAccessible(true);
                            m.invoke(mapping, null, newKey);
                            KeyMapping.resetMapping();
                            success = true;
                            System.out.println("[KeyShuffler] setKeyModifierAndCode (reflection) succeeded for " + name + " -> " + newKey.getValue());
                        } catch (NoSuchMethodException nsme) {
                            System.out.println("[KeyShuffler] setKeyModifierAndCode not found for " + name + ": " + nsme);
                        } catch (Throwable t2) {
                            System.out.println("[KeyShuffler] reflection setKeyModifierAndCode failed for " + name + ": " + t2);
                            t2.printStackTrace(System.out);
                        }
                    }
                } catch (Throwable t) {
                    System.out.println("[KeyShuffler] Unexpected error while setting key for " + name + ": " + t);
                    t.printStackTrace(System.out);
                }

                if (!success) {
                    System.out.println("[KeyShuffler] Failed to change key mapping for " + name + " - leaving as-is");
                }
            }

            // オプション保存（必要なら有効にする）
            try {
                options.save();
                System.out.println("[KeyShuffler] options.save() completed successfully");
            } catch (Throwable t) {
                System.out.println("[KeyShuffler] options.save() failed: " + t);
                t.printStackTrace(System.out);
            }

            System.out.println("[KeyShuffler] performRandomizeNow end");
        }

        private static InputConstants.Key randomSingle() {
            InputConstants.Key k = POSSIBLE_SINGLE.get(RAND.nextInt(POSSIBLE_SINGLE.size()));
            System.out.println("[KeyShuffler] randomSingle -> " + k.getValue());
            return k;
        }
    }
}