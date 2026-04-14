package com.bliss.mesh.networking.server.chunkhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.networking.common.Packets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class ChunkHostPacketSender {
    private final Channel channel;

    public ChunkHostPacketSender(Channel channel) {
        this.channel = channel;
    }

    public void respondGeneratableChunk(int x, int z) {
        Channel ch = channel;
        if (ch == null) {
            return;
        }
        ByteBuf buf = ch.alloc().buffer();
        buf.writeInt(Packets.CLAIM_INFO.id);
        buf.writeInt(x);
        buf.writeInt(z);
        ch.writeAndFlush(buf);
    }

    public void syncSeed(long seed) {
        Channel ch = channel;
        if (ch == null) {
            return;
        }
        ByteBuf buf = ch.alloc().buffer();
        buf.writeInt(Packets.SEED.id);
        buf.writeLong(seed);
        ch.writeAndFlush(buf);
        Mesh.LOGGER.info("[Mesh-Net] Sent seed: {}", seed);
    }
}
