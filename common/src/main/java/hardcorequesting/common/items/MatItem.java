package hardcorequesting.common.items;

import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.config.HQMConfig;
import hardcorequesting.common.items.mat.MatHandler;
import hardcorequesting.common.items.mat.MatMode;
import hardcorequesting.common.items.mat.MatPlayerData;
import hardcorequesting.common.items.mat.TrackedLocation;
import hardcorequesting.common.quests.QuestingDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.Optional;

/**
 * MAT (Multipurpose Assistance Terminal) item
 * ENABLE_MAT must be enabled to use it
 */
public class MatItem extends Item {
    private static final String TAG_MODE = "Mode";
    // Compass target NBT, taken from the user's tracked location
    public static final String TAG_TARGET_POS = "TargetPos";
    public static final String TAG_TARGET_DIMENSION = "TargetDimension";
    public static final String TAG_TARGET_TRACKED = "TargetTracked";

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

    // The pointer faces this target using CompassItemPropertyFunction
    public static boolean hasTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && (tag.contains(TAG_TARGET_DIMENSION) || tag.contains(TAG_TARGET_POS));
    }

    private static Optional<ResourceKey<Level>> getTargetDimension(CompoundTag tag) {
        return Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_TARGET_DIMENSION)).result();
    }

    @Nullable
    public static GlobalPos getTargetPosition(CompoundTag tag) {
        boolean hasPos = tag.contains(TAG_TARGET_POS);
        boolean hasDimension = tag.contains(TAG_TARGET_DIMENSION);
        if (hasPos && hasDimension) {
            Optional<ResourceKey<Level>> dimension = getTargetDimension(tag);
            if (dimension.isPresent()) {
                return GlobalPos.of(dimension.get(), NbtUtils.readBlockPos(tag.getCompound(TAG_TARGET_POS)));
            }
        }
        return null;
    }

    private static void setTarget(ResourceKey<Level> dimension, BlockPos pos, CompoundTag tag) {
        tag.put(TAG_TARGET_POS, NbtUtils.writeBlockPos(pos));
        Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, dimension).resultOrPartial(message -> HardcoreQuestingCore.LOGGER.error(message))
                .ifPresent(encoded -> tag.put(TAG_TARGET_DIMENSION, encoded));
        tag.putBoolean(TAG_TARGET_TRACKED, true);
    }

    // Pulls target from the user's tracked location
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
        TrackedLocation location = null;
        if (mat.selectedLocation >= 0 && mat.selectedLocation < mat.locations.size()) {
            location = mat.locations.get(mat.selectedLocation);
        }
        if (location == null) {
            if (hasTarget(stack)) {
                CompoundTag tag = stack.getTag();
                tag.remove(TAG_TARGET_POS);
                tag.remove(TAG_TARGET_DIMENSION);
                tag.remove(TAG_TARGET_TRACKED);
            }
        } else if (!matchesTarget(stack, location)) {
            setTarget(ResourceKey.create(Registries.DIMENSION, location.dimension()), location.pos(), stack.getOrCreateTag());
        }
    }

    private static boolean matchesTarget(ItemStack stack, TrackedLocation location) {
        if (!hasTarget(stack)) return false;
        GlobalPos target = getTargetPosition(stack.getTag());
        return target != null
                && target.dimension().location().equals(location.dimension())
                && target.pos().equals(location.pos());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack), Component.translatable(getMode(stack).getNameKey()));
    }
}
