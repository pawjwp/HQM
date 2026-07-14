package hardcorequesting.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatHandler;
import hardcorequesting.common.items.mat.MatMode;
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
    public static KeyMapping openMatDefault;
    public static KeyMapping openMatCrafting;
    public static KeyMapping openMatTracking;
    public static KeyMapping openMatStorage;

    public static void register() {
        openBook = register("key.hqm.openBook");
        openMatDefault = register("key.hqm.mat.default");
        openMatCrafting = register("key.hqm.mat.crafting");
        openMatTracking = register("key.hqm.mat.tracking");
        openMatStorage = register("key.hqm.mat.storage");
    }

    private static KeyMapping register(String name) {
        KeyMapping mapping = new KeyMapping(name, InputConstants.UNKNOWN.getValue(), CATEGORY);
        KeyMappingRegistry.register(mapping);
        return mapping;
    }

    public static void handleTick(Minecraft mc) {
        while (openBook.consumeClick()) {
            if (mc.screen instanceof GuiQuestBook) {
                if (HQMConfig.getInstance().Keybind.TOGGLE) mc.setScreen(null);
            } else if (mc.screen == null && mc.player != null) {
                if (matEnabled() && MatMode.QUEST.isEnabled() && MatHandler.hasMat(mc.player)) {
                    GeneralUsage.sendOpenMatMode(MatMode.QUEST, false);
                } else {
                    GeneralUsage.REQUEST_BOOK_OPEN.sendMessageToServer(new CompoundTag());
                }
            }
        }

        handleModeKey(mc, openMatDefault, MatMode.DEFAULT);
        handleModeKey(mc, openMatCrafting, MatMode.CRAFTING);
        handleModeKey(mc, openMatTracking, MatMode.TRACKING);
        handleModeKey(mc, openMatStorage, MatMode.STORAGE);
    }

    private static void handleModeKey(Minecraft mc, KeyMapping mapping, MatMode mode) {
        while (mapping.consumeClick()) {
            if (mc.screen == null && mc.player != null && matEnabled() && mode.isEnabled() && MatHandler.hasMat(mc.player)) {
                GeneralUsage.sendOpenMatMode(mode, false);
            }
        }
    }

    private static boolean matEnabled() {
        return HQMConfig.getInstance().MAT.ENABLE_MAT;
    }
}
