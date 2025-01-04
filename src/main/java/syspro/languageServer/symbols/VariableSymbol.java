package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

public class VariableSymbol implements syspro.tm.symbols.VariableSymbol {

    private final String name;
    private final TypeLikeSymbol type;
    private final SemanticSymbol owner;
    private final SymbolKind kind;
    private final SyntaxNode definition;

    public VariableSymbol(String name, TypeLikeSymbol type, SemanticSymbol owner, SymbolKind kind, SyntaxNode definition) {
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.kind = kind;
        this.definition = definition;
    }

    @Override
    public TypeLikeSymbol type() {
        return type;
    }

    @Override
    public SemanticSymbol owner() {
        return owner;
    }

    @Override
    public SymbolKind kind() {
        return kind;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SyntaxNode definition() {
        return definition;
    }
}
