package hardcorequesting.forge.datagen;

import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.blocks.DeliveryBlock;
import hardcorequesting.common.blocks.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class HQMBlockStates extends BlockStateProvider {
    public HQMBlockStates(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, HardcoreQuestingCore.ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(ModBlocks.blockTracker.get(), leavesModel("quest_tracker", "hqm_quest_tracker"));

        ModelFile bound = leavesModel("item_barrel", "hqm_item_barrel");
        ModelFile empty = leavesModel("item_barrel_empty", "hqm_item_barrel_empty");
        getVariantBuilder(ModBlocks.blockBarrel.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(DeliveryBlock.BOUND) ? bound : empty)
                        .build());
    }

    private ModelFile leavesModel(String name, String texture) {
        return models().withExistingParent(name, mcLoc("block/leaves"))
                .texture("all", modLoc("block/" + texture));
    }
}
