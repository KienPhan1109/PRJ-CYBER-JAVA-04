package com.cyber.util;

public class PrintUtils {
    // Tự động thiết lập ANSI Code cho màu
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";

    /**
     * In ra thông báo thành công với màu Xanh Lá
     */
    public static void printSuccess(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ANSI_GREEN + "[THÀNH CÔNG] " + formattedMessage + ANSI_RESET);
    }

    /**
     * In ra thông báo lỗi với màu Đỏ
     */
    public static void printError(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ANSI_RED + "[LỖI] " + formattedMessage + ANSI_RESET);
    }

    /**
     * In ra thông báo cảnh báo với màu Vàng
     */
    public static void printWarning(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ANSI_YELLOW + "[CẢNH BÁO] " + formattedMessage + ANSI_RESET);
    }
}
