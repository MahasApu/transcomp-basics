package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.MemberSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

import java.util.ArrayList;
import java.util.List;

import static syspro.tm.lexer.Keyword.*;

public class TypeSymbol implements syspro.tm.symbols.TypeSymbol {

    private final String name;
    public List<? extends MemberSymbol> members;
    public List<? extends TypeLikeSymbol> typeArguments;
    public List<? extends TypeSymbol> baseTypes;
    public SyntaxNode definition;
    private final SymbolKind kind;

    public TypeSymbol(String name, SyntaxNode definition) {
        this.name = name;
        this.members = new ArrayList<>();
        this.typeArguments = new ArrayList<>();
        this.baseTypes = new ArrayList<>();
        this.definition = definition;
        this.kind = defineSymbolKind();
    }

    private SymbolKind defineSymbolKind() {
        return switch (definition.slot(0).kind()) {
            case CLASS -> SymbolKind.CLASS;
            case INTERFACE -> SymbolKind.INTERFACE;
            case OBJECT -> SymbolKind.OBJECT;
            default -> throw new IllegalStateException("Unexpected value: " + definition.kind());
        };
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public List<? extends syspro.tm.symbols.TypeSymbol> baseTypes() {
        return baseTypes;
    }

    @Override
    public List<? extends TypeLikeSymbol> typeArguments() {
        return typeArguments;
    }

    @Override
    public syspro.tm.symbols.TypeSymbol originalDefinition() {
        return this;
    }

    @Override
    public syspro.tm.symbols.TypeSymbol construct(List<? extends TypeLikeSymbol> typeArguments) {
        return null;
    }

    @Override
    public List<? extends MemberSymbol> members() {
        return members;
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
