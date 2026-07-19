package hardcorequesting.common.items.datachip;

import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatUnlocks;
import hardcorequesting.common.items.mat.StatKey;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Unlocks a statistic from either a StatType:Stat pair or a custom stat.
 */

public class StatisticDataChipItem extends DataChipItem {

    public StatisticDataChipItem() {
        super(MatMode.DEFAULT);
    }

    @Override
    protected boolean apply(ServerPlayer player, ItemStack stack) {
        String key = statKey(stack.getOrCreateTag());
        if (key == null) {
            player.sendSystemMessage(Component.translatable("hqm.mat.chip.invalid"));
            return false;
        }
        return switch (MatUnlocks.unlockStat(player, key, true)) {
            case UNLOCKED -> true;
            case ALREADY -> feedback(player, "hqm.mat.chip.alreadyUnlocked");
            case INVALID -> feedback(player, "hqm.mat.chip.invalid");
        };
    }

    // Builds the stat key from the chip's NBT, or null if none is present.
    private static String statKey(CompoundTag tag) {
        String stat = tag.getString("Stat");
        if (stat.isEmpty()) {
            return null;
        }
        if (tag.contains("StatType")) {
            return StatKey.vanillaKey(tag.getString("StatType"), stat);
        }
        return stat;
    }

    private static boolean feedback(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return false;
    }

    @Environment(EnvType.CLIENT)
    @Override
    protected void appendSummary(ItemStack stack, List<Component> tooltip) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        String key = statKey(tag);
        if (key != null) {
            tooltip.add(StatKey.displayName(key).copy().withStyle(ChatFormatting.AQUA));
        }
    }
}