package com.bliss.mesh.generation.server;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.generation.ChunkTracker;
import com.bliss.mesh.mixins.server.ServerChunkCacheAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.concurrent.CompletableFuture;

public class ServerGenerator {

    public record GenerationResult(ChunkAccess chunk, ServerLevel world) {}

    public static CompletableFuture<GenerationResult> executeGeneration(ChunkPos task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;

        var future = new CompletableFuture<GenerationResult>();
        if (Thread.currentThread() != server.getRunningThread()) {
            Mesh.LOGGER.debug("[Mesh-Worker] Offloading task {} to Server Main Thread", task);
            server.execute(() -> executeGeneration(task, future, server));
            return future;
        }
        executeGeneration(task, future, server);
        return future;
    }

    private static void executeGeneration(ChunkPos task, CompletableFuture<GenerationResult> future, MinecraftServer server) {
        Mesh.LOGGER.info("[Mesh-Worker] Starting generation/loading for chunk {}", task);
        ChunkTracker.markLocallyGenerating(task.x, task.z);

        ServerLevel world = server.overworld();
        final ServerChunkCache serverChunkCache = world.getChunkSource();
        serverChunkCache.addRegionTicket(TicketType.FORCED, task, 0, task);
        ((ServerChunkCacheAccessor) serverChunkCache).mesh$runDistanceManagerUpdates();

        ((ServerChunkCacheAccessor) serverChunkCache).mesh$getChunkFutureMainThread(task.x, task.z, ChunkStatus.FULL, true)
                .handleAsync((result, throwable) -> {
                    Mesh.LOGGER.info("[Mesh-Worker] Generation future completed for chunk {}", task);
                    try {

                        serverChunkCache.removeRegionTicket(TicketType.FORCED, task, 0, task);

                        if (throwable != null || result == null) {
                            Mesh.LOGGER.error("Mesh: Failed generating chunk [{}, {}]", task.x, task.z);
                            return null;
                        }

                        result.ifSuccess(chunk -> {
                            ChunkTracker.unmarkLocallyGenerating(task.x, task.z);
                            future.complete(new GenerationResult(chunk, world));
                        });
                    } finally {
                        ChunkTracker.releaseClaim(task.x, task.z);
                    }
                    return null;
                }, server);
    }

}
