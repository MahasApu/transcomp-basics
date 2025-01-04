package syspro.languageServer;

import syspro.languageServer.exceptions.InterpreterException;
import syspro.languageServer.semantic.SemanticNode;
import syspro.languageServer.symbols.TypeSymbol;
import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.BooleanLiteralToken;
import syspro.tm.lexer.Keyword;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static java.util.Objects.isNull;
import static syspro.tm.parser.SyntaxKind.IDENTIFIER;
import static syspro.tm.parser.SyntaxKind.TYPE_DEFINITION;

public class Environment {


    private final SyntaxNode tree;
    private final Environment enclosing;
    private final Map<String, SyntaxNodeWithSymbols> values = new HashMap<>();
    private final Stack<SemanticSymbol> scope = new Stack<>();


    public Environment(SyntaxNode tree) {
        this.tree = tree;
        this.enclosing = null;
        initBuildInTypes();
    }


    public boolean hasDefinition(String def) {
        if (values.containsKey(def)) return true;
        if (!isNull(enclosing)) return enclosing.hasDefinition(def);
        return false;
    }

    private void initBuildInTypes() {

        List<String> names = List.of("Int32", "Int64", "UInt32", "UInt64", "Boolean", "Rune");
        for (String name : names) {

            ASTNode keyword = new ASTNode(Keyword.CLASS, null);
            ASTNode identifier = new ASTNode(IDENTIFIER, null);
            ASTNode node = new ASTNode(TYPE_DEFINITION, null, keyword, identifier, null, null, null, null, null, null, null);

            TypeSymbol symbol = new TypeSymbol(name, null, null, null, node);
            values.put(name, new SemanticNode(symbol, node));
        }
    }

    public void addDefinition(String def, SyntaxNodeWithSymbols node) {
        values.put(def, node);
    }

    public void pushToScope(SemanticSymbol symbol) { scope.add(symbol); }

    public SemanticSymbol getTopScope() { return scope.getLast(); }

    public void popFromScope() { scope.pop(); }

    public boolean getBoolean(SyntaxNode node) {
        if (isNull(node)) return false;

        return switch (node.token()) {
            case BooleanLiteralToken ignored -> node.toString().equals("true");
            default -> false;
        };
    }


    Object get(SyntaxNode node) {
        String name = node.toString();
        if (values.containsKey(name)) return values.get(name);
        if (!isNull(enclosing)) return enclosing.get(node);
        return null;
    }

    void assign(String name, SyntaxNodeWithSymbols node) {
        if (values.containsKey(name)) {
            values.put(name, node);
            return;
        }
        if (!isNull(enclosing)) {
            enclosing.assign(name, node);
            return;
        }
        throw new InterpreterException(node, "Undefined variable '" + node + "'.");

    }

    Environment getBaseEnvironment(int height) {
        Environment env = this;
        for (int i = 0; i < height; i++) env = env.enclosing;
        return env;
    }

    SyntaxNodeWithSymbols getAt(int height, String name) {
        return getBaseEnvironment(height).values.get(name);
    }

    SyntaxNodeWithSymbols assignAt(int height, String name, SyntaxNodeWithSymbols node) {
        return getBaseEnvironment(height).values.put(name, node);
    }


    @Override
    public String toString() {
        String result = values.toString();
        if (enclosing != null) result += " -> " + enclosing;
        return result;
    }
}
