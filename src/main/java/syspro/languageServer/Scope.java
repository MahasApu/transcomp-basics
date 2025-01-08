package syspro.languageServer;

import syspro.languageServer.symbols.VariableSymbol;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;

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
            if (symbols.get(name).equals(symbol)) throw new IllegalArgumentException("Symbol '" + name + "' is already declared in this scope.");
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

    public List<VariableSymbol> getAllLocals() {
        return symbols.values().stream()
                .filter(symbol -> symbol instanceof VariableSymbol)
                .map(symbol -> (VariableSymbol) symbol)
                .filter(symbol -> symbol.kind().equals(SymbolKind.LOCAL))
                .toList();
    }


}
