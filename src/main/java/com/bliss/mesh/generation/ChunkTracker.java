package com.bliss.mesh.generation;

import com.bliss.mesh.Mesh;
import com.bliss.mesh.common.MeshConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class ChunkTracker {

    private static final Set<ChunkPos> CLAIMS = ConcurrentHashMap.newKeySet();
    private static final BlockingQueue<ChunkPos> GENERATION_QUEUE = new LinkedBlockingQueue<>();
    private static final Queue<ChunkPos> LOCAL_TASKS = new ConcurrentLinkedQueue<>();
    private static final Map<ChunkPos, CompletableFuture<CompoundTag>> FUTURE_REQUESTS = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, Long> REQUEST_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Set<ChunkPos> LOCAL_GENERATING = ConcurrentHashMap.newKeySet();
    private static final int MAX_PENDING_REQUESTS = 200;
    private static final long STALE_THRESHOLD_MS = 10000;

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

    public static boolean hasPendingRequest(int x, int z) {
        return FUTURE_REQUESTS.containsKey(new ChunkPos(x, z));
    }

    public static CompletableFuture<CompoundTag> createRequest(int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);
        return FUTURE_REQUESTS.computeIfAbsent(pos, k -> {
            REQUEST_TIMESTAMPS.put(pos, System.currentTimeMillis());
            return new CompletableFuture<>();
        });
    }

    public static void removeRequest(int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);
        FUTURE_REQUESTS.remove(pos);
    }

    public static boolean isRequested(int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);
        Long timestamp = REQUEST_TIMESTAMPS.get(pos);
        return timestamp != null;
    }

    public static void markRequested(int x, int z, boolean requested) {
        ChunkPos pos = new ChunkPos(x, z);
        if (requested) {
            REQUEST_TIMESTAMPS.put(pos, System.currentTimeMillis());
        } else {
            REQUEST_TIMESTAMPS.remove(pos);
        }
    }

    public static CompletableFuture<CompoundTag> getFuture(int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);
        return FUTURE_REQUESTS.computeIfAbsent(pos, k -> {
            REQUEST_TIMESTAMPS.put(pos, System.currentTimeMillis());
            return new CompletableFuture<>();
        });
    }

    public static boolean canAcceptRequest() {
        return FUTURE_REQUESTS.size() < MAX_PENDING_REQUESTS;
    }

    //-------COMMON-------

    public static void addChunkToWorld(int x, int z, CompoundTag nbt) {
        ChunkPos pos = new ChunkPos(x, z);
        CompletableFuture<CompoundTag> future = FUTURE_REQUESTS.remove(pos);
        REQUEST_TIMESTAMPS.remove(pos);
        if (future != null) {
            Mesh.LOGGER.debug("[Mesh-Orch] Received chunk {} ({} pending)", pos, FUTURE_REQUESTS.size());
            future.complete(nbt);
        } else {
            Mesh.LOGGER.warn("[Mesh-Orch] Unsolicited chunk data for {}", pos);
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

    public static int getLocalTaskCount() {
        return LOCAL_TASKS.size();
    }

    public static void markLocallyGenerating(int x, int z) {
        LOCAL_GENERATING.add(new ChunkPos(x, z));
    }

    public static void unmarkLocallyGenerating(int x, int z) {
        LOCAL_GENERATING.remove(new ChunkPos(x, z));
    }

    public static boolean isLocallyGenerating(int x, int z) {
        return LOCAL_GENERATING.contains(new ChunkPos(x, z));
    }

    public static void failAllRequests(String reason) {
        int count = FUTURE_REQUESTS.size();
        if (count > 0) {
            Mesh.LOGGER.warn("[Mesh-Orch] Failing {} pending requests. Reason: {}", count, reason);

            FUTURE_REQUESTS.forEach((pos, future) -> {
                future.completeExceptionally(new RuntimeException("Mesh connection lost: " + reason));
            });

            FUTURE_REQUESTS.clear();
            REQUEST_TIMESTAMPS.clear();
            CLAIMS.clear();
        }
    }

    public static void clearAll() {
        CLAIMS.clear();
        GENERATION_QUEUE.clear();
        FUTURE_REQUESTS.forEach((pos, future) -> future.cancel(true));
        FUTURE_REQUESTS.clear();
        REQUEST_TIMESTAMPS.clear();
    }

    public static int getPendingCount() {
        return FUTURE_REQUESTS.size();
    }

    public static int getQueueSize() {
        return GENERATION_QUEUE.size();
    }

    public static void checkForStaleRequests() {
        long now = System.currentTimeMillis();
        FUTURE_REQUESTS.forEach((pos, future) -> {
            Long timestamp = REQUEST_TIMESTAMPS.get(pos);
            if (timestamp != null && now - timestamp > STALE_THRESHOLD_MS) {
                Mesh.LOGGER.warn("[Mesh-Orch] Stale request detected for {} (age: {}ms)", pos, now - timestamp);
            }
        });
    }
}
