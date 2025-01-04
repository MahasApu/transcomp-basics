package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.MemberSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

import java.util.List;

import static syspro.tm.lexer.Keyword.*;

public class TypeSymbol implements syspro.tm.symbols.TypeSymbol {

    private final String name;
    private final List<? extends MemberSymbol> members;
    private final List<? extends TypeLikeSymbol> typeArguments;
    private final List<? extends syspro.tm.symbols.TypeSymbol> baseTypes;
    private final SyntaxNode definition;
    private final SymbolKind kind;

    public TypeSymbol(String name,
                      List<? extends MemberSymbol> members,
                      List<? extends TypeLikeSymbol> typeArguments,
                      List<? extends syspro.tm.symbols.TypeSymbol> baseTypes,
                      SyntaxNode definition) {
        this.name = name;
        this.members = members;
        this.typeArguments = typeArguments;
        this.baseTypes = baseTypes;
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
        return new TypeSymbol(name, members, typeArguments, baseTypes, definition);
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
