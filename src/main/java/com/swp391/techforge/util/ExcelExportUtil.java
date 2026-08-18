package com.swp391.techforge.util;

import com.swp391.techforge.entity.Order;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExportUtil {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static ByteArrayOutputStream exportOrdersToExcel(List<Order> orders) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Orders");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        // Header Row
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(20);

        String[] headers = {"Mã đơn", "Ngày đặt", "Khách hàng", "SĐT", "Địa chỉ", "Trạng thái", "Tổng tiền"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowNum = 1;
        for (Order order : orders) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue("#" + order.getOrderId());
            
            Cell dateCell = row.createCell(1);
            dateCell.setCellValue(order.getOrderDate().format(dateFormatter));
            dateCell.setCellStyle(dateStyle);

            row.createCell(2).setCellValue(order.getRecipientName() != null ? order.getRecipientName() : "");
            row.createCell(3).setCellValue(order.getPhone() != null ? order.getPhone() : "");
            row.createCell(4).setCellValue(order.getShippingAddress() != null ? order.getShippingAddress() : "");
            row.createCell(5).setCellValue(order.getStatus().toString());

            Cell amountCell = row.createCell(6);
            amountCell.setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
            amountCell.setCellStyle(currencyStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Output to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return style;
    }

    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm:ss"));
        return style;
    }
}
