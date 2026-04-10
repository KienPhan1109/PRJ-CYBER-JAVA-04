package com.cyber.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.cyber.util.PrintUtils;

public class SessionHeartbeatManager {
    private static SessionHeartbeatManager instance;
    private ScheduledExecutorService scheduler;

    private SessionHeartbeatManager() {
        // Initialize an executor with 1 core thread
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public static synchronized SessionHeartbeatManager getInstance() {
        if (instance == null) {
            instance = new SessionHeartbeatManager();
        }
        return instance;
    }

    public void startHeartbeat() {
        int intervalMs = com.cyber.util.EnvUtils.getInt("HEARTBEAT_INTERVAL_MS", 10000);
        int intervalSeconds = intervalMs / 1000;
        if (intervalSeconds == 0) intervalSeconds = 1;

        Runnable task = () -> {
            try {
                BookingService.getInstance().processHeartbeatSession(intervalMs);
                // Quét và hủy các reservation quá hạn (1 phút để test nhanh)
                BookingService.getInstance().processOverdueReservations(1);
            } catch (Exception e) {
                // Log silently to not disrupt the console too much
                System.err.println("[Heartbeat Error] Lỗi xử lý phiên chạy ngầm: " + e.getMessage());
            }
        };
        // Schedule fixed rate
        scheduler.scheduleAtFixedRate(task, intervalSeconds, intervalSeconds, TimeUnit.SECONDS); 
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
