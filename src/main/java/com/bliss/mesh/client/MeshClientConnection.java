package com.bliss.mesh.client;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.client.listener.MeshChunkPacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.net.InetSocketAddress;

public class MeshClientConnection {

    public static void connectToChunkHost(String ip, int port, Minecraft minecraft) {
        InetSocketAddress address = new InetSocketAddress(ip, port);
        Connection chunkConnection = Connection.connectToServer(address, true, null);
        MeshChunkPacketListener chunkListener = new MeshChunkPacketListener(minecraft, chunkConnection);

        if (minecraft.level != null) {
            RegistryAccess registries = minecraft.level.registryAccess();
            ProtocolInfo<ClientGamePacketListener> playProtocol = GameProtocols.CLIENTBOUND_TEMPLATE.bind(
                    RegistryFriendlyByteBuf.decorator(registries, ConnectionType.NEOFORGE)
            );
            chunkConnection.setupInboundProtocol(playProtocol, chunkListener);
            Mesh.LOGGER.info("Mesh: Secondary chunk stream established to {}:{}", ip, port);
        } else {
            Mesh.LOGGER.error("Mesh: Failed to establish chunk stream - World registries not yet loaded.");
            chunkConnection.disconnect(Component.literal("Level not loaded"));
        }
    }

}
