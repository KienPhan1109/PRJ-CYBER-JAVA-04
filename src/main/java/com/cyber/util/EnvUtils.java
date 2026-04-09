package com.cyber.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvUtils {
    private static final Map<String, String> ENV_STORE = new HashMap<>();
    private static boolean loaded = false;

    private static void loadEnv() {
        if (loaded) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Bỏ qua comments và dòng trống
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf("=");
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    // Loại bỏ comments bên trong line nếu có #
                    int commentIdx = value.indexOf("#");
                    if (commentIdx >= 0) {
                        value = value.substring(0, commentIdx).trim();
                    }
                    ENV_STORE.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("⚠ WARNING: Không tìm thấy file '.env' ở thư mục gốc, hệ thống sẽ sử dụng cấu hình mặc định hoặc Environment Variables của OS.");
        }
        loaded = true;
    }

    public static String get(String key, String defaultValue) {
        if (!loaded) loadEnv();
        String sysEnv = System.getenv(key);
        if (sysEnv != null && !sysEnv.isEmpty()) return sysEnv;
        return ENV_STORE.getOrDefault(key, defaultValue);
    }
    
    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val); } catch (Exception e) { return defaultValue; }
    }
}
