package hardcorequesting.common.items.datachip;

import hardcorequesting.common.items.mat.LocationResolver;
import hardcorequesting.common.items.mat.MatUnlocks;
import hardcorequesting.common.items.mat.TrackedLocation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Unlocks tracked locations in the form of coordinates, structures, or biomes.
 * NBT can also include the optional "Name" and "Dimension" fields.
 */
public class LocationDataChipItem extends DataChipItem {

    @Override
    protected boolean apply(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();

        ResourceLocation dimensionId = null;
        if (tag.contains("Dimension")) {
            dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        }
        ServerLevel target = LocationResolver.level(player, dimensionId);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("hqm.mat.chip.invalid"));
            return false;
        }

        String name = locationName(tag);
        BlockPos anchor = LocationResolver.scaledAnchor(player, target);
        List<TrackedLocation> results = new ArrayList<>();

        if (tag.contains("Coordinates")) {
            CompoundTag coords = tag.getCompound("Coordinates");
            results.add(new TrackedLocation(name, target.dimension().location(),
                    new BlockPos(coords.getInt("X"), coords.getInt("Y"), coords.getInt("Z"))));
        }
        if (tag.contains("Structure")) {
            LocationResolver.parseStructures(target, tag.getString("Structure"))
                    .flatMap(holders -> LocationResolver.nearestStructure(target, holders, anchor, name))
                    .ifPresent(location -> results.add(location));
        }
        if (tag.contains("Biome")) {
            LocationResolver.parseBiomes(target, tag.getString("Biome"))
                    .flatMap(predicate -> LocationResolver.nearestBiome(target, predicate, anchor, name))
                    .ifPresent(location -> results.add(location));
        }

        if (results.isEmpty()) {
            player.sendSystemMessage(Component.translatable("hqm.mat.chip.location.notFound"));
            return false;
        }
        MatUnlocks.addLocations(player, results, true);
        return true;
    }

    // Returns a readable name for the content, defaulting to its "Name" field if present
    private static String locationName(CompoundTag tag) {
        if (tag.contains("Name")) {
            return tag.getString("Name");
        }
        if (tag.contains("Structure")) {
            return readableName(tag.getString("Structure"));
        }
        if (tag.contains("Biome")) {
            return readableName(tag.getString("Biome"));
        }
        if (tag.contains("Coordinates")) {
            CompoundTag coords = tag.getCompound("Coordinates");
            return coords.getInt("X") + ", " + coords.getInt("Y") + ", " + coords.getInt("Z");
        }
        return "";
    }

    // Converts a namespace:path string into a more readable format
    private static String readableName(String spec) {
        String name = spec;
        if (name.startsWith("#")) {
            name = name.substring(1);
        }
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1);
        }
        return name;
    }

    // Tooltip
    @Environment(EnvType.CLIENT)
    @Override
    protected void appendSummary(ItemStack stack, List<Component> tooltip) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tooltip.add(Component.literal(locationName(tag)).withStyle(ChatFormatting.AQUA));
        }
    }
}