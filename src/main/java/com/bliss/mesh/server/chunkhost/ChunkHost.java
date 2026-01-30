package com.bliss.mesh.server.chunkhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.server.networking.MeshInternalPacketSender;

public class ChunkHost {
    private final MeshInternalPacketSender packetSender;

    public ChunkHost() {
        packetSender = new MeshInternalPacketSender();
        Mesh.PACKET_SENDER = packetSender;
        start();
    }

    private void start() {
        Mesh.LOGGER.info("Chunk Host Networking Init");
        packetSender.init(MeshConfig.REMOTE_ADDRESS.get(), MeshConfig.PORT.get(), true);
    }
}
