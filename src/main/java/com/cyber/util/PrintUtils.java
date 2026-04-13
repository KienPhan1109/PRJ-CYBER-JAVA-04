package com.cyber.util;

public class PrintUtils {
    // Tự động thiết lập ANSI Code cho màu
    public static String colorText(String text, String color) {
        String code = switch (color.toUpperCase()) {
            case "RED" -> ColorConst.RED;
            case "GREEN" -> ColorConst.GREEN;
            case "YELLOW" -> ColorConst.YELLOW;
            case "BLUE" -> ColorConst.BLUE;
            case "PURPLE" -> ColorConst.PURPLE;
            case "CYAN" -> ColorConst.CYAN;
            case "GRAY" -> ColorConst.GRAY;
            case "WHITE" -> ColorConst.WHITE;
            default -> ColorConst.RESET;
        };
        return code + text + ColorConst.RESET;
    }

    public static void printSuccess(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ColorConst.GREEN + "[THÀNH CÔNG] " + formattedMessage + ColorConst.RESET);
    }

    public static void printError(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ColorConst.RED + "[LỖI] " + formattedMessage + ColorConst.RESET);
    }

    public static void printWarning(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ColorConst.YELLOW + "[CẢNH BÁO] " + formattedMessage + ColorConst.RESET);
    }

    public static void printTableSeparator(int width) {
        System.out.println("+" + "-".repeat(width - 2) + "+");
    }

    public static void printSeparator(int width) {
        System.out.println("=".repeat(width));
    }
}
