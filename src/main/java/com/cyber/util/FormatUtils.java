package com.cyber.util;

import com.cyber.constant.ColorConstant;
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
            case AVAILABLE -> ColorConstant.GREEN + "Sẵn sàng" + ColorConstant.RESET;
            case IN_USE -> ColorConstant.YELLOW + "Đang sử dụng" + ColorConstant.RESET;
            case MAINTENANCE -> ColorConstant.CYAN + "Bảo trì" + ColorConstant.RESET;
            case HIDDEN -> ColorConstant.RED + "Đã ẩn"+ ColorConstant.RESET;
        };
    }

    public static String formatFbTemperature(FbTemperature temperature) {
        if (temperature == null) return "---";
        return switch (temperature) {
            case HOT  -> ColorConstant.RED + "Nóng" + ColorConstant.RESET;
            case COLD -> ColorConstant.BLUE + "Lạnh" + ColorConstant.RESET;
            case ICED -> ColorConstant.CYAN + "Đá" + ColorConstant.RESET;
            case NONE -> ColorConstant.WHITE + "Không" + ColorConstant.RESET;
        };
    }

    public static String formatFbStatus(FBStatus status) {
        if (status == null) return "---";
        return switch (status) {
            case ACTIVE -> ColorConstant.GREEN + "Đang bán" + ColorConstant.RESET;
            case OUT_OF_STOCK -> ColorConstant.YELLOW + "Hết hàng" + ColorConstant.RESET;
            case HIDDEN -> ColorConstant.RED + "Đã ẩn" + ColorConstant.RESET;
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
            case PENDING -> ColorConstant.YELLOW + "Chờ duyệt" + ColorConstant.RESET;
            case ACTIVE -> ColorConstant.GREEN + "Đang chơi" + ColorConstant.RESET;
            case COMPLETED -> ColorConstant.CYAN + "Hoàn thành" + ColorConstant.RESET;
            case CANCELLED -> ColorConstant.RED + "Đã hủy" + ColorConstant.RESET;
            case RESERVED -> ColorConstant.PURPLE + "Đã đặt trước" + ColorConstant.RESET;
        };
    }

    public static String formatUserStatus(UserStatus status) {
        if (status == null) return "---";
        return switch (status) {
            case ACTIVE -> ColorConstant.GREEN + "Hoạt động" + ColorConstant.RESET;
            case LOCKED -> ColorConstant.RED + "Bị khóa" + ColorConstant.RESET;
        };
    }
}
