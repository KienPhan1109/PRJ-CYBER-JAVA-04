package com.cyber.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class InputUtils {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static String inputString(String message) {
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                System.out.println("Lỗi: Dữ liệu không được để trống. Vui lòng nhập lại.");
            } else {
                return input.trim();
            }
        }
    }

    public static String inputString(String message, String regex, String errorMsg) {
        Pattern pattern = Pattern.compile(regex);
        while (true) {
            String input = inputString(message);
            if (pattern.matcher(input).matches()) {
                return input;
            } else {
                System.out.println("Lỗi: " + errorMsg);
            }
        }
    }

    public static int inputInt(String message) {
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                System.out.println("Lỗi: Dữ liệu không được để trống.");
                continue;
            }
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: Vui lòng nhập một số nguyên hợp lệ.");
            }
        }
    }

    public static int inputInt(String message, int min, int max) {
        while (true) {
            int value = inputInt(message);
            if (value >= min && value <= max) {
                return value;
            } else {
                System.out.println("Lỗi: Giá trị phải nằm trong khoảng từ " + min + " đến " + max + ".");
            }
        }
    }

    public static BigDecimal inputBigDecimal(String message, BigDecimal min) {
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                System.out.println("Lỗi: Dữ liệu không được để trống.");
                continue;
            }
            try {
                BigDecimal value = new BigDecimal(input.trim());
                if (value.compareTo(min) >= 0) {
                    return value;
                } else {
                    System.out.println("Lỗi: Mức giá trị không được nhỏ hơn " + min.toString() + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: Vui lòng nhập định dạng số thập phân hợp lệ.");
            }
        }
    }

    public static LocalDate inputLocalDate(String message, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        while (true) {
            System.out.print(message + " (" + pattern + "): ");
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                System.out.println("Lỗi: Dữ liệu không được để trống.");
                continue;
            }
            try {
                return LocalDate.parse(input.trim(), formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Lỗi: Ngày không hợp lệ hoặc không đúng định dạng " + pattern + ".");
            }
        }
    }

    public static String inputPassword(String message) {
        while (true) {
            String password;
            if (System.console() != null) {
                char[] passwordChars = System.console().readPassword(message);
                password = (passwordChars == null) ? "" : new String(passwordChars);
            } else {
                System.out.print(message);
                password = SCANNER.nextLine();
            }

            if (password == null || password.trim().isEmpty()) {
                System.out.println("Lỗi: Mật khẩu không được để trống.");
            } else if (password.length() < 6) {
                System.out.println("Lỗi: Mật khẩu phải có tối thiểu 6 ký tự.");
            } else {
                return password;
            }
        }
    }
}
