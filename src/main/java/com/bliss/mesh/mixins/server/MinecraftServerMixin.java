package com.bliss.mesh.mixins.server;

import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "tickChildren", at = @At("HEAD"), cancellable = true)
    private void mesh$disableTickingOnChunkHost(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (MeshConfig.MODE.get() == MeshModes.CHUNK_HOST) {
            // 1. Still process networking (Keep the Mesh connection alive)
            MinecraftServer server = (MinecraftServer)(Object)this;
            server.getConnection().tick();

            // 3. CANCEL everything else (No entity AI, no block ticks, no weather)
            ci.cancel();
        }
    }
}
