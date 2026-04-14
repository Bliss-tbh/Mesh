package com.bliss.mesh.networking.server.tickhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.networking.common.ChunkHostSeedHolder;
import com.bliss.mesh.networking.common.Packets;
import com.bliss.mesh.generation.ChunkTracker;
import io.netty.buffer.ByteBuf;

public class TickHostPacketHandler {

    public void decode(int type, ByteBuf msg) {
        if (type == Packets.SEED.id) {
            handleSeedPacket(msg.readLong());
        }
        else if (type == Packets.CLAIM_INFO.id) {
            int x = msg.readInt();
            int z = msg.readInt();
            ChunkTracker.addLocalTask(x, z);
            Mesh.LOGGER.info("[Mesh-TH] Received CLAIM_INFO for chunk [{}, {}]", x, z);
        }
    }

    private void handleSeedPacket(long seed) {
        Mesh.LOGGER.info("[Mesh-TH] Received seed from Chunk Host: {}", seed);
        ChunkHostSeedHolder.receivedSeed(seed);
    }

}
