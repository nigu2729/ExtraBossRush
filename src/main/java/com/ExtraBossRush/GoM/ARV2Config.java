package com.ExtraBossRush.GoM;
import net.minecraftforge.common.ForgeConfigSpec;
public class ARV2Config {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static ForgeConfigSpec.BooleanValue ENABLE_BEAM_BREAK;
    public static ForgeConfigSpec.BooleanValue RANDOM_KEY_CONFIG;
    public static ForgeConfigSpec.IntValue MAX_RANDOM_KEY_TICKS;
    public static ForgeConfigSpec.IntValue MIN_RANDOM_KEY_TICKS;
    public static ForgeConfigSpec.BooleanValue ENABLE_CULLING;
    public static ForgeConfigSpec.BooleanValue ENABLE_DISTANCE_CULLING;
    public static ForgeConfigSpec.BooleanValue ENABLE_FRUSTUM_CULLING;
    public static ForgeConfigSpec.IntValue CULLING_DISTANCE;
    static {
        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();
        serverBuilder.comment("ARV2 System Settings").push("general");
        ENABLE_BEAM_BREAK = serverBuilder
                .comment("If false, all beam destruction and block replacement will be disabled.")
                .define("enableBeamBreak", false);
        serverBuilder.pop();
        SERVER_SPEC = serverBuilder.build();
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.comment("ARV2 Client Settings").push("culling");
        ENABLE_CULLING = clientBuilder
                .comment("Master switch for culling. If false, all culling is disabled.")
                .define("enableCulling", true);
        ENABLE_DISTANCE_CULLING = clientBuilder
                .comment("If true, cull effects beyond cullingDistance.")
                .define("enableDistanceCulling", true);
        ENABLE_FRUSTUM_CULLING = clientBuilder
                .comment("If true, cull effects outside the camera frustum.")
                .define("enableFrustumCulling", true);
        CULLING_DISTANCE = clientBuilder
                .comment("Maximum render distance (in blocks) for ARV2 effects.",
                        "Only used when enableDistanceCulling is true.")
                .defineInRange("cullingDistance", 256, 16, Integer.MAX_VALUE);
        RANDOM_KEY_CONFIG = clientBuilder
                .comment("If false, the randomization of key configurations will be disabled.")
                .define("enableRandomKeyConfig", true);
        MAX_RANDOM_KEY_TICKS = clientBuilder
                .comment("Maximum interval (in ticks) between key shuffles.")
                .defineInRange("maxRandomTicks", 1200, 1200, 72000);
        MIN_RANDOM_KEY_TICKS = clientBuilder
                .comment("Minimum interval (in ticks) between key shuffles. (20 ticks = 1 second)")
                .defineInRange("minRandomTicks", 2400, 1200, 72000);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }
}