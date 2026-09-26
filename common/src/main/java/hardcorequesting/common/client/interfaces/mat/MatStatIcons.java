package hardcorequesting.common.client.interfaces.mat;

import com.mojang.blaze3d.systems.RenderSystem;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.client.interfaces.GuiBase;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.items.mat.StatKey;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Chooses the icon associated with each statistic, used in the MAT's statistic screen.
 */
@Environment(EnvType.CLIENT)
public class MatStatIcons {
    private static final int SIZE = 18;

    // Draws a statistic icon in the provided screen
    public interface Icon {
        void draw(GuiBase gui, GuiGraphics graphics, int x, int y);
    }
    
    // Icons for custom statistics that can't be derived automatically
    private static final Map<ResourceLocation, Icon> CUSTOM = Map.ofEntries(
        
        // Misc
        Map.entry(Stats.CROUCH_TIME, effect(MobEffects.MOVEMENT_SLOWDOWN)),
        Map.entry(Stats.DEATHS, item(Items.SKELETON_SKULL)),
        Map.entry(Stats.DROP, item(Items.HOPPER)),
        Map.entry(Stats.FISH_CAUGHT, item(Items.FISHING_ROD)),
        Map.entry(Stats.LEAVE_GAME, item(Items.BARRIER)),
        Map.entry(Stats.PLAY_TIME, item(Items.CLOCK)),
        Map.entry(Stats.RAID_TRIGGER, effect(MobEffects.BAD_OMEN)),
        Map.entry(Stats.RAID_WIN, effect(MobEffects.HERO_OF_THE_VILLAGE)),
        Map.entry(Stats.TIME_SINCE_DEATH, item(Items.RECOVERY_COMPASS)),
        Map.entry(Stats.TIME_SINCE_REST, item(Items.RED_BED)),
        Map.entry(Stats.TOTAL_WORLD_TIME, item(Items.CLOCK)),
        
        // Damage
        Map.entry(Stats.DAMAGE_ABSORBED, effect(MobEffects.ABSORPTION)),
        Map.entry(Stats.DAMAGE_BLOCKED_BY_SHIELD, item(Items.SHIELD)),
        Map.entry(Stats.DAMAGE_DEALT_ABSORBED, effect(MobEffects.WEAKNESS)),
        Map.entry(Stats.DAMAGE_DEALT_RESISTED, effect(MobEffects.WEAKNESS)),
        Map.entry(Stats.DAMAGE_DEALT, effect(MobEffects.DAMAGE_BOOST)),
        Map.entry(Stats.DAMAGE_RESISTED, effect(MobEffects.DAMAGE_RESISTANCE)),
        Map.entry(Stats.DAMAGE_TAKEN, effect(MobEffects.HARM)),
        
        // Movement
        Map.entry(Stats.AVIATE_ONE_CM, item(Items.ELYTRA)),
        Map.entry(Stats.BOAT_ONE_CM, item(Items.OAK_BOAT)),
        Map.entry(Stats.CLIMB_ONE_CM, item(Items.LADDER)),
        Map.entry(Stats.CROUCH_ONE_CM, effect(MobEffects.MOVEMENT_SPEED)),
        Map.entry(Stats.FALL_ONE_CM, effect(MobEffects.SLOW_FALLING)),
        Map.entry(Stats.FLY_ONE_CM, effect(MobEffects.SLOW_FALLING)),
        Map.entry(Stats.HORSE_ONE_CM, item(Items.SADDLE)),
        Map.entry(Stats.JUMP, effect(MobEffects.JUMP)),
        Map.entry(Stats.MINECART_ONE_CM, item(Items.MINECART)),
        Map.entry(Stats.PIG_ONE_CM, item(Items.CARROT_ON_A_STICK)),
        Map.entry(Stats.SPRINT_ONE_CM, effect(MobEffects.MOVEMENT_SPEED)),
        Map.entry(Stats.STRIDER_ONE_CM, item(Items.WARPED_FUNGUS_ON_A_STICK)),
        Map.entry(Stats.SWIM_ONE_CM, effect(MobEffects.DOLPHINS_GRACE)),
        Map.entry(Stats.WALK_ON_WATER_ONE_CM, effect(MobEffects.WATER_BREATHING)),
        Map.entry(Stats.WALK_ONE_CM, effect(MobEffects.MOVEMENT_SPEED)),
        Map.entry(Stats.WALK_UNDER_WATER_ONE_CM, effect(MobEffects.WATER_BREATHING)),
        
        // Mob Interaction
        Map.entry(Stats.ANIMALS_BRED, item(Items.WHEAT)),
        Map.entry(Stats.MOB_KILLS, item(Items.ZOMBIE_HEAD)),
        Map.entry(Stats.PLAYER_KILLS, item(Items.PLAYER_HEAD)),
        Map.entry(Stats.TALKED_TO_VILLAGER, item(Items.VILLAGER_SPAWN_EGG)),
        Map.entry(Stats.TRADED_WITH_VILLAGER, item(Items.EMERALD)),
        
        // Block Interaction
        Map.entry(Stats.BELL_RING, item(Items.BELL)),
        Map.entry(Stats.CLEAN_ARMOR, item(Items.CAULDRON)),
        Map.entry(Stats.CLEAN_BANNER, item(Items.CAULDRON)),
        Map.entry(Stats.CLEAN_SHULKER_BOX, item(Items.CAULDRON)),
        Map.entry(Stats.EAT_CAKE_SLICE, item(Items.CAKE)),
        Map.entry(Stats.ENCHANT_ITEM, item(Items.ENCHANTING_TABLE)),
        Map.entry(Stats.FILL_CAULDRON, item(Items.CAULDRON)),
        Map.entry(Stats.INSPECT_DISPENSER, item(Items.DISPENSER)),
        Map.entry(Stats.INSPECT_DROPPER, item(Items.DROPPER)),
        Map.entry(Stats.INSPECT_HOPPER, item(Items.HOPPER)),
        Map.entry(Stats.INTERACT_WITH_ANVIL, item(Items.ANVIL)),
        Map.entry(Stats.INTERACT_WITH_BEACON, item(Items.BEACON)),
        Map.entry(Stats.INTERACT_WITH_BLAST_FURNACE, item(Items.BLAST_FURNACE)),
        Map.entry(Stats.INTERACT_WITH_BREWINGSTAND, item(Items.BREWING_STAND)),
        Map.entry(Stats.INTERACT_WITH_CAMPFIRE, item(Items.CAMPFIRE)),
        Map.entry(Stats.INTERACT_WITH_CARTOGRAPHY_TABLE, item(Items.CARTOGRAPHY_TABLE)),
        Map.entry(Stats.INTERACT_WITH_CRAFTING_TABLE, item(Items.CRAFTING_TABLE)),
        Map.entry(Stats.INTERACT_WITH_FURNACE, item(Items.FURNACE)),
        Map.entry(Stats.INTERACT_WITH_GRINDSTONE, item(Items.GRINDSTONE)),
        Map.entry(Stats.INTERACT_WITH_LECTERN, item(Items.LECTERN)),
        Map.entry(Stats.INTERACT_WITH_LOOM, item(Items.LOOM)),
        Map.entry(Stats.INTERACT_WITH_SMITHING_TABLE, item(Items.SMITHING_TABLE)),
        Map.entry(Stats.INTERACT_WITH_SMOKER, item(Items.SMOKER)),
        Map.entry(Stats.INTERACT_WITH_STONECUTTER, item(Items.STONECUTTER)),
        Map.entry(Stats.OPEN_BARREL, item(Items.BARREL)),
        Map.entry(Stats.OPEN_CHEST, item(Items.CHEST)),
        Map.entry(Stats.OPEN_ENDERCHEST, item(Items.ENDER_CHEST)),
        Map.entry(Stats.OPEN_SHULKER_BOX, item(Items.SHULKER_BOX)),
        Map.entry(Stats.PLAY_NOTEBLOCK, item(Items.NOTE_BLOCK)),
        Map.entry(Stats.PLAY_RECORD, item(Items.JUKEBOX)),
        Map.entry(Stats.POT_FLOWER, item(Items.FLOWER_POT)),
        Map.entry(Stats.SLEEP_IN_BED, item(Items.RED_BED)),
        Map.entry(Stats.TARGET_HIT, item(Items.TARGET)),
        Map.entry(Stats.TRIGGER_TRAPPED_CHEST, item(Items.TRAPPED_CHEST)),
        Map.entry(Stats.TUNE_NOTEBLOCK, item(Items.NOTE_BLOCK)),
        Map.entry(Stats.USE_CAULDRON, item(Items.CAULDRON))
    );

    // Returns the icon resource, prioritizing textures defined in textures/gui/stat_icons/<statistic key>.
    // If those icons are not preset, falls back to the items defined below or a stat data chip.
    public static Icon get(String key) {
        ResourceLocation texture = new ResourceLocation(HardcoreQuestingCore.ID, "textures/gui/stat_icons/" + key.replace(':', '/') + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            return texture(texture);
        }
        Icon icon = fromCode(key);
        if (icon != null) {
            return icon;
        }
        return item(ModItems.statisticDataChip.get());
    }

    // The icon, derived from items or effects, based on the stat's associated item, block, or mob, or the CUSTOM map above
    @Nullable
    private static Icon fromCode(String key) {
        if (StatKey.isLifeSign(key)) {
            return item(Items.COMPASS);
        }
        Stat<?> stat = StatKey.resolveVanilla(key);
        if (stat == null) {
            return null;
        }
        Object value = stat.getValue();
        if (value instanceof ResourceLocation id) {
            return CUSTOM.get(id);
        }
        if (value instanceof ItemLike itemLike && itemLike.asItem() != Items.AIR) {
            return item(itemLike.asItem());
        }
        if (value == EntityType.PLAYER) {
            return item(Items.PLAYER_HEAD);
        }
        if (value instanceof EntityType<?> type && SpawnEggItem.byId(type) != null) {
            return item(SpawnEggItem.byId(type));
        }
        return null;
    }

    // Items used as icons are 16x16, centered
    private static Icon item(Item item) {
        ItemStack stack = new ItemStack(item);
        return (gui, graphics, x, y) -> gui.drawItemStack(graphics, stack, x + 1, y + 1, false);
    }

    // Status effects used as icons are 18x18
    private static Icon effect(MobEffect effect) {
        return (gui, graphics, x, y) -> {
            RenderSystem.enableBlend();
            graphics.blit(gui.getLeft() + x, gui.getTop() + y, 0, SIZE, SIZE, Minecraft.getInstance().getMobEffectTextures().get(effect));
        };
    }

    // Custom textures used as icons are taken from textures/gui/stat_icons/<statistic key>
    private static Icon texture(ResourceLocation texture) {
        return (gui, graphics, x, y) -> {
            RenderSystem.enableBlend();
            graphics.blit(texture, gui.getLeft() + x, gui.getTop() + y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        };
    }
}
