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
                PrintUtils.printError("Dữ liệu không được để trống. Vui lòng nhập lại.");
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
                PrintUtils.printError(errorMsg);
            }
        }
    }

    public static int inputInt(String message) {
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                PrintUtils.printError("Dữ liệu không được để trống.");
                continue;
            }
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập một số nguyên hợp lệ.");
            }
        }
    }

    public static int inputInt(String message, int min, int max) {
        while (true) {
            int value = inputInt(message);
            if (value >= min && value <= max) {
                return value;
            } else {
                PrintUtils.printError("Giá trị phải nằm trong khoảng từ " + min + " đến " + max + ".");
            }
        }
    }

    public static BigDecimal inputBigDecimal(String message, BigDecimal min) {
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                PrintUtils.printError("Dữ liệu không được để trống.");
                continue;
            }
            try {
                BigDecimal value = new BigDecimal(input.trim());
                if (value.compareTo(min) >= 0) {
                    return value;
                } else {
                    PrintUtils.printError("Mức giá trị không được nhỏ hơn " + min.toString() + ".");
                }
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập định dạng số thập phân hợp lệ.");
            }
        }
    }

    public static LocalDate inputLocalDate(String message, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        while (true) {
            System.out.print(message + " (" + pattern + "): ");
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                PrintUtils.printError("Dữ liệu không được để trống.");
                continue;
            }
            try {
                return LocalDate.parse(input.trim(), formatter);
            } catch (DateTimeParseException e) {
                PrintUtils.printError("Ngày không hợp lệ hoặc không đúng định dạng " + pattern + ".");
            }
        }
    }

    public static String inputPassword(String message) {
        // Hàm này gọi khi login, không cần validate format
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                PrintUtils.printError("Mật khẩu không được để trống.");
            } else {
                return input;
            }
        }
    }

    public static String inputRegisterPassword(String message) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[\\W_]).{8,}$";
        while (true) {
            System.out.print(message);
            String password = SCANNER.nextLine();
            
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
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                return oldValue;
            } else {
                return input.trim();
            }
        }
    }

    public static String inputStringUpdate(String message, String oldValue, String regex, String errorMsg) {
        Pattern pattern = Pattern.compile(regex);
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                return oldValue;
            }
            if (pattern.matcher(input.trim()).matches()) {
                return input.trim();
            } else {
                PrintUtils.printError(errorMsg);
            }
        }
    }

    public static int inputIntUpdate(String message, int oldValue, int min, int max) {
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                return oldValue;
            }
            try {
                int value = Integer.parseInt(input.trim());
                if (value >= min && value <= max) {
                    return value;
                } else {
                    PrintUtils.printError("Giá trị phải nằm trong khoảng từ " + min + " đến " + max + ".");
                }
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập một số nguyên hợp lệ.");
            }
        }
    }

    public static BigDecimal inputBigDecimalUpdate(String message, BigDecimal oldValue, BigDecimal min) {
        while (true) {
            System.out.print(message);
            String input = SCANNER.nextLine();
            if (input == null || input.trim().isEmpty()) {
                return oldValue;
            }
            try {
                BigDecimal value = new BigDecimal(input.trim());
                if (value.compareTo(min) >= 0) {
                    return value;
                } else {
                    PrintUtils.printError("Mức giá trị không được nhỏ hơn " + min.toString() + ".");
                }
            } catch (NumberFormatException e) {
                PrintUtils.printError("Vui lòng nhập định dạng số thập phân hợp lệ.");
            }
        }
    }

    public static String inputPasswordUpdate(String message, String oldPassword) {
        while (true) {
            System.out.print(message);
            String password = SCANNER.nextLine();
            if (password == null || password.trim().isEmpty()) {
                return oldPassword;
            } else if (password.length() < 6) {
                PrintUtils.printError("Mật khẩu phải có tối thiểu 6 ký tự.");
            } else {
                return password;
            }
        }
    }
}
