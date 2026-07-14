package hardcorequesting.common.client.interfaces.mat;

import com.mojang.blaze3d.systems.RenderSystem;
import hardcorequesting.common.client.interfaces.BookTheme;
import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.util.Translator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

// A placeholder screen for the incomplete MAT modes with label
@Environment(EnvType.CLIENT)
public class GuiMatPlaceholder extends GuiBase {
    private final MatMode mode;
    private final MatTabBar tabBar;
    private final ResourceLocation background;

    public GuiMatPlaceholder(Player player, MatMode mode) {
        super(CommonComponents.EMPTY);
        this.mode = mode;
        this.tabBar = new MatTabBar(mode, GuiQuestBook.TEXTURE_WIDTH);
        this.mapTexture = BookTheme.MAT.map;
        this.background = ResourceHelper.getResource(mode.getBackgroundName());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        left = (width - GuiQuestBook.TEXTURE_WIDTH) / 2;
        top = (height - GuiQuestBook.TEXTURE_HEIGHT) / 2;

        applyColor(0xFFFFFFFF);
        // Allow transparent pixels
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(background, left, top, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT,
                0, 0, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT, BookTheme.MAT.sheetSize, BookTheme.MAT.sheetSize);

        applyColor(0xFFFFFFFF);
        drawCenteredString(graphics, Translator.translatable(mode.getNameKey()), 0, -font.lineHeight,
                1F, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT, HQMConfig.TEXT_NORMAL);
        drawCenteredString(graphics, Translator.translatable("hqm.mat.wip"), 0, font.lineHeight,
                1F, GuiQuestBook.TEXTURE_WIDTH, GuiQuestBook.TEXTURE_HEIGHT, HQMConfig.TEXT_HINT);

        // Render tab bar
        tabBar.render(graphics, left, top, mouseX, mouseY);
        tabBar.renderTooltip(graphics, left, top, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabBar.mouseClicked(left, top, mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
