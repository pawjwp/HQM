package hardcorequesting.common.items;

import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatHandler;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.items.mat.TrackedLocation;
import hardcorequesting.common.quests.QuestingDataManager;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MAT (Multipurpose Assistance Terminal) item
 * ENABLE_MAT must be enabled to use it
 */
public class MatItem extends Item {
    private static final String TAG_MODE = "Mode";

    public MatItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static MatMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MODE)) {
            return MatMode.fromId(tag.getInt(TAG_MODE));
        }
        return MatMode.DEFAULT;
    }

    public static void setMode(ItemStack stack, MatMode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.getId());
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Open MAT or switch MAT mode if holding shift
        if (!world.isClientSide && HQMConfig.getInstance().MAT.ENABLE_MAT) {
            if (player.isSecondaryUseActive()) {
                // Switch mode if holding shift
                MatMode mode = getMode(stack).nextEnabled();
                setMode(stack, mode);
                world.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS,
                        0.4F, 0.5F + 0.1F * mode.getId());
            } else {
                // Open MAT if not holding shift
                MatHandler.openMatMode(player, getMode(stack));
            }
            return InteractionResultHolder.success(stack);
        }
        return super.use(world, player, hand);
    }

    // If the MAT is in tracking mode and the pointer is visible
    public static boolean isTracking(ItemStack stack, Entity holder) {
        return getMode(stack) == MatMode.TRACKING && selectedLocation(holder) != null;
    }

    // Returns the target location from MAT data
    @Nullable
    public static GlobalPos getTrackedTarget(Entity holder) {
        TrackedLocation location = selectedLocation(holder);
        if (location == null) return null;
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, location.dimension()), location.pos());
    }

    @Nullable
    private static TrackedLocation selectedLocation(Entity holder) {
        if (!(holder instanceof Player player)) return null;
        // Don't initialize questing data just from trying to render the MAT
        if (!QuestingDataManager.getInstance().hasData(player)) return null;
        MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
        if (mat.selectedLocation < 0 || mat.selectedLocation >= mat.locations.size()) return null;
        return mat.locations.get(mat.selectedLocation);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack), Component.translatable(getMode(stack).getNameKey()));
    }
}
