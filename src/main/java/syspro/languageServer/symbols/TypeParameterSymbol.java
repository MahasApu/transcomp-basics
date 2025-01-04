package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

import java.util.ArrayList;
import java.util.List;

public class TypeParameterSymbol implements syspro.tm.symbols.TypeParameterSymbol {

    private final String name;
    private final SemanticSymbol owner;
    private final List<TypeLikeSymbol> bounds;

    public TypeParameterSymbol(String name, SemanticSymbol owner) {
        this.name = name;
        this.owner = owner;
        this.bounds = new ArrayList<>();
    }

    @Override
    public SymbolKind kind() {
        return null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SyntaxNode definition() {
        return null;
    }

    @Override
    public SemanticSymbol owner() {
        return owner;
    }

    @Override
    public List<? extends TypeLikeSymbol> bounds() {
        return bounds;
    }

}
