package hardcorequesting.common.client.interfaces.mat;

import hardcorequesting.common.items.mat.MatMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Player;

// Clientside MAT opening for certain modes
@Environment(EnvType.CLIENT)
public class MatScreens {
    public static void open(Player player, MatMode mode, CompoundTag payload) {
        if (player == null) return;
        closeOpenContainer();
        switch (mode) {
            case QUEST -> GuiMatQuestBook.displayGui(player);
            case DEFAULT -> {
                MatClientData.update(payload);
                Minecraft.getInstance().setScreen(new GuiMatDefault(player));
            }
            default -> Minecraft.getInstance().setScreen(new GuiMatPlaceholder(player, mode));
        }
    }

    // Used to properly close server containers when switching to another tab
    private static void closeOpenContainer() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && player.containerMenu != player.inventoryMenu) {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundContainerClosePacket(player.containerMenu.containerId));
            }
            player.containerMenu = player.inventoryMenu;
        }
    }
}