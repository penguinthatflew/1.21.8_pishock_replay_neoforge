package net.ptf.tutorialmod;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {

    enum ShockMode {
        //Ordinal of enum corresponds to OpCode in HTTP request
        SHOCK,
        VIBRATE,
        BEEP
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<ShockMode> OP_MODE = BUILDER
            .comment("the operation the device should execute").defineEnum("op_mode",ShockMode.VIBRATE);
    public static final ModConfigSpec.IntValue INTENSITY = BUILDER
            .comment("intensity").defineInRange("intensity",10,1,100);
    public static final ModConfigSpec.IntValue DURATION = BUILDER
            .comment("duration").defineInRange("duration",3,1,15);
    public static final ModConfigSpec.ConfigValue<String> API_KEY = BUILDER
            .comment("api key").define("api_key", "default", s -> s instanceof String);
    public static final ModConfigSpec.ConfigValue<String> SHARE_CODE = BUILDER
            .comment("share code").define("share_code", "default", s -> s instanceof String);
    public static final ModConfigSpec.ConfigValue<String> USERNAME = BUILDER
            .comment("username").define("username", "default", s -> s instanceof String);


    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
