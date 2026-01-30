package com.bliss.mesh.server.networking;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.server.MeshWorker;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.channel.Channel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.DataOutputStream;

public class MeshInternalPacketSender {
    private Channel channel;
    private EventLoopGroup group;

    public void init(String targetIp, int port, boolean isServer) {
        group = new NioEventLoopGroup();
        // If we are the Chunk Host, we BIND (Listen). If Tick Host, we CONNECT.
        if (isServer) {
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
                Mesh.LOGGER.info("Mesh (Server) listening on port {}", port);
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
                Mesh.LOGGER.info("Mesh (Client) connected to {}:{}", targetIp, port);
            } catch (InterruptedException e) {
                Mesh.LOGGER.error("Failed to connect Mesh Back-Channel", e);
            }
        }
    }

    private void setupPipeline(SocketChannel ch) {
        ch.pipeline().addLast(new LengthFieldPrepender(4));
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1048576 * 10, 0, 4, 0, 4));
        ch.pipeline().addLast(new MeshInternalPacketHandler(this));
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        Mesh.LOGGER.info("Mesh Back-Channel active: {}", channel.remoteAddress());
        Mesh.LOGGER.info("Worker Init");
        MeshWorker.start();
    }

    //send chunks back/fourth
    public void sendChunkData(int x, int z, CompoundTag nbt) {
        ByteBuf buf = channel.alloc().buffer();
        buf.writeInt(MeshInternalPackets.PUSH.id);
        buf.writeInt(x);
        buf.writeInt(z);
        try (DataOutputStream dos = new DataOutputStream(new ByteBufOutputStream(buf))) {
            NbtIo.write(nbt, dos);
            channel.writeAndFlush(buf);
        } catch (Exception e) {
            if (buf.refCnt() > 0) {
                buf.release();
            }
            Mesh.LOGGER.error("Mesh: Failed to serialize or send chunk [{}, {}] to remote. " +
                    "This may result in missing chunks on the receiver.", x, z, e);
        }
    }

    //ask for chunk from chunk pc
    public void requestChunkFromStorage(int x, int z) {
        ByteBuf buf = channel.alloc().buffer();
        buf.writeInt(MeshInternalPackets.REQUEST.id); // We defined this in the enum earlier
        buf.writeInt(x);
        buf.writeInt(z);
        channel.writeAndFlush(buf);
    }

    //ask if there is any work to be done from chunk pc
    public void requestGeneratableChunk() {
        ByteBuf buf = channel.alloc().buffer();
        buf.writeInt(MeshInternalPackets.CLAIM.id);
        channel.writeAndFlush(buf);
    }

    //send if there is unclaimed work
    public void sendGeneratableChunk(int x, int z) {
        ByteBuf buf = channel.alloc().buffer();
        buf.writeInt(MeshInternalPackets.CLAIM_INFO.id);
        buf.writeInt(x);
        buf.writeInt(z);
        channel.writeAndFlush(buf);
    }

    public void shutdown() {
        Mesh.LOGGER.info("Shutting down Mesh Back-Channel...");
        try {
            if (channel != null) {
                channel.close().sync();
            }
            MeshWorker.stopWorker();
            MeshChunkOrchestrator.clearAll();
        } catch (InterruptedException e) {
            Mesh.LOGGER.error("Error closing Mesh channel", e);
        } finally {
            if (group != null) {
                // This is the important part: it shuts down all Netty threads
                group.shutdownGracefully();
            }
        }
    }
}
