package syspro.languageServer;

import syspro.languageServer.exceptions.LanguageServerException;
import syspro.languageServer.symbols.*;
import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.Keyword;
import syspro.tm.parser.*;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

import java.util.*;

import static syspro.tm.parser.SyntaxKind.IDENTIFIER;
import static syspro.tm.parser.SyntaxKind.TYPE_DEFINITION;

public class Environment {

    private final Deque<Scope> scopes;
    private final Map<String, ASTNode> definitions;
    private final Collection<TextSpan> invalidRanges = new ArrayList<>();
    private final Collection<Diagnostic> diagnostics = new ArrayList<>();

    public Environment(SyntaxNode tree) {
        scopes = new ArrayDeque<>();
        definitions = new HashMap<>();
        push(new Scope(null, "GlobalScope", null));
        initBuildInTypes();
        initDefinitions(tree.slot(0));
    }

    private void initDefinitions(SyntaxNode tree) {
        for (int i = 0; i < tree.slotCount(); i++) {
            ASTNode node = (ASTNode) tree.slot(i);
            String name = node.slot(1).token().toString();
            if (definitions.containsKey(name)) throw new LanguageServerException(node, "Type already exists.");
            TypeSymbol symbol = new TypeSymbol(name, node);

            ASTNode params = (ASTNode) node.slot(3);
            List<TypeLikeSymbol> symbolArgs = new ArrayList<>();
            if (params != null)
                for (int j = 0; j < params.slotCount(); j += 2) {
                    symbolArgs.add(new TypeParameterSymbol(params.slot(j).slot(0).token().toString(), symbol, params.slot(j)));
                }
            symbol.typeArguments = symbolArgs;
            node.updateSymbol(symbol);
            define(name, node);
            declare(name, symbol);
        }
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


    public SemanticSymbol lookup(String name) {
        return get().lookupSymbol(name);
    }

    public SemanticSymbol lookupFunction(String name, List<VariableSymbol> params) {
        return get().lookupFunction(name, params);
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
        return !Objects.isNull(ownerScope) ? ownerScope.getSymbol() : null;
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
        return get().getSymbol().kind().equals(SymbolKind.FUNCTION);
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


    public Collection<TextSpan> invalidRanges() {
        return invalidRanges;
    }

    public Collection<Diagnostic> diagnostics() {
        return diagnostics;
    }

    public void addInvalidRange(TextSpan textSpan, ErrorCode error) {


        invalidRanges.add(textSpan);
        diagnostics.add(new Diagnostic(
                new DiagnosticInfo(error, null),
                textSpan,
                null
        ));
    }


}
