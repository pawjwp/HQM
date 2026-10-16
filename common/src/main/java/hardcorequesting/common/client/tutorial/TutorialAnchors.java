package hardcorequesting.common.client.tutorial;

import com.mojang.blaze3d.platform.Window;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.client.BookPage;
import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.graphic.MainPageGraphic;
import hardcorequesting.common.client.interfaces.graphic.MenuPageGraphic;
import hardcorequesting.common.client.interfaces.graphic.QuestGraphic;
import hardcorequesting.common.client.interfaces.graphic.QuestRewardsGraphic;
import hardcorequesting.common.client.interfaces.graphic.QuestSetsGraphic;
import hardcorequesting.common.client.interfaces.mat.GuiMatInfo;
import hardcorequesting.common.client.interfaces.mat.GuiMatTracking;
import hardcorequesting.common.client.interfaces.mat.MatScreen;
import hardcorequesting.common.client.interfaces.widget.LargeButton;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.quests.Quest;
import hardcorequesting.common.quests.QuestSet;
import hardcorequesting.common.reputation.ReputationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The anchors available at which text boxes can be placed based on GUI coordinates
 * - Window anchors are locations based on the bounds of the screen
 * - HUD anchors are locations on the vanilla HUD
 * - Container anchors are locations on an active container UI
 * - Slot anchors can name their slot by number, item, or #tag
 * - MAT anchors are the MAT's main panel, tabs, and tab bar
 * - Info screen anchors are the Info mode's tutorial list, buttons, navigation bar, and the statistic list
 * - Tracking screen anchors are the Tracking mode's location list, buttons, and the tracked location's details
 * - Quest book anchors are the book itself and various buttons and screen regions
 */
@Environment(EnvType.CLIENT)
public class TutorialAnchors {
    private static final int QUEST_BOOK_TEXT_WIDTH = 130; // width of the text area of the quest book pages
    // Slot anchor types, each followed by a slot number, item id, or #tag
    private static final String HOTBAR = "hud/hotbar/";
    private static final String OFFHAND = "hud/offhand/";
    private static final String SLOT = "slot/";
    private static final String INVENTORY = "inventory/";

    @Nullable
    public static Rect2i resolve(String anchor) {
        Window window = Minecraft.getInstance().getWindow();
        Player player = Minecraft.getInstance().player;
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        int horizontalCenter = width / 2;
        int verticalCenter = height / 2;

        // Calculates hotbar locations, 20 pixels each
        if (anchor.startsWith(HOTBAR)) {
            Integer slot = hotbarSlot(anchor.substring(HOTBAR.length()), player.getInventory());
            if (slot == null) return null;
            return new Rect2i(horizontalCenter - 90 + 20 * slot, height - 21, 20, 20);
        }

        // The offhand slot holding the named item
        if (anchor.startsWith(OFFHAND)) {
            if (!TutorialItems.matches(anchor.substring(OFFHAND.length()), player.getOffhandItem())) return null;
            return resolve("hud/offhand");
        }

        // Container anchors are only valid when a container screen is open
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) {
            Rect2i panel = HardcoreQuestingCore.platform.getContainerPanel(screen);
            if (anchor.equals("screen/panel")) return panel;
            for (Slot slot : screen.getMenu().slots) {
                if (namesSlot(anchor, SLOT, slot.index, slot) || (slot.container == player.getInventory() && namesSlot(anchor, INVENTORY, slot.getContainerSlot(), slot))) {
                    return new Rect2i(panel.getX() + slot.x - 1, panel.getY() + slot.y - 1, 18, 18);
                }
            }
        }

        // MAT anchors are only valid when a MAT screen is open
        if (Minecraft.getInstance().screen instanceof MatScreen mat) {
            Rect2i panel = mat.getPanel();
            if (anchor.equals("mat/panel")) return panel;
            if (anchor.equals("mat/tab_bar")) return mat.getTabBar().barBounds(panel.getX(), panel.getY());
            for (MatMode mode : MatMode.values()) {
                if (anchor.equals("mat/tab/" + TutorialScreens.modeName(mode))) return mat.getTabBar().tabBounds(mode, panel.getX(), panel.getY());
            }
        }

        // Info screen anchors are only valid while the MAT's Info screen is open
        if (Minecraft.getInstance().screen instanceof GuiMatInfo info) {
            Rect2i area = matInfoArea(anchor, info);
            if (area != null) return new Rect2i(info.getPanel().getX() + area.getX(), info.getPanel().getY() + area.getY(), area.getWidth(), area.getHeight());
        }

        // Tracking screen anchors are only valid while the MAT's Tracking screen is open
        if (Minecraft.getInstance().screen instanceof GuiMatTracking tracking) {
            Rect2i area = matTrackingArea(anchor, tracking);
            if (area != null) return new Rect2i(tracking.getPanel().getX() + area.getX(), tracking.getPanel().getY() + area.getY(), area.getWidth(), area.getHeight());
        }

        // Quest book anchors are only valid when a quest book/questing MAT is open
        if (Minecraft.getInstance().screen instanceof GuiQuestBook book) {
            Rect2i area = questBookArea(anchor, book, player);
            if (area != null) return new Rect2i(book.getLeft() + area.getX(), book.getTop() + area.getY(), area.getWidth(), area.getHeight());
        }

        return switch (anchor) {
            case "window/top_left" -> new Rect2i(0, 0, 0, 0);
            case "window/top" -> new Rect2i(horizontalCenter, 0, 0, 0);
            case "window/top_right" -> new Rect2i(width, 0, 0, 0);
            case "window/left" -> new Rect2i(0, verticalCenter, 0, 0);
            case "window/center" -> new Rect2i(horizontalCenter, verticalCenter, 0, 0);
            case "window/right" -> new Rect2i(width, verticalCenter, 0, 0);
            case "window/bottom_left" -> new Rect2i(0, height, 0, 0);
            case "window/bottom" -> new Rect2i(horizontalCenter, height, 0, 0);
            case "window/bottom_right" -> new Rect2i(width, height, 0, 0);

            case "hud/hotbar" -> new Rect2i(horizontalCenter - 91, height - 22, 182, 22);
            case "hud/offhand" -> new Rect2i(player.getMainArm() == HumanoidArm.RIGHT ? horizontalCenter - 120 : horizontalCenter + 98, height - 23, 22, 24);
            case "hud/health" -> {
                int rows = Mth.ceil((player.getMaxHealth() + player.getAbsorptionAmount()) / 20F);
                int rowSpacing = Math.max(10 - (rows - 2), 3);
                yield new Rect2i(horizontalCenter - 91, height - 39 - (rows - 1) * rowSpacing, 81, 9 + (rows - 1) * rowSpacing);
            }
            case "hud/food" -> new Rect2i(horizontalCenter + 10, height - 39, 81, 9);
            case "hud/experience" -> new Rect2i(horizontalCenter - 91, height - 29, 182, 5);
            case "hud/crosshair" -> new Rect2i((width - 15) / 2, (height - 15) / 2, 15, 15);

            default -> null;
        };
    }

    // If a hotbar anchor names this slot, either by number, item match, selected item, or selected item match
    @Nullable
    private static Integer hotbarSlot(String target, Inventory inventory) {
        if (target.matches("[0-8]")) return target.charAt(0) - '0';
        if (target.equals("selected")) return inventory.selected;
        if (target.startsWith("selected/")) return TutorialItems.matches(target.substring("selected/".length()), inventory.getSelected()) ? inventory.selected : null;
        if (TutorialItems.matches(target, inventory.getSelected())) return inventory.selected;
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (TutorialItems.matches(target, inventory.getItem(i))) return i;
        }
        return null;
    }

    // If a slot or inventory anchor names this slot, either by number or item match
    private static boolean namesSlot(String anchor, String family, int number, Slot slot) {
        if (!anchor.startsWith(family)) return false;
        String target = anchor.substring(family.length());
        if (target.matches("\\d+")) return target.equals(String.valueOf(number));
        return TutorialItems.matches(target, slot.getItem());
    }

    // An info screen anchor's area relative to the MAT's upper-left corner
    @Nullable
    private static Rect2i matInfoArea(String anchor, GuiMatInfo info) {
        return switch (anchor) {
            case "mat/info/tutorials" -> new Rect2i(GuiMatInfo.TUTORIAL_X + GuiMatInfo.ICON_X, GuiMatInfo.LIST_TOP, GuiMatInfo.TUTORIAL_LIST_WIDTH, GuiMatInfo.LIST_HEIGHT);
            case "mat/info/play" -> new Rect2i(GuiMatInfo.PLAY_X, GuiMatInfo.BUTTON_Y, LargeButton.BUTTON_WIDTH, LargeButton.BUTTON_HEIGHT);
            case "mat/info/restart" -> new Rect2i(GuiMatInfo.RESTART_X, GuiMatInfo.BUTTON_Y, LargeButton.BUTTON_WIDTH, LargeButton.BUTTON_HEIGHT);
            case "mat/info/buttons" -> new Rect2i(GuiMatInfo.PLAY_X, GuiMatInfo.BUTTON_Y, GuiMatInfo.RESTART_X + LargeButton.BUTTON_WIDTH - GuiMatInfo.PLAY_X, LargeButton.BUTTON_HEIGHT);
            case "mat/info/navigation" -> info.getNavigationArea();
            case "mat/info/statistics" -> new Rect2i(GuiMatInfo.STAT_X + GuiMatInfo.ICON_X, GuiMatInfo.LIST_TOP, GuiMatInfo.STAT_LIST_WIDTH, GuiMatInfo.LIST_HEIGHT);
            default -> null;
        };
    }

    // A tracking screen anchor's area relative to the MAT's upper-left corner
    @Nullable
    private static Rect2i matTrackingArea(String anchor, GuiMatTracking tracking) {
        return switch (anchor) {
            case "mat/tracking/locations" -> new Rect2i(GuiMatTracking.LOCATION_X, GuiMatTracking.LIST_TOP, GuiMatTracking.LOCATION_WIDTH, GuiMatTracking.LIST_HEIGHT);
            case "mat/tracking/delete" -> new Rect2i(GuiMatTracking.DELETE_X, GuiMatTracking.BUTTON_Y, LargeButton.BUTTON_WIDTH, LargeButton.BUTTON_HEIGHT);
            case "mat/tracking/stop" -> new Rect2i(GuiMatTracking.STOP_X, GuiMatTracking.BUTTON_Y, LargeButton.BUTTON_WIDTH, LargeButton.BUTTON_HEIGHT);
            case "mat/tracking/buttons" -> new Rect2i(GuiMatTracking.DELETE_X, GuiMatTracking.BUTTON_Y, GuiMatTracking.STOP_X + LargeButton.BUTTON_WIDTH - GuiMatTracking.DELETE_X, LargeButton.BUTTON_HEIGHT);
            case "mat/tracking/details" -> tracking.getDetailsArea();
            default -> null;
        };
    }

    // A quest book anchor's area relative to the book's upper-left corner
    @Nullable
    private static Rect2i questBookArea(String anchor, GuiQuestBook book, Player player) {
        BookPage page = book.getPage();
        int textHeight = (int) (GuiBase.TEXT_HEIGHT * 0.7F); // the pages' small text
        return switch (anchor) {
            case "quest_book/panel" -> new Rect2i(0, 0, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT);
            case "quest_book/back" -> book.shouldDisplayBackArrow() ? new Rect2i(GuiQuestBook.BACK_ARROW_X, GuiQuestBook.BACK_ARROW_Y, GuiQuestBook.BACK_ARROW_WIDTH, GuiQuestBook.BACK_ARROW_HEIGHT) : null;
            case "quest_book/claim" -> page instanceof BookPage.QuestPage questPage && questPage.getQuest().getRewards().hasReward(player.getUUID()) ? new Rect2i(QuestRewardsGraphic.CLAIM_X, QuestRewardsGraphic.CLAIM_Y, LargeButton.BUTTON_WIDTH, LargeButton.BUTTON_HEIGHT) : null;
            case "quest_book/open" -> page instanceof BookPage.SetsPage && QuestSetsGraphic.getSelectedSet() != null ? new Rect2i(QuestSetsGraphic.OPEN_X, QuestSetsGraphic.OPEN_Y, LargeButton.BUTTON_WIDTH, LargeButton.BUTTON_HEIGHT) : null;
            case "quest_book/description" -> {
                if (page instanceof BookPage.MainPage) yield new Rect2i(MainPageGraphic.DESCRIPTION_X, MainPageGraphic.DESCRIPTION_Y, QUEST_BOOK_TEXT_WIDTH, MainPageGraphic.VISIBLE_MAIN_DESCRIPTION_LINES * textHeight);
                if (page instanceof BookPage.SetsPage && QuestSetsGraphic.getSelectedSet() != null) yield new Rect2i(QuestSetsGraphic.DESCRIPTION_X, QuestSetsGraphic.DESCRIPTION_Y, QUEST_BOOK_TEXT_WIDTH, QuestSetsGraphic.VISIBLE_DESCRIPTION_LINES * textHeight);
                if (page instanceof BookPage.QuestPage) yield new Rect2i(QuestGraphic.START_X, QuestGraphic.DESCRIPTION_START_Y, QUEST_BOOK_TEXT_WIDTH, QuestGraphic.VISIBLE_DESCRIPTION_LINES * textHeight);
                yield null;
            }
            case "quest_book/menu/quests" -> page instanceof BookPage.MenuPage ? new Rect2i(MenuPageGraphic.INFO_LEFT_X, MenuPageGraphic.INFO_QUESTS_Y, QUEST_BOOK_TEXT_WIDTH, MenuPageGraphic.QUEST_CLICK_TEXT_Y + textHeight) : null;
            case "quest_book/menu/lives" -> page instanceof BookPage.MenuPage ? new Rect2i(MenuPageGraphic.INFO_RIGHT_X, MenuPageGraphic.INFO_LIVES_Y, QUEST_BOOK_TEXT_WIDTH, MenuPageGraphic.INFO_HEARTS_Y + 18) : null;
            case "quest_book/menu/reputation" -> {
                if (!(page instanceof BookPage.MenuPage)) yield null;
                int bars = Math.min(ReputationManager.getInstance().getReputationList().size(), MenuPageGraphic.VISIBLE_DISPLAY_REPUTATIONS);
                yield new Rect2i(MenuPageGraphic.INFO_LEFT_X, MenuPageGraphic.INFO_REPUTATION_Y, QUEST_BOOK_TEXT_WIDTH, MenuPageGraphic.INFO_REPUTATION_OFFSET_Y + bars * MenuPageGraphic.REPUTATION_OFFSET_Y);
            }
            default -> {
                // a visible quest on its quest line
                if (anchor.startsWith(TutorialScreens.QUEST_BOOK_QUEST) && page instanceof BookPage.SetMapPage map) {
                    for (Quest quest : map.getSet().getQuests().values()) {
                        if (TutorialScreens.matchesQuest(quest, anchor.substring(TutorialScreens.QUEST_BOOK_QUEST.length())) && (Quest.canQuestsBeEdited() || quest.isVisible(player))) {
                            yield new Rect2i(quest.getGuiX(), quest.getGuiY(), quest.getGuiW(), quest.getGuiH());
                        }
                    }
                }
                // a visible quest set in the set list
                if (anchor.startsWith(TutorialScreens.QUEST_BOOK_SET) && book.getPageGraphic() instanceof QuestSetsGraphic sets) {
                    List<QuestSet> rows = sets.getVisibleSets();
                    for (int i = 0; i < rows.size(); i++) {
                        if (TutorialScreens.matchesSet(rows.get(i), anchor.substring(TutorialScreens.QUEST_BOOK_SET.length()))) {
                            int rowY = QuestSetsGraphic.LIST_Y + i * (GuiBase.TEXT_HEIGHT + QuestSetsGraphic.TEXT_SPACING);
                            yield new Rect2i(QuestSetsGraphic.LIST_X, rowY, QUEST_BOOK_TEXT_WIDTH, QuestSetsGraphic.LINE_2_Y + textHeight);
                        }
                    }
                }
                yield null;
            }
        };
    }

    // If an anchor name is valid or not
    public static boolean isKnownAnchor(String anchor) {
        if (resolve(anchor) != null || isClickableAnchor(anchor)) return true;
        if (anchor.startsWith(HOTBAR + "selected/")) return TutorialItems.isKnownItem(anchor.substring((HOTBAR + "selected/").length()));
        if (anchor.startsWith(HOTBAR)) return TutorialItems.isKnownItem(anchor.substring(HOTBAR.length()));
        if (anchor.startsWith(OFFHAND)) return TutorialItems.isKnownItem(anchor.substring(OFFHAND.length()));
        return false;
    }

    // Container, MAT, and quest book anchors are the only clickable ones, and only exist while their screen is open
    public static boolean isClickableAnchor(String anchor) {
        if (anchor.matches("screen/panel|mat/panel|mat/tab_bar")) return true;
        for (String family : List.of(SLOT, INVENTORY)) {
            if (anchor.startsWith(family)) {
                String target = anchor.substring(family.length());
                return target.matches("\\d+") || TutorialItems.isKnownItem(target);
            }
        }
        if (anchor.matches("mat/info/(tutorials|play|restart|buttons|navigation|statistics)")) return true;
        if (anchor.matches("mat/tracking/(locations|delete|stop|buttons|details)")) return true;
        if (anchor.matches("quest_book/(panel|back|claim|open|description|menu/quests|menu/lives|menu/reputation)")) return true;
        if (anchor.startsWith(TutorialScreens.QUEST_BOOK_QUEST) || anchor.startsWith(TutorialScreens.QUEST_BOOK_SET)) return TutorialScreens.isKnownScreen(anchor);
        for (MatMode mode : MatMode.values()) {
            if (anchor.equals("mat/tab/" + TutorialScreens.modeName(mode))) return true;
        }
        return false;
    }
}
