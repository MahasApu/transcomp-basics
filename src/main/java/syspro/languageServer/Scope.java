package syspro.languageServer;

import syspro.tm.symbols.SemanticSymbol;

import java.util.*;

public class Scope {

    private final Scope parent;
    private final Map<String, SemanticSymbol> symbols;
    private final String name;

    public Scope(Scope parent, String name) {
        this.parent = parent;
        this.symbols = new HashMap<>();
        this.name = name;
    }

    public void declareSymbol(String name, SemanticSymbol symbol) {
        if (symbols.containsKey(name))
            throw new IllegalArgumentException("Symbol '" + name + "' is already declared in this scope.");
        symbols.put(name, symbol);
    }

    public SemanticSymbol lookupSymbol(String name) {
        SemanticSymbol symbol = symbols.get(name);
        if (symbol != null) return symbol;
        if (parent != null) return parent.lookupSymbol(name);
        return null;
    }

    public boolean isDeclaredLocally(String name) {
        return symbols.containsKey(name);
    }

    public Scope getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }
}
