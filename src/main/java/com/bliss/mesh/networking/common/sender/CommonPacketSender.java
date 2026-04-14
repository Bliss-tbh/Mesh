package com.bliss.mesh.networking.common.sender;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.networking.common.Packets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.DataOutputStream;

public class CommonPacketSender {
    private final Channel channel;

    public CommonPacketSender(Channel channel) {
        this.channel = channel;
    }

    public void moveChunkData(int x, int z, CompoundTag nbt) {
        Channel ch = channel;
        if (ch == null) {
            Mesh.LOGGER.warn("[Mesh-Net] Cannot send chunk [{}, {}] - channel not connected", x, z);
            return;
        }

        ByteBuf buf = ch.alloc().buffer();
        buf.writeInt(Packets.PUSH.id);
        buf.writeInt(x);
        buf.writeInt(z);

        try {
            try (ByteBufOutputStream bbos = new ByteBufOutputStream(buf);
                 DataOutputStream dos = new DataOutputStream(bbos)) {
                NbtIo.write(nbt, dos);
            }
            Mesh.LOGGER.debug("[Mesh-Net] Serialized chunk [{}, {}], writing to channel", x, z);
        } catch (Exception e) {
            buf.release();
            Mesh.LOGGER.error("Mesh: Failed to serialize chunk [{}, {}]", x, z, e);
            return;
        }
        ch.writeAndFlush(buf);
        Mesh.LOGGER.debug("[Mesh-Net] Flushed chunk [{}, {}] to wire", x, z);
    }
}
