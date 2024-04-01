package com.eerussianguy.blazemap.feature;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.feature.mapping.*;
import com.eerussianguy.blazemap.feature.maps.*;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorGui;
import com.eerussianguy.blazemap.feature.waypoints.WaypointManagerGui;
import com.eerussianguy.blazemap.feature.waypoints.WaypointRenderer;
import com.eerussianguy.blazemap.feature.waypoints.WaypointStore;
import com.mojang.blaze3d.platform.InputConstants;

public class BlazeMapFeaturesClient {
    public static final KeyMapping KEY_MAPS = new KeyMapping("blazemap.key.maps", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, BlazeMap.MOD_NAME);
    public static final KeyMapping KEY_ZOOM = new KeyMapping("blazemap.key.zoom", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, BlazeMap.MOD_NAME);
    public static final KeyMapping KEY_WAYPOINTS = new KeyMapping("blazemap.key.waypoints", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, BlazeMap.MOD_NAME);

    @SubscribeEvent
    public static void onKeyBindRegister(RegisterKeyMappingsEvent event) {
        event.register(KEY_MAPS);
        event.register(KEY_ZOOM);
        event.register(KEY_WAYPOINTS);
    }

    public static void initMapping() {
        BlazeMapAPI.LAYERS.register(new TerrainHeightLayer());
        BlazeMapAPI.LAYERS.register(new WaterLevelLayer());
        BlazeMapAPI.LAYERS.register(new TerrainIsolinesLayer());
        BlazeMapAPI.LAYERS.register(new BlockColorLayer());
        BlazeMapAPI.LAYERS.register(new NetherLayer());

        BlazeMapAPI.MAPTYPES.register(new AerialViewMapType());
        BlazeMapAPI.MAPTYPES.register(new TopographyMapType());
        BlazeMapAPI.MAPTYPES.register(new NetherMapType());
    }

    public static void initMaps() {
        BlazeMapAPI.OBJECT_RENDERERS.register(new DefaultObjectRenderer());

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(MapRenderer::onDimensionChange);
        bus.addListener(MapRenderer::onMapLabelAdded);
        bus.addListener(MapRenderer::onMapLabelRemoved);
        bus.addListener(BlazeMapFeaturesClient::mapKeybinds);
    }

    private static void mapKeybinds(InputEvent.Key evt) {
        if(KEY_MAPS.isDown()) {
            if(Screen.hasShiftDown()) {
                MinimapOptionsGui.open();
            }
            else {
                WorldMapGui.open();
            }
        }
        if(KEY_WAYPOINTS.isDown()) {
            if(Screen.hasShiftDown()) {
                WaypointManagerGui.open();
            }
            else {
                WaypointEditorGui.open();
            }
        }
        if(KEY_ZOOM.isDown()) {
            if(Screen.hasShiftDown()) {
                MinimapRenderer.INSTANCE.synchronizer.zoomOut();
            }
            else {
                MinimapRenderer.INSTANCE.synchronizer.zoomIn();
            }
        }
    }

    public static void initWaypoints() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(WaypointEditorGui::onDimensionChanged);
        bus.addListener(WaypointManagerGui::onDimensionChanged);
        bus.addListener(EventPriority.HIGHEST, WaypointStore::onServerJoined);
        bus.addListener(MapRenderer::onWaypointAdded);
        bus.addListener(MapRenderer::onWaypointRemoved);
        WaypointRenderer.init();
    }
}
