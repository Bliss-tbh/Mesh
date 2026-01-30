package com.bliss.mesh.mixins.server;

import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheAccessor {

    @Invoker("runDistanceManagerUpdates")
    boolean mesh$runDistanceManagerUpdates();

    @Invoker("getChunkFutureMainThread")
    CompletableFuture<ChunkResult<ChunkAccess>> mesh$getChunkFutureMainThread(int x, int z, ChunkStatus status, boolean create);

}
