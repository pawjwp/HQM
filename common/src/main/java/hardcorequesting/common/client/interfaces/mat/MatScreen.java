package hardcorequesting.common.client.interfaces.mat;

import hardcorequesting.common.items.mat.MatMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Rect2i;

// A MAT screen's mode, panel's rectangle, and tab bar
@Environment(EnvType.CLIENT)
public interface MatScreen {
    MatMode getMode();

    Rect2i getPanel();

    MatTabBar getTabBar();
}
