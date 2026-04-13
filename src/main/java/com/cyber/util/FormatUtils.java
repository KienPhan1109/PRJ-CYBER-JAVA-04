package com.cyber.util;

import com.cyber.model.enums.BookingStatus;
import com.cyber.model.enums.ComputerStatus;
import com.cyber.model.enums.FBStatus;
import com.cyber.model.enums.FbTemperature;
import com.cyber.model.enums.UserStatus;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.sql.Timestamp;

public class FormatUtils {
    private static final Locale VN_LOCALE = Locale.of("vi", "VN");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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

    public static String formatId(String prefix, int id) {
        if (prefix == null) prefix = "ID";
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

    public static String truncate(String text) {
        return truncate(text, 50);
    }

    public static String truncate(String text, int max) {
        String str = formatValue(text);
        if ("---".equals(str) || str.length() <= max) return str;
        return str.substring(0, Math.max(0, max - 3)) + "...";
    }

    public static String formatValue(Object value) {
        String str = value == null ? "" : value.toString().trim();
        return str.isEmpty() ? "---" : str;
    }

    public static String formatComputerStatus(ComputerStatus status) {
        if (status == null) return "---";
        return switch (status) {
            case AVAILABLE -> ColorConst.GREEN + "Sẵn sàng" + ColorConst.RESET;
            case IN_USE -> ColorConst.YELLOW + "Đang sử dụng" + ColorConst.RESET;
            case MAINTENANCE -> ColorConst.CYAN + "Bảo trì" + ColorConst.RESET;
            case HIDDEN -> ColorConst.RED + "Đã ẩn"+ ColorConst.RESET;
        };
    }

    public static String formatFbTemperature(FbTemperature temperature) {
        if (temperature == null) return "---";
        return switch (temperature) {
            case HOT  -> ColorConst.RED + "Nóng" + ColorConst.RESET;
            case COLD -> ColorConst.BLUE + "Lạnh" + ColorConst.RESET;
            case ICED -> ColorConst.CYAN + "Đá" + ColorConst.RESET;
            case NONE -> ColorConst.WHITE + "Không" + ColorConst.RESET;
        };
    }

    public static String formatFbStatus(FBStatus status) {
        if (status == null) return "---";
        return switch (status) {
            case ACTIVE -> ColorConst.GREEN + "Đang bán" + ColorConst.RESET;
            case OUT_OF_STOCK -> ColorConst.YELLOW + "Hết hàng" + ColorConst.RESET;
            case HIDDEN -> ColorConst.RED + "Đã ẩn" + ColorConst.RESET;
        };
    }

    public static String formatFbAvailability(String availability) {
        if (availability == null || availability.isBlank()) return "---";
        return switch (availability.toUpperCase()) {
            case "ALL" -> "Cả ngày";
            case "MORNING" -> "Buổi sáng";
            case "AFTERNOON" -> "Buổi chiều";
            case "EVENING" -> "Buổi tối";
            default -> availability;
        };
    }

    public static String formatBookingStatus(BookingStatus status) {
        if (status == null) return "---";
        return switch (status) {
            case PENDING -> ColorConst.YELLOW + "Chờ duyệt" + ColorConst.RESET;
            case ACTIVE -> ColorConst.GREEN + "Đang chơi" + ColorConst.RESET;
            case COMPLETED -> ColorConst.CYAN + "Hoàn thành" + ColorConst.RESET;
            case CANCELLED -> ColorConst.RED + "Đã hủy" + ColorConst.RESET;
            case RESERVED -> ColorConst.PURPLE + "Đã đặt trước" + ColorConst.RESET;
        };
    }

    public static String formatUserStatus(UserStatus status) {
        if (status == null) return "---";
        return switch (status) {
            case ACTIVE -> ColorConst.GREEN + "Hoạt động" + ColorConst.RESET;
            case LOCKED -> ColorConst.RED + "Bị khóa" + ColorConst.RESET;
        };
    }
}
