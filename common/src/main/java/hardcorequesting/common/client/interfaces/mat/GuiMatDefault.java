package hardcorequesting.common.client.interfaces.mat;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import hardcorequesting.common.client.interfaces.BookTheme;
import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.util.Translator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The MAT's Default mode contains the player's unlocked tutorials on the left and their unlocked statistics on the right.
 */
@Environment(EnvType.CLIENT)
public class GuiMatDefault extends GuiBase {
    private static final int COLUMN_TOP = 22;      // y of each column's header
    private static final int LIST_TOP = 36;        // y of the first row under a header
    private static final int ROW_HEIGHT = 12;
    private static final int LEFT_COLUMN_X = 14;   // tutorials
    private static final int RIGHT_COLUMN_X = 184; // statistics
    private static final int COLUMN_WIDTH = 142;

    private final Player player;
    private final MatTabBar tabBar;
    private final ResourceLocation background;

    public GuiMatDefault(Player player) {
        super(CommonComponents.EMPTY);
        this.player = player;
        this.tabBar = new MatTabBar(MatMode.DEFAULT, GuiQuestBook.TEXTURE_WIDTH);
        this.mapTexture = BookTheme.MAT.map;
        this.background = ResourceHelper.getResource(MatMode.DEFAULT.getBackgroundName());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        left = (width - GuiQuestBook.TEXTURE_WIDTH) / 2;
        top = (height - GuiQuestBook.TEXTURE_HEIGHT) / 2;

        applyColor(0xFFFFFFFF);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(background, left, top, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT,
                0, 0, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT, BookTheme.MAT.sheetSize, BookTheme.MAT.sheetSize);

        renderTutorials(graphics);
        renderStatistics(graphics);

        tabBar.render(graphics, left, top, mouseX, mouseY);
        tabBar.renderTooltip(graphics, left, top, mouseX, mouseY);
    }

    private void renderTutorials(GuiGraphics graphics) {
        drawString(graphics, Translator.translatable("hqm.mat.default.tutorials"), LEFT_COLUMN_X, COLUMN_TOP, HQMConfig.TEXT_NORMAL);
        List<String> tutorials = matData().unlockedTutorials.stream().toList();
        if (tutorials.isEmpty()) {
            drawString(graphics, Translator.translatable("hqm.mat.default.noTutorials"), LEFT_COLUMN_X, LIST_TOP, HQMConfig.TEXT_HINT);
            return;
        }
        int y = LIST_TOP;
        for (String id : tutorials) {
            boolean completed = matData().completedTutorials.contains(id);
            Component title = Component.literal(id);
            drawString(graphics, title, LEFT_COLUMN_X, y, HQMConfig.TEXT_HINT);
            if (completed) {
                drawString(graphics, Component.literal("✔"), LEFT_COLUMN_X + COLUMN_WIDTH - 8, y, HQMConfig.COMPLETED_UNSELECTED_IN_BOUNDS_SET);
            }
            y += ROW_HEIGHT;
        }
    }

    private void renderStatistics(GuiGraphics graphics) {
        drawString(graphics, Translator.translatable("hqm.mat.default.statistics"), RIGHT_COLUMN_X, COLUMN_TOP, HQMConfig.TEXT_NORMAL);
        List<MatClientData.StatRow> rows = MatClientData.stats();
        int y = LIST_TOP;
        for (MatClientData.StatRow row : rows) {
            drawString(graphics, row.title(), RIGHT_COLUMN_X, y, HQMConfig.TEXT_NORMAL);
            int valueWidth = getStringWidth(row.value());
            drawString(graphics, row.value(), RIGHT_COLUMN_X + COLUMN_WIDTH - valueWidth, y, HQMConfig.TEXT_HINT);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabBar.mouseClicked(left, top, mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private MatPlayerData matData() {
        return QuestingDataManager.getInstance().getQuestingData(player).matData;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}