package hardcorequesting.common.tutorial;

import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.io.DataReader;
import hardcorequesting.common.io.DataWriter;
import hardcorequesting.common.io.adapter.TutorialAdapter;
import hardcorequesting.common.quests.QuestLine;
import hardcorequesting.common.quests.Serializable;

import java.util.LinkedHashMap;
import java.util.Map;

public class TutorialManager implements Serializable {
    public final Map<String, Tutorial> tutorials = new LinkedHashMap<>();
    public final Map<String, String> files = new LinkedHashMap<>();

    public static TutorialManager getInstance() {
        return QuestLine.getActiveQuestLine().tutorialManager;
    }

    @Override
    public boolean isData() {
        return false;
    }

    // Disabled to avoid editing or overwriting tutorial files
    @Override
    public void save(DataWriter writer) {
    }

    @Override
    public void load(DataReader reader) {
        tutorials.clear();
        files.clear();
        for (Map.Entry<String, String> file : reader.readFolder("tutorials").entrySet()) {
            String fileName = file.getKey();
            String id = fileName.substring(0, fileName.length() - ".json".length());
            try {
                tutorials.put(id, TutorialAdapter.read(id, file.getValue()));
                files.put(fileName, file.getValue());
            } catch (RuntimeException e) {
                HardcoreQuestingCore.LOGGER.warn("Skipped tutorial %s: %s", fileName, e.getMessage());
            }
        }
        HardcoreQuestingCore.LOGGER.info("Loaded %d tutorials.", tutorials.size());
    }
}
