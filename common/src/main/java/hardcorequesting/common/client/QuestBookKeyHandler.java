package hardcorequesting.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.network.GeneralUsage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

@Environment(EnvType.CLIENT)
public class QuestBookKeyHandler {
    public static final String CATEGORY = "key.hqm.category";
    public static KeyMapping openBook;

    public static void register() {
        openBook = new KeyMapping("key.hqm.openBook", InputConstants.UNKNOWN.getValue(), CATEGORY);
        KeyMappingRegistry.register(openBook);
    }

    public static void handleTick(Minecraft mc) {
        while (openBook.consumeClick()) {
            if (mc.screen instanceof GuiQuestBook) {
                if (HQMConfig.getInstance().Keybind.TOGGLE) mc.setScreen(null);
            } else if (mc.screen == null && mc.player != null) {
                GeneralUsage.REQUEST_BOOK_OPEN.sendMessageToServer(new CompoundTag());
            }
        }
    }
}
