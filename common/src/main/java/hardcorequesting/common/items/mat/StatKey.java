package hardcorequesting.common.items.mat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;

/**
 * Functions for the MAT statistic keys, used in the MAT's statistic screen.
 * Each key is either a vanilla stat or one of HQM's custom stats.
 */
public final class StatKey {
    public static final String LIFE_SIGNS_HOSTILE = "hqm:life_signs_hostile";
    public static final String LIFE_SIGNS_NEUTRAL = "hqm:life_signs_neutral";
    public static final String LIFE_SIGNS_FRIENDLY = "hqm:life_signs_friendly";

    private StatKey() {}

    public static boolean isLifeSign(String key) {
        return key.equals(LIFE_SIGNS_HOSTILE) || key.equals(LIFE_SIGNS_NEUTRAL) || key.equals(LIFE_SIGNS_FRIENDLY);
    }

    // Whether the key resolves to a valid stat
    public static boolean isValid(String key) {
        return isLifeSign(key) || resolveVanilla(key) != null;
    }

    // Resolves vanilla stats
    public static Stat<?> resolveVanilla(String key) {
        String[] parts = key.split("/", 2);
        if (parts.length != 2) return null;
        StatType<?> type = BuiltInRegistries.STAT_TYPE.getOptional(ResourceLocation.tryParse(parts[0])).orElse(null);
        if (type == null) return null;
        ResourceLocation value = ResourceLocation.tryParse(parts[1]);
        return value == null ? null : resolve(type, value);
    }

    private static <T> Stat<T> resolve(StatType<T> type, ResourceLocation value) {
        return type.getRegistry().getOptional(value).map(type::get).orElse(null);
    }

    // Builds type/value key string for a resolved vanilla stat
    public static String vanillaKey(String typeId, String valueId) {
        return typeId + "/" + valueId;
    }
}