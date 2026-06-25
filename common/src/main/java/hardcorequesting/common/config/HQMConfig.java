package hardcorequesting.common.config;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.api.SyntaxError;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.client.KeyboardHandler;
import hardcorequesting.common.io.SaveHandler;
import hardcorequesting.common.items.BagItem;
import hardcorequesting.common.quests.Quest;
import hardcorequesting.common.team.RewardSetting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class HQMConfig {
    private static transient HQMConfig instance;
    
    @Comment("Settings related to hardcore mode")
    //@Name("Hardcore settings")
    public Hardcore Hardcore = new Hardcore();
    @Comment("Settings related to server start & modes")
    //@Name("Server start settings")
    public Starting Starting = new Starting();
    @Comment("Settings related to loot bags and loot tiers")
    //@Name("Loot settings")
    public Loot Loot = new Loot();
    @Comment("Settings related to the interface")
    //@Name("Interface settings")
    public Interface Interface = new Interface();
    
    //@Name("Enable spawn with book")
    @Comment("Enable this to cause the player to be granted a gift on spawning into the world")
    public boolean SPAWN_BOOK = false;
    
    //@Name("Lose quest book upon death")
    @Comment("Loose the quest book when you die, if set to false it will stay in your inventory")
    public boolean LOSE_QUEST_BOOK = true;
    
    //@Name("All in party get rewards")
    @Comment("Allow every single player in a party to claim the reward for a quest. Setting this to false will give the party one set of rewards to share.")
    public boolean MULTI_REWARD = true;
    
    @Comment("Allow teams, currently extremely buggy!")
    public boolean ENABLE_TEAMS = true;
    
    //@Name("NBT Subset Tags to Filter")
    @Comment("Use this to specify NBT tags that should be ignored when comparing items with NBT subset")
    public String[] NBT_SUBSET_FILTER = new String[]{"RepairCost"};
    
    @Comment("Settings related to messages sent from the server")
    //@Name("Message settings")
    public Message Message = new Message();
    @Comment("Settings related to edit mode")
    //@Name("Editing settings")
    public Editing Editing = new Editing();
    
    public static int OVERLAY_XPOS;
    
    public static int OVERLAY_YPOS;
    
    public static int OVERLAY_XPOSDEFAULT = 2;
    
    public static int OVERLAY_YPOSDEFAULT = 2;
    
    public static int CURRENTLY_MODIFYING_QUEST_SET = 0x4040dd;
    
    public static int COMPLETED_SELECTED_IN_BOUNDS_SET = 0x40bb40;
    
    public static int COMPLETED_SELECTED_OUT_OF_BOUNDS_SET = 0x40a040;
    
    public static int COMPLETED_UNSELECTED_IN_BOUNDS_SET = 0x10a010;
    
    public static int COMPLETED_UNSELECTED_OUT_OF_BOUNDS_SET = 0x107010;
    
    public static int UNCOMPLETED_SELECTED_IN_BOUNDS_SET = 0xaaaaaa;
    
    public static int UNCOMPLETED_SELECTED_OUT_OF_BOUNDS_SET = 0x888888;
    
    public static int UNCOMPLETED_UNSELECTED_IN_BOUNDS_SET = 0x666666;
    
    public static int UNCOMPLETED_UNSELECTED_OUT_OF_BOUNDS_SET = 0x404040;
    
    public static int DISABLED_SET = 0xdddddd;
    
    public static int QUEST_INVISIBLE = 0x55FFFFFF;
    
    public static int QUEST_DISABLED = 0xFF888888;
    
    public static int QUEST_COMPLETE = 0xFFFFFFFF;
    
    public static int QUEST_COMPLETE_REPEATABLE = 0xFFFFFFCC;
    
    public static int QUEST_AVAILABLE = 0x554286f4;
    
    public static int TEXT_NORMAL = 0x404040;

    public static int TEXT_HINT = 0x707070;

    public static int TEXT_WARNING = 0xff5555;

    public static int TEXT_ERROR = 0xff0000;

    public static int TEXT_HOVERED = 0xaaaaaa;

    public static int TEXT_SELECTED = 0xc0c0c0;

    public static int TEXT_SELECTED_HOVERED = 0xd0d0d0;

    public static int MAP_CONNECTING_LINE = 0xff404040;

    public static int MAP_OPTION_LINK_LINE = 0xff4040dd;

    public static int MAP_SELECTED_MARKER = 0xffbbffbb;

    public static int MAP_SPECIAL_SELECTED_MARKER = 0xfff8bbff;

    // Same RGB as the line color but with the dimming used for links to invisible quests.
    public static int dimmed(int color) {
        return (color & 0xFFFFFF) | 0x55000000;
    }

    public static HQMConfig getInstance() {
        if (instance == null) {
            try {
                Jankson jankson = Jankson.builder().build();
                Path path = HardcoreQuestingCore.configDir.resolve("config.json5");
                if (!Files.exists(path.getParent()))
                    Files.createDirectories(path.getParent());
                instance = SaveHandler.load(path).flatMap(s -> {
                    try {
                        return Optional.of(jankson.fromJson(s, HQMConfig.class));
                    } catch (SyntaxError syntaxError) {
                        syntaxError.printStackTrace();
                    }
                    return Optional.empty();
                }).orElse(new HQMConfig());
                SaveHandler.save(path, jankson.toJson(instance).toJson(JsonGrammar.JSON5));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        return instance;
    }
    
    public static void parseSetColours() {
        try {
            COMPLETED_SELECTED_IN_BOUNDS_SET = Long.decode(getInstance().Interface.QuestSets.COMPLETED_SELECTED_IN_BOUNDS_SET.toLowerCase()).intValue();
            COMPLETED_UNSELECTED_IN_BOUNDS_SET = Long.decode(getInstance().Interface.QuestSets.COMPLETED_UNSELECTED_IN_BOUNDS_SET.toLowerCase()).intValue();
            UNCOMPLETED_SELECTED_IN_BOUNDS_SET = Long.decode(getInstance().Interface.QuestSets.UNCOMPLETED_SELECTED_IN_BOUNDS_SET.toLowerCase()).intValue();
            UNCOMPLETED_UNSELECTED_IN_BOUNDS_SET = Long.decode(getInstance().Interface.QuestSets.UNCOMPLETED_UNSELECTED_IN_BOUNDS_SET.toLowerCase()).intValue();
            DISABLED_SET = Long.decode(getInstance().Interface.QuestSets.DISABLED_SET.toLowerCase()).intValue();
        } catch (NumberFormatException e) {
            HardcoreQuestingCore.LOGGER.error("Unable to parse set colours", e);
        }
    }
    
    public static void parseQuestColours() {
        try {
            QUEST_INVISIBLE = Long.decode(getInstance().Interface.Quests.QUEST_INVISIBLE.toLowerCase()).intValue();
            QUEST_DISABLED = Long.decode(getInstance().Interface.Quests.QUEST_DISABLED.toLowerCase()).intValue();
            QUEST_COMPLETE = Long.decode(getInstance().Interface.Quests.QUEST_COMPLETE.toLowerCase()).intValue();
            QUEST_COMPLETE_REPEATABLE = Long.decode(getInstance().Interface.Quests.QUEST_COMPLETE_REPEATABLE.toLowerCase()).intValue();
            QUEST_AVAILABLE = Long.decode(getInstance().Interface.Quests.QUEST_AVAILABLE.toLowerCase()).intValue();
        } catch (NumberFormatException e) {
            HardcoreQuestingCore.LOGGER.error("Unable to parse quest colours", e);
        }
    }
    
    public static void parseTextColours() {
        try {
            Interface.Text t = getInstance().Interface.Text;
            TEXT_NORMAL = Long.decode(t.NORMAL.toLowerCase()).intValue();
            TEXT_HINT = Long.decode(t.HINT.toLowerCase()).intValue();
            TEXT_WARNING = Long.decode(t.WARNING.toLowerCase()).intValue();
            TEXT_ERROR = Long.decode(t.ERROR.toLowerCase()).intValue();
            TEXT_HOVERED = Long.decode(t.HOVERED.toLowerCase()).intValue();
            TEXT_SELECTED = Long.decode(t.SELECTED.toLowerCase()).intValue();
            TEXT_SELECTED_HOVERED = Long.decode(t.SELECTED_HOVERED.toLowerCase()).intValue();
        } catch (NumberFormatException e) {
            HardcoreQuestingCore.LOGGER.error("Unable to parse text colours", e);
        }
    }

    public static void parseMapColours() {
        try {
            Interface.QuestMap m = getInstance().Interface.QuestMap;
            MAP_CONNECTING_LINE = Long.decode(m.CONNECTING_LINE.toLowerCase()).intValue();
            MAP_OPTION_LINK_LINE = Long.decode(m.OPTION_LINK_LINE.toLowerCase()).intValue();
            MAP_SELECTED_MARKER = Long.decode(m.SELECTED_MARKER.toLowerCase()).intValue();
            MAP_SPECIAL_SELECTED_MARKER = Long.decode(m.SPECIAL_SELECTED_MARKER.toLowerCase()).intValue();
        } catch (NumberFormatException e) {
            HardcoreQuestingCore.LOGGER.error("Unable to parse map colours", e);
        }
    }

    @SuppressWarnings("deprecation")
    public static void loadConfig() {
        parseSetColours();
        parseQuestColours();
        parseTextColours();
        parseMapColours();
        
        RewardSetting.isAllModeEnabled = getInstance().MULTI_REWARD;
        BagItem.displayGui = getInstance().Loot.REWARD_INTERFACE;
        
        Quest.isEditing = getInstance().Editing.USE_EDITOR;
        if (HardcoreQuestingCore.proxy.isClient()) {
            KeyboardHandler.clear();
            KeyboardHandler.initDefault();
        }
    }
    
    public static class Hardcore {
        //@Name("Default lives")
        @Comment("How many lives players should start with.")
        public int DEFAULT_LIVES = 3;
        
        //@Name("Heart Rot Timer in Seconds")
        @Comment("Define in seconds how long the rot timer is.")
        //@RangeInt(min = 1)
        public int HEART_ROT_TIME = 120;
        
        //@Name("Enable rot timer")
        @Comment("Set to true to enable the heart rot timer")
        public boolean HEART_ROT_ENABLE = false;
        
        //@Name("Maximum lives obtainable")
        @Comment("Use this to set the maximum lives obtainable")
        public int MAX_LIVES = 20;
    }
    
    public static class Starting {
        //@Name("Auto-start hardcore mode")
        @Comment("If set to true, new worlds will automatically activate Hardcore mode")
        public boolean AUTO_HARDCORE = false;
        //@Name("Auto-start questing mode")
        @Comment("If set to true, new worlds will automatically activate Questing mode")
        public boolean AUTO_QUESTING = true;
    }
    
    public static class Loot {
        //@Name("Always use tier name for reward titles")
        @Comment("Always display the tier name, instead of the individual bag's name, when opening a reward bag.")
        public boolean ALWAYS_USE_TIER = false;
        //@Name("Display reward interface")
        @Comment("Set to true to display an interface with the contents of the reward bag when you open it.")
        public boolean REWARD_INTERFACE = true;
    }
    
    public static class Message {
        //@Name("Enable hardcore mode status message")
        @Comment("Set to true to enable sending a status message if Hardcore Questing mode is off")
        public boolean NO_HARDCORE_MESSAGE = false;
        
        //@Name("Enable OP command reminder")
        @Comment("Set to false to prevent the 'use /hqm op instead' message when operators use '/hqm edit' instead.")
        public boolean OP_REMINDER = true;
    }
    
    public static class Editing {
        //@Name("Enable edit mode by default")
        @Comment("Set to true to automatically enable edit mode when entering worlds in single-player. Has no effect in multiplayer.")
        public boolean USE_EDITOR = false;
    }
    
    public static class Interface {
        //@Name("Quest Set Colours")
        @Comment("Colour settings for quest set rendering")
        public QuestSets QuestSets = new QuestSets();
        //@Name("Quest Colours")
        @Comment("Colour settings for quests")
        public Quests Quests = new Quests();
        //@Name("Text Colours")
        @Comment("Colour settings for the quest book text")
        public Text Text = new Text();
        //@Name("Quest Map Colours")
        @Comment("Colour settings for the quest map")
        public QuestMap QuestMap = new QuestMap();

        public static class Text {
            //@Name("Standard text")
            @Comment("Use the HTML format, e.g.: #404040")
            public String NORMAL = "#404040";
            //@Name("Hint text")
            @Comment("Use the HTML format, e.g.: #707070")
            public String HINT = "#707070";
            //@Name("Warning text")
            @Comment("Use the HTML format, e.g.: #ff5555")
            public String WARNING = "#ff5555";
            //@Name("Error text")
            @Comment("Use the HTML format, e.g.: #ff0000")
            public String ERROR = "#ff0000";
            //@Name("Hovered row")
            @Comment("Use the HTML format, e.g.: #aaaaaa")
            public String HOVERED = "#aaaaaa";
            //@Name("Selected row")
            @Comment("Use the HTML format, e.g.: #c0c0c0")
            public String SELECTED = "#c0c0c0";
            //@Name("Selected hovered row")
            @Comment("Use the HTML format, e.g.: #d0d0d0")
            public String SELECTED_HOVERED = "#d0d0d0";
        }

        public static class QuestMap {
            //@Name("Requirement line")
            @Comment("Use the HTML format with alpha, e.g.: #ff404040")
            public String CONNECTING_LINE = "#ff404040";
            //@Name("Option link line")
            @Comment("Use the HTML format with alpha, e.g.: #ff4040dd")
            public String OPTION_LINK_LINE = "#ff4040dd";
            //@Name("Selected marker")
            @Comment("Use the HTML format with alpha, e.g.: #ffbbffbb")
            public String SELECTED_MARKER = "#ffbbffbb";
            //@Name("Specially selected marker")
            @Comment("Use the HTML format with alpha, e.g.: #fff8bbff")
            public String SPECIAL_SELECTED_MARKER = "#fff8bbff";
        }

        public static class QuestSets {
            //@Name("Set is completed, selected")
            @Comment("Use the HTML format, e.g.: #ffffff")
            public String COMPLETED_SELECTED_IN_BOUNDS_SET = "#40bb40";
            //@Name("Set is complete, unselected")
            @Comment("Use the HTML format, e.g.: #ffffff")
            public String COMPLETED_UNSELECTED_IN_BOUNDS_SET = "#10a010";
            //@Name("Set is incomplete, selected")
            @Comment("Use the HTML format, e.g.: #ffffff")
            public String UNCOMPLETED_SELECTED_IN_BOUNDS_SET = "#aaaaaa";
            //@Name("Set is incomplete, unselected")
            @Comment("Use the HTML format, e.g.: #ffffff")
            public String UNCOMPLETED_UNSELECTED_IN_BOUNDS_SET = "#666666";
            //@Name("Set is currently disabled")
            @Comment("Use the HTML format, e.g.: #ffffff")
            public String DISABLED_SET = "#dddddd";
        }
        
        public static class Quests {
            
            public String QUEST_INVISIBLE = "#55FFFFFF";
            
            //@Name("Quest is not accessible")
            @Comment("Use the HTML format with alpha, e.g.: #55FFFFFF")
            public String QUEST_DISABLED = "#FF888888";
            
            //@Name("Quest is completed and not repeatable")
            @Comment("Use the HTML format with alpha, e.g.: #55FFFFFF")
            public String QUEST_COMPLETE = "#FFFFFFFF";
            
            //@Name("Quest is completed but repeatable")
            @Comment("Use the HTML format with alpha, e.g.: #55FFFFFF")
            public String QUEST_COMPLETE_REPEATABLE = "#FFFFFFCC";
            
            //@Name("Quest is available")
            @Comment("Use the HTML format with alpha, e.g: #55FFFFFF")
            public String QUEST_AVAILABLE = "#554286f4";
            
            //@Name("Use single colour for available quest")
            @Comment("Set to true to disable the default colour pulse and use the colour specified above")
            public boolean SINGLE_COLOUR = false;
        }
    }
}
