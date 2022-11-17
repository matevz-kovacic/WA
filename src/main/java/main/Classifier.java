package main;

import java.util.*;
import java.util.stream.Collectors;

import static main.Utils.*;
import static main.GeneLexicon.*;

public class Classifier
{
    private static final Hpo hpo = new Hpo("./data/hpo.csv");

    private final Map<String, Integer> freq  = new HashMap<>();
    private final Set<String>          genes = new HashSet<>();

    public Classifier(final List<String> fileNames) {
        load(fileNames);
    }

    public void test(final String testFileName, final String resultFileName, final Integer maxGenes, final boolean showIntermediateResultsInfo) {
        if (getCanonicalPath(testFileName).equals(getCanonicalPath(resultFileName)))
            throw new RuntimeException("input [" + testFileName + "] and output file [" + resultFileName +
                    "] for testing must not be the same");

        fileWriter(resultFileName, f-> {
            f.write("top\t%");
            f.newLine();

            final Map<Integer, Integer> freq = test(testFileName, showIntermediateResultsInfo);
            if (freq.isEmpty())
                return;

            final int max = freq.keySet().stream().max(Integer::compareTo).get();

            for (int i = 1; i <= max; ++i) {
                if (maxGenes != null && maxGenes < i)
                    return;

                f.write(String.format(Locale.US, "%d\t%.2f", i, top(i, freq)));
                f.newLine();
            }
        });
    }

    public Map<Integer, Integer> test(final String testFileName, final boolean showIntermediateResultsInfo) {
        final Map<Integer, Integer> freq = new HashMap<>();

        fileLineReader(testFileName, (lineNo, row) -> {

            final String[] columns = row.split("\\t");
            if (columns.length < 3)
                throw new RuntimeException("Illegal number of columns: " + row);

            final String correctGene = GeneLexicon.toEntrez(columns[1]);
            if (correctGene == null) {
                System.out.println("Illegal or unknown gene Id " + columns[1] + " (check the content of gene lexicon file: " + GeneLexicon.loadedFrom() + ")");
                freq.compute(0, (k, v) -> v == null ? 1 : v + 1);
                return;
            }

            final Set<String> phenotypes = new HashSet<>(Arrays.asList(Arrays.copyOfRange(columns, 2, columns.length)));

            final List<String> genes = prioritizeGenes(phenotypes);
            for (int i = 0; i < genes.size(); ++i) {
                if (correctGene.equals(GeneLexicon.toEntrez(genes.get(i)))) {
                    freq.compute(i + 1, (k, v) -> v == null ? 1 : v + 1);
                    top(freq, lineNo, showIntermediateResultsInfo);

                    return;
                }
            }

            // gene not present - missed gene frequency is stored at key == 0
            freq.compute(0, (k, v) -> v == null ? 1 : v + 1);
            top(freq, lineNo, showIntermediateResultsInfo);
        });

        return freq;
    }

    public static void top(final Map<Integer, Integer> freq, final int patientNo, final boolean showIntermediateResultsInfo) {
        if (!showIntermediateResultsInfo)
            return;

        System.out.format(Locale.US, "top   1: %6.2f %%\n", top(1, freq));
        System.out.format(Locale.US, "top   3: %6.2f %%\n", top(3, freq));
        System.out.format(Locale.US, "top   5: %6.2f %%\n", top(5, freq));
        System.out.format(Locale.US, "top  10: %6.2f %%\n", top(10, freq));
        System.out.format(Locale.US, "top  50: %6.2f %%\n", top(50, freq));
        System.out.format(Locale.US, "top 100: %6.2f %%\n", top(100, freq));

        int i = 0;
        while (++i < freq.size()) {
            double p = top(i, freq);
            if (p >= 50.) {
                System.out.format(Locale.US, "top " + i + " prioritized genes include pathogenic gene with %6.2f %% probability\n", p);
                break;
            }
        }

        System.out.println("=============================  patient " + patientNo + "\n");
    }

    public static double top(final int n, final Map<Integer, Integer> freq) {
        int topFreq = 0;
        for (int i = 1; i <= n; ++i)
            topFreq += freq.getOrDefault(i, 0);

        final int sumFreq = freq.values().stream().reduce(0, Integer::sum);

        return 100. * topFreq / sumFreq;
    }

    public void prioritize(final String testFileName,
                           final String resultFileName,
                           final Integer maxGenes,
                           final String geneIdType) {
        if (getCanonicalPath(testFileName).equals(getCanonicalPath(resultFileName)))
            throw new RuntimeException("input [" + testFileName + "] and output file [" + resultFileName +
                    "] for prioritization must not be the same");

        fileWriter(resultFileName, f ->
            fileLineReader(testFileName, (lineNo, row) -> {
                final String[] columns = row.split("\\t");
                if (columns.length == 0) // empty lines are ignored
                    return;

                if (columns.length < 2)
                    throw new RuntimeException("Illegal number of columns: " + row);

                f.write(columns[0] + "\t");

                final Set<String> phenotypes = new HashSet<>(Arrays.asList(Arrays.copyOfRange(columns, 1, columns.length)));

                int i = 0;
                for (final String gene : prioritizeGenes(phenotypes)) {
                    ++i;
                    if (maxGenes != null && i > maxGenes)
                        break;
                    f.write("\t" + ("H".equals(geneIdType) ? toHugo(gene) : toEntrez(gene)));
                }
                f.newLine();
            })
        );
    }

    public List<String> prioritizeGenes(final Set<String> phenotypes) {
            return prioritize(phenotypes)
                       .stream()
                       .map(GenePlausibility::gene)
                       .collect(Collectors.toCollection(ArrayList::new));
    }


    public List<GenePlausibility> prioritize(final Set<String> phenotypes) {
        final List<GenePlausibility> result = new ArrayList<>();
        
        for (final String geneId : genes) {
            final GenePlausibility genePlausibility = new GenePlausibility(geneId);

            // evidence(phenotype | Gene)
            for (final String phenotype : phenotypes) {
                genePlausibility.set(phenotype, evidence(phenotype, geneId));
            }

            result.add(genePlausibility);
        }

        result.sort((p1, p2) -> comparePlausibility(p1, p2));
        return result;
    }

    private int comparePlausibility(final GenePlausibility gp1, final GenePlausibility gp2) {
        final double p1 = gp1.plausibility();
        final double p2 = gp2.plausibility();

        if (p1 < p2) return 1;
        if (p2 < p1) return -1;

        final double pg1 = freq.getOrDefault(gp1.gene(), 0);
        final double pg2 = freq.getOrDefault(gp2.gene(), 0);

        if (pg1 < pg2) return 1;
        if (pg2 < pg1) return -1;

        return 0;
    }

    private double evidence(final String phenotypeId, final String geneId) {
        final double CLIP_EVIDENCE = 30.;

        final double probPhenotypeGene = p(phenotypeId, geneId) * p(geneId);
        if (probPhenotypeGene == .0) { // no evidence for either gene of phenotype | gene => max penalization for gene
            return -CLIP_EVIDENCE;
        }

        double probPhenotypeOtherGenes = .0;
        for (final String otherGeneId : genes) {
            if (geneId.equals(otherGeneId))
                continue;

            probPhenotypeOtherGenes += p(phenotypeId, otherGeneId) * p(otherGeneId);
        }

        if (probPhenotypeOtherGenes == .0) { // // no evidence for phenotype | other genes => max reward for gene
            return CLIP_EVIDENCE;
        }

        return 10. * Math.log10(probPhenotypeGene / probPhenotypeOtherGenes);
    }

    private double p(final String geneId) {
        final double geneFreq     = freq.getOrDefault(geneId, 0);
        final double allGenesFreq = freq.get("");
        return geneFreq / allGenesFreq;
    }

    private double p(final String phenotypeId, final String geneId) {
        final double phenotypeCondGeneFreq  = freq.getOrDefault(phenotypeId + "|" + geneId, 0);
        final double geneFreq               = freq.getOrDefault(geneId, 0);

        return geneFreq == .0 ? .0 : phenotypeCondGeneFreq / geneFreq;
    }

    private void load(final List<String> fileNameList) {
        // filter out duplicate file names
        final Set<String> fileNames = fileNameList.stream()
                                                  .map(Utils::getCanonicalPath)
                                                  .collect(Collectors.toSet());

        for (final String fileName : fileNames) {
            loadFile(fileName);
        }
    }

    private void loadFile(final String fileName) {
        final Box<Boolean> hasContent = new Box<>(false);

        fileLineReader(fileName, (lineNo, row) -> {
            final String[] columns = row.split("\\t");
            if (columns.length < 3)
                throw new RuntimeException("line must have at least 3 tab delimited columns: " +
                        "PatientId, GeneId (Hugo or entrez code), and phenotype (HP code)");

            final String geneId = GeneLexicon.toEntrez(columns[1]);
            if (geneId == null) {
                System.out.println("Skipping Unknown gene Id " + columns[1] +
                        " (check the content of gene lexicon file: " + GeneLexicon.loadedFrom() + ")");
                return;
            }

            // empty string denotes frequency of all cases
            freq.compute("", (k, v) -> v == null ? 1 : v + 1);

            genes.add(geneId);
            // freq(Gene)
            freq.compute(geneId, (k, v) -> v == null ? 1 : v + 1);

            final Set<String> phenotypes = new HashSet<>();

            for (int i = 2; i < columns.length; ++i) {
                final String phenotype = columns[i];
                if (!phenotype.startsWith("HP:") || !isInteger(phenotype.substring(3)))
                    throw new IllegalArgumentException("Illegal phenotype form: " + phenotype);

                phenotypes.addAll(hpo.ancestorSet(phenotype));
            }

            // freq(phenotype | Gene)
            for (final String phenotype : phenotypes) {
                freq.compute(phenotype + "|" + geneId, (k, v) -> v == null ? 1 : v + 1);
            }

            hasContent.value = true;
        });

        if (!hasContent.value)
            throw new RuntimeException("no training cases available in file " + fileName);
    }
}
