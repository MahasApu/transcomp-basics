package syspro.languageServer;

import syspro.languageServer.symbols.VariableSymbol;
import syspro.tm.symbols.MemberSymbol;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;

import java.util.*;

public class Scope {

    private final Scope parent;
    private final LinkedHashMap<String, SemanticSymbol> symbols;
    private final String name;

    public Scope(Scope parent, String name) {
        this.parent = parent;
        this.symbols = new LinkedHashMap<>();
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

    public List<VariableSymbol> getAllParameters() {
        return symbols.values().stream()
                .filter(symbol -> symbol instanceof VariableSymbol)
                .map(symbol -> (VariableSymbol) symbol)
                .filter(symbol -> symbol.kind().equals(SymbolKind.PARAMETER))
                .toList();
    }


    public List<MemberSymbol> getAllMembers() {
        return symbols.values().stream()
                .filter(symbol -> symbol instanceof MemberSymbol)
                .map(symbol -> (MemberSymbol) symbol)
                .toList();
    }

    private List<SemanticSymbol> getAll(SymbolKind kind) {
        return symbols.values().stream()
                .filter(s -> s.kind().equals(kind))
                .toList();
    }


}
