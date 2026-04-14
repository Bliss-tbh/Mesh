package com.bliss.mesh.generation.common;

import com.bliss.mesh.common.MeshConfig;
import com.bliss.mesh.generation.server.chunkhost.ChunkHostWorker;
import com.bliss.mesh.generation.server.tickhost.TickHostWorker;

public class MeshWorker implements Runnable {

    protected volatile boolean running = true;
    private static MeshWorker INSTANCE;
    private static Thread WORKER_THREAD;

    public static void start() {
        if (INSTANCE != null) return;

        INSTANCE = switch (MeshConfig.MODE.get()) {
            case TICK_HOST -> new TickHostWorker();
            case CHUNK_HOST -> new ChunkHostWorker();
            default -> new MeshWorker();
        };

        WORKER_THREAD = new Thread(INSTANCE, "Mesh-Worker-Thread");
        WORKER_THREAD.setDaemon(true);
        WORKER_THREAD.start();
    }

    public void stop() {
        running = false;
    }

    public static void stopWorker() {
        if (INSTANCE != null) {
            INSTANCE.stop();
            WORKER_THREAD.interrupt();
            INSTANCE = null;
            WORKER_THREAD = null;
        }
    }

    @Override
    public void run() {

    }
}