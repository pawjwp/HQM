package hardcorequesting.common.items.mat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hardcorequesting.common.io.adapter.Adapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * A position that stores a name, dimension, and coordinates to be used by the MAT in tracking mode
 * The biome at the location and the time it was discovered are stored as well.
 */
public record TrackedLocation(String name, ResourceLocation dimension, BlockPos pos, @Nullable ResourceLocation biome, long discoveredTime) {

    // A newly found location records the biome and current time
    public static TrackedLocation discover(String name, ServerLevel level, BlockPos pos) {
        ResourceLocation biome = level.getUncachedNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()))
                .unwrapKey().map(key -> key.location()).orElse(null);
        return new TrackedLocation(name, level.dimension().location(), pos, biome, level.getDayTime());
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Dimension", dimension.toString());
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        if (biome != null) tag.putString("Biome", biome.toString());
        tag.putLong("DiscoveredTime", discoveredTime);
        return tag;
    }

    public static TrackedLocation fromNBT(CompoundTag tag) {
        ResourceLocation biome = null;
        if (tag.contains("Biome")) biome = new ResourceLocation(tag.getString("Biome"));
        long discoveredTime = -1;
        if (tag.contains("DiscoveredTime")) discoveredTime = tag.getLong("DiscoveredTime");
        return new TrackedLocation(
                tag.getString("Name"),
                new ResourceLocation(tag.getString("Dimension")),
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                biome,
                discoveredTime);
    }

    public JsonElement toJson() {
        return Adapter.object()
                .add("name", name)
                .add("dimension", dimension.toString())
                .add("x", pos.getX())
                .add("y", pos.getY())
                .add("z", pos.getZ())
                .use(builder -> {
                    if (biome != null) builder.add("biome", biome.toString());
                })
                .add("discoveredTime", discoveredTime)
                .build();
    }

    public static TrackedLocation fromJson(JsonObject object) {
        ResourceLocation biome = null;
        if (object.has("biome")) biome = new ResourceLocation(GsonHelper.getAsString(object, "biome"));
        return new TrackedLocation(
                GsonHelper.getAsString(object, "name"),
                new ResourceLocation(GsonHelper.getAsString(object, "dimension")),
                new BlockPos(GsonHelper.getAsInt(object, "x"), GsonHelper.getAsInt(object, "y"), GsonHelper.getAsInt(object, "z")),
                biome,
                GsonHelper.getAsLong(object, "discoveredTime", -1));
    }
}
