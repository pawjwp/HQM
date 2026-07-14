package hardcorequesting.common.items.mat;

import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.event.EventTrigger;
import hardcorequesting.common.inventory.MatCraftingMenu;
import hardcorequesting.common.items.MatItem;
import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.quests.QuestingDataManager;
import hardcorequesting.common.team.PlayerEntry;
import hardcorequesting.common.util.Translator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Functions for getting/setting/opening the MAT mode
 */
public class MatHandler {

    public static boolean hasMat(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof MatItem) return true;
        }
        return false;
    }

    // Writes a mode to the player's current MAT in their inventory
    public static void setDefaultMode(Player player, MatMode mode) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof MatItem) {
                MatItem.setMode(held, mode);
                return;
            }
        }
        // If not in hand, try to set on another MAT in inventory
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof MatItem) {
                MatItem.setMode(stack, mode);
                return;
            }
        }
    }

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
