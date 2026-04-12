package com.cyber.util;

import com.cyber.model.enums.ComputerStatus;
import com.cyber.model.enums.FBStatus;
import com.cyber.model.enums.FbTemperature;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.sql.Timestamp;

public class FormatUtils {
    private static final Locale VN_LOCALE = Locale.of("vi", "VN");
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
    
    public static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return timestamp.toLocalDateTime().format(DATETIME_FORMATTER);
    }

    public static String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d tiếng %02d phút %02d giây", hours, minutes, seconds);
        } else {
            return String.format("%d phút %02d giây", minutes, seconds);
        }
    }

    public static String formatComputerStatus(ComputerStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case AVAILABLE -> "\033[32m[SẴN SÀNG]" + ColorConst.RESET;
            case IN_USE -> "\033[31m[ĐANG SỬ DỤNG]" + ColorConst.RESET;
            case MAINTENANCE -> "\033[33m[BẢO TRÌ]" + ColorConst.RESET;
            case HIDDEN -> "\033[37m[ĐÃ ẨN]" + ColorConst.RESET;
        };
    }

    public static String formatFbTemperature(FbTemperature temperature) {
        if (temperature == null) return "N/A";
        return switch (temperature) {
            case HOT ->  ColorConst.RED + "HOT" + ColorConst.RESET;
            case COLD -> ColorConst.BLUE + "COLD" + ColorConst.RESET;
            case ICED -> ColorConst.CYAN + "ICED" + ColorConst.RESET;
            case NONE -> ColorConst.WHITE + "NONE" + ColorConst.RESET;
        };
    }

    public static String formatFbStatus(FBStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case ACTIVE ->  ColorConst.GREEN + "ACTIVE" + ColorConst.RESET;
            case OUT_OF_STOCK -> ColorConst.YELLOW + "OUT_OF_STOCK" + ColorConst.RESET;
            case HIDDEN -> ColorConst.RED + "HIDDEN" + ColorConst.RESET;
        };
    }
}
