package com.bliss.mesh.events.server;

import com.bliss.mesh.Mesh;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Mesh.MODID, value = Dist.DEDICATED_SERVER)
public class ChunkHostEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // I need to stop ticking on chunk host
    }

}
