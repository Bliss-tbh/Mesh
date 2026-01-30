package com.bliss.mesh.server.tickhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import com.bliss.mesh.common.networking.MeshHandshakePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Mesh.MODID, value = Dist.DEDICATED_SERVER)
public class TickHostEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 1. Check if we are actually in TICK_HOST mode
        if (MeshConfig.MODE.get() != MeshModes.TICK_HOST) {
            return; // Do nothing if we are a Chunk Host or a standard Client
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            String chunkHostIp = MeshConfig.REMOTE_ADDRESS.get();
            int chunkHostPort = MeshConfig.PORT.get();

            Mesh.LOGGER.info("Tick Host: Handshaking player {} to Chunk Host at {}:{}",
                    player.getScoreboardName(), chunkHostIp, chunkHostPort);

            PacketDistributor.sendToPlayer(player, new MeshHandshakePacket(chunkHostIp, chunkHostPort));
        }
    }
}
