package hardcorequesting.common.client.tutorial;

import hardcorequesting.common.quests.task.icon.VisitLocationTask;
import hardcorequesting.common.tutorial.Tutorial;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The places a location trigger can get activated by
 */
@Environment(EnvType.CLIENT)
public class TutorialLocations {

    // Whether the player is at the location provided
    public static boolean matches(Tutorial.Trigger.Location location, Player player) {
        Level level = player.level();
        if (!location.dimensions().isEmpty() && !matchesAnyDimension(location, level)) return false;
        if (location.position() != null && !VisitLocationTask.isWithinRadius(player, location.position(), location.radius())) return false;
        if (!location.biomes().isEmpty() && !matchesAnyBiome(location, level, player.blockPosition())) return false;
        return true;
    }

    private static boolean matchesAnyDimension(Tutorial.Trigger.Location location, Level level) {
        for (String dimension : location.dimensions()) {
            if (VisitLocationTask.matchesDimension(level, dimension)) return true;
        }
        return false;
    }

    private static boolean matchesAnyBiome(Tutorial.Trigger.Location location, Level level, BlockPos pos) {
        for (String biome : location.biomes()) {
            if (VisitLocationTask.matchesBiome(level, pos, biome)) return true;
        }
        return false;
    }

    // Whether the submitted dimension is valid
    public static boolean isKnownDimension(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id != null && Minecraft.getInstance().getConnection().levels().contains(ResourceKey.create(Registries.DIMENSION, id));
    }

    // Whether the submitted biome or biome tag is valid
    public static boolean isKnownBiome(String name) {
        if (name.startsWith("#")) return ResourceLocation.tryParse(name.substring(1)) != null;
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id != null && Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.BIOME).containsKey(id);
    }
}
