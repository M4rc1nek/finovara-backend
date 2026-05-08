package com.finovara.finovarabackend.pdfexport.report.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static java.awt.Color.DARK_GRAY;

public class PdfReportDocument implements AutoCloseable {
    private static final float MARGIN = 48;
    private static final float BOTTOM_MARGIN = 104;
    private static final float ROW_HEIGHT = 26;
    private static final Color NAVY = new Color(23, 55, 82);
    private static final Color TEAL = new Color(38, 142, 126);
    private static final Color GREEN = new Color(81, 158, 111);
    private static final Color RED = new Color(205, 94, 82);
    private static final Color AMBER = new Color(222, 162, 72);
    private static final Color BLUE = new Color(75, 128, 186);
    private static final Color PURPLE = new Color(126, 104, 176);
    private static final Color BORDER = new Color(203, 213, 224);
    private static final Color MUTED = new Color(92, 105, 118);
    private static final Color[] CHART_COLORS = {TEAL, RED, BLUE, AMBER, PURPLE, GREEN, new Color(93, 154, 201), new Color(190, 122, 89), new Color(99, 132, 109), new Color(164, 104, 143)};
    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String FOOTER_TEXT = "\u00a9 2026 Finovara. Wszelkie prawa zastrze\u017cone. Niniejszy dokument zosta\u0142 wygenerowany automatycznie przez platform\u0119 Finovara jako indywidualny raport u\u017cytkownika oparty na jego danych finansowych. Raport ma charakter informacyjny i analityczny oraz jest przeznaczony wy\u0142\u0105cznie do u\u017cytku w\u0142asnego u\u017cytkownika w ramach korzystania z us\u0142ugi Finovara. Finovara \u2014 bezpieczna i nowoczesna platforma zarz\u0105dzania finansami.";

    private final PDDocument document;
    private final PDType0Font regularFont;
    private final PDType0Font boldFont;
    private final String title;
    private final DecimalFormat decimalFormat;
    private PDPage page;
    private PDPageContentStream contentStream;
    private float y;

    public PdfReportDocument(String title) throws IOException {
        this.document = new PDDocument();
        this.regularFont = loadFont("fonts/LiberationSans-Regular.ttf");
        this.boldFont = loadFont("fonts/LiberationSans-Bold.ttf");
        this.title = title;
        this.decimalFormat = createDecimalFormat();
        addPage();
        drawHeader();
    }

    public void addSection(String heading) throws IOException {
        ensureSpace(50);
        y -= 6;
        text(heading, MARGIN, y, 15, boldFont, NAVY);
        y -= 12;
        horizontalLine(y, BORDER);
        y -= 18;
    }

    public void addInfo(String label, String value) throws IOException {
        ensureSpace(24);
        fillRect(MARGIN, y - 14, contentWidth(), 22, new Color(247, 250, 252));
        strokeRect(MARGIN, y - 14, contentWidth(), 22, BORDER);
        text(label, MARGIN + 12, y, 10, boldFont, MUTED);
        text(value, MARGIN + 130, y, 10, regularFont, new Color(42, 47, 54));
        y -= 30;
    }

    public void addSummary(String label, BigDecimal value) throws IOException {
        ensureSpace(34);
        fillRect(MARGIN, y - 20, contentWidth(), 32, new Color(235, 247, 244));
        strokeRect(MARGIN, y - 20, contentWidth(), 32, new Color(174, 211, 203));
        text(label, MARGIN + 12, y - 2, 11, boldFont, new Color(27, 73, 61));
        text(formatMoney(value), MARGIN + 350, y - 2, 11, boldFont, new Color(27, 73, 61));
        y -= 42;
    }

    public void addTable(String[] headers, List<String[]> rows) throws IOException {
        ensureSpace(ROW_HEIGHT * (Math.min(rows.size(), 8) + 1) + 18);
        float[] widths = calculateColumnWidths(headers.length);
        drawTableRow(headers, widths, true);

        if (rows.isEmpty()) {
            drawTableRow(new String[]{"Brak danych"}, new float[]{contentWidth()}, false);
            return;
        }

        for (String[] row : rows) {
            drawTableRow(row, widths, false);
        }
        y -= 8;
    }

    public void addBarChart(String heading, List<String> labels, List<BigDecimal> values, boolean moneyValues) throws IOException {
        ensureSpace(210);
        float chartX = MARGIN;
        float chartY = y - 190;
        float chartWidth = contentWidth();
        float chartHeight = 170;
        drawPanel(chartX, chartY, chartWidth, chartHeight + 22);
        text(heading, chartX + 14, chartY + chartHeight, 12, boldFont, NAVY);

        if (values.isEmpty() || max(values).compareTo(BigDecimal.ZERO) <= 0) {
            text("Brak danych do wykresu", chartX + 20, chartY + 82, 10, regularFont, MUTED);
            y -= 216;
            return;
        }

        float axisX = chartX + 42;
        float axisY = chartY + 42;
        float plotWidth = chartWidth - 76;
        float plotHeight = 96;
        strokeLine(axisX, axisY, axisX, axisY + plotHeight, BORDER, 0.8f);
        strokeLine(axisX, axisY, axisX + plotWidth, axisY, BORDER, 0.8f);
        BigDecimal max = max(values);
        float gap = 14;
        float barWidth = Math.max(18, (plotWidth - gap * (values.size() + 1)) / values.size());

        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = safeAmount(values.get(i));
            float barHeight = value.divide(max, 4, RoundingMode.HALF_UP).floatValue() * plotHeight;
            float x = axisX + gap + i * (barWidth + gap);
            Color color = CHART_COLORS[i % CHART_COLORS.length];
            fillRect(x, axisY, barWidth, barHeight, color);
            strokeRect(x, axisY, barWidth, barHeight, darken(color));
            text(trimToWidth(labels.get(i), barWidth + 10, regularFont, 8), x - 2, axisY - 14, 8, regularFont, MUTED);
            text(moneyValues ? formatMoney(value) : formatPercent(value), x - 4, axisY + barHeight + 8, 8, regularFont, MUTED);
        }
        y -= 216;
    }

    public void addLineChart(String heading, List<String> labels, List<BigDecimal> values, boolean moneyValues) throws IOException {
        ensureSpace(190);
        float chartX = MARGIN;
        float chartY = y - 170;
        float chartWidth = contentWidth();
        float chartHeight = 150;
        drawPanel(chartX, chartY, chartWidth, chartHeight + 22);
        text(heading, chartX + 14, chartY + chartHeight, 12, boldFont, NAVY);

        if (values.isEmpty() || max(values).compareTo(BigDecimal.ZERO) <= 0) {
            text("Brak danych do wykresu", chartX + 20, chartY + 72, 10, regularFont, MUTED);
            y -= 196;
            return;
        }

        float axisX = chartX + 46;
        float axisY = chartY + 40;
        float plotWidth = chartWidth - 92;
        float plotHeight = 84;
        strokeLine(axisX, axisY, axisX, axisY + plotHeight, BORDER, 0.8f);
        strokeLine(axisX, axisY, axisX + plotWidth, axisY, BORDER, 0.8f);

        BigDecimal max = max(values);
        List<Float> xs = new ArrayList<>();
        List<Float> ys = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            float x = values.size() == 1 ? axisX + plotWidth / 2 : axisX + (plotWidth / (values.size() - 1)) * i;
            BigDecimal value = safeAmount(values.get(i));
            float pointY = axisY + value.divide(max, 4, RoundingMode.HALF_UP).floatValue() * plotHeight;
            xs.add(x);
            ys.add(pointY);
        }

        for (int i = 0; i < xs.size() - 1; i++) {
            strokeLine(xs.get(i), ys.get(i), xs.get(i + 1), ys.get(i + 1), TEAL, 2f);
        }

        for (int i = 0; i < xs.size(); i++) {
            fillCircle(xs.get(i), ys.get(i), 4, TEAL);
            text(trimToWidth(labels.get(i), 72, regularFont, 8), xs.get(i) - 18, axisY - 14, 8, regularFont, MUTED);
            text(moneyValues ? formatMoney(values.get(i)) : formatPercent(values.get(i)), xs.get(i) - 20, ys.get(i) + 12, 8, regularFont, MUTED);
        }
        y -= 196;
    }

    public void addPieChart(String heading, List<String> labels, List<BigDecimal> values) throws IOException {
        ensureSpace(198);

        float chartX = MARGIN;
        float chartY = y - 174;
        float chartWidth = contentWidth();
        float chartHeight = 154;

        drawPanel(chartX, chartY, chartWidth, chartHeight + 22);
        text(heading, chartX + 14, chartY + chartHeight + 8, 12, boldFont, NAVY);

        BigDecimal total = values.stream().map(value -> value == null ? BigDecimal.ZERO : value).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (values.isEmpty() || total.compareTo(BigDecimal.ZERO) <= 0) {
            text("Brak danych do wykresu", chartX + 20, chartY + 74, 10, regularFont, MUTED);
            y -= 198;
            return;
        }

        float centerX = chartX + 110;
        float centerY = chartY + 78;
        float radius = 52;
        double startAngle = 0;

        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            if (value == null) {
                value = BigDecimal.ZERO;
            }

            double angle = value.max(BigDecimal.ZERO).divide(total, 6, RoundingMode.HALF_UP).doubleValue() * 360d;

            fillPieSlice(centerX, centerY, radius, startAngle, startAngle + angle, CHART_COLORS[i % CHART_COLORS.length]);
            startAngle += angle;
        }

        strokeCircle(centerX, centerY, radius, NAVY, 0.7f);
        float legendX = chartX + 200;
        float legendStartY = chartY + 160;

        for (int i = 0; i < labels.size(); i++) {
            float itemY = legendStartY - (i * 13);
            fillRect(legendX, itemY - 7, 8, 8, CHART_COLORS[i % CHART_COLORS.length]);
            text(trimToWidth(labels.get(i), 165, regularFont, 8), legendX + 14, itemY - 5, 8, regularFont, MUTED);
            text(formatPercent(values.get(i)), legendX + 190, itemY - 5, 8, regularFont, DARK_GRAY);
        }
        y -= 198;
    }

    public String formatMoney(BigDecimal amount) {
        return decimalFormat.format(safeAmount(amount)) + " PLN";
    }

    public String formatPercent(BigDecimal percentage) {
        return decimalFormat.format(safeAmount(percentage)) + "%";
    }

    public byte[] toByteArray() throws IOException {
        closeContentStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        return outputStream.toByteArray();
    }

    @Override
    public void close() throws IOException {
        closeContentStream();
        document.close();
    }

    private void addPage() throws IOException {
        closeContentStream();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        y = page.getMediaBox().getHeight() - MARGIN;
    }

    private void drawHeader() throws IOException {
        PDImageXObject logo = PDImageXObject.createFromByteArray(document, readResource("Logo.png"), "Finovara logo");
        contentStream.drawImage(logo, 40, y - 52, 84, 84);
        text("Finovara", MARGIN + 54, y - 15, 19, boldFont, NAVY);
        text("Bezpieczne Finanse", MARGIN + 55, y - 32, 9, regularFont, MUTED);

        horizontalLine(y - 54, BORDER);
        text("Raport PDF", MARGIN, y - 82, 11, boldFont, TEAL);
        text(title, MARGIN, y - 110, 22, boldFont, NAVY);
        text("Wygenerowano: " + LocalDateTime.now().format(CREATED_AT_FORMAT), MARGIN, y - 130, 10, regularFont, MUTED);
        y -= 158;
    }

    private void drawTableRow(String[] cells, float[] widths, boolean header) throws IOException {
        ensureSpace(ROW_HEIGHT + 4);
        Color background = header ? NAVY : new Color(249, 251, 252);
        Color fontColor = header ? Color.WHITE : new Color(39, 45, 52);

        fillRect(MARGIN, y - 17, contentWidth(), ROW_HEIGHT, background);
        strokeRect(MARGIN, y - 17, contentWidth(), ROW_HEIGHT, BORDER);
        float x = MARGIN + 8;
        for (int i = 0; i < cells.length && i < widths.length; i++) {
            text(trimToWidth(cells[i], widths[i] - 14, header ? boldFont : regularFont, 10), x, y - 2, 10, header ? boldFont : regularFont, fontColor);
            if (i > 0) {
                strokeLine(x - 8, y - 17, x - 8, y - 17 + ROW_HEIGHT, BORDER, 0.5f);
            }
            x += widths[i];
        }
        y -= ROW_HEIGHT;
    }

    private void text(String value, float x, float y, float size, PDType0Font font, Color color) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(color);
        contentStream.setFont(font, size);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(value == null ? "" : value);
        contentStream.endText();
    }

    private void fillRect(float x, float y, float width, float height, Color color) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.addRect(x, y, width, height);
        contentStream.fill();
    }

    private void strokeRect(float x, float y, float width, float height, Color color) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.setLineWidth(0.6f);
        contentStream.addRect(x, y, width, height);
        contentStream.stroke();
    }

    private void strokeLine(float x1, float y1, float x2, float y2, Color color, float lineWidth) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.setLineWidth(lineWidth);
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }

    private void drawPanel(float x, float y, float width, float height) throws IOException {
        fillRect(x, y, width, height, Color.WHITE);
        strokeRect(x, y, width, height, BORDER);
    }

    private void horizontalLine(float y, Color color) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.setLineWidth(0.7f);
        contentStream.moveTo(MARGIN, y);
        contentStream.lineTo(page.getMediaBox().getWidth() - MARGIN, y);
        contentStream.stroke();
    }

    private void fillCircle(float centerX, float centerY, float radius, Color color) throws IOException {
        fillPieSlice(centerX, centerY, radius, 0, 360, color);
    }

    private void strokeCircle(float centerX, float centerY, float radius, Color color, float lineWidth) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.setLineWidth(lineWidth);
        int steps = 72;
        contentStream.moveTo(centerX + radius, centerY);
        for (int i = 1; i <= steps; i++) {
            double angle = Math.toRadians((360d / steps) * i);
            contentStream.lineTo((float) (centerX + Math.cos(angle) * radius), (float) (centerY + Math.sin(angle) * radius));
        }
        contentStream.stroke();
    }

    private void fillPieSlice(float centerX, float centerY, float radius, double startAngle, double endAngle, Color color) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.moveTo(centerX, centerY);
        int steps = Math.max(6, (int) Math.ceil((endAngle - startAngle) / 8));
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * i / steps);
            contentStream.lineTo((float) (centerX + Math.cos(angle) * radius), (float) (centerY + Math.sin(angle) * radius));
        }
        contentStream.closePath();
        contentStream.fill();
    }

    private void ensureSpace(float requiredHeight) throws IOException {
        if (y - requiredHeight < BOTTOM_MARGIN) {
            addPage();
        }
    }

    private float[] calculateColumnWidths(int columns) {
        float[] widths = new float[columns];
        float width = contentWidth() / columns;
        for (int i = 0; i < columns; i++) {
            widths[i] = width;
        }
        return widths;
    }

    private String trimToWidth(String value, float maxWidth, PDType0Font font, float size) throws IOException {
        if (value == null) {
            return "";
        }
        if (font.getStringWidth(value) / 1000 * size <= maxWidth) {
            return value;
        }

        String suffix = "...";
        String result = value;
        while (!result.isEmpty() && font.getStringWidth(result + suffix) / 1000 * size > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
    }

    private float contentWidth() {
        return page.getMediaBox().getWidth() - 2 * MARGIN;
    }

    private PDType0Font loadFont(String path) throws IOException {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return PDType0Font.load(document, inputStream);
        }
    }

    private byte[] readResource(String path) throws IOException {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private void closeContentStream() throws IOException {
        if (contentStream != null) {
            drawFooter();
            contentStream.close();
            contentStream = null;
        }
    }

    private void drawFooter() throws IOException {
        horizontalLine(78, BORDER);
        List<String> lines = wrapText(FOOTER_TEXT, contentWidth(), regularFont, 7);
        float footerY = 64;
        for (String line : lines.stream().limit(5).toList()) {
            text(line, MARGIN, footerY, 7, regularFont, MUTED);
            footerY -= 9;
        }
    }

    private List<String> wrapText(String value, float maxWidth, PDType0Font font, float size) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * size <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private DecimalFormat createDecimalFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pl-PL"));
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(List<BigDecimal> values) {
        return values.stream().map(value -> value == null ? BigDecimal.ZERO : value).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    private Color darken(Color color) {
        return new Color(Math.max(0, color.getRed() - 35), Math.max(0, color.getGreen() - 35), Math.max(0, color.getBlue() - 35));
    }
}
