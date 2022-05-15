package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.client.gui.OverlayRegistry;

import com.eerussianguy.blazemap.BConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import static com.eerussianguy.blazemap.BlazeMap.MOD_NAME;

public class Overlays
{
    public static final IIngameOverlay MINIMAP = OverlayRegistry.registerOverlayTop(MOD_NAME + " Minimap", Overlays::renderMinimap);

    public static void reload()
    {
        OverlayRegistry.enableOverlay(MINIMAP, BConfig.CLIENT.enableMinimap.get());
    }

    public static void renderMinimap(ForgeIngameGui gui, PoseStack stack, float partialTicks, int width, int height)
    {
        stack.pushPose();
        //stack.translate(20f, 20f, 0f);
        stack.scale(0.5f, 0.5f, 1f);
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        MinimapRenderer.INSTANCE.draw(stack, buffers);
        buffers.endBatch();
        stack.popPose();
    }
}
