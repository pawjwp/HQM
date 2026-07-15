package hardcorequesting.common.items.mat;

import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Resolves location requests into TrackedLocation records, for use in location data chips and the `hqm mat unlock location` command.
 * Much of the code taken from vanilla's LocateCommand.
 */
public class LocationResolver {
    // Vanilla LocateCommand variables
    private static final int MAX_STRUCTURE_SEARCH_RADIUS = 100;
    private static final int MAX_BIOME_SEARCH_RADIUS = 6400;
    private static final int BIOME_SAMPLE_RESOLUTION_HORIZONTAL = 32;
    private static final int BIOME_SAMPLE_RESOLUTION_VERTICAL = 64;

    // Based on LocateCommand's getHolders function
    public static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(ResourceOrTagKeyArgument.Result<Structure> pStructure, Registry<Structure> pStructureRegistry) {
        return pStructure.unwrap().map((p_258231_) -> {
            return pStructureRegistry.getHolder(p_258231_).map((p_214491_) -> {
                return HolderSet.direct(p_214491_);
            });
        }, (p_214493_) -> {
            return pStructureRegistry.getTag(p_214493_);
        });
    }

    // Based on LocateCommand's locateStructure function
    public static Optional<TrackedLocation> nearestStructure(ServerLevel serverlevel, HolderSet<Structure> holderset, BlockPos blockpos, String name) {
        Pair<BlockPos, Holder<Structure>> pair = serverlevel.getChunkSource().getGenerator().findNearestMapStructure(serverlevel, holderset, blockpos, MAX_STRUCTURE_SEARCH_RADIUS, false);
        if (pair == null) {
            return Optional.empty();
        } else {
            return Optional.of(new TrackedLocation(name, serverlevel.dimension().location(), pair.getFirst()));
        }
    }

    // Based on LocateCommand's locateBiome function
    public static Optional<TrackedLocation> nearestBiome(ServerLevel serverlevel, Predicate<Holder<Biome>> pBiome, BlockPos blockpos, String name) {
        Pair<BlockPos, Holder<Biome>> pair = serverlevel.findClosestBiome3d(pBiome, blockpos, MAX_BIOME_SEARCH_RADIUS, BIOME_SAMPLE_RESOLUTION_HORIZONTAL, BIOME_SAMPLE_RESOLUTION_VERTICAL);
        if (pair == null) {
            return Optional.empty();
        } else {  
            return Optional.of(new TrackedLocation(name, serverlevel.dimension().location(), pair.getFirst()));
        }
    }

    // The target dimension's level
    public static ServerLevel level(ServerPlayer player, ResourceLocation dimensionId) {
        if (dimensionId == null) {
            return player.serverLevel();
        } else {
            return player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        }
    }

    // Returns the player's position adjusted to the target level's coordinate-scale
    public static BlockPos scaledAnchor(ServerPlayer player, ServerLevel target) {
        double ratio = player.serverLevel().dimensionType().coordinateScale() / target.dimensionType().coordinateScale();
        return BlockPos.containing(player.getX() * ratio, player.getY(), player.getZ() * ratio);
    }

    // Parses a namespace:id structure reference string
    public static Optional<HolderSet<Structure>> parseStructures(ServerLevel level, String spec) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        if (spec.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(spec.substring(1));
            if (id == null) {
                return Optional.empty();
            } else {
                return registry.getTag(TagKey.create(Registries.STRUCTURE, id)).map(named -> named);
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(spec);
        if (id == null) {
            return Optional.empty();
        } else {
            return registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id)).map(holder -> {
                return HolderSet.direct(holder);
            });
        }
    }

    // Parses a namespace:id biome reference string
    public static Optional<Predicate<Holder<Biome>>> parseBiomes(ServerLevel level, String spec) {
        Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        if (spec.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(spec.substring(1));
            if (id == null) {
                return Optional.empty();
            } else {
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, id);
                if (registry.getTag(tag).isEmpty()) {
                    return Optional.empty();
                } else {
                    return Optional.of(holder -> holder.is(tag));
                }
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(spec);
        if (id == null) {
            return Optional.empty();
        } else {
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
            if (registry.containsKey(key)) {
                return Optional.of(holder -> holder.is(key));
            } else {
                return Optional.empty();
            }
        }
    }
}