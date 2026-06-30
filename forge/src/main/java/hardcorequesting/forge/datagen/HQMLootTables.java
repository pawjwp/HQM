package hardcorequesting.forge.datagen;

import hardcorequesting.forge.datagen.loot.HQMBlockLootTables;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class HQMLootTables {
    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(HQMBlockLootTables::new, LootContextParamSets.BLOCK)
        ));
    }
}
