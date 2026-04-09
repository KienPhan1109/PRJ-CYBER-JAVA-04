package com.cyber;

import com.cyber.view.AppRouter;

public class Main {
    public static void main(String[] args) {
        // Khởi động luồng chạy ngầm tự động trừ tiền
        com.cyber.service.SessionHeartbeatManager.getInstance().startHeartbeat();

        // System shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            com.cyber.service.SessionHeartbeatManager.getInstance().shutdown();
            System.out.println("Đã đóng an toàn các luồng nền.");
        }));

        // Entry point is managed by AppRouter to keep Main.java clean.
        AppRouter router = new AppRouter();
        router.start();

        // Lỡ user thoát qua loop của AppRouter
        com.cyber.service.SessionHeartbeatManager.getInstance().shutdown();
    }
}
