package hardcorequesting.common.items.mat;

import hardcorequesting.common.config.HQMConfig;

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

    public int getId() {
        return id;
    }

    // Translation key for the mode name ("Default", "Questing", "Crafting", etc.)
    // This is used for the item name, tab name, and keybind name.
    public String getNameKey() {
        return nameKey;
    }

    // Name (under textures/gui) of this mode's screen background.
    public String getBackgroundName() {
        return backgroundName;
    }

    // Border/outline color of the mode's tablet screen (and tab border), 0xRRGGBB.
    public int getBaseColor() {
        return baseColor;
    }

    // Fill color of the mode's tablet screen (and tab fill), 0xRRGGBB.
    public int getOverlayColor() {
        return overlayColor;
    }

    // Whether this mode is turned on in the config.
    public boolean isEnabled() {
        HQMConfig.MAT cfg = HQMConfig.getInstance().MAT;
        return switch (this) {
            case DEFAULT -> cfg.ENABLE_DEFAULT;
            case QUEST -> cfg.ENABLE_QUEST;
            case CRAFTING -> cfg.ENABLE_CRAFTING;
            case TRACKING -> cfg.ENABLE_TRACKING;
            case STORAGE -> cfg.ENABLE_STORAGE;
        };
    }

    // The next enabled mode after this one, wrapping around. Falls back to this mode if no others are enabled.
    public MatMode nextEnabled() {
        MatMode[] values = values();
        for (int i = 1; i <= values.length; i++) {
            MatMode candidate = values[(ordinal() + i) % values.length];
            if (candidate.isEnabled()) return candidate;
        }
        return this;
    }

    public static MatMode fromId(int id) {
        for (MatMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return DEFAULT;
    }

    public static List<MatMode> enabledModes() {
        List<MatMode> modes = new ArrayList<>();
        for (MatMode mode : values()) {
            if (mode.isEnabled()) modes.add(mode);
        }
        return modes;
    }
}
