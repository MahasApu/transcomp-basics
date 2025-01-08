package syspro.languageServer.symbols;

import syspro.tm.lexer.BooleanLiteralToken;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.MemberSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

import java.util.ArrayList;
import java.util.List;

import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.parser.SyntaxKind.GENERIC_NAME_EXPRESSION;

public class TypeSymbol implements syspro.tm.symbols.TypeSymbol {

    private final String name;
    public List<? extends MemberSymbol> members;
    public List<? extends TypeLikeSymbol> typeArguments;
    public List<? extends TypeSymbol> baseTypes;
    public SyntaxNode definition;
    private final SymbolKind kind;
    public syspro.tm.symbols.TypeSymbol originalDefinition;

    public TypeSymbol(String name, SyntaxNode definition) {
        this.name = name;
        this.members = new ArrayList<>();
        this.typeArguments = new ArrayList<>();
        this.baseTypes = new ArrayList<>();
        this.definition = definition;
        this.kind = defineSymbolKind();
        this.originalDefinition = this;
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
        return kind.equals(SymbolKind.INTERFACE) || hasAbstractFunction();
    }

    private boolean hasAbstractFunction() {
        for (MemberSymbol member : members) {
            if (member instanceof FunctionSymbol symbol && symbol.isAbstract())
                return true;
        }
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
        return originalDefinition;
    }

    @Override
    public syspro.tm.symbols.TypeSymbol construct(List<? extends TypeLikeSymbol> typeArguments) {
        TypeSymbol constructedSymbol = new TypeSymbol(this.name, this.definition);
        for (TypeLikeSymbol arg: typeArguments) {
            List<MemberSymbol> constructedMembers = new ArrayList<>();
            for (MemberSymbol member : members) {
                constructedMembers.add(resolveMemberTypes(member, typeArguments));
            }

            constructedSymbol.members = constructedMembers;
            List<TypeLikeSymbol> constructedTypeArgs = new ArrayList<>();
            for (int i = 0; i < this.typeArguments.size(); i++) {
                TypeLikeSymbol originalTypeArg = this.typeArguments.get(i);
                TypeLikeSymbol resolvedType = resolveTypeArgument(originalTypeArg, typeArguments.get(i));
                constructedTypeArgs.add(resolvedType);
            }
            constructedSymbol.typeArguments = constructedTypeArgs;
        }
        return constructedSymbol;
    }


    private TypeLikeSymbol resolveTypeArgument(TypeLikeSymbol original, TypeLikeSymbol typeArg) {
        if (original.definition().kind().equals(GENERIC_NAME_EXPRESSION)) {
            return typeArg;
        }
        return original;
    }

    private MemberSymbol resolveMemberTypes(MemberSymbol member, List<? extends TypeLikeSymbol> typeArguments) {
        if (member instanceof VariableSymbol variable) {
            variable.type = (resolveTypeArgument(variable.type(), typeArguments.getFirst()));
        } else if (member instanceof FunctionSymbol function) {
            for (VariableSymbol param : function.parameters) {
                param.type = (resolveTypeArgument(param.type(), typeArguments.getFirst()));
            }
        }
        return member;
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
