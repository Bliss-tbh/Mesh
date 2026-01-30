package com.bliss.mesh.server;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import com.bliss.mesh.mixins.server.ChunkMapAccessor;
import com.bliss.mesh.mixins.server.ChunkStorageAccessor;
import com.bliss.mesh.mixins.server.ServerChunkCacheAccessor;
import com.bliss.mesh.server.networking.MeshChunkOrchestrator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class MeshWorker implements Runnable {
    /**
     * This flag prevents our Mixins from intercepting the calls
     * made by this worker thread, avoiding infinite loops.
     */
    public static final ThreadLocal<Boolean> IS_MESH_WORKER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> BYPASS_MESH = ThreadLocal.withInitial(() -> false);
    private volatile boolean running = true;
    private static MeshWorker INSTANCE;
    private static Thread WORKER_THREAD;

    @Override
    public void run() {
        IS_MESH_WORKER.set(true);
        Mesh.LOGGER.info("MeshWorker thread started.");

        while (running) {
            try {
                ChunkPos work = null;
                // 1. Poll for work from the distributed queue
                if (MeshConfig.MODE.get() != MeshModes.CHUNK_HOST) {
                    Mesh.PACKET_SENDER.requestGeneratableChunk();
                    Thread.sleep(100);
                } else {
                    work = MeshChunkOrchestrator.takeWork();
                    if (work != null) {
                        MeshChunkOrchestrator.addLocalTask(work.x, work.z);
                    }
                }

                if (!MeshChunkOrchestrator.isLocalTasksEmpty()) {
                    ChunkPos task = MeshChunkOrchestrator.takeLocalTask();
                    executeGeneration(task);
                } else if (work == null) {
                    Thread.sleep(10);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Mesh.LOGGER.error("MeshWorker encountered an error during processing", e);
            }
        }
    }

    private void executeGeneration(ChunkPos task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel world = server.overworld();

        if (Thread.currentThread() != server.getRunningThread()) {
            Mesh.LOGGER.debug("[Mesh-Worker] Offloading task {} to Server Main Thread", task);
            server.execute(() -> executeGeneration(task));
            return;
        }

        Mesh.LOGGER.debug("[Mesh-Worker] Starting generation/loading for chunk {}", task);
        try {
            BYPASS_MESH.set(true);

            final ServerChunkCache serverChunkCache = world.getChunkSource();
            serverChunkCache.addRegionTicket(TicketType.FORCED, task, 0, task);
            ((ServerChunkCacheAccessor) serverChunkCache).mesh$runDistanceManagerUpdates();

            ((ServerChunkCacheAccessor) serverChunkCache).mesh$getChunkFutureMainThread(task.x, task.z, ChunkStatus.FULL, true)
                    .handleAsync((result, throwable) -> {
                        Mesh.LOGGER.debug("[Mesh-Worker] Generation future completed for chunk {}", task);
                        try {
                            BYPASS_MESH.set(true);

                            serverChunkCache.removeRegionTicket(TicketType.FORCED, task, 0, task);

                            if (throwable != null || result == null) {
                                Mesh.LOGGER.error("Mesh: Failed generating chunk [{}, {}]", task.x, task.z);
                                return null;
                            }

                            result.ifSuccess(chunk -> {
                                if (MeshConfig.MODE.get() == MeshModes.CHUNK_HOST) {
                                    saveChunkManually(world.getChunkSource().chunkMap, chunk);
                                }
                                sendToRemote(chunk, world);
                            });
                        } finally {
                            BYPASS_MESH.set(false);
                            MeshChunkOrchestrator.releaseClaim(task.x, task.z); // Free the orchestrator
                        }
                        return null;
                    }, server);
        } finally {
            BYPASS_MESH.set(false);
        }
    }

    private void sendToRemote(ChunkAccess chunk, ServerLevel level) {
        try {
            CompoundTag tag = ChunkSerializer.write(level, chunk);
            Mesh.PACKET_SENDER.sendChunkData(chunk.getPos().x, chunk.getPos().z, tag);
        } catch (Exception e) {
            Mesh.LOGGER.error("Failed to serialize chunk for remote sync: {}", chunk.getPos(), e);
        }
    }

    @SuppressWarnings("resource")
    private void saveChunkManually(ChunkMap chunkMap, ChunkAccess chunk) {
        try {
            IOWorker worker = ((ChunkStorageAccessor) chunkMap).mesh$getWorker();
            ServerLevel level = ((ChunkMapAccessor) chunkMap).mesh$getLevel();

            // Serialize and Store
            CompoundTag nbt = ChunkSerializer.write(level, chunk);
            worker.store(chunk.getPos(), nbt);
        } catch (Exception e) {
            Mesh.LOGGER.error("Failed to manually save chunk on ChunkHost: {}", chunk.getPos(), e);
        }
    }

    public void stop() {
        running = false;
    }

    public static void start() {
        if (INSTANCE != null) return; // Prevent double-start
        INSTANCE = new MeshWorker();
        WORKER_THREAD = new Thread(INSTANCE, "Mesh-Worker-Thread");
        WORKER_THREAD.setDaemon(true);
        WORKER_THREAD.start();
    }

    public static void stopWorker() {
        if (INSTANCE != null) {
            INSTANCE.stop(); // Sets running = false
            WORKER_THREAD.interrupt(); // Wakes it up from takeWork() if idle
            INSTANCE = null;
            WORKER_THREAD = null;
        }
    }
}