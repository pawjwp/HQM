package hardcorequesting.common.client.interfaces.mat;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import hardcorequesting.common.client.interfaces.BookTheme;
import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.client.interfaces.WidgetSprites;
import hardcorequesting.common.client.interfaces.widget.ExtendedScrollBar;
import hardcorequesting.common.client.interfaces.widget.LargeButton;
import hardcorequesting.common.client.interfaces.widget.SelectableList;
import hardcorequesting.common.client.tutorial.TutorialPlayer;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.tutorial.Tutorial;
import hardcorequesting.common.tutorial.TutorialManager;
import hardcorequesting.common.util.Translator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The MAT's Default mode shows the unlocked tutorials on the left and statistics on the right
 */
@Environment(EnvType.CLIENT)
public class GuiMatDefault extends GuiBase implements MatScreen {
    private static final int HEADER_Y = 20;               // heading y on both sides
    private static final int TEXT_Y = 35;                 // first line of text content on both sides
    private static final int TUTORIAL_X = 20;             // x of the tutorial heading
    private static final int TUTORIAL_WIDTH = 137;
    private static final int TITLE_WIDTH = TUTORIAL_WIDTH - 10;  // leaves room for the completion check mark at the row's right end
    private static final int TUTORIAL_ROW_HEIGHT = 24;
    private static final int VISIBLE_TUTORIALS = 7;
    private static final int TUTORIAL_SCROLL_X = 159;
    private static final int DESCRIPTION_X = 10;          // a description's offset from its row
    private static final int DESCRIPTION_Y = 10;
    private static final float DESCRIPTION_SCALE = 0.7F;
    private static final int DESCRIPTION_WIDTH = (int) ((TUTORIAL_WIDTH - DESCRIPTION_X) / DESCRIPTION_SCALE); // the row's width past the offset, in unscaled font pixels
    private static final int PLAY_X = 36;                 // x of the play button
    private static final int RESTART_X = 95;              // x of the restart button
    private static final int BUTTON_Y = 200;              // y of both buttons
    private static final int STAT_X = 176;                // x of the statistics heading
    private static final int STAT_WIDTH = 142;
    private static final int STAT_ROW_HEIGHT = 18;        // height of each statistic row (18 pixel icons)
    private static final int VISIBLE_STATS = 9;
    private static final int STAT_SCROLL_X = 320;
    private static final int SCROLL_Y = 30;               // y of both scrollbars
    private static final int SCROLL_LENGTH = 166;         // height of both scrollbars
    private static final int STAT_ICON_X = STAT_X - 3;    // stat icons are a few pixels to the left of the stat heading
    private static final int STAT_TEXT_X = 18;            // the text's offset from its row: against the icon, which few icons fill edge to edge, and centered on the row
    private static final int STAT_TEXT_Y = 5;
    private static final int STAT_Y = TEXT_Y-STAT_TEXT_Y;
    private static final int STAT_VALUE_GAP = 4;          // the minimum gap between a statistic's name and value

    private final Player player;
    private final MatTabBar tabBar;
    private final ResourceLocation background;
    private final SelectableList<String> tutorialList;
    private final LargeButton playButton;
    private final LargeButton restartButton;
    private final ExtendedScrollBar<MatClientData.StatRow> statScroll;
    private String selectedTutorial;

    public GuiMatDefault(Player player) {
        super(CommonComponents.EMPTY);
        this.player = player;
        this.tabBar = new MatTabBar(MatMode.DEFAULT, GuiQuestBook.TEXTURE_WIDTH);
        this.mapTexture = BookTheme.MAT.map;
        this.background = ResourceHelper.getResource(MatMode.DEFAULT.getBackgroundName());
        this.widgetSprites = WidgetSprites.fromMatMode(background);

        this.tutorialList = new SelectableList<>(this, TUTORIAL_X, TEXT_Y, TUTORIAL_WIDTH, TUTORIAL_ROW_HEIGHT, VISIBLE_TUTORIALS, TUTORIAL_SCROLL_X, SCROLL_Y, SCROLL_LENGTH) {
            @Override
            protected List<String> getEntries() {
                return matData().unlockedTutorials.stream().toList();
            }

            @Override
            protected boolean isSelected(String tutorial) {
                return tutorial.equals(selectedTutorial);
            }

            // Shows a tutorial's title/description
            @Override
            protected void drawRow(GuiGraphics graphics, String id, int x, int y, int color) {
                Tutorial tutorial = TutorialManager.getInstance().tutorials.get(id);

                // Default title to tutorial ID and description to "unavailable text"
                FormattedText title = Component.literal(id);
                FormattedText description = Translator.translatable("hqm.mat.default.unavailable");

                // Set title and description
                if (tutorial != null) {
                    title = tutorial.title();
                    description = tutorial.description();
                } else if (!isSelected(id)) {
                    color = HQMConfig.TEXT_HINT;
                }
                drawString(graphics, trimToWidth(title, TITLE_WIDTH), x, y, color);
                drawString(graphics, trimToWidth(description, DESCRIPTION_WIDTH), x + DESCRIPTION_X, y + DESCRIPTION_Y, DESCRIPTION_SCALE, HQMConfig.TEXT_HINT);
                if (isCompleted(id)) {
                    drawString(graphics, Component.literal("✔"), x + TUTORIAL_WIDTH - getStringWidth("✔"), y, HQMConfig.COMPLETED_UNSELECTED_IN_BOUNDS_SET);
                }
            }

            // Cut off text shows the full title and description of the tutorial on hover
            @Override
            protected FormattedText getTooltip(String id) {
                Tutorial tutorial = TutorialManager.getInstance().tutorials.get(id);
                if (tutorial == null) {
                    if (getStringWidth(id) > TITLE_WIDTH) return Component.literal(id);
                    return null;
                }
                if (getStringWidth(tutorial.title()) <= TITLE_WIDTH && getStringWidth(tutorial.description()) <= DESCRIPTION_WIDTH) return null;
                return Component.empty().append(tutorial.title()).append("\n").append(tutorial.description().copy().withStyle(ChatFormatting.GRAY));
            }

            @Override
            protected void onRowClicked(String tutorial) {
                selectedTutorial = tutorial;
            }
        };

        // Plays the selected tutorial from its saved step, replacing any running tutorials, or pauses it if this tutorial is active
        this.playButton = new LargeButton(this, "hqm.mat.default.play", "hqm.mat.default.unavailable", PLAY_X, BUTTON_Y) {
            @Override
            public boolean isEnabled() {
                return TutorialManager.getInstance().tutorials.containsKey(selectedTutorial);
            }

            @Override
            public void onClick() {
                if (isPlaying(selectedTutorial)) TutorialPlayer.pause();
                else TutorialPlayer.start(TutorialManager.getInstance().tutorials.get(selectedTutorial), matData().tutorialProgress.getOrDefault(selectedTutorial, 0));
            }

            @Override
            protected FormattedText getName() {
                if (isPlaying(selectedTutorial)) return Translator.translatable("hqm.mat.default.pause");
                if (hasProgress(selectedTutorial)) return Translator.translatable("hqm.mat.default.resume");
                if (isCompleted(selectedTutorial)) return Translator.translatable("hqm.mat.default.replay");
                return Translator.translatable("hqm.mat.default.play");
            }

            @Override
            protected FormattedText getDescription() {
                if (isEnabled()) return null;
                return super.getDescription();
            }
        };

        // Starts the selected tutorial over from its first step, while it's running or has a saved step
        this.restartButton = new LargeButton(this, "hqm.mat.default.restart", "hqm.mat.default.unavailable", RESTART_X, BUTTON_Y) {
            @Override
            public boolean isEnabled() {
                return TutorialManager.getInstance().tutorials.containsKey(selectedTutorial) && (isPlaying(selectedTutorial) || hasProgress(selectedTutorial));
            }

            @Override
            public void onClick() {
                TutorialPlayer.start(TutorialManager.getInstance().tutorials.get(selectedTutorial), 0);
            }

            @Override
            protected FormattedText getDescription() {
                if (isEnabled()) return null;
                return super.getDescription();
            }
        };

        this.statScroll = new ExtendedScrollBar<>(this, SCROLL_LENGTH, STAT_SCROLL_X, SCROLL_Y, STAT_ICON_X, VISIBLE_STATS, () -> MatClientData.stats());
        this.selectedTutorial = TutorialPlayer.getPlayingId(); // the active tutorial is selected when opening the screen
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        left = (width - GuiQuestBook.TEXTURE_WIDTH) / 2;
        top = (height - GuiQuestBook.TEXTURE_HEIGHT) / 2;
        int x = mouseX - left;
        int y = mouseY - top;

        applyColor(0xFFFFFFFF);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(background, left, top, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT,
                0, 0, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT, BookTheme.MAT.sheetSize, BookTheme.MAT.sheetSize);

        renderTutorials(graphics, x, y);
        renderStatistics(graphics);
        statScroll.render(graphics, x, y);

        if (isPlaying(selectedTutorial)) {
            TutorialPlayer.drawNavigation(graphics, font, navigationX(), top + HEADER_Y, mouseX, mouseY,
                    0xFF000000 | HQMConfig.TEXT_HINT,    // color
                    0xFF000000 | HQMConfig.TEXT_NORMAL,  // hovered
                    0xFF000000 | HQMConfig.DISABLED_SET, // disabled
                    HQMConfig.TEXT_NORMAL     // text color
            );
        }

        tabBar.render(graphics, left, top, mouseX, mouseY);
        tutorialList.renderTooltip(graphics, x, y);
        playButton.renderTooltip(graphics, x, y);
        restartButton.renderTooltip(graphics, x, y);
        renderStatisticTooltip(graphics, x, y);
        tabBar.renderTooltip(graphics, left, top, mouseX, mouseY);
    }

    // Draws the tutorial list
    private void renderTutorials(GuiGraphics graphics, int mX, int mY) {
        drawString(graphics, Translator.translatable("hqm.mat.default.tutorials"), TUTORIAL_X, HEADER_Y, HQMConfig.TEXT_NORMAL);
        if (matData().unlockedTutorials.isEmpty()) {
            drawString(graphics, Translator.translatable("hqm.mat.default.noTutorials"), TUTORIAL_X, TEXT_Y, HQMConfig.TEXT_HINT);
        }
        tutorialList.render(graphics, mX, mY);
        playButton.render(graphics, mX, mY);
        restartButton.render(graphics, mX, mY);
    }

    // Draws the statistic list
    private void renderStatistics(GuiGraphics graphics) {
        drawString(graphics, Translator.translatable("hqm.mat.default.statistics"), STAT_X, HEADER_Y, HQMConfig.TEXT_NORMAL);
        List<MatClientData.StatRow> rows = statScroll.getVisibleEntries();
        for (int i = 0; i < rows.size(); i++) {
            MatClientData.StatRow row = rows.get(i);
            int rowY = STAT_Y + i * STAT_ROW_HEIGHT;
            row.icon().draw(graphics, left + STAT_ICON_X, top + rowY);
            drawString(graphics, trimToWidth(row.title(), getNameWidth(row)), STAT_X + STAT_TEXT_X, rowY + STAT_TEXT_Y, HQMConfig.TEXT_NORMAL);
            drawString(graphics, row.value(), STAT_X + STAT_WIDTH - getStringWidth(row.value()), rowY + STAT_TEXT_Y, HQMConfig.TEXT_HINT);
        }
    }

    // Cut off text shows the full name of the statistic on hover
    private void renderStatisticTooltip(GuiGraphics graphics, int mX, int mY) {
        List<MatClientData.StatRow> rows = statScroll.getVisibleEntries();
        for (int i = 0; i < rows.size(); i++) {
            MatClientData.StatRow row = rows.get(i);
            if (inBounds(STAT_ICON_X, STAT_Y + i * STAT_ROW_HEIGHT, STAT_X + STAT_WIDTH - STAT_ICON_X, STAT_ROW_HEIGHT, mX, mY) && getStringWidth(row.title()) > getNameWidth(row)) {
                renderTooltip(graphics, row.title(), mX + left, mY + top);
            }
        }
    }

    // The width available for a statistic's name (minus the size of the icon and value)
    private int getNameWidth(MatClientData.StatRow row) {
        return STAT_WIDTH - STAT_TEXT_X - getStringWidth(row.value()) - STAT_VALUE_GAP;
    }

    // Horizontal starting coord of the navigation bar
    private int navigationX() {
        return left + TUTORIAL_X + TUTORIAL_WIDTH - TutorialPlayer.navigationWidth(font);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabBar.mouseClicked(left, top, mouseX, mouseY)) {
            return true;
        }
        // Navigation arrows intercept clicks before the text box, even if grayed out
        if (isPlaying(selectedTutorial)) {
            if (TutorialPlayer.previousArea(navigationX(), top + HEADER_Y).contains((int) mouseX, (int) mouseY)) {
                TutorialPlayer.previousStep();
                return true;
            }
            if (TutorialPlayer.nextArea(font, navigationX(), top + HEADER_Y).contains((int) mouseX, (int) mouseY)) {
                TutorialPlayer.nextStep();
                return true;
            }
        }
        int x = (int) (mouseX - left);
        int y = (int) (mouseY - top);
        if (playButton.onClick(x, y) || restartButton.onClick(x, y) || tutorialList.onClick(x, y) || statScroll.onClick(x, y)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int x = (int) (mouseX - left);
        int y = (int) (mouseY - top);
        if (tutorialList.onDrag(x, y) || statScroll.onDrag(x, y)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int x = (int) (mouseX - left);
        int y = (int) (mouseY - top);
        if (tutorialList.onRelease(x, y) || statScroll.onRelease(x, y)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        tutorialList.onScroll(mouseX - left, mouseY - top, scroll);
        statScroll.onScroll(mouseX - left, mouseY - top, scroll);
        return true;
    }

    private boolean isCompleted(String tutorial) {
        return matData().completedTutorials.contains(tutorial);
    }

    private boolean hasProgress(String tutorial) {
        return matData().tutorialProgress.containsKey(tutorial);
    }

    private boolean isPlaying(String tutorial) {
        return tutorial != null && tutorial.equals(TutorialPlayer.getPlayingId());
    }

    private MatPlayerData matData() {
        return QuestingDataManager.getInstance().getQuestingData(player).matData;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public MatMode getMode() {
        return MatMode.DEFAULT;
    }

    @Override
    public Rect2i getPanel() {
        return new Rect2i(left, top, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT);
    }

    @Override
    public MatTabBar getTabBar() {
        return tabBar;
    }
}
