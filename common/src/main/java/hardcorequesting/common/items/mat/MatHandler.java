package hardcorequesting.common.items.mat;

import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.event.EventTrigger;
import hardcorequesting.common.inventory.MatCraftingMenu;
import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.team.PlayerEntry;
import hardcorequesting.common.util.Translator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

// Functions for opening the MAT mode
public class MatHandler {

    public static void openMatMode(Player player, MatMode mode) {
        if (!HQMConfig.getInstance().MAT.ENABLE_MAT || !mode.isEnabled()) {
            return;
        }
        switch (mode) {
            case CRAFTING -> player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new MatCraftingMenu(id, inventory),
                    Component.translatable("container.hardcorequesting.mat.crafting")));
            case QUEST -> openQuest(player);
            default -> GeneralUsage.sendOpenMat(player, mode);
        }
    }

    private static void openQuest(Player player) {
        QuestingDataManager questingData = QuestingDataManager.getInstance();
        if (!questingData.isQuestActive()) {
            player.sendSystemMessage(Translator.translatable("hqm.message.noQuestYet"));
            return;
        }
        EventTrigger.instance().onBookOpening(new EventTrigger.BookOpeningEvent(player.getUUID(), false, true));
        PlayerEntry entry = questingData.getQuestingData(player).getTeam().getEntry(player.getUUID());
        if (entry != null) {
            GeneralUsage.sendOpenMat(player, MatMode.QUEST);
        } else {
            player.sendSystemMessage(Component.translatable("hqm.message.bookNoPlayer"));
        }
    }
}
