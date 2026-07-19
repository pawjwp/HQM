package hardcorequesting.common.items.datachip;

import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatUnlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// Unlocks the tutorial named by its NBT "Tutorial" id.
public class TutorialDataChipItem extends DataChipItem {

    public TutorialDataChipItem() {
        super(MatMode.QUEST);
    }

    @Override
    protected boolean apply(ServerPlayer player, ItemStack stack) {
        String id = stack.getOrCreateTag().getString("Tutorial");
        if (id.isEmpty()) {
            player.sendSystemMessage(Component.translatable("hqm.mat.chip.invalid"));
            return false;
        }
        return switch (MatUnlocks.unlockTutorial(player, id, true)) {
            case UNLOCKED -> true;
            case ALREADY -> feedback(player, "hqm.mat.chip.alreadyUnlocked");
            case INVALID -> feedback(player, "hqm.mat.chip.invalid");
        };
    }

    private static boolean feedback(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return false;
    }

    @Environment(EnvType.CLIENT)
    @Override
    protected void appendSummary(ItemStack stack, List<Component> tooltip) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Tutorial")) {
            tooltip.add(Component.literal(tag.getString("Tutorial")).withStyle(ChatFormatting.AQUA));
        }
    }
}
