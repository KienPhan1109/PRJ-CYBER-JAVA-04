package com.cyber;

import com.cyber.view.AppRouter;

public class Main {
    public static void main(String[] args) {
        // Entry point is managed by AppRouter to keep Main.java clean.
        AppRouter router = new AppRouter();
        router.start();
    }
}
