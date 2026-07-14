package hardcorequesting.common.items.mat;

import java.util.ArrayList;
import java.util.List;

/**
 * The modes that the MAT can be used in. Each one has its own
 * - ID (for the item's NBT)
 * - Translation key
 * - Background asset
 * - Base color
 * - Overlay color
 */
public enum MatMode {
    // Declared in the tab order, with the default mode in the center=
    CRAFTING(-2, "hqm.mat.mode.crafting", "mat_mode_crafting", 0x9F8B28, 0xDFDFA7),
    TRACKING(-1, "hqm.mat.mode.tracking", "mat_mode_tracking", 0x289F28, 0xB1DFA7),
    DEFAULT(0, "hqm.mat.mode.default", "mat_mode_default", 0x28959F, 0xA7DFDA),
    QUEST(1, "hqm.mat.mode.questing", "mat_mode_questing", 0x28469F, 0xA7BFDF),
    STORAGE(2, "hqm.mat.mode.storage", "mat_mode_storage", 0x5A289F, 0xC8A7DF),
    ;

    private final int id;
    private final String nameKey;
    private final String backgroundName;
    private final int baseColor;
    private final int overlayColor;

    MatMode(int id, String nameKey, String backgroundName, int baseColor, int overlayColor) {
        this.id = id;
        this.nameKey = nameKey;
        this.backgroundName = backgroundName;
        this.baseColor = baseColor;
        this.overlayColor = overlayColor;
    }
}
