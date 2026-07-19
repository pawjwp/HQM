package hardcorequesting.common.items.datachip;

import hardcorequesting.common.items.mat.MatMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * A data chip is used to "upload" unlocked information to be used in a MAT unit.
 */
public abstract class DataChipItem extends Item {
    private final MatMode themeMode;

    protected DataChipItem(MatMode themeMode) {
        super(new Item.Properties().stacksTo(16));
        this.themeMode = themeMode;
    }

    // The chip's texture is made of stacked tinted layers like the MAT and vanilla spawn eggs
    public int getBaseColor() {
        return themeMode.getBaseColor();
    }

    public int getOverlayColor() {
        return themeMode.getOverlayColor();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        if (apply(serverPlayer, stack)) {
            if (shouldConsume(stack)) stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    // Applies the chip's feature unlock, returning true if successful
    protected abstract boolean apply(ServerPlayer player, ItemStack stack);

    // Consumes unless NBT contains Consume:false
    private static boolean shouldConsume(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains("Consume") || tag.getBoolean("Consume");
    }

    // Tooltip
    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("hqm.mat.chip.usage").withStyle(ChatFormatting.GRAY));
        appendSummary(stack, tooltip);
    }

    @Environment(EnvType.CLIENT)
    protected abstract void appendSummary(ItemStack stack, List<Component> tooltip);
}
