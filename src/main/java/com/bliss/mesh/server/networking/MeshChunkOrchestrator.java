package com.bliss.mesh.server.networking;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class MeshChunkOrchestrator {

    private static final Set<ChunkPos> CLAIMS = ConcurrentHashMap.newKeySet();
    private static final Map<ChunkPos, CompoundTag> PENDING_CHUNKS = new ConcurrentHashMap<>();
    private static final BlockingQueue<ChunkPos> GENERATION_QUEUE = new LinkedBlockingQueue<>();
    private static final Queue<ChunkPos> LOCAL_TASKS = new ConcurrentLinkedQueue<>();
    private static final Set<ChunkPos> ACTIVE_REQUESTS = ConcurrentHashMap.newKeySet();
    private static final Map<ChunkPos, CompletableFuture<CompoundTag>> FUTURE_REQUESTS = new ConcurrentHashMap<>();

    //-------CHUNK HOST-------

    public static void enqueueWork(int x, int z) {
        GENERATION_QUEUE.add(new ChunkPos(x, z));
    }

    public static ChunkPos takeWork() {
        ChunkPos target = null;

        while ((target = GENERATION_QUEUE.poll()) != null) {
            if (!CLAIMS.contains(target)) {
                markClaimed(target.x, target.z);
                return target;
            }
        }
        return null;
    }

    public static void markClaimed(int x, int z) {
        CLAIMS.add(new ChunkPos(x, z));
        Mesh.LOGGER.info("Host {} has claimed chunk [{}, {}]", MeshConfig.MODE.get(), x, z);
    }

    public static void releaseClaim(int x, int z) {
        CLAIMS.remove(new ChunkPos(x, z));
    }

    //-------TICK HOST-------

    public static boolean isRequested(int x, int z) {
        return ACTIVE_REQUESTS.contains(new ChunkPos(x, z));
    }

    public static void markRequested(int x, int z, boolean active) {
        if (active) ACTIVE_REQUESTS.add(new ChunkPos(x, z));
        else ACTIVE_REQUESTS.remove(new ChunkPos(x, z));
    }

    public static CompletableFuture<CompoundTag> getFuture(int x, int z) {
        return FUTURE_REQUESTS.computeIfAbsent(new ChunkPos(x, z), k -> new CompletableFuture<>());
    }

    //-------COMMON-------

    public static void receiveChunkData(int x, int z, CompoundTag nbt) {
        ChunkPos pos = new ChunkPos(x, z);
        Mesh.LOGGER.debug("[Mesh-Orch] Data received for [{}, {}]. Pending futures: {}",
                x, z, FUTURE_REQUESTS.containsKey(pos));

        CompletableFuture<CompoundTag> future = FUTURE_REQUESTS.remove(pos);
        if (future != null) {
            future.complete(nbt);
            Mesh.LOGGER.debug("[Mesh-Orch] Future completed for chunk {}", pos);
        } else {
            Mesh.LOGGER.warn("[Mesh] Received unsolicited chunk data for {}", pos);
        }
    }

    public static void addLocalTask(int x, int z) {
        LOCAL_TASKS.add(new ChunkPos(x, z));
    }

    public static ChunkPos takeLocalTask() {
        return LOCAL_TASKS.poll();
    }

    public static boolean isLocalTasksEmpty() {
        return LOCAL_TASKS.isEmpty();
    }

    public static void failAllRequests(String reason) {
        int count = FUTURE_REQUESTS.size();
        if (count > 0) {
            Mesh.LOGGER.warn("[Mesh-Orch] Failing {} pending requests. Reason: {}", count, reason);

            FUTURE_REQUESTS.forEach((pos, future) -> {
                future.completeExceptionally(new RuntimeException("Mesh connection lost: " + reason));
            });

            FUTURE_REQUESTS.clear();
            ACTIVE_REQUESTS.clear();
            CLAIMS.clear();
        }
    }

    public static void clearAll() {
        CLAIMS.clear();
        PENDING_CHUNKS.clear();
        GENERATION_QUEUE.clear();
        ACTIVE_REQUESTS.clear();
        FUTURE_REQUESTS.forEach((pos, future) -> future.cancel(true));
        FUTURE_REQUESTS.clear();
    }
}
