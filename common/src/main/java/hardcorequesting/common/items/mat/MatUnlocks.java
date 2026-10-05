package hardcorequesting.common.items.mat;

import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.tutorial.Tutorial;
import hardcorequesting.common.tutorial.TutorialManager;
import net.minecraft.nbt.CompoundTag;
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
        // Send an warning if tutorial is invalid but REQUIRE_TUTORIAL_FILES is false
        if (!TutorialManager.getInstance().tutorials.containsKey(id)) {
            if (HQMConfig.getInstance().MAT.REQUIRE_TUTORIAL_FILES) return Result.INVALID;
            HardcoreQuestingCore.LOGGER.warn("Unlocked the tutorial %s for %s, but it has no tutorial file", id, player.getScoreboardName());
        }
        mat.unlockedTutorials.add(id);
        GeneralUsage.sendMatDataSync(player);
        if (showToast) {
            // Tutorial chip as the default icon if another one isn't available
            CompoundTag icon = itemIcon(ModItems.tutorialDataChip.get().getDefaultInstance());
            Component message = Component.literal(id);
            Tutorial tutorial = TutorialManager.getInstance().tutorials.get(id);
            if (tutorial != null) {
                message = tutorial.title();
                if (tutorial.icon() != null) icon.putString("Name", tutorial.icon());
            }
            toast(player, icon, Component.translatable("hqm.mat.toast.tutorial"), message);
        }
        GeneralUsage.sendMatTutorialAutoPlay(player);
        return Result.UNLOCKED;
    }

    // Unlocks tutorial marked as default_unlocked
    public static void unlockDefaultTutorials(ServerPlayer player) {
        MatPlayerData mat = data(player);
        boolean added = false;
        for (Tutorial tutorial : TutorialManager.getInstance().tutorials.values()) {
            if (tutorial.defaultUnlocked() && mat.unlockedTutorials.add(tutorial.id())) added = true;
        }
        if (added) {
            GeneralUsage.sendMatDataSync(player);
            GeneralUsage.sendMatTutorialAutoPlay(player);
        }
    }

    // Unlocks a statistic
    public static Result unlockStat(ServerPlayer player, String statKey, boolean showToast) {
        if (!StatKey.isValid(statKey)) return Result.INVALID;
        MatPlayerData mat = data(player);
        if (mat.unlockedStats.contains(statKey)) return Result.ALREADY;
        mat.unlockedStats.add(statKey);
        GeneralUsage.sendMatDataSync(player);
        if (showToast) toast(player, statIcon(statKey),
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
            toast(player, itemIcon(ModItems.locationDataChip.get().getDefaultInstance()),
                    Component.translatable("hqm.mat.toast.location"), message);
        }
        // returns count of added locations
        return locations.size();
    }

    // Removes the matching tutorials along with their completion and saved progress, so unlocking one again starts fresh
    public static int removeTutorials(ServerPlayer player, Predicate<String> filter) {
        MatPlayerData mat = data(player);
        int before = mat.unlockedTutorials.size();
        mat.unlockedTutorials.removeIf(filter);
        mat.completedTutorials.removeIf(filter);
        mat.tutorialProgress.keySet().removeIf(filter);
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
        TrackedLocation tracked = mat.getSelectedLocation();
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

    private static void toast(ServerPlayer player, CompoundTag icon, Component title, Component message) {
        GeneralUsage.sendMatUnlockToast(player, icon, title, message);
    }

    // A toast icon showing an item
    private static CompoundTag itemIcon(ItemStack stack) {
        CompoundTag icon = new CompoundTag();
        icon.put("Item", stack.save(new CompoundTag()));
        return icon;
    }

    // A toast icon showing a statistic's icon
    private static CompoundTag statIcon(String key) {
        CompoundTag icon = new CompoundTag();
        icon.putString("Stat", key);
        return icon;
    }

    private static MatPlayerData data(ServerPlayer player) {
        return QuestingDataManager.getInstance().getQuestingData(player).matData;
    }
}