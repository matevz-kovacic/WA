package main;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static main.GeneLexicon.*;
import static main.Utils.*;

public class ExcelGenePrioritizationReport
{
    public ExcelGenePrioritizationReport() {}

    public void ExcelReport(final List<GenePlausibility> genePlausibilities,
                            final Set<String> phenotypes,
                            final Integer maxEntries,
                            final String geneIdType,
                            final String fileName) {
        try (final Workbook workbook = new XSSFWorkbook();
            final FileOutputStream out = new FileOutputStream(fileName)) {

            final Sheet sheet  = createSheet(workbook, 2, 1);
            final int   topRow = header(workbook, sheet, 0, 0, phenotypes);

            final CellStyle valueCellStyle = valueCellStyle(workbook, "0.0");
            int row = topRow;

            for (int i = 0; i < genePlausibilities.size(); ++i) {
                if (maxEntries != null && i >= maxEntries)
                    break;

                set(sheet, row, 0, i + 1);
                displayPlausibilities(phenotypes, genePlausibilities.get(i), geneIdType, sheet, row++, 1, valueCellStyle);
            }

            averages(sheet, topRow, row++, 1, 1 + phenotypes.size(), valueCellStyle);

            final CellRangeAddress table = new CellRangeAddress(topRow, row - 1, 1, 1 + phenotypes.size());
            heatmap(sheet, table);

            autosizeColumns(sheet, 0, phenotypes.size() + 2);

            workbook.write(out);
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void averages(final Sheet sheet,
                                 final int firstRow, final int lastRow,
                                 final int firstCol, final int lastCol,
                                 final CellStyle valueCellStyle) {

        for (int i = firstCol; i < lastCol; ++i) {

            final CellReference startCell = new CellReference(firstRow, i);
            final CellReference endCell   = new CellReference(lastRow - 1, i);

            final Cell cell = get(sheet, lastRow, i);
            cell.setCellFormula("AVERAGE(" + startCell.formatAsString() + ":" + endCell.formatAsString() + ")");
            cell.setCellStyle(valueCellStyle);
        }

        set(sheet, lastRow, lastCol, "average");
    }

    private static void heatmap(final Sheet sheet, final CellRangeAddress table) {
        final SheetConditionalFormatting conditionalFormatting = sheet.getSheetConditionalFormatting();
        final ConditionalFormattingRule rule = conditionalFormatting.createConditionalFormattingColorScaleRule();
        final ColorScaleFormatting colorScaleFormat = rule.getColorScaleFormatting();

        colorScaleFormat.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
        colorScaleFormat.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.NUMBER);
        colorScaleFormat.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
        ((ExtendedColor)colorScaleFormat.getColors()[0]).setARGBHex("FFFFFF");
        ((ExtendedColor)colorScaleFormat.getColors()[1]).setARGBHex("FFAB2A");

        conditionalFormatting.addConditionalFormatting(new CellRangeAddress[]{ table }, rule);
    }

    private static void displayPlausibilities(final Set<String> phenotypes,
                                              final GenePlausibility genePlausibility,
                                              final String geneIdType,
                                              final Sheet sheet,
                                              final int row, int col,
                                              final CellStyle valueCellStyle) {
       for (final String phenotype : phenotypes) {
           set(sheet, row, col, genePlausibility.plausibilityByPhenotype(phenotype));
           get(sheet, row, col++).setCellStyle(valueCellStyle);
       }

       set(sheet, row, col, "H".equals(geneIdType) ? toHugo(genePlausibility.gene()) : toEntrez(genePlausibility.gene()));
    }

    private static int header(final Workbook workbook, final Sheet sheet, int row, int col, final Set<String> phenotypes) {
        final Hpo hpo = new Hpo("./data/hpo.csv");

        final CellStyle rotatedTextStyle = rotatedTextStyle(workbook);

        set(sheet, row + 1, col++, "#");

        for (final String phenotype : phenotypes) {
            set(sheet, row, col, hpo.get(phenotype).name());
            get(sheet, row, col).setCellStyle(rotatedTextStyle);
            set(sheet, row + 1, col++, phenotype);
        }

        set(sheet, row + 1, col++, "gene");
        set(sheet, row + 1, col, "all values in dB");
        return row + 2;
    }

    private static void autosizeColumns(final Sheet sheet, final int firstCol, final int lastCol) {
        for (int i = firstCol; i < lastCol; ++i)
            sheet.autoSizeColumn(i);
    }

    private static CellStyle valueCellStyle(final Workbook workbook, final String formatStr) {
        final CellStyle style = workbook.createCellStyle();
        final DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat(formatStr));

        return style;
    }

    private static CellStyle rotatedTextStyle(final Workbook workbook) {
        final CellStyle style = workbook.createCellStyle();
        style.setRotation((short)90);
        return style;
    }

    private static Sheet createSheet(final Workbook workbook, final int freezeRow, final int freezeCol) {
        Sheet sheet = workbook.createSheet("WA " + new SimpleDateFormat("dd MMM yyyy @ HH mm ss").format(new Date()));
        sheet.createFreezePane(freezeCol, freezeRow);
        return sheet;
    }

    private static void set(final Sheet sheet, final int row, final int col, final Object... values) {

        int i = col - 1;
        for (final Object value : values) {
            ++i;

            if (value == null)
                continue;

            Cell c = get(sheet, row, i);

            if (value instanceof Byte)       c.setCellValue((Byte)value);      else
            if (value instanceof Integer)    c.setCellValue((Integer)value);   else
            if (value instanceof Long)       c.setCellValue((Long)value);      else
            if (value instanceof Float)      c.setCellValue((Float)value);     else
            if (value instanceof Double)     c.setCellValue((Double)value);    else
            if (value instanceof BigDecimal) c.setCellValue(((BigDecimal) value).doubleValue()); else
            if (value instanceof Date)       c.setCellValue((Date)value);      else
            if (value instanceof LocalDate)  c.setCellValue((LocalDate)value); else
                c.setCellValue(value.toString());
        }
    }

    /*
        private static <T> void set(final Sheet sheet, final int row, final int col, final T... values) {

        int i = col - 1;
        for (final T value : values) {
            ++i;

            if (value == null)
                continue;

            Cell c = get(sheet, row, i);

            if (value instanceof Byte)       c.setCellValue((Byte)value);      else
            if (value instanceof Integer)    c.setCellValue((Integer)value);   else
            if (value instanceof Long)       c.setCellValue((Long)value);      else
            if (value instanceof Float)      c.setCellValue((Float)value);     else
            if (value instanceof Double)     c.setCellValue((Double)value);    else
            if (value instanceof BigDecimal) c.setCellValue(((BigDecimal) value).doubleValue()); else
            if (value instanceof Date)       c.setCellValue((Date)value);      else
            if (value instanceof LocalDate)  c.setCellValue((LocalDate)value); else
                c.setCellValue(value.toString());
        }
    }

     */
    private static Cell get(final Sheet sheet, final int row, final int col) {
        Row r = sheet.getRow(row);
        if (r == null)
            r = sheet.createRow(row);

        Cell c = r.getCell(col);
        if (c == null)
            c = r.createCell(col);

        return c;
    }

    public void tsvReport(final List<GenePlausibility> genePlausibilities,
                          final Set<String> phenotypes,
                          final Integer maxEntries,
                          final String geneIdType,
                          final String fileName) {
        final Hpo hpo = new Hpo("./data/hpo.csv");

        fileWriter(fileName, f -> {
            int i = 0;

            // header
            for (final String phenotype : phenotypes) {
                f.write("!\t" + phenotype + " " + hpo.get(phenotype).name());
                f.newLine();
            }

            f.write("#");
            for (final String phenotype: phenotypes) {
                f.write("\t" + phenotype);
            }
            f.write("\tgene\tall values in dB");
            f.newLine();

            for (final GenePlausibility genePlausibility : genePlausibilities) {
                if (maxEntries != null && i >= maxEntries)
                    break;

                f.write(String.valueOf(++i));
                for (final String phenotype: phenotypes) {
                    f.write("\t" + String.format(Locale.US, "%,.1f", genePlausibility.plausibilityByPhenotype(phenotype)));
                }
                f.write("\t" + ("H".equals(geneIdType) ? toHugo(genePlausibility.gene()) : toEntrez(genePlausibility.gene())));

                f.newLine();
            }
        });
    }
}
