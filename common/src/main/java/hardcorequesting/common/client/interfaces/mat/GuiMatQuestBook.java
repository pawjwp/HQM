package hardcorequesting.common.client.interfaces.mat;

import hardcorequesting.common.client.interfaces.BookTheme;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.client.interfaces.ResourceHelper;
import hardcorequesting.common.items.mat.MatMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;


// The MAT's questing mode, an extension of GuiQuestBook with the MAT theme and the tab bar added on top
@Environment(EnvType.CLIENT)
public class GuiMatQuestBook extends GuiQuestBook {
    private final MatTabBar tabBar = new MatTabBar(MatMode.QUEST, TEXTURE_WIDTH);

    private GuiMatQuestBook(Player player) {
        super(player, false, BookTheme.MAT);
        this.background = ResourceHelper.getResource(MatMode.QUEST.getBackgroundName());
    }

    public static void displayGui(Player player) {
        if (player != null) {
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.screen instanceof GuiMatQuestBook)) {
                mc.setScreen(new GuiMatQuestBook(player));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        left = (width - TEXTURE_WIDTH) / 2;
        top = (height - TEXTURE_HEIGHT) / 2;
        super.render(graphics, mouseX, mouseY, partialTicks);
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
}
