package com.bliss.mesh.networking.server.chunkhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.generation.ChunkTracker;
import com.bliss.mesh.networking.common.Packets;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ChunkHostPacketHandler {

    public void decode(int type, ByteBuf msg) {
        if (type == Packets.CLAIM.id) {
            checkGenerationQueue();
        }
        else if (type == Packets.REQUEST.id) {
            getChunkData(msg.readInt(), msg.readInt());
        }
    }

    private void checkGenerationQueue() {
        ChunkPos chunkPos = ChunkTracker.takeWork();
        if (chunkPos != null) {
            Mesh.LOGGER.info("[Mesh-CH] Sending CLAIM_INFO for [{}, {}] to tick host (queue: {})", chunkPos.x, chunkPos.z, ChunkTracker.getQueueSize());
            Mesh.PACKET_SENDER.chunkHost.respondGeneratableChunk(chunkPos.x, chunkPos.z);
        }
    }

    private void getChunkData(int x, int z) {
        Mesh.LOGGER.info("[Mesh-CH] Received REQUEST for chunk [{}, {}] (queue size: {})", x, z, ChunkTracker.getQueueSize());
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerLevel level = server.overworld();
            ChunkAccess loaded = level.getChunk(x, z, ChunkStatus.FULL, false);
            if (loaded != null) {
                Mesh.LOGGER.info("[Mesh-CH] Sending existing chunk [{}, {}] to tick host", x, z);
                CompoundTag tag = ChunkSerializer.write(level, loaded);
                Mesh.PACKET_SENDER.common.moveChunkData(x, z, tag);
            } else {
                Mesh.LOGGER.info("[Mesh-CH] Chunk [{}, {}] not loaded, enqueuing for generation", x, z);
                ChunkTracker.enqueueWork(x, z);
            }
        }
    }
}
