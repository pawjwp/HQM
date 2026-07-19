package hardcorequesting.common.items.mat;

import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.quests.QuestingDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;

// Builds the statistics list the server sends when a player opens the MAT's Default mode.
public class MatStats {
    // The three statistics unlocked by default
    private static final ResourceLocation[] BASE_STATS = {
        Stats.PLAY_TIME,
        Stats.DEATHS,
        Stats.MOB_KILLS
    };

    // Produces the {Stats:[{Key,Value}]} format used by Default MAT mode
    public static CompoundTag buildStatsPayload(ServerPlayer player) {
        ListTag list = new ListTag();
        for (ResourceLocation stat : BASE_STATS) {
            add(list, StatKey.vanillaKey("minecraft:custom", stat.toString()), player.getStats().getValue(Stats.CUSTOM, stat));
        }
        for (String key : QuestingDataManager.getInstance().getQuestingData(player).matData.unlockedStats) {
            add(list, key, value(player, key));
        }
        CompoundTag payload = new CompoundTag();
        payload.put("Stats", list);
        return payload;
    }

    private static long value(ServerPlayer player, String key) {
        if (StatKey.isLifeSign(key)) {
            return lifeSigns(player, key);
        }
        Stat<?> stat = StatKey.resolveVanilla(key);
        if (stat == null) return 0;
        return player.getStats().getValue(stat);
    }

    // Counts nearby mobs of the of a given kind within a configured radius
    private static long lifeSigns(ServerPlayer player, String key) {
        int radius = HQMConfig.getInstance().MAT.LIFE_SIGNS_RADIUS;
        long count = 0;
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius))) {
            boolean hostile = mob instanceof Enemy;
            boolean neutral = mob instanceof NeutralMob && !hostile;
            if (key.equals(StatKey.LIFE_SIGNS_HOSTILE) && hostile) count++;
            else if (key.equals(StatKey.LIFE_SIGNS_NEUTRAL) && neutral) count++;
            else if (key.equals(StatKey.LIFE_SIGNS_FRIENDLY) && !hostile && !neutral) count++;
        }
        return count;
    }

    private static void add(ListTag list, String key, long value) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Key", key);
        entry.putLong("Value", value);
        list.add(entry);
    }
}