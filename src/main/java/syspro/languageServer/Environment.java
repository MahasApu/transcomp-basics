package syspro.languageServer;

import syspro.languageServer.exceptions.LanguageServerException;
import syspro.languageServer.symbols.*;
import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.Keyword;
import syspro.tm.parser.Diagnostic;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.parser.TextSpan;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;

import java.util.*;

import static syspro.tm.parser.SyntaxKind.IDENTIFIER;
import static syspro.tm.parser.SyntaxKind.TYPE_DEFINITION;

public class Environment {

    private final Deque<Scope> scopes;
    private final Map<String, ASTNode> definitions;
    private final List<TypeSymbol> unresolvedTypes = new ArrayList<>();
    private final List<TextSpan> invalidRanges = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public Environment(SyntaxNode tree) {
        scopes = new ArrayDeque<>();
        definitions = new HashMap<>();
        push(new Scope(null, "GlobalScope"));
        initBuildInTypes();
    }


    private void initBuildInTypes() {

        List<String> names = List.of("Int32", "Int64", "UInt32", "UInt64", "Boolean", "Rune");
        for (String name : names) {

            ASTNode keyword = new ASTNode(Keyword.CLASS, null);
            ASTNode identifier = new ASTNode(IDENTIFIER, null);
            ASTNode node = new ASTNode(TYPE_DEFINITION, null, keyword, identifier, null, null, null, null, null, null, null);

            TypeSymbol symbol = new TypeSymbol(name, node);
            get().declareSymbol(name, symbol);
        }
    }

//    public void addUnresolvedType(String name, TypeSymbol typeSymbol) {
//        unresolvedTypes.add(typeSymbol);
//    }
//
//    public void resolveUnresolvedTypes() {
//        for (TypeSymbol unresolvedType : unresolvedTypes) {
//            if (isDefined(unresolvedType.name())) {
//                TypeSymbol resolvedType = (TypeSymbol) definitions.get(unresolvedType.name()).symbol();
//            }
//        }
//        unresolvedTypes.clear();
//    }

    public SemanticSymbol lookup(String name) {
        return get().lookupSymbol(name);
    }

    public void push(Scope scope) {
        if (Objects.isNull(scope)) throw new IllegalArgumentException("Scope cannot be null");
        scopes.push(scope);
    }

    public void pop() {
        scopes.pop();
    }

    public Scope get() {
        return scopes.getFirst();
    }

    public SemanticSymbol getOwner() {
        Scope ownerScope = get().getParent();
        return !Objects.isNull(ownerScope) ? get().lookupSymbol(ownerScope.getName()) : null;
    }

    public void define(String name, ASTNode node) {
        definitions.put(name, node);
    }

    public boolean isDefined(String name) {
        return definitions.containsKey(name) || !Objects.isNull(lookup(name));
    }

    public boolean isDefinedLocally(String name) {
        return get().isDeclaredLocally(name);
    }

    public boolean isInsideFunction() {
        return lookup(get().getName()).kind().equals(SymbolKind.FUNCTION);
    }

    public void declare(String name, SemanticSymbol semanticSymbol) {
        switch (semanticSymbol) {
            case TypeSymbol symbol -> get().declareSymbol(name, symbol);
            case VariableSymbol symbol -> get().declareSymbol(name, symbol);
            case FunctionSymbol symbol -> get().declareSymbol(name, symbol);
            case TypeParameterSymbol symbol -> get().declareSymbol(name, symbol);
            default -> {
                ASTNode node = new ASTNode(null, null);
                node.updateSymbol(semanticSymbol);
                throw new LanguageServerException(node, "Unsupported symbol type for declaration: " + semanticSymbol);
            }
        }
    }


}
