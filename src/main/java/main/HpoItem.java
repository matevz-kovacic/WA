package main;

import java.util.Arrays;
import java.util.List;

public class HpoItem {
    private final String id;
    private final String name;
    private final List<String> succ;


    public HpoItem(final String id, final String name, final String[] succ) {
        this.id     = Utils.nonEmpty(id);
        this.name   = Utils.nonEmpty(name);
        this.succ   = Arrays.asList(succ);
    }

    public final String       id()     { return id;     }
    public final String       name()   { return name;   }
    public final List<String> succ()   { return succ;   }
}
