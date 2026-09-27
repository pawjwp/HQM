package hardcorequesting.common.items.mat;

import hardcorequesting.common.network.GeneralUsage;
import hardcorequesting.common.quests.QuestingDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Tracked locations show a map of their area, based on vanilla's explorer maps.
 * The map will fill in as the player explores the are if they are carrying a MAT in Tracking mode.
 */
public class MatMaps {
    public static final byte ZOOM = 2; // zoom level 2 to match the explorer maps, a 512x512 area
    private static final MapItem MAP_ITEM = (MapItem) Items.FILLED_MAP;

    // Create's a new map of the area around the provided position and returns the map ID, based on ExplorationMapFunction's run function
    public static int create(ServerLevel level, BlockPos pos) {
        if (level.dimensionType().hasCeiling()) return -1;
        ItemStack stack = MapItem.create(level, pos.getX(), pos.getZ(), ZOOM, false, false);
        MapItem.renderBiomePreviewMap(level, stack);
        return MapItem.getMapId(stack);
    }

    // The center of the map
    public static int center(int coordinate) {
        int size = 128 * (1 << ZOOM);
        return Mth.floor((coordinate + 64.0D) / size) * size + size / 2 - 64;
    }

    // Updates the map as the player explores, based on MapItem's inventoryTick function and map update packets
    public static void explore(ServerPlayer player, ItemStack mat) {
        TrackedLocation location = QuestingDataManager.getInstance().getQuestingData(player).matData.getSelectedLocation();
        if (location == null || location.mapId() < 0) return;
        MapItemSavedData data = player.level().getMapData(MapItem.makeKey(location.mapId()));
        if (data == null) return;
        data.tickCarriedBy(player, mat);
        MAP_ITEM.update(player.level(), player, data);
        Packet<?> packet = data.getUpdatePacket(location.mapId(), player);
        if (packet != null) player.connection.send(packet);
    }

    // Sends the map or creates one if needed
    public static void sendMap(ServerPlayer player, int index) {
        MatPlayerData mat = QuestingDataManager.getInstance().getQuestingData(player).matData;
        if (index < 0 || index >= mat.locations.size()) return;
        TrackedLocation location = mat.locations.get(index);
        if (location.mapId() < 0) {
            ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location.dimension()));
            if (level == null) return;
            int mapId = create(level, location.pos());
            if (mapId < 0) return;
            location = location.withMapId(mapId);
            mat.locations.set(index, location);
            GeneralUsage.sendMatDataSync(player);
        }
        MapItemSavedData data = player.level().getMapData(MapItem.makeKey(location.mapId()));
        if (data != null) {
            player.connection.send(new ClientboundMapItemDataPacket(location.mapId(), data.scale, data.locked, null, new MapItemSavedData.MapPatch(0, 0, 128, 128, data.colors)));
        }
    }
}
