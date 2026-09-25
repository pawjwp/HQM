package hardcorequesting.common.event;

import dev.architectury.hooks.level.entity.PlayerHooks;
import hardcorequesting.common.death.DeathType;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.quests.QuestingDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

public class PlayerDeathEventListener {
    public static PlayerDeathEventListener instance;
    
    public PlayerDeathEventListener() {
        instance = this;
    }
    
    public void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            QuestingDataManager.getInstance().getQuestingData(player).die(player);
            DeathType.onDeath(player, source);
        }
    }

    // If this item is considered a quest book for the purposes of losing the quest book on death or not
    public static boolean isQuestBook(ItemStack stack) {
        return stack.is(ModItems.book.get()) || stack.is(ModItems.enabledBook.get()) || stack.is(ModItems.mat.get());
    }

    // On death, move any quest book that would be dropped back into the player's inventory
    public void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame) {
        if (wonGame || PlayerHooks.isFake(newPlayer) || newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        Inventory oldInventory = oldPlayer.getInventory();
        for (int i = 0; i < oldInventory.getContainerSize(); i++) {
            ItemStack stack = oldInventory.getItem(i);
            if (isQuestBook(stack)) {
                if (newPlayer.getInventory().getItem(i).isEmpty()) {
                    newPlayer.getInventory().setItem(i, stack);
                } else {
                    newPlayer.getInventory().placeItemBackInInventory(stack);
                }
                oldInventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
