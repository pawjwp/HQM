package hardcorequesting.forge.datagen.loot;

import hardcorequesting.common.blocks.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Set;

public class HQMBlockLootTables extends BlockLootSubProvider {
    public HQMBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.blockBarrel.get());
        // The tracker drops nothing when broken
        add(ModBlocks.blockTracker.get(), LootTable.lootTable());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(ModBlocks.blockBarrel.get(), ModBlocks.blockTracker.get());
    }
}
