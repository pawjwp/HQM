package hardcorequesting.common.client.interfaces;

import hardcorequesting.common.HardcoreQuestingCore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Toast shown when the MAT unlocks a new tutorial, statistic, or location, based on AdvancementToast.
 */
@Environment(EnvType.CLIENT)
public class MatUnlockToast implements Toast {
    private static final ResourceLocation TEXTURE = new ResourceLocation(HardcoreQuestingCore.ID, "textures/gui/mat_toast.png");
    private static final long DURATION = 5000L;

    private final ItemStack icon;
    private final Component title;
    @Nullable
    private final Component message;

    private MatUnlockToast(ItemStack icon, Component title, @Nullable Component message) {
        this.icon = icon;
        this.title = title;
        this.message = message;
    }

    public static void show(ItemStack icon, Component title, @Nullable Component message) {
        Minecraft.getInstance().getToasts().addToast(new MatUnlockToast(icon, title, message));
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        graphics.blit(TEXTURE, 0, 0, 0, 0, width(), height());

        Font font = toastComponent.getMinecraft().font;
        if (message == null) {
            graphics.drawString(font, title, 30, 12, 0xFFFFFF00, false);
        } else {
            graphics.drawString(font, title, 30, 7, 0xFFFFFF00, false);
            graphics.drawString(font, message, 30, 18, 0xFFFFFFFF, false);
        }

        graphics.renderFakeItem(icon, 8, 8);
        return timeSinceLastVisible >= DURATION ? Visibility.HIDE : Visibility.SHOW;
    }
}