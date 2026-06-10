package hardcorequesting.common.quests;

import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.reputation.ReputationBar;
import hardcorequesting.common.util.Translator;
import hardcorequesting.common.util.WrappedText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestSet {
    
    private WrappedText name;
    private WrappedText description;
    private List<FormattedText> cachedDescription;
    private Map<UUID, Quest> quests = new ConcurrentHashMap<>();
    private List<ReputationBar> reputationBars;
    private int id;
    
    public QuestSet(WrappedText name, WrappedText description) {
        this.name = name;
        this.description = description;
        this.reputationBars = new ArrayList<>();
        this.id = Quest.getQuestSets().size();
    }
    
    public Map<UUID, Quest> getQuests() {
        return quests;
    }
    
    public List<ReputationBar> getReputationBars() {
        validateBars();
        return reputationBars;
    }
    
    private void validateBars() {
        List<ReputationBar> toRemove = new ArrayList<>();
        for (ReputationBar reputationBar : reputationBars)
            if (!reputationBar.isValid())
                toRemove.add(reputationBar);
        reputationBars.removeAll(toRemove);
    }
    
    public MutableComponent getName() {
        return name.getText();
    }

    public WrappedText getRawName() {
        return name;
    }

    private static final List<String> FORBIDDEN_SET_NAMES = Arrays.asList("sets", "reputations", "bags", "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "com0", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "lpt0");

    public boolean setName(WrappedText name) {
        // The raw text also serves as the quest chapter filename, reject reserved names and duplicates
        if (FORBIDDEN_SET_NAMES.contains(sanitizeFilename(name.getRawText()).toLowerCase()))
            return false;
        if (Quest.getQuestSets().stream().anyMatch(set -> set != this && set.getRawName().getRawText().equalsIgnoreCase(name.getRawText())))
            return false;

        this.name = name;
        return true;
    }

    // Strips whitespace, periods, and filesystem-illegal characters
    private static String sanitizeFilename(String raw) {
        return raw.replaceAll("[\\s.<>:\"/\\\\|?*]", "_");
    }

    public String getFilename() {
        return sanitizeFilename(name.getRawText());
    }

    // Assigns each set a unique sanitized filename to avoid accidental overwrites for similarly named quest lines
    public static Map<QuestSet, String> assignUniqueFilenames(Collection<QuestSet> sets) {
        Map<QuestSet, String> result = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();
        for (QuestSet set : sets) {
            String base = set.getFilename(), unique = base;
            int i = 2;
            while (!used.add(unique.toLowerCase()))
                unique = base + "_" + i++;
            result.put(set, unique);
        }
        return result;
    }

    public MutableComponent getName(int i) {
        return Translator.text((i + 1) + ". ").append(name.getText());
    }
    
    @Environment(EnvType.CLIENT)
    public List<FormattedText> getDescription(GuiBase gui) {
        if (cachedDescription == null) {
            cachedDescription = gui.getLinesFromText(getDescription(), 0.7F, 130);
        }
        
        return cachedDescription;
    }
    
    public boolean isEnabled(Player player) {
        return isEnabled(player, new HashMap<>(), new HashMap<>());
    }
    
    public boolean isEnabled(Player player, Map<Quest, Boolean> isVisibleCache, Map<Quest, Boolean> isLinkFreeCache) {
        if (quests.isEmpty()) return false;
        
        for (Quest quest : quests.values()) {
            if (quest.isEnabled(player, isVisibleCache, isLinkFreeCache)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isCompleted(Player player) {
        if (quests.isEmpty()) return false;
        
        Map<Quest, Boolean> isLinkFreeCache = new HashMap<>();
        for (Quest quest : quests.values()) {
            if (quest.countsForCompletion(player, isLinkFreeCache) && !quest.isCompleted(player)) {
                return false;
            }
        }
        
        return true;
    }
    
    public void removeQuest(Quest quest) {
        quests.remove(quest.getQuestId());
    }
    
    public void addQuest(Quest quest) {
        quests.put(quest.getQuestId(), quest);
    }
    
    public void removeRepBar(ReputationBar repBar) {
        reputationBars.remove(repBar);
    }
    
    public void addRepBar(ReputationBar repBar) {
        if (repBar == null) return;
        reputationBars.add(repBar);
    }
    
    public int getCompletedCount(Player player) {
        return getCompletedCount(player, new HashMap<>());
    }
    
    public int getCompletedCount(Player player, Map<Quest, Boolean> isLinkFreeCache) {
        int count = 0;
        for (Quest quest : quests.values()) {
            if (quest.countsForCompletion(player, isLinkFreeCache) && quest.isCompleted(player)) {
                count++;
            }
        }
        
        return count;
    }
    
    public MutableComponent getDescription() {
        return description.getText();
    }
    
    public WrappedText getRawDescription() {
        return description;
    }
    
    public void setDescription(WrappedText description) {
        this.description = description;
        cachedDescription = null;
    }
    
    public int getId() {
        return id;
    }
    
    public void decreaseId() {
        id--;
    }
}
