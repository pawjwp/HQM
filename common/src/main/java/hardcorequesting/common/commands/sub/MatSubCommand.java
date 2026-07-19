package hardcorequesting.common.commands.sub;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import hardcorequesting.common.commands.CommandHandler;
import hardcorequesting.common.items.mat.LocationResolver;
import hardcorequesting.common.items.mat.MatUnlocks;
import hardcorequesting.common.items.mat.StatKey;
import hardcorequesting.common.items.mat.TrackedLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Grants MAT unlockables to a players via /hqm mat unlock <tutorial|statistic|location>
 */
public class MatSubCommand implements CommandHandler.SubCommand {
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID =
            new DynamicCommandExceptionType(object -> Component.translatable("commands.locate.structure.invalid", object));
    private static final DynamicCommandExceptionType ERROR_BIOME_INVALID =
            new DynamicCommandExceptionType(object -> Component.translatable("hqm.mat.command.biome.invalid", object));

    // Unlocks the requested option
    private interface UnlockAction {
        int run(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean silent) throws CommandSyntaxException;
    }

    // Unlocks the requested location incluidng a dimension option
    private interface LocationAction {
        int run(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean silent, ServerLevel dimension) throws CommandSyntaxException;
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
                .then(Commands.literal("unlock")
                        .then(tutorialBranch())
                        .then(statisticBranch())
                        .then(locationBranch()));
    }

    private ArgumentBuilder<CommandSourceStack, ?> tutorialBranch() {
        return Commands.literal("tutorial")
                .then(withOptions(Commands.argument("id", StringArgumentType.string()),
                        (context, targets, silent) -> {
                            String id = StringArgumentType.getString(context, "id");
                            int count = 0;
                            for (ServerPlayer target : targets) {
                                if (MatUnlocks.unlockTutorial(target, id, !silent) == MatUnlocks.Result.UNLOCKED) count++;
                            }
                            return feedback(context, count);
                        }));
    }

    private ArgumentBuilder<CommandSourceStack, ?> statisticBranch() {
        return Commands.literal("statistic")
                .then(Commands.argument("statType", ResourceLocationArgument.id())
                        .then(withOptions(Commands.argument("stat", ResourceLocationArgument.id()),
                                (context, targets, silent) -> {
                                    String key = StatKey.vanillaKey(
                                            ResourceLocationArgument.getId(context, "statType").toString(),
                                            ResourceLocationArgument.getId(context, "stat").toString());
                                    return unlockStat(context, targets, silent, key);
                                })))
                .then(Commands.literal("special")
                        .then(withOptions(Commands.literal("life_signs_hostile"),
                                (context, targets, silent) -> unlockStat(context, targets, silent, StatKey.LIFE_SIGNS_HOSTILE)))
                        .then(withOptions(Commands.literal("life_signs_neutral"),
                                (context, targets, silent) -> unlockStat(context, targets, silent, StatKey.LIFE_SIGNS_NEUTRAL)))
                        .then(withOptions(Commands.literal("life_signs_friendly"),
                                (context, targets, silent) -> unlockStat(context, targets, silent, StatKey.LIFE_SIGNS_FRIENDLY))));
    }

    private int unlockStat(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean silent, String key) {
        int count = 0;
        for (ServerPlayer target : targets) {
            if (MatUnlocks.unlockStat(target, key, !silent) == MatUnlocks.Result.UNLOCKED) count++;
        }
        return feedback(context, count);
    }

    private ArgumentBuilder<CommandSourceStack, ?> locationBranch() {
        return Commands.literal("location")
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.literal("coordinates")
                                .then(locationValue(Commands.argument("pos", BlockPosArgument.blockPos()), this::coordinates)))
                        .then(Commands.literal("structure")
                                .then(locationValue(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE)), this::structure)))
                        .then(Commands.literal("biome")
                                .then(locationValue(Commands.argument("biome", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.BIOME)), this::biome))));
    }

    // Adds the optional "in <dimension>" segment and the shared target/silent tail to a location value node.
    private <T extends ArgumentBuilder<CommandSourceStack, T>> T locationValue(T node, LocationAction action) {
        withOptions(node, (context, targets, silent) -> action.run(context, targets, silent, null));
        node.then(Commands.literal("in")
                .then(withOptions(Commands.argument("dimension", DimensionArgument.dimension()),
                        (context, targets, silent) -> action.run(context, targets, silent, DimensionArgument.getDimension(context, "dimension")))));
        return node;
    }

    private int coordinates(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean silent, ServerLevel dimension) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getSpawnablePos(context, "pos");
        String name = StringArgumentType.getString(context, "name");
        int count = 0;
        for (ServerPlayer target : targets) {
            ServerLevel level = target.serverLevel();
            if (dimension != null) level = dimension;
            count += MatUnlocks.addLocations(target, List.of(new TrackedLocation(name, level.dimension().location(), pos)), !silent);
        }
        return feedback(context, count);
    }

    private int structure(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean silent, ServerLevel dimension) throws CommandSyntaxException {
        ResourceOrTagKeyArgument.Result<Structure> result = ResourceOrTagKeyArgument.getResourceOrTagKey(context, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID);
        String name = StringArgumentType.getString(context, "name");
        int count = 0;
        for (ServerPlayer target : targets) {
            ServerLevel level = target.serverLevel();
            if (dimension != null) level = dimension;
            Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Optional<? extends HolderSet.ListBacked<Structure>> holders = LocationResolver.getHolders(result, registry);
            if (holders.isEmpty()) continue;
            Optional<TrackedLocation> found = LocationResolver.nearestStructure(level, holders.get(), LocationResolver.scaledAnchor(target, level), name);
            if (found.isPresent()) count += MatUnlocks.addLocations(target, List.of(found.get()), !silent);
        }
        return feedback(context, count);
    }

    private int biome(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean silent, ServerLevel dimension) throws CommandSyntaxException {
        ResourceOrTagKeyArgument.Result<Biome> result = ResourceOrTagKeyArgument.getResourceOrTagKey(context, "biome", Registries.BIOME, ERROR_BIOME_INVALID);
        String name = StringArgumentType.getString(context, "name");
        int count = 0;
        for (ServerPlayer target : targets) {
            ServerLevel level = target.serverLevel();
            if (dimension != null) level = dimension;
            Optional<TrackedLocation> found = LocationResolver.nearestBiome(level, LocationResolver.biomePredicate(result), LocationResolver.scaledAnchor(target, level), name);
            if (found.isPresent()) count += MatUnlocks.addLocations(target, List.of(found.get()), !silent);
        }
        return feedback(context, count);
    }

    private <T extends ArgumentBuilder<CommandSourceStack, T>> T withOptions(T node, UnlockAction action) {
        return node
                .executes(context -> action.run(context, sender(context), false))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> action.run(context, EntityArgument.getPlayers(context, "targets"), false))
                        .then(Commands.literal("silent")
                                .executes(context -> action.run(context, EntityArgument.getPlayers(context, "targets"), true))))
                .then(Commands.literal("silent")
                        .executes(context -> action.run(context, sender(context), true)));
    }

    private static Collection<ServerPlayer> sender(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return List.of(context.getSource().getPlayerOrException());
    }

    private static int feedback(CommandContext<CommandSourceStack> context, int count) {
        if (count == 0) {
            context.getSource().sendFailure(Component.translatable("hqm.mat.command.none"));
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("hqm.mat.command.unlocked", count), true);
        }
        return count;
    }
}