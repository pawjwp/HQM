package hardcorequesting.common.items.mat;

import hardcorequesting.common.config.HQMConfig;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.List;

/**
 * MAT settings that clients need to read from the server
 */
public record MatSettings(List<MatMode> enabledModes, int lifeSignsRadius, boolean showTrackingCoordinates) {

    public static MatSettings fromConfig() {
        HQMConfig.MAT config = HQMConfig.getInstance().MAT;
        return new MatSettings(MatMode.enabledModes(), config.LIFE_SIGNS_RADIUS, config.SHOW_TRACKING_COORDINATES);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("EnabledModes", enabledModes.stream().map(MatMode::getId).toList());
        tag.putInt("LifeSignsRadius", lifeSignsRadius);
        tag.putBoolean("ShowTrackingCoordinates", showTrackingCoordinates);
        return tag;
    }

    public static MatSettings fromNBT(CompoundTag tag) {
        return new MatSettings(Arrays.stream(tag.getIntArray("EnabledModes")).mapToObj(MatMode::fromId).toList(),
                tag.getInt("LifeSignsRadius"), tag.getBoolean("ShowTrackingCoordinates"));
    }
}
