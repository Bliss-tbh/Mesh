package com.bliss.mesh.server.chunkhost;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Mesh.MODID, value = Dist.DEDICATED_SERVER)
public class ChunkHostEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (MeshConfig.MODE.get() == MeshModes.CHUNK_HOST) {
        }
    }

}
