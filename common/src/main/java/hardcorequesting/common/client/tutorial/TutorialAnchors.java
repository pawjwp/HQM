package hardcorequesting.common.client.tutorial;

import com.mojang.blaze3d.platform.Window;
import hardcorequesting.common.HardcoreQuestingCore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * The anchors available at which text boxes can be placed based on GUI coordinates
 * - Window anchors are locations based on the bounds of the screen
 * - HUD anchors are locations on the vanilla HUD
 * - Container anchors are locations on an active container UI
 */
@Environment(EnvType.CLIENT)
public class TutorialAnchors {
    @Nullable
    public static Rect2i resolve(String anchor) {
        Window window = Minecraft.getInstance().getWindow();
        Player player = Minecraft.getInstance().player;
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        int horizontalCenter = width / 2;
        int verticalCenter = height / 2;

        // Calculates hotbar locations, 20 pixels each
        if (anchor.matches("hud/hotbar/[0-8]")) {
            int slot = anchor.charAt(anchor.length() - 1) - '0';
            return new Rect2i(horizontalCenter - 90 + 20 * slot, height - 22, 20, 22);
        }

        // Container anchors are only valid when a container screen is open
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) {
            Rect2i panel = HardcoreQuestingCore.platform.getContainerPanel(screen);
            if (anchor.equals("screen/panel")) return panel;
            for (Slot slot : screen.getMenu().slots) {
                if (anchor.equals("slot/" + slot.index) || (slot.container == player.getInventory() && anchor.equals("inventory/" + slot.getContainerSlot()))) {
                    return new Rect2i(panel.getX() + slot.x - 1, panel.getY() + slot.y - 1, 18, 18);
                }
            }
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

    // If an anchor name is valid or not
    public static boolean isKnownAnchor(String anchor) {
        return resolve(anchor) != null || isContainerAnchor(anchor);
    }

    // Container anchors are the only clickable ones
    public static boolean isContainerAnchor(String anchor) {
        return anchor.matches("slot/\\d+|inventory/\\d+|screen/panel");
    }
}
