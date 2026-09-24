package hardcorequesting.common.client.interfaces;

import hardcorequesting.common.config.HQMConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

// Displays the GUI at the configured scale when HQM's GUIs are open.
@Environment(EnvType.CLIENT)
public final class UIScale {
    private UIScale() {}

    // The scale of vanilla's GUI scale
    private static int vanillaScale() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getWindow().calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode());
    }

    // HQM's scale, equal to the user-configured GUI scale multiplied by HQM's configured scale.
    // It is rounded down to the nearest integer and clamped to valid screen values (between 1 and the max available size).
    public static int hqmUIScale() {
        Minecraft mc = Minecraft.getInstance();
        int max = mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
        return Mth.clamp((int) (vanillaScale() * HQMConfig.getInstance().Interface.UI_SCALE_MULTIPLIER), 1, max);
    }

    public static void apply() {
        Minecraft.getInstance().getWindow().setGuiScale(hqmUIScale());
    }

    public static void restore() {
        Minecraft.getInstance().getWindow().setGuiScale(vanillaScale());
    }
}
