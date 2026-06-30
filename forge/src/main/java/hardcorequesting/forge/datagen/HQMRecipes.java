package hardcorequesting.forge.datagen;

import com.google.gson.JsonObject;
import hardcorequesting.common.HardcoreQuestingCore;
import hardcorequesting.common.blocks.ModBlocks;
import hardcorequesting.common.items.ModItems;
import hardcorequesting.common.items.crafting.ModRecipes;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class HQMRecipes extends RecipeProvider {
    public HQMRecipes(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.book.get())
                .requires(Items.BOOK)
                .requires(Items.STRING)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(consumer, new ResourceLocation(HardcoreQuestingCore.ID, "book"));

        // Heart fusions
        heart(consumer, "full_from_half_half_hearts", ModItems.heart.get(),
                ModItems.halfHeart.get(), ModItems.halfHeart.get());
        heart(consumer, "full_from_quarter_quarter_half_hearts", ModItems.heart.get(),
                ModItems.quarterHeart.get(), ModItems.quarterHeart.get(), ModItems.halfHeart.get());
        heart(consumer, "full_from_quarter_quarter_quarter_quarter_hearts", ModItems.heart.get(),
                ModItems.quarterHeart.get(), ModItems.quarterHeart.get(), ModItems.quarterHeart.get(), ModItems.quarterHeart.get());
        heart(consumer, "full_from_quarter_threequarts_hearts", ModItems.heart.get(),
                ModItems.quarterHeart.get(), ModItems.threeQuartsHeart.get());
        heart(consumer, "half_from_quarter_quarter_hearts", ModItems.halfHeart.get(),
                ModItems.quarterHeart.get(), ModItems.quarterHeart.get());
        heart(consumer, "threequarts_from_quarter_half_hearts", ModItems.threeQuartsHeart.get(),
                ModItems.quarterHeart.get(), ModItems.halfHeart.get());
        heart(consumer, "threequarts_from_quarter_quarter_quarter_hearts", ModItems.threeQuartsHeart.get(),
                ModItems.quarterHeart.get(), ModItems.quarterHeart.get(), ModItems.quarterHeart.get());

        // Item barrels are crafted around a quest book that is returned
        bookCatalyst(consumer, "barrel", ModItems.book.get());
        bookCatalyst(consumer, "barrel_2", ModItems.enabledBook.get());
    }

    private void heart(Consumer<FinishedRecipe> consumer, String name, ItemLike result, ItemLike... ingredients) {
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result);
        for (ItemLike ingredient : ingredients) builder.requires(ingredient);
        builder.unlockedBy("has_heart_piece", has(ingredients[0]))
                .save(consumer, new ResourceLocation(HardcoreQuestingCore.ID, name));
    }

    private void bookCatalyst(Consumer<FinishedRecipe> consumer, String name, ItemLike book) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.blockBarrel.get())
                .pattern("wgw")
                .pattern("gqg")
                .pattern("wgw")
                .define('w', ItemTags.PLANKS)
                .define('g', Items.GLASS)
                .define('q', book)
                .unlockedBy("has_quest_book", has(ModItems.book.get()))
                .save(finished -> consumer.accept(new BookCatalystResult(finished)),
                        new ResourceLocation(HardcoreQuestingCore.ID, name));
    }

    private record BookCatalystResult(FinishedRecipe wrapped) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(@NotNull JsonObject json) {
            wrapped.serializeRecipeData(json);
        }

        @NotNull
        @Override
        public ResourceLocation getId() {
            return wrapped.getId();
        }

        @NotNull
        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipes.bookCatalystSerializer.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return wrapped.serializeAdvancement();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return wrapped.getAdvancementId();
        }
    }
}
