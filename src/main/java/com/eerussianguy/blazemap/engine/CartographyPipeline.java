package com.eerussianguy.blazemap.engine;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.mapping.Collector;
import com.eerussianguy.blazemap.api.mapping.Layer;
import com.eerussianguy.blazemap.api.mapping.MapType;
import com.eerussianguy.blazemap.api.mapping.MasterData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

// TODO: use concurrent debouncing sets to mitigate repeated changes to the same objects
public class CartographyPipeline {
    private final MasterDataCache mdCache = new MasterDataCache();
    private final ResourceKey<Level> world;
    private final File levelDir;

    private final Map<ResourceLocation, Collector<?>> collectors = new HashMap<>();
    private final Map<ResourceLocation, List<MapType>> mapTriggers = new HashMap<>();
    private final Map<ResourceLocation, List<Layer<?>>> layerTriggers = new HashMap<>();

    public CartographyPipeline(File serverDir, ResourceKey<Level> world) {
        this.levelDir = new File(serverDir, world.getRegistryName().toString().replace(':', '+'));
        this.levelDir.mkdirs();
        this.world = world;

        // Trim dependencies:
        // - Check what map types render on this world
        // - Check what layers those map types use
        // - Discard layers that don't render for this world
        // - List all needed MD collectors for those layers.
        // Build dependents tree:
        // - What layers depend on each MD collector?
        // - What maps depend on each layer?
        final Set<Layer<?>> layers = new HashSet<>();
        for (ResourceLocation key : BlazeMapAPI.MAPTYPES.keys()) {
            MapType maptype = BlazeMapAPI.MAPTYPES.get(key);
            if(!maptype.shouldRenderForWorld(world)) continue;
            for(ResourceLocation layerID : maptype.getLayers()){
                Layer<?> layer = BlazeMapAPI.LAYERS.get(layerID);
                // TODO: check ids match
                if(!layer.shouldRenderForWorld(world)) continue;
                mapTriggers.computeIfAbsent(layerID, $ -> new ArrayList<>(8)).add(maptype);
                if(layers.contains(layer)) continue;
                layers.add(layer);
                for(ResourceLocation collectorID : layer.getCollectors()){
                    Collector<?> collector = BlazeMapAPI.COLLECTORS.get(collectorID);
                    // TODO: check ids match
                    layerTriggers.computeIfAbsent(collectorID, $ -> new ArrayList<>(8)).add(layer);
                    if(collectors.containsKey(collectorID)) continue;
                    collectors.put(collectorID, collector);
                }
            }
        }
    }

    // TODO: make chunk update orders wait for a few ms in a queue and start processing when timed out.
    public void markChunkDirty(ChunkPos pos) {
        BlazeMapEngine.threading()
            .startOnGameThread($ -> this.collectFromChunk(pos))
            .thenOnDataThread(md -> this.processMasterData(md, pos))
            .thenOnGameThread(this::sendMapUpdates)
            .start();
    }

    private Map<ResourceLocation, MasterData> collectFromChunk(ChunkPos pos) {
        Map<ResourceLocation, MasterData> data = new HashMap<>();
        int x0 = pos.getMinBlockX();
        int x1 = pos.getMaxBlockX();
        int z0 = pos.getMinBlockZ();
        int z1 = pos.getMaxBlockZ();
        for(Collector<?> collector : collectors.values()){
            data.put(collector.getID(), collector.collect(null, null, x0, z0, x1, z1));
        }
        return data;
    }

    // Redraw tiles based on MD changes
    // Check what MDs changed, mark dependent layers for redraw
    // Ask layers to redraw tiles, if applicable:
    // - if tile was redrawn:
    // -  - mark dependent map types as changed
    // -  - update map files with new tile
    // Generate update events for changed map types
    private List<Event> processMasterData(Map<ResourceLocation, MasterData> data, ChunkPos pos){
        Set<Layer<?>> dirtyLayers = new HashSet<>();
        for(Map.Entry<ResourceLocation, MasterData> entry : data.entrySet()){
            if(entry.getValue() != null){
                // TODO: more advanced diffing
                dirtyLayers.addAll(layerTriggers.get(entry.getKey()));
            }
        }

        Set<MapType> dirtyMaps = new HashSet<>();
        MapView<ResourceLocation, MasterData> view = new MapView<>(data);
        for(Layer<?> layer : dirtyLayers){
            BufferedImage tile = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            view.setFilter(layer.getCollectors());
            if(layer.renderTile(tile, view)) {
                ResourceLocation layerID = layer.getID();
                this.updateTile(tile, layerID, pos);
                dirtyMaps.addAll(mapTriggers.get(layerID));
            }
        }

        List<Event> updateEvents = new LinkedList<>();
        for(MapType mapType : dirtyMaps){
            // TODO: create map changed events
        }

        return updateEvents;
    }

    private void updateTile(BufferedImage tile, ResourceLocation layerID, ChunkPos pos){

    }

    // TODO: figure out why void gives generic errors but null Void is OK. Does it have to be an Object?
    private Void sendMapUpdates(List<Event> events){
        for(Event event : events){
            MinecraftForge.EVENT_BUS.post(event);
        }
        return null;
    }

    public void shutdown() {
        // TODO: Release all memory dedicated to caches and such. Close resources. Flush to disk.
    }
}
