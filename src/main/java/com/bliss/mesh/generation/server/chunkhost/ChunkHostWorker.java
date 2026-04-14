package com.bliss.mesh.generation.server.chunkhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.generation.ChunkTracker;
import com.bliss.mesh.generation.common.MeshWorker;
import com.bliss.mesh.generation.server.ServerGenerator;
import com.bliss.mesh.mixins.server.ChunkMapAccessor;
import com.bliss.mesh.mixins.server.ChunkStorageAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.IOWorker;

public class ChunkHostWorker extends MeshWorker {

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        Mesh.LOGGER.info("ChunkHostWorker started.");

        while (running) {
            try {
                ChunkPos work = ChunkTracker.takeWork();
                if (work != null) {
                    ChunkTracker.addLocalTask(work.x, work.z);
                }

                if (!ChunkTracker.isLocalTasksEmpty()) {
                    ChunkPos task = ChunkTracker.takeLocalTask();
                    Mesh.LOGGER.info("[ChunkHost-Worker] Taking local task for chunk {} ({} tasks remaining)", task, ChunkTracker.getLocalTaskCount());
                    ServerGenerator.executeGeneration(task).thenAccept(result -> {
                        ServerLevel world = result.world();
                        ChunkAccess chunk = result.chunk();
                        saveChunkToDisk(chunk, world.getChunkSource().chunkMap);
                        sendChunkToTickHost(chunk, world);
                    });
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

    private void sendChunkToTickHost(ChunkAccess chunk, ServerLevel level) {
        try {
            Mesh.LOGGER.info("[Mesh-CH] Sending generated chunk [{}, {}] to tick host (pending: {})", chunk.getPos().x, chunk.getPos().z, ChunkTracker.getPendingCount());
            CompoundTag tag = ChunkSerializer.write(level, chunk);
            Mesh.PACKET_SENDER.common.moveChunkData(chunk.getPos().x, chunk.getPos().z, tag);
        } catch (Exception e) {
            Mesh.LOGGER.error("Failed to serialize chunk for remote sync: {}", chunk.getPos(), e);
        }
    }

    @SuppressWarnings("resource")
    private void saveChunkToDisk(ChunkAccess chunk, ChunkMap chunkMap) {
        try {
            IOWorker worker = ((ChunkStorageAccessor) chunkMap).mesh$getWorker();
            ServerLevel level = ((ChunkMapAccessor) chunkMap).mesh$getLevel();

            CompoundTag nbt = ChunkSerializer.write(level, chunk);
            worker.store(chunk.getPos(), nbt);
        } catch (Exception e) {
            Mesh.LOGGER.error("Failed to manually save chunk on ChunkHost: {}", chunk.getPos(), e);
        }
    }
}
