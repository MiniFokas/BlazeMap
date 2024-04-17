package com.eerussianguy.blazemap.mixin;

import java.util.BitSet;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;
import com.eerussianguy.blazemap.profiling.Profilers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundLevelChunkWithLightPacket.class)
public class ClientboundChunkLightPacketMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/lighting/LevelLightEngine;Ljava/util/BitSet;Ljava/util/BitSet;)V", at = @At("RETURN"))
    private void constructor(LevelChunk chunk, LevelLightEngine light, BitSet b1, BitSet b2, CallbackInfo ci) {
        Profilers.Server.Mixin.CHUNKPACKET_LOAD_PROFILER.hit();
        Profilers.Server.Mixin.CHUNKPACKET_TIME_PROFILER.begin();

        BlazeMapServerEngine.onChunkChanged(chunk.getLevel().dimension(), chunk.getPos());

        Profilers.Server.Mixin.CHUNKPACKET_TIME_PROFILER.end();
    }
}
