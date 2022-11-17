package main;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.util.*;

public class Hpo {
    private static final String topNodeId = "HP:0000001";

    private final Map<String, HpoItem> items;
    private final Map<String, Set<String>> parents;

    public Hpo(final String hpoFileName) {
        // execution order matters here
        items   = loadHpo(hpoFileName);
        parents = phenotypeParents();
        checkConsistency();
    }

    public boolean isValidId(final String id) { return items.containsKey(id); }

    public HpoItem get() { return get(topNodeId); }

    public HpoItem get(final String id) {
        final HpoItem result = items.get(id);
        if (result == null)
            throw new IllegalArgumentException("Hpo id " + id + "  does not exist");

        return result;
    }


    public Set<String> ancestorSet(final String phenotypeId) {
        final Set<String> result = new HashSet<>();
        ancestorSet_(phenotypeId, result);
        return result;
    }

    private void ancestorSet_(final String phenotypeId, final Set<String> result) {
        final Set<String> phenotypeParents = parents.get(phenotypeId);
        if (phenotypeParents == null) {
            result.add(topNodeId);
            return;
        }

        result.add(phenotypeId);
        for (final String parent : phenotypeParents)
            ancestorSet_(parent, result);
    }


    private void checkConsistency() {
        Set<String> usedIds = new HashSet<>();

        for (final HpoItem item : items.values())
            for (final String id : item.succ()) {
                get(id);
                usedIds.add(id);
            }

        usedIds.add(topNodeId);
        for (final String itemId : items.keySet())
            if (!usedIds.contains(itemId))
                throw new RuntimeException("Hpo id " + itemId + " is not used in Hpo tree");
    }

    private Map<String, Set<String>> phenotypeParents() {
        final Map<String, Set<String>> result = new HashMap<>();
        phenotypeParents_(topNodeId, result);

        return result;
    }

    private void phenotypeParents_(final String phenotypeId, Map<String, Set<String>> parents) {
        for (final String succ : get(phenotypeId).succ()) {
            parents.compute(succ, (k, v) -> {
                if (v == null) v = new HashSet<>();
                v.add(phenotypeId);
                return v;
            });

            phenotypeParents_(succ, parents);
        }
    }

    private Map<String, HpoItem> loadHpo(final String fileName) {
        final Map<String, HpoItem> items =  new HashMap<>();

        long lineNo = 1;
        try {
            final CSVReader reader = new CSVReader(new FileReader(fileName));

            String[] line;

            while ((line = reader.readNext()) != null) {
                final HpoItem item = new HpoItem(line[0], line[1],Arrays.copyOfRange(line, 2, line.length));
                final String id = item.id();

                if (items.containsKey(id)) {
                    throw new RuntimeException("Duplicate HPO item id " + item.id());
                }

                items.put(id, item);
                ++lineNo;
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage() + " line " + lineNo + " file: " + fileName);
        }

        return items;
    }
}
