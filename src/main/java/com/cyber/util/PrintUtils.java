package com.cyber.util;

import com.cyber.constant.ColorConstant;

public class PrintUtils {
    // Tự động thiết lập ANSI Code cho màu
    public static String colorText(String text, String color) {
        String code = switch (color.toUpperCase()) {
            case "RED" -> ColorConstant.RED;
            case "GREEN" -> ColorConstant.GREEN;
            case "YELLOW" -> ColorConstant.YELLOW;
            case "BLUE" -> ColorConstant.BLUE;
            case "PURPLE" -> ColorConstant.PURPLE;
            case "CYAN" -> ColorConstant.CYAN;
            case "GRAY" -> ColorConstant.GRAY;
            case "WHITE" -> ColorConstant.WHITE;
            default -> ColorConstant.RESET;
        };
        return code + text + ColorConstant.RESET;
    }

    public static void printSuccess(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ColorConstant.GREEN + "[THÀNH CÔNG] " + formattedMessage + ColorConstant.RESET);
    }

    public static void printError(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ColorConstant.RED + "[LỖI] " + formattedMessage + ColorConstant.RESET);
    }

    public static void printWarning(String message, Object... args) {
        String formattedMessage = args != null && args.length > 0 ? String.format(message, args) : message;
        System.out.println(ColorConstant.YELLOW + "[CẢNH BÁO] " + formattedMessage + ColorConstant.RESET);
    }

    public static void printTableSeparator(int width) {
        System.out.println("+" + "-".repeat(width - 2) + "+");
    }

    public static void printSeparator(int width) {
        System.out.println("=".repeat(width));
    }
}
