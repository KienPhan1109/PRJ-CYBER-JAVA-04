package com.cyber.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtils {
    private static final Locale VN_LOCALE = new Locale("vi", "VN");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String formatVND(double amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(VN_LOCALE);
        return currencyFormatter.format(amount);
    }

    public static String formatVND(BigDecimal amount) {
        if (amount == null) {
            return "0 ₫";
        }
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(VN_LOCALE);
        return currencyFormatter.format(amount);
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "N/A";
        }
        return date.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    public static String formatTime(LocalTime time) {
        if (time == null) {
            return "N/A";
        }
        return time.format(TIME_FORMATTER);
    }

    public static String formatId(String prefix, int id) {
        return String.format("%s-%03d", prefix, id);
    }

    public static String formatComputerStatus(com.cyber.model.enums.ComputerStatus status) {
        if (status == null) return "N/A";
        switch (status) {
            case AVAILABLE: return "\033[32m[SẴN SÀNG]\033[0m";
            case IN_USE: return "\033[31m[ĐANG SỬ DỤNG]\033[0m";
            case MAINTENANCE: return "\033[33m[BẢO TRÌ]\033[0m";
            default: return status.name();
        }
    }

    public static String formatServiceItemStatus(com.cyber.model.enums.ServiceItemStatus status) {
        if (status == null) return "N/A";
        switch (status) {
            case ACTIVE: return "\033[32m[ĐANG BÁN]\033[0m";
            case OUT_OF_STOCK: return "\033[31m[HẾT HÀNG]\033[0m";
            default: return status.name();
        }
    }
}
