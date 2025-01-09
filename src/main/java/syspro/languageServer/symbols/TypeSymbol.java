package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.MemberSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;
import syspro.tm.symbols.TypeParameterSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static syspro.tm.lexer.Keyword.*;

public class TypeSymbol implements syspro.tm.symbols.TypeSymbol {

    private final String name;
    public List<? extends MemberSymbol> members;
    public List<? extends TypeLikeSymbol> typeArguments;
    public List<TypeSymbol> baseTypes;
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

        HashMap<String, TypeLikeSymbol> map = new HashMap<>();
        for (int k = 0; k < this.typeArguments.size(); k++) {
            map.put(this.typeArguments.get(k).name(), typeArguments.get(k));
        }

        TypeSymbol constructedSymbol = new TypeSymbol(this.name, this.definition);

        List<TypeLikeSymbol> constructedTypeArgs = new ArrayList<>();
        for (TypeLikeSymbol typeArgument : this.typeArguments) {
            TypeLikeSymbol resolvedType = resolveTypeArgument(typeArgument, map, constructedSymbol);
            constructedTypeArgs.add(resolvedType);
        }
        constructedSymbol.typeArguments = constructedTypeArgs;


        List<MemberSymbol> constructedMembers = new ArrayList<>();
        for (MemberSymbol member : members) {
            constructedMembers.add(resolveMemberTypes(member, map, constructedSymbol));
        }
        constructedSymbol.members = constructedMembers;

        return constructedSymbol;
    }


    private TypeLikeSymbol resolveTypeArgument(TypeLikeSymbol original, HashMap<String, TypeLikeSymbol> typeArg, TypeSymbol owner) {
        if (original instanceof TypeParameterSymbol type) {
            return typeArg.getOrDefault(type.name(),
                    new syspro.languageServer.symbols.TypeParameterSymbol(type.name(), owner, type.definition()));
        }
        return original;
    }

    private MemberSymbol resolveMemberTypes(MemberSymbol member, HashMap<String, TypeLikeSymbol> typeArguments, TypeSymbol owner) {
        MemberSymbol result = null;
        if (member instanceof VariableSymbol variable) {
            result = new VariableSymbol(variable.name(),
                    resolveTypeArgument(variable.type(), typeArguments, owner),
                    owner, variable.kind(), variable.definition());

        } else if (member instanceof FunctionSymbol function) {
            List<VariableSymbol> params = new ArrayList<>();

            for (VariableSymbol param : function.parameters) {
                TypeLikeSymbol resolvedType = resolveTypeArgument(param.type(), typeArguments, owner);
                params.add(new VariableSymbol(param.name(), resolvedType, owner, param.kind(), param.definition()));
            }
            result = new FunctionSymbol(function.name(), resolveTypeArgument(function.returnType(), typeArguments, owner),
                    function.isNative(), function.isVirtual(), function.isAbstract(), function.isOverride(),
                    owner, function.definition());
            ((FunctionSymbol) result).parameters = params;
        }
        return result;
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
