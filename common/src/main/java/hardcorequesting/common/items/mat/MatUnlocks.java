package hardcorequesting.common.items.mat;

import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.quests.QuestingDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * Utilities for unlocking and removing content within a MAT (statistics, tutorials, and locations).
 * Used by data chips and the unlock and remove commands.
 */
public class MatUnlocks {
    public enum Result { UNLOCKED, ALREADY, INVALID }

    // Unlocks a tutorial
    public static Result unlockTutorial(ServerPlayer player, String id, boolean showToast) {
        MatPlayerData mat = data(player);
        if (mat.unlockedTutorials.contains(id)) return Result.ALREADY;
        mat.unlockedTutorials.add(id);
        GeneralUsage.sendMatDataSync(player);
        if (showToast) toast(player, ModItems.tutorialDataChip.get().getDefaultInstance(),
                Component.translatable("hqm.mat.toast.tutorial"), Component.literal(id));
        return Result.UNLOCKED;
    }

    // Unlocks a statistic
    public static Result unlockStat(ServerPlayer player, String statKey, boolean showToast) {
        if (!StatKey.isValid(statKey)) return Result.INVALID;
        MatPlayerData mat = data(player);
        if (mat.unlockedStats.contains(statKey)) return Result.ALREADY;
        mat.unlockedStats.add(statKey);
        GeneralUsage.sendMatDataSync(player);
        if (showToast) toast(player, ModItems.statisticDataChip.get().getDefaultInstance(),
                Component.translatable("hqm.mat.toast.statistic"), StatKey.displayName(statKey));
        return Result.UNLOCKED;
    }

    // Unlocks a location (must be resolved already)
    public static int addLocations(ServerPlayer player, List<TrackedLocation> locations, boolean showToast) {
        if (locations.isEmpty()) return 0;
        data(player).locations.addAll(locations);
        GeneralUsage.sendMatDataSync(player);
        if (showToast) {
            Component message = locations.size() == 1
                    ? Component.literal(locations.get(0).name())
                    : Component.translatable("hqm.mat.toast.location.multiple", locations.size());
            toast(player, ModItems.locationDataChip.get().getDefaultInstance(),
                    Component.translatable("hqm.mat.toast.location"), message);
        }
        // returns count of added locations
        return locations.size();
    }

    // Removes the matching tutorials along with their completion, so unlocking one again starts fresh
    public static int removeTutorials(ServerPlayer player, Predicate<String> filter) {
        MatPlayerData mat = data(player);
        int before = mat.unlockedTutorials.size();
        mat.unlockedTutorials.removeIf(filter);
        mat.completedTutorials.removeIf(filter);
        return synced(player, before - mat.unlockedTutorials.size());
    }

    // Removes the matching statistics
    public static int removeStats(ServerPlayer player, Predicate<String> filter) {
        MatPlayerData mat = data(player);
        int before = mat.unlockedStats.size();
        mat.unlockedStats.removeIf(filter);
        return synced(player, before - mat.unlockedStats.size());
    }

    // Removes the matching locations, keeping the tracked selection on the same location if it wasn't removed
    public static int removeLocations(ServerPlayer player, Predicate<TrackedLocation> filter) {
        MatPlayerData mat = data(player);
        TrackedLocation tracked = null;
        if (mat.selectedLocation >= 0 && mat.selectedLocation < mat.locations.size()) {
            tracked = mat.locations.get(mat.selectedLocation);
        }
        int before = mat.locations.size();
        mat.locations.removeIf(filter);
        mat.selectedLocation = mat.locations.indexOf(tracked);
        return synced(player, before - mat.locations.size());
    }

    // Resyncs the player's MAT data when anything was removed, and returns how many entries were
    private static int synced(ServerPlayer player, int removed) {
        if (removed > 0) GeneralUsage.sendMatDataSync(player);
        return removed;
    }

    private static void toast(ServerPlayer player, ItemStack icon, Component title, Component message) {
        GeneralUsage.sendMatUnlockToast(player, icon, title, message);
    }

    private static MatPlayerData data(ServerPlayer player) {
        return QuestingDataManager.getInstance().getQuestingData(player).matData;
    }
}