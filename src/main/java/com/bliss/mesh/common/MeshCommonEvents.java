package com.bliss.mesh.common;

import com.bliss.mesh.client.MeshClientPacketHandler;
import com.bliss.mesh.common.networking.MeshHandshakePacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import static com.bliss.mesh.Mesh.MODID;

@EventBusSubscriber(modid = MODID)
public class MeshCommonEvents {

    @SubscribeEvent
    public static void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                MeshHandshakePacket.TYPE,
                MeshHandshakePacket.CODEC,
                MeshClientPacketHandler::handleHandshake
        );
    }
}
