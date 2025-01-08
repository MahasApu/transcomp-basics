package syspro.languageServer;

import syspro.languageServer.symbols.FunctionSymbol;
import syspro.languageServer.symbols.TypeSymbol;
import syspro.languageServer.symbols.VariableSymbol;
import syspro.tm.symbols.MemberSymbol;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;

import java.util.*;

import static java.util.Objects.isNull;

public class Scope {

    private final Scope parent;
    private final List<SemanticSymbol> orderedDefinitions;
    private final LinkedHashMap<String, SemanticSymbol> symbols;
    private final LinkedHashMap<String, List<SemanticSymbol>> functionSymbols;
    private final String name;
    private SemanticSymbol scopeSymbol;

    public Scope(Scope parent, String name, SemanticSymbol scopeSymbol) {
        this.parent = parent;
        this.scopeSymbol = scopeSymbol;
        this.symbols = new LinkedHashMap<>();
        this.functionSymbols = new LinkedHashMap<>();
        this.orderedDefinitions = new ArrayList<>();
        this.name = name;
    }

    public void declareSymbol(String name, SemanticSymbol symbol) {
        if (symbol instanceof FunctionSymbol functionSymbol) {
            List<SemanticSymbol> overloads = functionSymbols.getOrDefault(name, new ArrayList<>());
            for (SemanticSymbol overload : overloads) {
                if (hasClashingSignature((FunctionSymbol) overload, functionSymbol.parameters))
                    throw new IllegalArgumentException("Symbol '" + name + "' is already declared in this scope.");
            }
            overloads.add(functionSymbol);
            functionSymbols.put(name, overloads);
        } else {
            if (symbol instanceof TypeSymbol && this.name.equals("GlobalScope")) symbols.put(name, symbol);
            else if (symbols.containsKey(name))
                throw new IllegalArgumentException("Symbol '" + name + "' is already declared in this scope.");
        }
        orderedDefinitions.add(symbol);
    }


    private boolean hasClashingSignature(FunctionSymbol existingFunc, List<VariableSymbol> actualParams) {
        List<VariableSymbol> existingParams = existingFunc.parameters;
        if (existingParams.size() != actualParams.size()) return false;
        for (int i = 0; i < actualParams.size(); i++)
            if (!Objects.equals(actualParams.get(i).type().name(), existingParams.get(i).type().name())) return false;
        return true;
    }

    public SemanticSymbol lookupSymbol(String name) {
        SemanticSymbol symbol = symbols.get(name);
        if (symbol != null)
            return symbol;
        if (parent != null)
            return parent.lookupSymbol(name);
        return null;
    }

    public SemanticSymbol lookupFunction(String name, List<VariableSymbol> params) {
        List<SemanticSymbol> list = functionSymbols.get(name);
        if (isNull(list)) return null;
        for (SemanticSymbol s : list) {
            FunctionSymbol func = (FunctionSymbol) s;
            if (name.equals(func.name()) && hasClashingSignature(func, params)) return func;
        }
        if (parent != null) return parent.lookupFunction(name, params);
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

    public SemanticSymbol getSymbol() {
        return scopeSymbol;
    }

    public List<VariableSymbol> getAllLocals() {
        return orderedDefinitions.stream()
                .filter(symbol -> symbol instanceof VariableSymbol)
                .map(symbol -> (VariableSymbol) symbol)
                .filter(symbol -> symbol.kind().equals(SymbolKind.LOCAL))
                .toList();
    }

    public List<VariableSymbol> getAllParameters() {
        return orderedDefinitions.stream()
                .filter(symbol -> symbol instanceof VariableSymbol)
                .map(symbol -> (VariableSymbol) symbol)
                .filter(symbol -> symbol.kind().equals(SymbolKind.PARAMETER))
                .toList();
    }


    public List<MemberSymbol> getAllMembers() {
        return orderedDefinitions.stream()
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
