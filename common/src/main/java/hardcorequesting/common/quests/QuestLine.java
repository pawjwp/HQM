package hardcorequesting.common.quests;

import com.google.common.collect.Lists;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.bag.GroupTier;
import hardcorequesting.common.bag.GroupTierManager;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.graphic.QuestSetsGraphic;
import hardcorequesting.common.death.DeathStatsManager;
import hardcorequesting.common.io.DataReader;
import hardcorequesting.common.io.DataWriter;
import hardcorequesting.common.io.SaveHandler;
import hardcorequesting.common.network.NetworkManager;
import hardcorequesting.common.network.message.DeathStatsMessage;
import hardcorequesting.common.network.message.PlayerDataSyncMessage;
import hardcorequesting.common.network.message.TeamStatsMessage;
import hardcorequesting.common.reputation.ReputationManager;
import hardcorequesting.common.team.TeamManager;
import hardcorequesting.common.util.SaveHelper;
import hardcorequesting.common.util.WrappedText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class QuestLine {
    
    private static QuestLine activeQuestLine;
    
    public final ReputationManager reputationManager;
    public final GroupTierManager groupTierManager;
    public final QuestingDataManager questingDataManager;
    public final DeathStatsManager deathStatsManager;
    public final QuestSetsManager questSetsManager;
    public final TeamManager teamManager;
    public final Serializable descriptionManager;
    
    private WrappedText mainDescription = WrappedText.create("No description");
    @Environment(EnvType.CLIENT)
    public ResourceLocation front;
    private final List<Serializable> questSerializables = Lists.newArrayList();
    private final List<Serializable> dataSerializables = Lists.newArrayList();
    
    private QuestLine() {
        this.reputationManager = new ReputationManager();
        this.groupTierManager = new GroupTierManager();
        this.questingDataManager = new QuestingDataManager();
        this.deathStatsManager = new DeathStatsManager();
        this.questSetsManager = new QuestSetsManager();
        this.teamManager = new TeamManager();
        this.descriptionManager = new Serializable() {
            @Override
            public boolean isData() {
                return false;
            }
    
            @Override
            public void save(DataWriter writer) {
                writer.write("description.json", getRawMainDescription().toJson().toString());
            }
        
            @Override
            public void load(DataReader reader) {
                setMainDescription(WrappedText.create("No description"));
                Optional<String> json = reader.read("description.json");
                if (json.isPresent()) {
                    setMainDescription(WrappedText.fromJson(SaveHandler.JSON_PARSER.parse(json.get()), "No description", false));
                } else {    // Migrate the pre-translation-key plain text file, if present
                    reader.read("description.txt").ifPresent(text -> setMainDescription(WrappedText.create(text)));
                }
            }
        };
        GroupTier.initBaseTiers(this);
    
        add(descriptionManager);
        add(this.questingDataManager.state);
        add(this.deathStatsManager);
        add(this.reputationManager);
        add(this.groupTierManager);
        add(this.questSetsManager);
        add(this.teamManager);
        add(this.questingDataManager.data);
    }
    
    @Environment(EnvType.CLIENT)
    private static void resetClient() {
        GuiQuestBook.resetBookPosition();
        
        QuestSetsGraphic.loginReset();
    }
    
    public void add(Serializable serializable) {
        if (serializable.isData())
            this.dataSerializables.add(serializable);
        else
            this.questSerializables.add(serializable);
    }
    
    public static QuestLine getActiveQuestLine() {
        return activeQuestLine;
    }
    
    public static QuestLine reset() {
        SaveHelper.onLoad();
        
        if (HardcoreQuestingCore.platform.isClient()) {
            resetClient();
        }
        return activeQuestLine = new QuestLine();
    }
    
    public static void sendDataToClient(ServerPlayer player) {
        QuestLine questLine = getActiveQuestLine();
        // Sync various data, but not for the player that hosts the server (if any), as they already share the same data as the server
        if (!player.server.isSingleplayerOwner(player.getGameProfile())) {
            boolean side = !HardcoreQuestingCore.platform.isClient();
            NetworkManager.sendToPlayer(new PlayerDataSyncMessage(questLine, player), player);
    
            NetworkManager.sendToPlayer(new DeathStatsMessage(side), player);
        }
        NetworkManager.sendToPlayer(new TeamStatsMessage(StreamSupport.stream(questLine.teamManager.getNamedTeams().spliterator(), false)), player);
    }
    
    public MutableComponent getMainDescription() {
        return mainDescription.getText();
    }

    public WrappedText getRawMainDescription() {
        return mainDescription;
    }
    
    public void setMainDescription(WrappedText mainDescription) {
        this.mainDescription = mainDescription;
    }
    
    public void saveQuests(@NotNull DataWriter cfqWriter) {
        for (Serializable serializable : questSerializables) {
            serializable.save(cfqWriter);
        }
    }
    
    public void saveData(@NotNull DataWriter dataWriter) {
        for (Serializable serializable : dataSerializables) {
            serializable.save(dataWriter);
        }
    }
    
    public void loadAll(DataReader cfgReader, DataReader dataReader) {
        HardcoreQuestingCore.LOGGER.info("[HQM] Loading Quest Line, with data: %s", cfgReader);
        
        // Load quests and such before loading player and team data
        for (Serializable serializable : questSerializables) {
            serializable.load(cfgReader);
        }
        for (Serializable serializable : dataSerializables) {
            serializable.load(dataReader);
        }
    }
}
