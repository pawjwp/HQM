package hardcorequesting.common.tutorial;

import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.io.DataReader;
import hardcorequesting.common.io.DataWriter;
import hardcorequesting.common.io.adapter.TutorialAdapter;
import hardcorequesting.common.items.mat.MatUnlocks;
import hardcorequesting.common.network.NetworkManager;
import hardcorequesting.common.network.message.TutorialSyncMessage;
import hardcorequesting.common.quests.QuestLine;
import hardcorequesting.common.quests.Serializable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class TutorialManager implements Serializable {
    private static final String BUNDLED_TUTORIAL = "mat.json"; // the name of the default MAT tutorial
    public final Map<String, Tutorial> tutorials = new LinkedHashMap<>();
    public final Map<String, String> files = new LinkedHashMap<>();
    public int skippedFiles; // files not read in the last load

    public static TutorialManager getInstance() {
        return QuestLine.getActiveQuestLine().tutorialManager;
    }

    // Reloads tutorial files
    public static void reload(MinecraftServer server) {
        TutorialManager manager = getInstance();
        manager.load(HardcoreQuestingCore.packManager);
        NetworkManager.sendToAllPlayers(new TutorialSyncMessage(manager.files));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            MatUnlocks.unlockDefaultTutorials(player);
        }
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
        skippedFiles = 0;
        if (reader == HardcoreQuestingCore.packManager) copyBundledTutorial();
        for (Map.Entry<String, String> file : reader.readFolder("tutorials").entrySet()) {
            String fileName = file.getKey();
            String id = fileName.substring(0, fileName.length() - ".json".length());
            try {
                Tutorial tutorial = TutorialAdapter.read(id, file.getValue());
                tutorials.put(id, tutorial);
                files.put(fileName, file.getValue());
                if (tutorial.icon() != null && !isKnownIcon(tutorial.icon())) {
                    HardcoreQuestingCore.LOGGER.warn("Tutorial %s has an invalid icon %s, it will fall back to the default", id, tutorial.icon());
                }
            } catch (RuntimeException e) {
                HardcoreQuestingCore.LOGGER.warn("Skipped tutorial %s: %s", fileName, e.getMessage());
                skippedFiles++;
            }
        }
        HardcoreQuestingCore.LOGGER.info("Loaded %d tutorials.", tutorials.size());
    }

    // Copies the bundled tutorial into the modpack's tutorials folder if it isn't there already
    private static void copyBundledTutorial() {
        Path file = HardcoreQuestingCore.packDir.resolve("tutorials").resolve(BUNDLED_TUTORIAL);
        if (Files.exists(file)) return;
        try (InputStream bundled = TutorialManager.class.getResourceAsStream("/assets/hardcorequesting/tutorials/" + BUNDLED_TUTORIAL)) {
            Files.createDirectories(file.getParent());
            Files.copy(bundled, file);
        } catch (IOException e) {
            HardcoreQuestingCore.LOGGER.warn("Couldn't copy the bundled tutorial %s into the tutorial folder: %s", BUNDLED_TUTORIAL, e.getMessage());
        }
    }

    private static boolean isKnownIcon(String icon) {
        if (icon.endsWith(".png")) return true;
        ResourceLocation id = ResourceLocation.tryParse(icon);
        return id != null && (BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.MOB_EFFECT.containsKey(id));
    }
}
