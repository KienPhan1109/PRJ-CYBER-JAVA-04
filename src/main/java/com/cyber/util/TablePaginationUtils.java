package com.cyber.util;

import java.util.List;
import java.util.function.Function;

public class TablePaginationUtils {

    /**
     * Hiển thị bảng có phân trang và xử lý điều hướng người dùng (N/P/Q).
     *
     * @param data         Danh sách dữ liệu đối tượng T
     * @param title        Tiêu đề của bảng
     * @param headers      Mảng các tiêu đề cột
     * @param columnWidths Mảng độ rộng của các cột (tương ứng với headers)
     * @param rowMapper    Hàm ánh xạ từ đối tượng T sang mảng String để hiển thị dòng
     * @param <T>          Kiểu dữ liệu của đối tượng trong danh sách
     * @return true nếu người dùng thoát bằng phím "Q", false nếu danh sách trống hoặc thoát khác (tùy mở rộng)
     */
    public static <T> void display(List<T> data, String title, String[] headers, int[] columnWidths, Function<T, String[]> rowMapper) {
        display(data, title, headers, columnWidths, rowMapper, "Quay lại/Thoát");
    }

    public static <T> void display(List<T> data, String title, String[] headers, int[] columnWidths, Function<T, String[]> rowMapper, String quitLabel) {
        if (data == null || data.isEmpty()) {
            PrintUtils.printWarning("Không có dữ liệu để hiển thị.");
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) data.size() / pageSize);
        int currentPage = 1;

        int totalWidth = 0;
        for (int w : columnWidths) {
            totalWidth += w + 3;
        }
        totalWidth -= 1;

        while (currentPage <= totalPages) {
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, data.size());

            System.out.println("\n" + "=".repeat(totalWidth));
            System.out.printf("  %s (Trang %d/%d)\n", title, currentPage, totalPages);
            System.out.println("=".repeat(totalWidth));

            StringBuilder headerFormat = new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                headerFormat.append("%-").append(columnWidths[i]).append("s");
                if (i < headers.length - 1) headerFormat.append(" | ");
            }
            System.out.printf(headerFormat + "%n", (Object[]) headers);
            System.out.println("-".repeat(totalWidth));

            for (int i = start; i < end; i++) {
                String[] rowData = rowMapper.apply(data.get(i));
                System.out.printf(headerFormat + "%n", (Object[]) rowData);
            }

            System.out.println("=".repeat(totalWidth));
            System.out.printf("Tổng: %d bản ghi | Trang %d/%d\n", data.size(), currentPage, totalPages);

            if (totalPages == 1) break;

            System.out.printf("[N] Trang sau | [P] Trang trước | [Q] %s\n", quitLabel);
            String nav = InputUtils.inputString("Lựa chọn: ").toUpperCase();

            if (nav.equals("N") && currentPage < totalPages) {
                currentPage++;
            } else if (nav.equals("P") && currentPage > 1) {
                currentPage--;
            } else if (nav.equals("Q")) {
                break;
            }
        }
    }
}
