package com.bliss.mesh.client.listener;

import com.bliss.mesh.mixins.client.ClientChunkCacheAccessor;
import com.bliss.mesh.mixins.client.ClientPacketListenerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

import static com.bliss.mesh.Mesh.LOGGER;

public class MeshChunkPacketListener extends MeshBaseListener {
    private final Minecraft minecraft;
    private final Connection connection;

    public MeshChunkPacketListener(Minecraft minecraft, Connection connection) {
        this.minecraft = minecraft;
        this.connection = connection;
    }

    @Override
    public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, minecraft);

        if (minecraft.level != null) {
            ((ClientChunkCacheAccessor) minecraft.level.getChunkSource())
                    .meshmod$replaceWithPacketData(
                            packet.getX(),
                            packet.getZ(),
                            packet.getChunkData().getReadBuffer(),
                            packet.getChunkData().getHeightmaps(),
                            packet.getChunkData().getBlockEntitiesTagsConsumer(
                                    packet.getX(), packet.getZ()
                            )
                    );

            ((ClientPacketListenerAccessor) this)
                    .meshmod$applyLightData(
                            packet.getX(),
                            packet.getZ(),
                            packet.getLightData()
                    );
        }
    }

    @Override
    public void handleKeepAlive(ClientboundKeepAlivePacket packet) {
        this.connection.send(new ServerboundKeepAlivePacket(packet.getId()));
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        LOGGER.info("Mesh Chunk Stream Closed: {}", details.reason().getString());
    }

}
