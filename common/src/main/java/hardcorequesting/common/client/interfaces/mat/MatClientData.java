package hardcorequesting.common.client.interfaces.mat;

import hardcorequesting.common.items.mat.MatSettings;
import hardcorequesting.common.items.mat.StatKey;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the statistics the server sent for the most recent opening of the Info MAT mode.
 * Also formats them for display on the MAT screen.
 */
@Environment(EnvType.CLIENT)
public class MatClientData {
    // The play-time statistic is shown in Minecraft days rather than the vanilla time format.
    private static final String PLAY_TIME_KEY = StatKey.vanillaKey("minecraft:custom", Stats.PLAY_TIME.toString());

    public record StatRow(Component title, Component value, MatIcons.Icon icon) {}

    private static MatSettings settings;
    private static List<StatRow> stats = new ArrayList<>();

    public static void updateSettings(CompoundTag tag) {
        settings = MatSettings.fromNBT(tag);
    }

    public static MatSettings settings() {
        return settings;
    }

    public static void update(CompoundTag payload) {
        List<StatRow> rows = new ArrayList<>();
        ListTag list = payload.getList("Stats", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String key = entry.getString("Key");
            rows.add(new StatRow(StatKey.displayName(key), formatValue(key, entry.getLong("Value")), MatIcons.get(key)));
        }
        stats = rows;
    }

    public static List<StatRow> stats() {
        return stats;
    }

    private static Component formatValue(String key, long value) {
        if (key.equals(PLAY_TIME_KEY)) {
            return Component.translatable("hqm.mat.stat.playtime.days", String.format("%.1f", value / 24000.0));
        }
        if (StatKey.isLifeSign(key)) {
            return Component.translatable("hqm.mat.stat.life_signs.value", value, settings.lifeSignsRadius());
        }
        Stat<?> stat = StatKey.resolveVanilla(key);
        if (stat != null) {
            return Component.literal(stat.format((int) value));
        }
        return Component.literal(Long.toString(value));
    }
}