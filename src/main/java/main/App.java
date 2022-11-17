package main;

import java.util.*;
/*

parameters for testing on CADA data
java -jar WA.jar -m 100 -t ./data/cases/cada-test.tsv -o ./results/cada.tsv -L ./data/cases/cada-train.tsv -L ./data/cases/cada-validate.tsv

parameters for testing on CADA data with ClinVar with cada test cases removed :
java -jar WA.jar -m 100 -t ./data/cases/cada-test.tsv -o ./results/cada-with-clinvar.tsv -L ./data/clinvar/clinvar-minus-cada-test-instances.tsv -L ./data/cases/cada-train.tsv -L ./data/cases/cada-validate.tsv

Note: due to legal prohibition of publishing confidential medical records UMCL dataset is not included in the WA repository
parameters for testing on UMCL dataset with CADA data:
java -jar WA.jar -m 100 -t ./data/UKC/UKC-normalized-data.tsv -o ./results/UKC-patients.tsv -L ./data/cases/cada-train.tsv -L ./data/cases/cada-validate.tsv -L ./data/cases/cada-test.tsv

parameters for testing on UMCL dataset with ClinVar and CADA data:
java -jar WA.jar -m 100 -t ./data/UKC/UKC-normalized-data.tsv -o ./results/ukc-with-clivnar.tsv -L ./data/cases/clinvar.tsv -L ./data/cases/cada-train.tsv -L ./data/cases/cada-validate.tsv -L ./data/cases/cada-test.tsv

parameters for generating heatmap in the WA paper:
java -jar WA.jar -m 10 HP:0002376 HP:0002353 HP:0002240 HP:0010780 HP:0010729  HP:0001250 -L ./data/cases/cada-train.tsv -L ./data/cases/cada-validate.tsv

ClinVar data source: https://ftp.ncbi.nlm.nih.gov/pub/clinvar/tab_delimited/variant_summary.txt.gz
CADA data source: https://github.com/Chengyao-Peng/CADA/tree/main/data/processed/cases
 */

public class App
{
    public static void main(String[] args) {

        final Params params = new Params(args);

        if (params.help()) {
            params.displayHelp();
            return;
        }

        // let's get this baby off the ground
        GeneLexicon.load("./data/Homo_sapiens.gene_info");

        final Classifier classifier = new Classifier(params.learningFiles());

        if (!params.phenotypes().isEmpty()) {
            final Set<String> phenotypes = new HashSet<>(params.phenotypes());
            final List<GenePlausibility> genePlausibilities = classifier.prioritize(phenotypes);

            final ExcelGenePrioritizationReport report = new ExcelGenePrioritizationReport();

            final String outputFile = params.outputFile() == null ? "./WA.xlsx" : params.outputFile();
            if (outputFile.toLowerCase().endsWith(".tsv")) {
                report.tsvReport(genePlausibilities, phenotypes, params.max(), params.geneIdType(), outputFile);
            }
            else {
                report.ExcelReport(genePlausibilities, phenotypes, params.max(), params.geneIdType(), outputFile);
            }

            System.out.println("Gene prioritization results saved to file: " + outputFile);
            return;
        }

        if (params.prioritizationFile() != null) {
            classifier.prioritize(params.prioritizationFile(), params.outputFile(), params.max(), params.geneIdType());
            return;
        }

        if (params.testFile() != null) {
            classifier.test(params.testFile(), params.outputFile(), params.max(), params.showIntermediateResultsInfo());
        }
    }
}
