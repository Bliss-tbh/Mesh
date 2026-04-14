package com.bliss.mesh.networking.common.sender;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.generation.common.MeshWorker;
import com.bliss.mesh.networking.common.handler.PacketHandlerService;
import com.bliss.mesh.networking.server.chunkhost.ChunkHostPacketSender;
import com.bliss.mesh.networking.server.tickhost.TickHostPacketSender;
import com.bliss.mesh.generation.ChunkTracker;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.channel.Channel;

public class PacketSenderService {
    private volatile Channel channel;
    private EventLoopGroup group;
    public CommonPacketSender common;
    public TickHostPacketSender tickHost;
    public ChunkHostPacketSender chunkHost;

    public void init(String targetIp, int port, boolean isHost) {
        group = new NioEventLoopGroup();
        if (isHost) {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            setupPipeline(ch);
                        }
                    });
            try {
                b.bind(port).sync();
                Mesh.LOGGER.info("ChunkHost (Host) listening on port {}", port);
            } catch (InterruptedException e) {
                Mesh.LOGGER.error("Failed to bind Mesh Back-Channel", e);
            }
        } else {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            setupPipeline(ch);
                        }
                    });
            try {
                this.channel = b.connect(targetIp, port).sync().channel();
                Mesh.LOGGER.info("TickHost (Client) connected to {}:{}", targetIp, port);
            } catch (InterruptedException e) {
                Mesh.LOGGER.error("Failed to connect Mesh Back-Channel", e);
            }
        }
    }

    private void setupPipeline(SocketChannel ch) {
        ch.pipeline().addLast(new LengthFieldPrepender(4));
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1048576 * 10, 0, 4, 0, 4));
        ch.pipeline().addLast(new PacketHandlerService(this));
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        this.common = new CommonPacketSender(this.channel);
        this.tickHost = new TickHostPacketSender(this.channel);
        this.chunkHost = new ChunkHostPacketSender(this.channel);
        Mesh.LOGGER.info("Mesh Back-Channel active: {}", channel.remoteAddress());
        Mesh.LOGGER.info("Worker Init");
        MeshWorker.start();
    }

    public void shutdown() {
        Mesh.LOGGER.info("Shutting down Mesh Back-Channel...");
        try {
            if (channel != null) {
                channel.close().sync();
            }
            MeshWorker.stopWorker();
            ChunkTracker.clearAll();
        } catch (InterruptedException e) {
            Mesh.LOGGER.error("Error closing Mesh channel", e);
        } finally {
            if (group != null) {
                group.shutdownGracefully();
            }
        }
    }
}
