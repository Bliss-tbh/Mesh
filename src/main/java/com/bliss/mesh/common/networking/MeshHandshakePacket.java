package com.bliss.mesh.common.networking;

import com.bliss.mesh.Mesh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MeshHandshakePacket(String chunkHostIp, int port) implements CustomPacketPayload {

    // The unique ID for this packet so Minecraft knows how to route it
    public static final Type<MeshHandshakePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Mesh.MODID, "handshake_packet"));

    // The Codec defines HOW to write/read the data to the network buffer
    public static final StreamCodec<FriendlyByteBuf, MeshHandshakePacket> CODEC = StreamCodec.composite(
            StreamCodec.of((buf, val) -> buf.writeUtf(val), FriendlyByteBuf::readUtf),
            MeshHandshakePacket::chunkHostIp,
            StreamCodec.of((buf, val) -> buf.writeInt(val), FriendlyByteBuf::readInt),
            MeshHandshakePacket::port,
            MeshHandshakePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}