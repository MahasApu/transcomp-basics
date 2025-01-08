package syspro.languageServer.symbols;

import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.*;
import syspro.tm.symbols.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

public class FunctionSymbol implements syspro.tm.symbols.FunctionSymbol {

    private final String name;
    private final TypeLikeSymbol returnType;
    public List<syspro.languageServer.symbols.VariableSymbol> parameters = new ArrayList<>();
    public List<syspro.languageServer.symbols.VariableSymbol> locals = new ArrayList<>();
    private final boolean isNative;
    private final boolean isVirtual;
    private final boolean isAbstract;
    private final boolean isOverride;
    public SemanticSymbol owner;
    private final SyntaxNode definition;

    public FunctionSymbol(String name,
                          TypeLikeSymbol returnType,
                          boolean isNative,
                          boolean isVirtual,
                          boolean isAbstract,
                          boolean isOverride,
                          SemanticSymbol owner,
                          SyntaxNode definition) {
        this.name = name;
        this.returnType = returnType;
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
