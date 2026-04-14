package com.bliss.mesh.generation.server.tickhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.generation.ChunkTracker;
import com.bliss.mesh.generation.common.MeshWorker;
import com.bliss.mesh.generation.server.ServerGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

public class TickHostWorker extends MeshWorker {

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        Mesh.LOGGER.info("TickHostWorker worker started.");

        while (running) {
            try {
                Mesh.PACKET_SENDER.tickHost.requestGeneratableChunk();
                Thread.sleep(100);

                if (!ChunkTracker.isLocalTasksEmpty()) {
                    ChunkPos task = ChunkTracker.takeLocalTask();
                    Mesh.LOGGER.info("[ChunkHost-Worker] Taking local task for chunk {} ({} tasks remaining)", task, ChunkTracker.getLocalTaskCount());
                    ServerGenerator.executeGeneration(task).thenAccept(result -> {
                        completeLocalFuture(result.chunk(), result.world());
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Mesh.LOGGER.error("MeshWorker encountered an error during processing", e);
            }
        }
    }

    private void completeLocalFuture(ChunkAccess chunk, ServerLevel level) {
        try {
            Mesh.LOGGER.info("[Mesh-TH] Completing local future for chunk [{}, {}] (pending: {})", chunk.getPos().x, chunk.getPos().z, ChunkTracker.getPendingCount());
            CompoundTag tag = ChunkSerializer.write(level, chunk);
            ChunkTracker.addChunkToWorld(chunk.getPos().x, chunk.getPos().z, tag);
        } catch (Exception e) {
            Mesh.LOGGER.error("Failed to complete local future for chunk: {}", chunk.getPos(), e);
        }
    }
}
