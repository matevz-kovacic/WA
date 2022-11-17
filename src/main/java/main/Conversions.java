package main;

import com.opencsv.CSVReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static main.Utils.*;

public class Conversions
{
    private Conversions() {}

    public static void Cada2TrainingData(final String cadaFileName, final String waFileName) {
        fileWriter(waFileName, f -> cada2Wa(cadaFileName, f));
    }

    private static void cada2Wa(final String cadaFileName, final BufferedWriter out) {

        fileLineReader(cadaFileName, (lineNo, row) -> {
            final String[] columns = row.split("\\t");

            if (columns.length < 4)
                throw new RuntimeException("Illegal number of columns");

            final String geneId = GeneLexicon.toEntrez(columns[2]);
            if (geneId == null)
                throw new RuntimeException("Illegal or unknown gene Id (check the content of gene lexicon file: " + GeneLexicon.loadedFrom() + ")");

            out.write(columns[0] + "\t" + "Entrez:" + geneId);

            final Set<String> phenotypes = new HashSet<>();
            for (final String phenotype : columns[3].split(",")) {
                if (phenotype.length() != 10 || !phenotype.startsWith("HP:") || !isInteger(phenotype.substring(3)))
                    throw new IllegalArgumentException("Illegal phenotype form: " + phenotype);

                // check for duplicate phenotypes
                if (!phenotypes.contains(phenotype))
                    out.write("\t" + phenotype);
                phenotypes.add(phenotype);
            }

            out.newLine();
        });
    }

    public static void clinVar2WA(final String clinVarFileName, final String waFileName) {
        fileWriter(waFileName, f -> clinVar2WA(clinVarFileName, f));
    }

    private static void clinVar2WA(final String cadaFileName, final BufferedWriter out) {
        Set<String> factor = new HashSet<>();

        fileLineReader(cadaFileName, (lineNo, row) -> {
            if (lineNo == 1)
                return;

            final String[] columns = row.split("\\t");


            if (columns.length < 13)
                throw new RuntimeException("Illegal number of columns");

            final String clinicalSignificance = columns[6].trim().toLowerCase(Locale.ROOT);

            if (clinicalSignificance.contains("benign") ||
                clinicalSignificance.contains("protective") ||
                clinicalSignificance.contains("uncertain") ||
                clinicalSignificance.contains("other") ||
                    clinicalSignificance.contains("association not found") ||
                clinicalSignificance.contains("conflicting") ||
                clinicalSignificance.contains("not provided"))
                return;

            final String rawGeneId = columns[3];
            if (rawGeneId.equals("-1"))
                return;

            final String geneId = GeneLexicon.toEntrez(rawGeneId);
            if (geneId == null) {
                System.out.println("Illegal or unknown gene Id " + rawGeneId + " (check the content of gene lexicon file: " + GeneLexicon.loadedFrom() + ")");
                return;
            }
            //throw new RuntimeException("Illegal or unknown gene Id " + rawGeneId + " (check the content of gene lexicon file: " + GeneLexicon.loadedFrom() + ")");

            final Set<String> phenotypes = new HashSet<>();
            int i = 0;
            for (final String phenotypeCodes : columns[12].split("\\|")) {
                for (final String rawPhenotype : phenotypeCodes.split(",")) {
                    int hpStart = rawPhenotype.indexOf("HP:");
                    if (hpStart == -1)
                        continue;

                    final String phenotype = rawPhenotype.substring(hpStart, Math.min(rawPhenotype.length(), hpStart + 10));
                    phenotypes.add(phenotype);
                }
            }

            if (phenotypes.isEmpty())
                return;

            out.write("Case:" + lineNo + "\t" + "Entrez:" + geneId);

            for (final String phenotype : phenotypes) {
                    out.write("\t" + phenotype);
            }

            out.newLine();
            factor.add(columns[7] + " " + columns[6]);
        });

        for (final String s: factor) {
            System.out.println("Factor" + s);
        }

        System.out.println("Done");
    }


    public static void UKC2WA(final String patientDataFileName, final String patientPhenotypeDir, final String outputFileName) {
        fileWriter(outputFileName, f ->
            fileLineReader(patientDataFileName, (lineNo, row) -> {
                if (lineNo == 1)
                    return;

                final String[] columns = row.split("\\t");
                if (columns.length < 3)
                    throw new RuntimeException("Illegal number of columns");

                final String patientId = columns[0];
                final String patientFileName = patientPhenotypeDir + File.separator + patientId + ".tsv";
                if (!fileExists(patientFileName)) {
                    System.out.println("Missing patient file: " + patientFileName);
                    return;
                }

                f.write(patientId + "\t" + columns[1]);
                loadPhenotypes(patientFileName, f);
                f.newLine();
            })
        );
    }

    private static void loadPhenotypes(final String fileName, final BufferedWriter f) {
        fileLineReader(fileName, (lineNo, row) -> {
            if (lineNo > 1)
                return;

            final String[] columns = row.split("\\t");
            if (columns.length < 3)
                throw new RuntimeException("Illegal number of columns");

            for (int i = 3; i < columns.length - 1; ++i) {
                String phenotype = columns[i].trim();
                final int firstIdx = phenotype.indexOf(":");
                final int lastIdx = phenotype.lastIndexOf(":");
                if (lastIdx == firstIdx || lastIdx < 0)
                    throw new RuntimeException("Illegal phenotyepeId: " + phenotype);
                f.write("\t" + phenotype.substring(0, lastIdx));
            }
        });
    }


    public static void subtractDataset(final String minuendFileName, final String subtrahendFileName, final String resultFileName) {
        final Set<Set<String>> subtrahend = new HashSet<>();

        // load subtrahend instances
        fileLineReader(subtrahendFileName, (lineNo, row) -> {
            final String[] columns = row.split("\\t");
            if (columns.length < 3)
                throw new RuntimeException("Illegal number of columns");

            final Set<String> instance = new HashSet<>();
            instance.add(columns[1].trim());

            for (int i = 2; i < columns.length; ++i) {
                final String phenotype = columns[i].trim();
                instance.add(phenotype);
            }

            subtrahend.add(instance);
        });

        // read minuend file and filter out subtrahend instances

        fileWriter(resultFileName, f ->
            fileLineReader(minuendFileName, (lineNo, row) -> {
                final String[] columns = row.split("\\t");
                if (columns.length < 3)
                    throw new RuntimeException("Illegal number of columns");

                final String patientId = columns[0];
                final String geneId     = columns[1];

                final Set<String> instance = new HashSet<>();
                instance.add(geneId);

                for (int i = 2; i < columns.length; ++i) {
                    final String phenotype = columns[i].trim();
                    instance.add(phenotype);
                }

                if (subtrahend.contains(instance))
                    return;

                f.write(patientId + "\t" + geneId);
                for (int i = 2; i < columns.length; ++i) {
                    final String phenotype = columns[i].trim();
                    f.write("\t" + phenotype);
                }

                f.newLine();
            })
        );
    }

    public static void convertHpo(final String hpoFileName, final String resultFileName) {

        fileWriter(resultFileName, f -> {
            long lineNo = 1;
            try {
                final CSVReader reader = new CSVReader(new FileReader(hpoFileName));

                String[] columns;

                while ((columns = reader.readNext()) != null) {
                    if (columns.length < 3)
                        throw new RuntimeException("Illegal number of columns");

                    f.write(csvCell(columns[0]) + "," + csvCell(columns[1]));

                    for (int i = columns[2].startsWith("HP:") ? 2 : 3; i < columns.length; ++i) {
                        f.write("," + csvCell(columns[i]));
                    }
                    f.newLine();

                    ++lineNo;
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e.getMessage() + " line " + lineNo + " file: " + hpoFileName);
            }
        });
    }

    private static String csvCell(final String s) {
        return s.contains(",") ? "\"" + s + "\"" : s;
    }
/*
    private static boolean isNumeric(String strNum) {
        if (strNum == null)
            return false;

        try {
            Double.parseDouble(strNum);
            return true;
        }
        catch (NumberFormatException e) {}

        return false;
    }
 */
}
