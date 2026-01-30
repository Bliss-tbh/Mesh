package com.bliss.mesh.mixins.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {

    @Invoker("applyLightData")
    void meshmod$applyLightData(
            int x,
            int z,
            ClientboundLightUpdatePacketData data
    );

}
