package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

import java.util.ArrayList;
import java.util.List;

public class TypeParameterSymbol implements syspro.tm.symbols.TypeParameterSymbol {

    private final String name;
    private SemanticSymbol owner;
    public List<TypeLikeSymbol> bounds;
    private SyntaxNode definition;

    public TypeParameterSymbol(String name, SemanticSymbol owner, SyntaxNode definition) {
        this.name = name;
        this.owner = owner;
        this.bounds = new ArrayList<>();
        this.definition = definition;
    }

    @Override
    public SymbolKind kind() {
        return SymbolKind.TYPE_PARAMETER;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SyntaxNode definition() {
        return definition;
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
