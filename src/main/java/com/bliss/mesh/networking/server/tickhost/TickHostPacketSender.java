package com.bliss.mesh.networking.server.tickhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.generation.ChunkTracker;
import com.bliss.mesh.networking.common.Packets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class TickHostPacketSender {
    private final Channel channel;

    public TickHostPacketSender(Channel channel) {
        this.channel = channel;
    }

    public void requestChunkFromStorage(int x, int z) {
        Channel ch = channel;
        if (ch == null) {
            Mesh.LOGGER.warn("[Mesh-Net] Cannot request chunk [{}, {}] - channel not connected", x, z);
            return;
        }
        ByteBuf buf = ch.alloc().buffer();
        buf.writeInt(Packets.REQUEST.id);
        buf.writeInt(x);
        buf.writeInt(z);
        ch.writeAndFlush(buf);
        Mesh.LOGGER.debug("[Mesh-TH] Sent REQUEST for chunk [{}, {}] ({} pending)", x, z, ChunkTracker.getPendingCount());
    }

    public void requestGeneratableChunk() {
        Channel ch = channel;
        if (ch == null) {
            return;
        }
        ByteBuf buf = ch.alloc().buffer();
        buf.writeInt(Packets.CLAIM.id);
        ch.writeAndFlush(buf);
    }
}
