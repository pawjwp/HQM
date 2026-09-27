package hardcorequesting.common.client.interfaces;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The locations of a screen's scrollbar and large button sprites
 */
public record WidgetSprites(ResourceLocation texture, int textureSize, UV longTrack, UV handle, UV button, UV hoveredButton, @Nullable UV disabledButton) {
    public record UV(int u, int v) {}

    // Sprites on a quest book sheet
    public static WidgetSprites fromTheme(ResourceLocation themeMap) {
        return new WidgetSprites(themeMap, 256, new UV(171, 69), new UV(250, 167), new UV(54, 235), new UV(111, 235), null);
    }

    // The sprites on a MAT mode sheet
    public static WidgetSprites fromMatMode(ResourceLocation matBackground) {
        return new WidgetSprites(matBackground, BookTheme.MAT.sheetSize, new UV(344, 0), new UV(353, 0), new UV(362, 0), new UV(362, 18), new UV(362, 36));
    }
}
