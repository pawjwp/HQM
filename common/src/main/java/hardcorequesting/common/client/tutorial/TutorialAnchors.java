package hardcorequesting.common.client.tutorial;

import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

// The anchors available at which text boxes can be placed based on GUI coordinates
@Environment(EnvType.CLIENT)
public class TutorialAnchors {

    // Window anchors are locations based on the edges and corners of the screen
    @Nullable
    public static Rect2i resolve(String anchor) {
        Window window = Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        return switch (anchor) {
            case "window/top_left" -> new Rect2i(0, 0, 0, 0);
            case "window/top" -> new Rect2i(width / 2, 0, 0, 0);
            case "window/top_right" -> new Rect2i(width, 0, 0, 0);
            case "window/left" -> new Rect2i(0, height / 2, 0, 0);
            case "window/center" -> new Rect2i(width / 2, height / 2, 0, 0);
            case "window/right" -> new Rect2i(width, height / 2, 0, 0);
            case "window/bottom_left" -> new Rect2i(0, height, 0, 0);
            case "window/bottom" -> new Rect2i(width / 2, height, 0, 0);
            case "window/bottom_right" -> new Rect2i(width, height, 0, 0);
            default -> null;
        };
    }
}
