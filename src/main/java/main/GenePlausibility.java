package main;

import java.util.HashMap;
import java.util.Map;

import static main.Utils.*;

public class GenePlausibility
{
    private String gene;
    private double plausibility;
    private Map<String, Double> byPhenotypePlausibility;

    public GenePlausibility(final String gene) {
        this.gene = nonEmpty(gene, "gene code must not be empty");
        plausibility = 0;
        byPhenotypePlausibility = new HashMap<>();
    }

    public void set(final String phenotypeId, final double phenotypeByGenePlausibility) {
        plausibility -= byPhenotypePlausibility.getOrDefault(phenotypeId, .0);
        byPhenotypePlausibility.put(phenotypeId, phenotypeByGenePlausibility);
        plausibility += phenotypeByGenePlausibility;
    }

    public String gene() { return gene; }

    public double plausibility() { return plausibility; }
    public double plausibilityByPhenotype(final String phenotypeId) { return byPhenotypePlausibility.getOrDefault(phenotypeId, .0); }
}
