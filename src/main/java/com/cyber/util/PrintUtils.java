package com.cyber.util;

public class PrintUtils {
    // Tự động thiết lập ANSI Code cho màu
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_BLUE = "\033[34m";
    private static final String ANSI_PURPLE = "\033[35m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_WHITE = "\033[37m";

    public static String colorText(String text, String color) {
        String code = switch (color.toUpperCase()) {
            case "RED" -> ANSI_RED;
            case "GREEN" -> ANSI_GREEN;
            case "YELLOW" -> ANSI_YELLOW;
            case "BLUE" -> ANSI_BLUE;
            case "PURPLE" -> ANSI_PURPLE;
            case "CYAN" -> ANSI_CYAN;
            default -> ANSI_RESET;
        };
        return code + text + ANSI_RESET;
    }

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
