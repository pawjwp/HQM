package hardcorequesting.common.items.mat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hardcorequesting.common.io.adapter.Adapter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * A position that stores a name, dimension, and coordinates to be used by the MAT in tracking mode
 */
public record TrackedLocation(String name, ResourceLocation dimension, BlockPos pos) {

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Dimension", dimension.toString());
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    public static TrackedLocation fromNBT(CompoundTag tag) {
        return new TrackedLocation(
                tag.getString("Name"),
                new ResourceLocation(tag.getString("Dimension")),
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")));
    }

    public JsonElement toJson() {
        return Adapter.object()
                .add("name", name)
                .add("dimension", dimension.toString())
                .add("x", pos.getX())
                .add("y", pos.getY())
                .add("z", pos.getZ())
                .build();
    }

    public static TrackedLocation fromJson(JsonObject object) {
        return new TrackedLocation(
                GsonHelper.getAsString(object, "name"),
                new ResourceLocation(GsonHelper.getAsString(object, "dimension")),
                new BlockPos(GsonHelper.getAsInt(object, "x"), GsonHelper.getAsInt(object, "y"), GsonHelper.getAsInt(object, "z")));
    }
}
