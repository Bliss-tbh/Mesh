package com.bliss.mesh.server.tickhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.server.networking.MeshInternalPacketSender;

public class TickHost {
    private final MeshInternalPacketSender packetSender;

    public TickHost() {
        packetSender = new MeshInternalPacketSender();
        Mesh.PACKET_SENDER = packetSender;
        start();
    }

    private void start() {
        Mesh.LOGGER.info("Tick Host Networking Init");
        packetSender.init(MeshConfig.REMOTE_ADDRESS.get(), MeshConfig.PORT.get(), false);
    }
}
