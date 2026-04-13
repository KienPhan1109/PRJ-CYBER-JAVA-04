package com.cyber.util;

import java.math.BigDecimal;
import java.util.Scanner;
import java.util.regex.Pattern;

public class InputUtils {
    private static final Scanner SCANNER = new Scanner(System.in);

    // --- Private Helpers ---
    
    private static String inputRaw(String message) {
        System.out.print(message);
        return SCANNER.nextLine();
    }

    private static String inputRequired(String message) {
        while (true) {
            String input = inputRaw(message);
            if (input == null || input.trim().isEmpty()) {
                PrintUtils.printError("Dữ liệu không được để trống. Vui lòng nhập lại.");
                continue;
            }
            return input.trim();
        }
    }

    // --- Public Methods ---

    public static String inputString(String message) {
        return inputRequired(message);
    }

    public static String inputString(String message, String regex, String errorMsg) {
        Pattern pattern = Pattern.compile(regex);
        while (true) {
            String input = inputRequired(message);
            if (pattern.matcher(input).matches()) {
                return input;
            }
            PrintUtils.printError(errorMsg);
        }
    }

    public static int inputInt(String message) {
        while (true) {
            String input = inputRequired(message);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập một số nguyên hợp lệ.");
            }
        }
    }

    public static int inputInt(String message, int min, int max) {
        while (true) {
            int value = inputInt(message);
            if (value >= min && value <= max) return value;
            PrintUtils.printError("Giá trị phải nằm trong khoảng từ " + min + " đến " + max + ".");
        }
    }

    public static BigDecimal inputBigDecimal(String message, BigDecimal min) {
        while (true) {
            String input = inputRequired(message);
            try {
                BigDecimal value = new BigDecimal(input);
                if (value.compareTo(min) >= 0) return value;
                PrintUtils.printError("Mức giá trị không được nhỏ hơn " + min + ".");
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập định dạng số thập phân hợp lệ.");
            }
        }
    }

    public static String inputStringOptional(String message) {
        String input = inputRaw(message);
        return (input == null) ? "" : input.trim();
    }

    public static String inputPassword(String message) {
        return inputRequired(message);
    }

    public static String inputRegisterPassword(String message) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[\\W_]).{8,}$";
        while (true) {
            String password = inputRaw(message);
            if (password == null || password.trim().isEmpty()) {
                PrintUtils.printError("Mật khẩu không được để trống.");
            } else if (!password.matches(regex)) {
                PrintUtils.printError("Mật khẩu yếu! Phải chứa ít nhất 8 ký tự, 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
            } else {
                return password;
            }
        }
    }

    public static String inputStringUpdate(String message, String oldValue) {
        String input = inputRaw(message);
        return (input == null || input.trim().isEmpty()) ? oldValue : input.trim();
    }

    public static String inputStringUpdate(String message, String oldValue, String regex, String errorMsg) {
        Pattern pattern = Pattern.compile(regex);
        while (true) {
            String input = inputRaw(message);
            if (input == null || input.trim().isEmpty()) return oldValue;
            String trimmed = input.trim();
            if (pattern.matcher(trimmed).matches()) return trimmed;
            PrintUtils.printError(errorMsg);
        }
    }

    public static int inputIntUpdate(String message, int oldValue, int min, int max) {
        while (true) {
            String input = inputRaw(message);
            if (input == null || input.trim().isEmpty()) return oldValue;
            try {
                int value = Integer.parseInt(input.trim());
                if (value >= min && value <= max) return value;
                PrintUtils.printError("Giá trị phải nằm trong khoảng từ " + min + " đến " + max + ".");
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập một số nguyên hợp lệ.");
            }
        }
    }

    public static BigDecimal inputBigDecimalUpdate(String message, BigDecimal oldValue, BigDecimal min) {
        while (true) {
            String input = inputRaw(message);
            if (input == null || input.trim().isEmpty()) return oldValue;
            try {
                BigDecimal value = new BigDecimal(input.trim());
                if (value.compareTo(min) >= 0) return value;
                PrintUtils.printError("Mức giá trị không được nhỏ hơn " + min + ".");
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập định dạng số thập phân hợp lệ.");
            }
        }
    }
}
