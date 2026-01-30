package com.bliss.mesh.server.networking;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.server.MeshWorker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;

public class MeshInternalPacketHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final MeshInternalPacketSender packetSender;

    public MeshInternalPacketHandler(MeshInternalPacketSender packetSender) {
        this.packetSender = packetSender;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.packetSender.setChannel(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Mesh.LOGGER.error("[Mesh-Net] Back-channel connection lost!");

        // 1. Fail all futures so the game can recover
        MeshChunkOrchestrator.failAllRequests("Connection closed");

        // 2. Shut down the worker or clean up state
        MeshWorker.stopWorker();

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Mesh.LOGGER.error("[Mesh-Net] Pipeline error: ", cause);
        ctx.close(); // This will trigger channelInactive
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws InterruptedException {
        if (msg.readableBytes() < 4) return; // Safety check

        int chunkState = msg.readInt();

        // ID only packets (4 bytes total)
        if (chunkState == MeshInternalPackets.CLAIM.id) {
            checkGenerationQueue();
            return; // Exit early! Don't try to read X and Z.
        }

        // Packets that include Coordinates (ID + X + Z = 12 bytes total)
        if (msg.readableBytes() < 8) return; // Ensure X and Z are actually there
        int x = msg.readInt();
        int z = msg.readInt();

        if (chunkState == MeshInternalPackets.REQUEST.id) {
            handleRequestFromTickHost(x, z);
        }
        else if (chunkState == MeshInternalPackets.PUSH.id) {
            handleDataPush(msg, x, z);
        }
        else if (chunkState == MeshInternalPackets.CLAIM_INFO.id) {
            generateChunk(x, z);
        }
    }

    //chunk pc checks if it has chunk if not put up for generation
    private void handleRequestFromTickHost(int x, int z) {
        Mesh.LOGGER.debug("[Mesh-Net] Tick Host requested chunk [{}, {}]", x, z);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerLevel level = server.overworld();
            ChunkAccess loaded = level.getChunk(x, z, ChunkStatus.FULL, false);
            if (loaded != null) { //has chunk
                Mesh.LOGGER.debug("[Mesh-Net] Chunk [{}, {}] found in RAM, sending immediately.", x, z);
                CompoundTag tag = ChunkSerializer.write(level, loaded); //write to disc
                this.packetSender.sendChunkData(x, z, tag);
            } else { //doesn't have chunk
                Mesh.LOGGER.debug("[Mesh-Net] Chunk [{}, {}] not loaded. Enqueueing for generation.", x, z);
                MeshChunkOrchestrator.enqueueWork(x, z);
            }
        }
    }

    public void checkGenerationQueue() {
        ChunkPos chunkPos = MeshChunkOrchestrator.takeWork();
        if (chunkPos != null) {
            Mesh.PACKET_SENDER.sendGeneratableChunk(chunkPos.x, chunkPos.z);
        }
    }

    private void generateChunk(int x, int z) {
        MeshChunkOrchestrator.addLocalTask(x, z);
    }

    //-------COMMON-------

    private void handleDataPush(ByteBuf msg, int x, int z) {
        try {
            ByteBufInputStream stream = new ByteBufInputStream(msg);
            MeshChunkOrchestrator.releaseClaim(x, z);
            MeshChunkOrchestrator.markRequested(x, z, false);

            CompoundTag nbt = NbtIo.read(stream, NbtAccounter.unlimitedHeap());
            MeshChunkOrchestrator.receiveChunkData(x, z, nbt);
        } catch (IOException e) {
            Mesh.LOGGER.error("Mesh: Failed to read chunk data for [{}, {}]", x, z, e);
        }
    }
}