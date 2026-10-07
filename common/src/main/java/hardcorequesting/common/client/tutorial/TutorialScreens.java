package hardcorequesting.common.client.tutorial;

import hardcorequesting.common.client.BookPage;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.mat.MatScreen;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.quests.Quest;
import hardcorequesting.common.quests.QuestSet;
import hardcorequesting.common.quests.QuestSetsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The screen names used to tell what screen is active
 * Most screens are containers and referenced by their menu (minecraft:crafting)
 * Screens that don't have names have their reference name defined below (inventory, pause, advancements, gameplay, and any)
 * MAT screens are the MAT itself or a specific MAT mode
 * Quest book screens are any page, page type, quest set, or quest in the quest book/questing MAT
 */
@Environment(EnvType.CLIENT)
public class TutorialScreens {
    private static final Set<String> BUILT_IN = Set.of("any", "gameplay", "inventory", "pause", "advancements", "mat",
    "quest_book", "quest_book/cover", "quest_book/menu", "quest_book/sets", "quest_book/set", "quest_book/quest",
    "quest_book/reputation", "quest_book/bags", "quest_book/group", "quest_book/team", "quest_book/team_list", "quest_book/deaths");

    static final String QUEST_BOOK_SET = "quest_book/set/";
    static final String QUEST_BOOK_QUEST = "quest_book/quest/";

    // Whether a screen name matches the open screen, which is null during gameplay
    public static boolean matches(String name, @Nullable Screen screen) {
        return switch (name) {
            case "any" -> screen != null;
            case "gameplay" -> screen == null;
            case "inventory" -> screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
            case "pause" -> screen instanceof PauseScreen;
            case "advancements" -> screen instanceof AdvancementsScreen;
            case "mat" -> screen instanceof MatScreen;
            case "quest_book" -> screen instanceof GuiQuestBook;
            case "quest_book/cover" -> page(screen) instanceof BookPage.MainPage;
            case "quest_book/menu" -> page(screen) instanceof BookPage.MenuPage;
            case "quest_book/sets" -> page(screen) instanceof BookPage.SetsPage;
            case "quest_book/set" -> page(screen) instanceof BookPage.SetMapPage;
            case "quest_book/quest" -> page(screen) instanceof BookPage.QuestPage;
            case "quest_book/reputation" -> page(screen) instanceof BookPage.ReputationPage;
            case "quest_book/bags" -> page(screen) instanceof BookPage.BagsPage;
            case "quest_book/group" -> page(screen) instanceof BookPage.GroupPage;
            case "quest_book/team" -> page(screen) instanceof BookPage.TeamPage;
            case "quest_book/team_list" -> page(screen) instanceof BookPage.TeamListPage;
            case "quest_book/deaths" -> page(screen) instanceof BookPage.DeathInfoPage;
            default -> {
                if (name.startsWith("mat/")) yield screen instanceof MatScreen mat && name.equals("mat/" + modeName(mat.getMode()));
                if (name.startsWith(QUEST_BOOK_SET)) yield page(screen) instanceof BookPage.SetMapPage map && matchesSet(map.getSet(), name.substring(QUEST_BOOK_SET.length()));
                if (name.startsWith(QUEST_BOOK_QUEST)) yield page(screen) instanceof BookPage.QuestPage questPage && matchesQuest(questPage.getQuest(), name.substring(QUEST_BOOK_QUEST.length()));
                ResourceLocation id = ResourceLocation.tryParse(name);
                yield id != null && screen instanceof AbstractContainerScreen<?> container && id.equals(menuId(container));
            }
        };
    }

    // Whether any of the screen names matches the open screen
    public static boolean matchesAny(List<String> names, @Nullable Screen screen) {
        for (String name : names) {
            if (matches(name, screen)) return true;
        }
        return false;
    }

    // Whether a screen name exists
    public static boolean isKnownScreen(String name) {
        if (name.startsWith(QUEST_BOOK_SET)) {
            for (QuestSet set : QuestSetsManager.getInstance().questSets) {
                if (matchesSet(set, name.substring(QUEST_BOOK_SET.length()))) return true;
            }
            return false;
        }
        if (name.startsWith(QUEST_BOOK_QUEST)) return isKnownQuest(name.substring(QUEST_BOOK_QUEST.length()));
        for (MatMode mode : MatMode.values()) {
            if (name.equals("mat/" + modeName(mode))) return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(name);
        return BUILT_IN.contains(name) || (id != null && BuiltInRegistries.MENU.containsKey(id));
    }

    // The open quest book's page
    @Nullable
    private static BookPage page(@Nullable Screen screen) {
        if (screen instanceof GuiQuestBook book) return book.getPage();
        return null;
    }

    // A quest, matched with either the UUID or name
    static boolean matchesQuest(Quest quest, String name) {
        return quest.getQuestId().toString().equalsIgnoreCase(name) || quest.getName().getString().equals(name);
    }

    // If there is a valid quest with the provided UUID or name
    static boolean isKnownQuest(String name) {
        for (Quest quest : QuestSetsManager.getInstance().quests.values()) {
            if (matchesQuest(quest, name)) return true;
        }
        return false;
    }

    // A quest set, matched by name
    static boolean matchesSet(QuestSet set, String name) {
        return set.getName().getString().equals(name);
    }

    // A MAT mode's name for use in screen names and anchors
    public static String modeName(MatMode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
    }

    // A containers menu id, null for menus without defined names
    @Nullable
    private static ResourceLocation menuId(AbstractContainerScreen<?> screen) {
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) return null;
        try {
            return BuiltInRegistries.MENU.getKey(screen.getMenu().getType());
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }
}
