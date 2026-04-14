package com.bliss.mesh;

import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import com.bliss.mesh.generation.common.MeshWorker;
import com.bliss.mesh.networking.common.sender.PacketSenderService;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(Mesh.MODID)
public class Mesh {
    public static final String MODID = "mesh";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static PacketSenderService PACKET_SENDER = new PacketSenderService();

    public Mesh(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("-- Mesh Init --");
        LOGGER.info("Jack Holding is a sussy baka (◕ᗜ◕)");

        NeoForge.EVENT_BUS.register(this);
        container.registerConfig(ModConfig.Type.STARTUP, MeshConfig.SPEC);

        modEventBus.addListener(this::onConfigLoad);

        MeshModes currentMode = MeshConfig.MODE.get();
        if (currentMode == MeshModes.TICK_HOST) {
            LOGGER.info("-> TICK MODE");
            PACKET_SENDER.init(MeshConfig.REMOTE_ADDRESS.get(), MeshConfig.PORT.get(), false);
        } else if (currentMode == MeshModes.CHUNK_HOST) {
            LOGGER.info("-> CHUNK MODE");
            PACKET_SENDER.init(MeshConfig.REMOTE_ADDRESS.get(), MeshConfig.PORT.get(), true);
        } else {
            LOGGER.info("-> STANDARD MODE (Client/Normal Server)");
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (PACKET_SENDER != null) PACKET_SENDER.shutdown();
        MeshWorker.stopWorker();
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(MODID)) {
            LOGGER.info("Mesh config loaded!");
        }
    }

    //TODO: I keep jumping between ChunkPos, ChunkPos.asLong and the x and z ints, I should probably just stick with one.
    //TODO: Client stuffs
    //TODO: Make openable in any order
}
