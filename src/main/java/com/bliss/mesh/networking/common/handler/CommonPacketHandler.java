package com.bliss.mesh.networking.common.handler;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.networking.common.Packets;
import com.bliss.mesh.generation.ChunkTracker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;

public class CommonPacketHandler {

    public void decode(int type, ByteBuf msg) {
        if (type == Packets.PUSH.id) {
            handleChunkData(msg, msg.readInt(), msg.readInt());
        }
    }

    private void handleChunkData(ByteBuf msg, int x, int z) {
        try {
            ByteBufInputStream stream = new ByteBufInputStream(msg);
            CompoundTag nbt = NbtIo.read(stream, NbtAccounter.unlimitedHeap());
            ChunkTracker.addChunkToWorld(x, z, nbt);
        } catch (IOException e) {
            Mesh.LOGGER.error("Mesh: Failed to read chunk data for [{}, {}]", x, z, e);
        }
    }
}
