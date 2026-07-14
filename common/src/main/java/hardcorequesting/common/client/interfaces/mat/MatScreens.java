package hardcorequesting.common.client.interfaces.mat;

import hardcorequesting.common.items.mat.MatMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

// Clientside MAT opening for certain modes
@Environment(EnvType.CLIENT)
public class MatScreens {
    public static void open(Player player, MatMode mode) {
        if (player == null) return;
        if (mode == MatMode.QUEST) {
            GuiMatQuestBook.displayGui(player);
        } else {
            Minecraft.getInstance().setScreen(new GuiMatPlaceholder(player, mode));
        }
    }
}