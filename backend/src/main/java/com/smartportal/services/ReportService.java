package com.smartportal.services;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartportal.models.AttendanceRecord;
import com.smartportal.repositories.AttendanceRecordRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class ReportService {

    @Autowired
    private AttendanceRecordRepository recordRepository;

    public List<AttendanceRecord> getAttendanceData(Long studentId, Long staffId, LocalDateTime start, LocalDateTime end) {
        if (studentId != null) {
            return recordRepository.findByStudentIdAndMarkedAtBetween(studentId, start, end);
        } else if (staffId != null) {
            return recordRepository.findBySessionStaffIdAndMarkedAtBetween(staffId, start, end);
        } else {
            return recordRepository.findByMarkedAtBetween(start, end);
        }
    }

    public byte[] generateCsvReport(Long studentId, Long staffId, LocalDateTime start, LocalDateTime end) {
        List<AttendanceRecord> list = getAttendanceData(studentId, staffId, start, end);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Student Name,Roll Number,Subject,Lecturer,Marked At,Latitude,Longitude,Status,Verified\n");
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AttendanceRecord r : list) {
            sb.append(r.getId()).append(",")
              .append("\"").append(r.getStudent().getName().replace("\"", "\"\"")).append("\",")
              .append(r.getStudent().getRollNumber()).append(",")
              .append("\"").append(r.getSession().getSubject().name.replace("\"", "\"\"")).append("\",")
              .append("\"").append(r.getSession().getStaff().getName().replace("\"", "\"\"")).append("\",")
              .append(r.getMarkedAt().format(dtf)).append(",")
              .append(r.getLatitude()).append(",")
              .append(r.getLongitude()).append(",")
              .append(r.getStatus()).append(",")
              .append(r.getIsVerified()).append("\n");
        }
        return sb.toString().getBytes();
    }

    public byte[] generateExcelReport(Long studentId, Long staffId, LocalDateTime start, LocalDateTime end) throws IOException {
        List<AttendanceRecord> list = getAttendanceData(studentId, staffId, start, end);
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Logs");
            
            // Header Styles
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Student Name", "Roll Number", "Subject", "Lecturer", "Marked At", "Latitude", "Longitude", "Status", "Verified"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowIdx = 1;
            for (AttendanceRecord r : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getStudent().getName());
                row.createCell(2).setCellValue(r.getStudent().getRollNumber());
                row.createCell(3).setCellValue(r.getSession().getSubject().name);
                row.createCell(4).setCellValue(r.getSession().getStaff().getName());
                row.createCell(5).setCellValue(r.getMarkedAt().format(dtf));
                row.createCell(6).setCellValue(r.getLatitude());
                row.createCell(7).setCellValue(r.getLongitude());
                row.createCell(8).setCellValue(r.getStatus());
                row.createCell(9).setCellValue(r.getIsVerified());
            }
            
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generatePdfReport(Long studentId, Long staffId, LocalDateTime start, LocalDateTime end) {
        List<AttendanceRecord> list = getAttendanceData(studentId, staffId, start, end);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, com.lowagie.text.Font.BOLD);
            Paragraph title = new Paragraph("Smart Student Portal - Attendance Logs Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            com.lowagie.text.Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph info = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) 
                    + "\nRange: " + start.toLocalDate() + " to " + end.toLocalDate(), infoFont);
            info.setSpacingAfter(15);
            document.add(info);
            
            // 8 Columns table
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 2f, 2.5f, 2.5f, 3f, 1.5f, 1.5f, 1.5f});
            
            com.lowagie.text.Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, com.lowagie.text.Font.BOLD);
            String[] headers = {"Student", "Roll Number", "Subject", "Lecturer", "Marked At", "Status", "GPS", "Verified"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(h, headFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }
            
            com.lowagie.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (AttendanceRecord r : list) {
                table.addCell(new PdfPCell(new Paragraph(r.getStudent().getName(), cellFont)));
                table.addCell(new PdfPCell(new Paragraph(r.getStudent().getRollNumber(), cellFont)));
                table.addCell(new PdfPCell(new Paragraph(r.getSession().getSubject().name, cellFont)));
                table.addCell(new PdfPCell(new Paragraph(r.getSession().getStaff().getName(), cellFont)));
                table.addCell(new PdfPCell(new Paragraph(r.getMarkedAt().format(dtf), cellFont)));
                
                PdfPCell statusCell = new PdfPCell(new Paragraph(r.getStatus(), cellFont));
                if ("PRESENT".equals(r.getStatus())) {
                    statusCell.setBackgroundColor(new java.awt.Color(200, 255, 200));
                } else {
                    statusCell.setBackgroundColor(new java.awt.Color(255, 200, 200));
                }
                table.addCell(statusCell);
                
                String coords = String.format("%.3f,%.3f", r.getLatitude(), r.getLongitude());
                table.addCell(new PdfPCell(new Paragraph(coords, cellFont)));
                table.addCell(new PdfPCell(new Paragraph(String.valueOf(r.getIsVerified()), cellFont)));
            }
            
            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        
        return out.toByteArray();
    }
}
