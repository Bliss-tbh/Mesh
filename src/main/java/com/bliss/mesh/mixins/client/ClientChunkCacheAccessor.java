package com.bliss.mesh.mixins.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public interface ClientChunkCacheAccessor {

    /**
     * This invokes the private vanilla method that processes chunk packets.
     * We use this to inject chunks from our "Chunk Host" (Secondary Connection).
     */
    @Invoker("replaceWithPacketData")
    LevelChunk meshmod$replaceWithPacketData(
            int x,
            int z,
            FriendlyByteBuf chunkData,
            CompoundTag lightData,
            Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntityOutput
    );
}
