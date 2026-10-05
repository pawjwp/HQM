package hardcorequesting.common.items.mat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-player MAT state, stored in QuestingData
 * Stored by QuestingAdapter, synced by MAT_DATA_SYNC and login (PlayerDataSyncMessage).
 */
public class MatPlayerData {
    public final Set<String> unlockedTutorials = new LinkedHashSet<>();
    public final Set<String> completedTutorials = new HashSet<>();
    public final Map<String, Integer> tutorialProgress = new HashMap<>(); // the current step
    public final Map<String, Integer> tutorialFurthest = new HashMap<>(); // the furthest step reached, for navigating forward again
    public final List<String> unlockedStats = new ArrayList<>(); // stats, unlock order
    public final List<TrackedLocation> locations = new ArrayList<>(); // locations, insertion order
    public int selectedLocation = -1; // start with no selected location
    public ItemStack dockedTerminal = ItemStack.EMPTY;

    @Nullable
    public TrackedLocation getSelectedLocation() {
        if (selectedLocation < 0 || selectedLocation >= locations.size()) return null;
        return locations.get(selectedLocation);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("UnlockedTutorials", stringList(unlockedTutorials));
        tag.put("CompletedTutorials", stringList(completedTutorials));
        CompoundTag progress = new CompoundTag();
        for (Map.Entry<String, Integer> entry : tutorialProgress.entrySet()) progress.putInt(entry.getKey(), entry.getValue());
        tag.put("TutorialProgress", progress);
        CompoundTag furthest = new CompoundTag();
        for (Map.Entry<String, Integer> entry : tutorialFurthest.entrySet()) furthest.putInt(entry.getKey(), entry.getValue());
        tag.put("TutorialFurthest", furthest);
        tag.put("UnlockedStats", stringList(unlockedStats));

        ListTag locationList = new ListTag();
        for (TrackedLocation location : locations) locationList.add(location.toNBT());
        tag.put("Locations", locationList);
        tag.putInt("SelectedLocation", selectedLocation);

        if (!dockedTerminal.isEmpty()) tag.put("Dock", dockedTerminal.save(new CompoundTag()));
        return tag;
    }

    public void fromNBT(CompoundTag tag) {
        unlockedTutorials.clear();
        readStrings(tag.getList("UnlockedTutorials", Tag.TAG_STRING), unlockedTutorials);
        completedTutorials.clear();
        readStrings(tag.getList("CompletedTutorials", Tag.TAG_STRING), completedTutorials);
        tutorialProgress.clear();
        CompoundTag progress = tag.getCompound("TutorialProgress");
        for (String id : progress.getAllKeys()) tutorialProgress.put(id, progress.getInt(id));
        tutorialFurthest.clear();
        CompoundTag furthest = tag.getCompound("TutorialFurthest");
        for (String id : furthest.getAllKeys()) tutorialFurthest.put(id, furthest.getInt(id));
        unlockedStats.clear();
        readStrings(tag.getList("UnlockedStats", Tag.TAG_STRING), unlockedStats);

        locations.clear();
        ListTag locationList = tag.getList("Locations", Tag.TAG_COMPOUND);
        for (int i = 0; i < locationList.size(); i++) locations.add(TrackedLocation.fromNBT(locationList.getCompound(i)));
        selectedLocation = tag.contains("SelectedLocation") ? tag.getInt("SelectedLocation") : -1;

        dockedTerminal = tag.contains("Dock") ? ItemStack.of(tag.getCompound("Dock")) : ItemStack.EMPTY;
    }

    private static ListTag stringList(Iterable<String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        return list;
    }

    private static void readStrings(ListTag list, java.util.Collection<String> target) {
        for (int i = 0; i < list.size(); i++) target.add(list.getString(i));
    }
}
