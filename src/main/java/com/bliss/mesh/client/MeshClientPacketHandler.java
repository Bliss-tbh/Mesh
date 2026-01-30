package com.bliss.mesh.client;

import com.bliss.mesh.common.networking.MeshHandshakePacket;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.bliss.mesh.Mesh.LOGGER;

public class MeshClientPacketHandler {

    public static void handleHandshake(final MeshHandshakePacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            LOGGER.info("Received Mesh Handshake! Attempting secondary connection...");

            MeshClientConnection.connectToChunkHost(
                    data.chunkHostIp(),
                    data.port(),
                    Minecraft.getInstance()
            );
        });
    }

}
