package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;
import syspro.tm.symbols.VariableSymbol;

import java.util.List;

public class FunctionSymbol implements syspro.tm.symbols.FunctionSymbol {

    private final String name;
    private final TypeLikeSymbol returnType;
    private final List<? extends VariableSymbol> parameters;
    private final List<? extends VariableSymbol> locals;
    private final boolean isNative;
    private final boolean isVirtual;
    private final boolean isAbstract;
    private final boolean isOverride;
    private final SemanticSymbol owner;
    private final SyntaxNode definition;

    public FunctionSymbol(String name,
                          TypeLikeSymbol returnType,
                          List<? extends VariableSymbol> parameters,
                          List<? extends VariableSymbol> locals,
                          boolean isNative,
                          boolean isVirtual,
                          boolean isAbstract,
                          boolean isOverride,
                          SemanticSymbol owner,
                          SyntaxNode definition) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.locals = locals;
        this.isNative = isNative;
        this.isVirtual = isVirtual;
        this.isAbstract = isAbstract;
        this.isOverride = isOverride;
        this.owner = owner;
        this.definition = definition;
    }

    @Override
    public boolean isNative() {
        return isNative;
    }

    @Override
    public boolean isVirtual() {
        return isVirtual;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isOverride() {
        return isOverride;
    }


    @Override
    public List<? extends VariableSymbol> parameters() {
        return parameters;
    }

    @Override
    public TypeLikeSymbol returnType() {
        return returnType;
    }

    @Override
    public List<? extends VariableSymbol> locals() {
        return locals;
    }

    @Override
    public SemanticSymbol owner() {
        return owner;
    }

    @Override
    public SymbolKind kind() {
        return SymbolKind.FUNCTION;
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
