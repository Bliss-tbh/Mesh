package com.bliss.mesh.events.server;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Mesh.MODID, value = Dist.DEDICATED_SERVER)
public class TickHostEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // check if we are actually in TICK_HOST mode
        if (MeshConfig.MODE.get() != MeshModes.TICK_HOST) {
            return; // nothing if we are a Chunk Host or a standard Client maybe there is a way so we don't check every playerlogin, just once???
        }

        // DISABLED: Direct client-to-chunk-host connections cause protocol mismatch
        // Chunks flow: Chunk Host -> Tick Host (Mesh raw bytes) -> Client (Minecraft protocol)
        // The handshake packet was causing clients to connect to Chunk Host using Minecraft protocol,
        // but Chunk Host's internal listener uses Mesh's raw byte protocol.
        /*
        if (event.getEntity() instanceof ServerPlayer player) {
            String chunkHostIp = MeshConfig.REMOTE_ADDRESS.get();
            int chunkHostPort = MeshConfig.PORT.get();

            Mesh.LOGGER.info("Tick Host: Handshaking player {} to Chunk Host at {}:{}",
                    player.getScoreboardName(), chunkHostIp, chunkHostPort);

            PacketDistributor.sendToPlayer(player, new MeshHandshakePacket(chunkHostIp, chunkHostPort));
        }
        */
    }
}
