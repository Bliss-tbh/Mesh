package com.bliss.mesh.mixins.server;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.common.MeshModes;
import com.bliss.mesh.server.MeshWorker;
import com.bliss.mesh.server.networking.MeshChunkOrchestrator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(IOWorker.class)
public class IOWorkerMixin {

    @Shadow @Final private RegionFileStorage storage;

    @Inject(method = "loadAsync", at = @At("HEAD"), cancellable = true)
    private void mesh$interceptLoadAsync(ChunkPos pos, CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> cir) {
        if (MeshConfig.MODE.get() == MeshModes.TICK_HOST && this.storage.info().type().equals("chunk")) {
            if (MeshWorker.BYPASS_MESH.get()) {
                Mesh.LOGGER.debug("[Mesh-IO] Bypassing mesh for chunk {}", pos);
                cir.setReturnValue(CompletableFuture.completedFuture(Optional.empty()));
                return;
            }
            CompletableFuture<CompoundTag> networkFuture = MeshChunkOrchestrator.getFuture(pos.x, pos.z);

            if (!MeshChunkOrchestrator.isRequested(pos.x, pos.z)) {
                Mesh.LOGGER.debug("[Mesh-IO] Requesting chunk {} from Chunk Host", pos);
                MeshChunkOrchestrator.markRequested(pos.x, pos.z, true); //waiting for chunk so localy say this is active
                Mesh.PACKET_SENDER.requestChunkFromStorage(pos.x, pos.z); //actual request
            } else {
                Mesh.LOGGER.debug("[Mesh-IO] Request already in flight for chunk {}", pos);
            }

            CompletableFuture<Optional<CompoundTag>> vanillaFuture = networkFuture
                    .handle((nbt, throwable) -> {
                        if (throwable != null) {
                            return Optional.empty();
                        }
                        return Optional.ofNullable(nbt);
                    });

            cir.setReturnValue(vanillaFuture);
        }
    }

    @Inject(method = "store", at = @At("HEAD"), cancellable = true)
    private void mesh$blockTickHostSaving(ChunkPos pos, CompoundTag nbt, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (MeshConfig.MODE.get() == MeshModes.TICK_HOST) {
            cir.setReturnValue(CompletableFuture.completedFuture(null));
        }
    }
}