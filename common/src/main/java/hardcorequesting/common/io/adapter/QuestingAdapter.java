package hardcorequesting.common.io.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import hardcorequesting.common.bag.GroupData;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.items.mat.TrackedLocation;
import hardcorequesting.common.quests.QuestingData;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.team.Team;
import net.minecraft.Util;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestingAdapter {
    public static final Adapter<QuestingData> QUESTING_DATA_ADAPTER = new Adapter<QuestingData>() {
        public static final String KEY_TEAM = "team";
        public static final String KEY_LIVES = "lives";
        public static final String KEY_UUID = "uuid";
        public static final String KEY_NAME = "name";
        public static final String KEY_GROUP_DATA = "groupData";
        public static final String KEY_SELECTED_QUEST = "selectedQuest";
        public static final String KEY_PLAYER_LORE = "playedLore";
        public static final String KEY_RECEIVED_BOOK = "receivedBook";
        public static final String KEY_MAT = "mat";

        @Override
        public JsonElement serialize(QuestingData src) {
            return object()
                    .add(KEY_UUID, src.getPlayerId().toString())
                    .add(KEY_NAME, src.getName())
                    .add(KEY_LIVES, src.getRawLives())
                    .add(KEY_TEAM, src.getTeam().isSingle() ? TeamAdapter.TEAM_ADAPTER.serialize(src.getTeam()) : new JsonPrimitive(src.getTeam().getId().toString()))
                    .add(KEY_SELECTED_QUEST, src.selectedQuestId != null ? src.selectedQuestId.toString() : null)
                    .add(KEY_PLAYER_LORE, src.playedLore)
                    .add(KEY_RECEIVED_BOOK, src.receivedBook)
                    .add(KEY_MAT, serializeMat(src.matData))
                    .add(KEY_GROUP_DATA, object()
                            .use(builder -> {
                                for (Map.Entry<UUID, GroupData> entry : src.getGroupData().entrySet())
                                    if (entry.getKey() != null)
                                        builder.add(entry.getKey().toString(), entry.getValue().retrieved);
                            })
                            .build())
                    .build();
        }

        private JsonElement serializeMat(MatPlayerData mat) {
            JsonArray locations = new JsonArray();
            for (TrackedLocation location : mat.locations) locations.add(location.toJson());
            return object()
                    .add("unlockedTutorials", stringArray(mat.unlockedTutorials))
                    .add("completedTutorials", stringArray(mat.completedTutorials))
                    .add("tutorialSeen", mat.matTutorialSeen)
                    .add("unlockedStats", stringArray(mat.unlockedStats))
                    .add("locations", locations)
                    .add("selectedLocation", mat.selectedLocation)
                    .add("dock", MinecraftAdapter.ITEM_STACK.serialize(mat.dockedTerminal))
                    .build();
        }

        private JsonArray stringArray(Iterable<String> values) {
            JsonArray array = new JsonArray();
            for (String value : values) array.add(value);
            return array;
        }

        private void deserializeMat(JsonObject object, MatPlayerData mat) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "unlockedTutorials", new JsonArray()))
                mat.unlockedTutorials.add(element.getAsString());
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "completedTutorials", new JsonArray()))
                mat.completedTutorials.add(element.getAsString());
            mat.matTutorialSeen = GsonHelper.getAsBoolean(object, "tutorialSeen", false);
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "unlockedStats", new JsonArray()))
                mat.unlockedStats.add(element.getAsString());
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "locations", new JsonArray()))
                mat.locations.add(TrackedLocation.fromJson(element.getAsJsonObject()));
            mat.selectedLocation = GsonHelper.getAsInt(object, "selectedLocation", -1);
            if (object.has("dock"))
                mat.dockedTerminal = MinecraftAdapter.ITEM_STACK.deserialize(object.get("dock"));
        }
        
        @Override
        public QuestingData deserialize(JsonElement json) {
            JsonObject object = json.getAsJsonObject();
            
            String uuid = object.get(KEY_UUID).getAsString();
            int lives = GsonHelper.getAsInt(object, KEY_LIVES, 0);
            UUID teamId = object.get(KEY_TEAM).isJsonPrimitive() && object.get(KEY_TEAM).getAsJsonPrimitive().isString() ? UUID.fromString(object.get(KEY_TEAM).getAsString()) : Util.NIL_UUID;
            Team team = !teamId.equals(Util.NIL_UUID) ? null : TeamAdapter.TEAM_ADAPTER.fromJsonTree(object.get(KEY_TEAM));
            String selectedQuest = GsonHelper.getAsString(object, KEY_SELECTED_QUEST, null);
            boolean playerLore = GsonHelper.getAsBoolean(object, KEY_PLAYER_LORE, false);
            boolean receivedBook = GsonHelper.getAsBoolean(object, KEY_RECEIVED_BOOK, false);
            Map<UUID, GroupData> data = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(object, KEY_GROUP_DATA, new JsonObject()).entrySet()) {
                data.put(UUID.fromString(entry.getKey()), new GroupData(entry.getValue().getAsInt()));
            }
            
            QuestingData questingData = new QuestingData(QuestingDataManager.getInstance(), UUID.fromString(uuid), lives, data);
            questingData.playedLore = playerLore;
            questingData.receivedBook = receivedBook;
            if (selectedQuest != null) {
                questingData.selectedQuestId = UUID.fromString(selectedQuest);
            }
            if (teamId.equals(Util.NIL_UUID))
                questingData.setTeam(team);
            questingData.setName(GsonHelper.getAsString(object, KEY_NAME, null));
            if (object.has(KEY_MAT))
                deserializeMat(GsonHelper.getAsJsonObject(object, KEY_MAT), questingData.matData);
            return questingData;
        }
    };
}