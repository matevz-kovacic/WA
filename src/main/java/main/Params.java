package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Params {
    private List<String> learningFiles;
    private final List<String> phenotypes;
    private String outputFile;
    private String prioritizationFile;
    private String testFile;
    private String geneIdType;
    private Integer max;
    private boolean showIntermediateResultsInfo;
    private boolean help;

    public Params(String[] args) {
        learningFiles =  new ArrayList<>();
        phenotypes = new ArrayList<>();
        outputFile = null;
        testFile = null;
        geneIdType = "H";
        max = null;
        showIntermediateResultsInfo = false;
        help = false;

        load(args);
    }

    public List<String> phenotypes()                  { return  phenotypes;                 }
    public List<String> learningFiles()               { return  learningFiles;              }
    public String       outputFile()                  { return outputFile;                  }
    public String       prioritizationFile()          { return prioritizationFile;          }
    public String       testFile()                    { return testFile;                    }
    public Integer      max()                         { return max;                         }
    public String       geneIdType()                  { return geneIdType;                  }
    public boolean      showIntermediateResultsInfo() { return showIntermediateResultsInfo; }
    public boolean      help()                        { return help;                        }

    private void load(String[] args) {
        if (args.length == 0) {
            help = true;
            return;
        }

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-h" :
                case "-H" :
                case "-help" : help = true;
                               return;

                case "-f" : geneIdType = getArg(arg(args, ++i), "missing classify file for -c option");
                            if (!geneIdType.equals("H") && !geneIdType.equals("E"))
                                throw new RuntimeException("-f option error: geneId type must be H or E");
                            break;

                case "-p" : prioritizationFile = getArg(arg(args, ++i), "missing input file for gene prioritization (-c option)");
                            break;

                case "-L" : addLearningFile(arg(args, ++i));
                            break;

                case "-i" : showIntermediateResultsInfo = true;
                            break;

                case "-m" : max = addInt(arg(args, ++i), "Illegal int value for max number of results");
                            if (max < 1)
                                throw new RuntimeException("-m (max number of results) value must be greater than 0");
                            break;

                case "-o" : outputFile = getArg(arg(args, ++i), "missing output file for -o option");
                            break;

                case "-t" : testFile = getArg(arg(args, ++i), "missing test file for -t option");
                            break;

                default:  addPhenotype(arg(args, i));
            }
        }

        if (prioritizationFile != null && testFile != null)
            throw new RuntimeException("You can choose only one of -c and -t options");

        if (prioritizationFile != null || testFile != null) {
            if (outputFile == null)
                throw new RuntimeException("Missing output file (-o parameter)");

            if (!phenotypes.isEmpty())
                throw new RuntimeException("Ambiguous parameters: specify either phenotypes or t(est)/c(lassify) files");
        }

        defaultValues();
    }


    private static String getArg(final String fileName, final String errorMessage) {
        if (fileName == null)
            throw new RuntimeException(errorMessage);
        return fileName;
    }

    private static int addInt(final String arg, final String errorMessage) {
        try {
            return Integer.parseInt(arg);
        }
        catch (Exception e) {
            throw new RuntimeException(errorMessage);
        }
    }


    private void addPhenotype(final String phenotype) {
        phenotypes.add(phenotype);
    }


    private void addLearningFile(final String fileName) {
        if (fileName == null)
            throw new RuntimeException("missing learning file for -L option");

        try {
            learningFiles.add(Paths.get(fileName).toRealPath().toString());
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Illegal learning file name: " + fileName);
        }

    }

    private static String arg(String[] args, int i) {
        return i < args.length ? args[i] : null;
    }

    private  void defaultValues() {
        if (learningFiles.isEmpty()) {
            try (final Stream<Path> fileStream = Files.list(Paths.get("./data/cases"))) {
                learningFiles.addAll(fileStream
                                       .filter(file -> !Files.isDirectory(file))
                                       .map(Path::toAbsolutePath)
                                       .map(Path::toString)
                                       .collect(Collectors.toList()));
            }
            catch (Exception e) {
                throw new RuntimeException("Error accessing ./data/cases directory: " + e.getMessage());
            }

            if (learningFiles.isEmpty())
                throw new RuntimeException("No test cases files in ./data/cases directory. Populate test cases directory or use -L option to add test cases");
        }
    }

    public void displayHelp() {
        System.out.println("USAGE: wa [OPTIONS] [phenotype list]\n");
        System.out.println("EXAMPLE [prioritization of a single patient genes from command line, prioritization results for top 10 genes in Excel format saved to file ./WA.xlsx]:\njava -jar WA.jar -m 10 HP:0002376 HP:0002353 HP:0002240 HP:0010780 HP:0010729  HP:0001250");
        System.out.println("\nEXAMPLE [prioritization of a single patient genes from command line, prioritization results for all genes in tsv format saved to file ./results.tsv]:\njava -jar WA.jar -o ./results.tsv HP:0002376 HP:0002353 HP:0002240 HP:0010780 HP:0010729  HP:0001250");

        System.out.println("\nEXAMPLE [prioritization of patients' genes from file]:\njava -jar WA.jar -p ./patients.tsv -o ./results.tsv");
        System.out.println("Prioritization input file format");
        System.out.println("\t- every patient on separate line");
        System.out.println("\t- patient data columns are tab separated in the following order");
        System.out.println("\t\t- patient id");
        System.out.println("\t\t- patient phenotypes");

        System.out.println("Output file format");
        System.out.println("\t- every patient on separate line");
        System.out.println("\t- patient data columns are tab separated in the following order");
        System.out.println("\t\t- patient id");
        System.out.println("\t\t- prioritized genes id (Entrez or Hugo id (see option -f))");

        System.out.println("\nEXAMPLE [testing wa performance on already diagnosed patients from file]:\njava -jar WA.jar -t ./diagnosed_patients.tsv -o ./results.txt");
        System.out.println("Test file format");
        System.out.println("\t- every patient on separate line");
        System.out.println("\t- patient data columns are tab separated in the following order");
        System.out.println("\t\t- patient id");
        System.out.println("\t\t- diagnosed gene id (Entrez or Hugo id; auto-detected)");
        System.out.println("\t\t- patient's phenotypes Human Phenotype Ontology codes");

        System.out.println("\nOPTIONS");
        System.out.println("\t-f gene id output format: possible values H (Hugo id/default) or E (Entrez Id)");
        System.out.println("\t-h help");
        System.out.println("\t-m <number> show only top <number> genes in classification or top <number> entries in test results frequency distribution");
        System.out.println("\t-i show intermediate results info in testing");
        System.out.println("\t-L learning file location(can be used multiple times; learning files are collated). Learning file format:");
        System.out.println("\t\t- every patient on separate line");
        System.out.println("\t\t- patient data columns are tab separated in the following order");
        System.out.println("\t\t\t- patient id");
        System.out.println("\t\t\t- diagnosed gene id (Entrez or Hugo id; auto-detected)");
        System.out.println("\t\t\t- patient's phenotypes Human Phenotype Ontology codes");
        System.out.println("\t if no learning files are specified WA uses default learning files in ./data/cases directory");

        System.out.println("\nADDITIONAL DEFAULT LEARNING INSTANCES");
        System.out.println("\t - additional learning instances can be added to the prioritization");
        System.out.println("\t\t - by using -L command line option for all WA learning files (IMPORTANT: using -L disables default WA learning files)");
        System.out.println("\t\t - by copying additional learning files in ./data/cases directory and NOT using -L option");
        System.out.println("\t - Learning cases file format:");
        System.out.println("\t\t- every patient on separate line");
        System.out.println("\t\t- patient data columns are tab separated in the following order");
        System.out.println("\t\t\t- patient id");
        System.out.println("\t\t\t- diagnosed gene id (Entrez or Hugo id; auto-detected)");
        System.out.println("\t\t\t- patient's phenotypes Human Phenotype Ontology codes");
    }
}
