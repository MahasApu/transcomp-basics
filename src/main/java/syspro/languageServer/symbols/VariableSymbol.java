package syspro.languageServer.symbols;

import syspro.parser.ast.ASTNode;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

public class VariableSymbol implements syspro.tm.symbols.VariableSymbol {

    private final String name;
    private final TypeLikeSymbol type;
    private SemanticSymbol owner;
    private SymbolKind kind;
    private SyntaxNode definition;

    public VariableSymbol(String name, TypeLikeSymbol type, SemanticSymbol owner, SymbolKind kind, SyntaxNode definition) {
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.kind = kind;
        this.definition = definition;
    }

    public void updateDefinition(ASTNode node) {
        this.definition = node;
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
