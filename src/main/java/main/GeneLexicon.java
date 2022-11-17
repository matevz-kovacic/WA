package main;

/*

     Human Gene Id data source:  https://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/

 */

import java.util.*;

import static main.Utils.*;

public class GeneLexicon
{
    private GeneLexicon() {}

    private static Map<String, String>      hugo2entrez        = new HashMap<>();
    private static Map<String, String>      entrez2PrimaryHugo = new HashMap<>();
    private static Map<String, String>      primaryHugo2entrez = new HashMap<>();
    private static Map<String, Set<String>> entrez2Hugo        = new HashMap<>();

    private static Box<String> loadedFrom = new Box<>(null);
    public static String loadedFrom() { return loadedFrom.value; }

    public static String toEntrez(String s) {

        s = s.trim();

        if (primaryHugo2entrez.containsKey(s))
            return primaryHugo2entrez.get(s);

        if (hugo2entrez.containsKey(s))
            return hugo2entrez.get(s);

        if (s.toUpperCase().startsWith("ENTREZ:"))
            s = s.substring(7);

        // remove leading zeros
        if (isInteger(s)) {
            s = ((Integer)Integer.parseInt(s)).toString();
        }

        return entrez2Hugo.containsKey(s) ? s : null;
    }

    public static String toHugo(String s) {
        s = s.trim();

        if (hugo2entrez.containsKey(s))
            return s;

        if (s.toUpperCase().startsWith("ENTREZ:"))
            s = s.substring(7);

        // remove leading zeros
        if (isInteger(s)) {
            s = ((Integer)Integer.parseInt(s)).toString();
        }

        return entrez2PrimaryHugo.get(s);
    }

    public static void load(final String fileName) {
        loadedFrom.value = fileName;

        hugo2entrez.clear();
        entrez2PrimaryHugo.clear();
        primaryHugo2entrez.clear();
        entrez2Hugo.clear();

        fileLineReader(fileName, (lineNo, row) -> {
            if (lineNo == 1)
                return;

            final String[] columns = row.split("\\t");
            if (columns.length < 5)
                return;

            String entrezId = columns[1];
            if (!isInteger(entrezId))
                throw new RuntimeException("Entrez id must be integer: " + entrezId);

            final String primaryHugoId = columns[2];
            // RNR1 may be mitochondrial or ribosomal gene
            //if (primaryHugo2entrez.containsKey(primaryHugoId))
            //    throw new RuntimeException("Duplicate primary Hugo id: " + primaryHugoId);
            primaryHugo2entrez.put(primaryHugoId, entrezId);

            if (entrez2PrimaryHugo.containsKey(entrezId))
                throw new RuntimeException("Duplicate entrezId: " + entrezId);
            entrez2PrimaryHugo.put(entrezId, primaryHugoId);


            final Set<String> hugoSynonyms = new HashSet<>(Arrays.asList(columns[4].split("\\|")));
            if (hugoSynonyms.size() == 1 && "-".equals(hugoSynonyms.iterator().next()))
                hugoSynonyms.clear();
            hugoSynonyms.add(primaryHugoId);

            for (final String hugoId : hugoSynonyms) {
                hugo2entrez.put(hugoId, entrezId);
            }

            entrez2Hugo.put(entrezId, hugoSynonyms);
        });

    }
}
