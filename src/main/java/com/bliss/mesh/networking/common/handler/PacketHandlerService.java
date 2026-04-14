package com.bliss.mesh.networking.common.handler;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import com.bliss.mesh.networking.common.sender.PacketSenderService;
import com.bliss.mesh.networking.server.chunkhost.ChunkHostPacketHandler;
import com.bliss.mesh.networking.server.tickhost.TickHostPacketHandler;
import com.bliss.mesh.generation.common.MeshWorker;
import com.bliss.mesh.generation.ChunkTracker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class PacketHandlerService extends SimpleChannelInboundHandler<ByteBuf> {

    private final CommonPacketHandler common = new CommonPacketHandler();
    private final ChunkHostPacketHandler chunkHost = new ChunkHostPacketHandler();
    private final TickHostPacketHandler tickHost = new TickHostPacketHandler();
    private final PacketSenderService packetSender;

    public PacketHandlerService(PacketSenderService packetSender) {
        this.packetSender = packetSender;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.packetSender.setChannel(ctx.channel());
        if (MeshConfig.MODE.get() == MeshModes.CHUNK_HOST) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                long seed = server.getWorldData().worldGenOptions().seed();
                this.packetSender.chunkHost.syncSeed(seed);
            }
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Mesh.LOGGER.error("[Mesh-Net] Back-channel connection lost!");

        // 1. Fail all futures so the game can recover
        ChunkTracker.failAllRequests("Connection closed");

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
        int chunkState = msg.readInt();
        common.decode(chunkState, msg);
        chunkHost.decode(chunkState, msg);
        tickHost.decode(chunkState, msg);
    }
}