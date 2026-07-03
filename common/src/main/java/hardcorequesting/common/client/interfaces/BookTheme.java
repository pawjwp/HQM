package hardcorequesting.common.client.interfaces;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

// Set of textures used by the quest book GUI
@Environment(EnvType.CLIENT)
public enum BookTheme {
    DEFAULT("book", "questmap", false, 256);

    // Texture for the background image
    public final ResourceLocation background;
    // Texture map for buttons and other icons
    public final ResourceLocation map;
    // When true, uses a full width texture instead of mirroring a texture to both sides
    public final boolean fullSpread;
    // Background texture size, should be a power of 2
    public final int sheetSize;

    BookTheme(String background, String map, boolean fullSpread, int sheetSize) {
        this.background = ResourceHelper.getResource(background);
        this.map = ResourceHelper.getResource(map);
        this.fullSpread = fullSpread;
        this.sheetSize = sheetSize;
    }
}
