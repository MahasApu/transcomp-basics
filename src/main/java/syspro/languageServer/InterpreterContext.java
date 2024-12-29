package syspro.languageServer;

import syspro.languageServer.exceptions.InterpreterException;
import syspro.tm.lexer.BooleanLiteralToken;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

public class InterpreterContext {


    private final SyntaxNode tree;
    private final InterpreterContext enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public InterpreterContext(SyntaxNode tree) {
        this.tree = tree;
        this.enclosing = null;
    }

    public boolean hasDefinition(String def) {
        if (values.containsKey(def)) return true;
        if (!isNull(enclosing)) return enclosing.hasDefinition(def);
        return false;
    }

    public void addDefinition(String def, SyntaxNodeWithSymbols node) {
        values.put(def, node);
    }

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


    public boolean isInsideClass() {
        if (values.containsKey("class")) return true;
        if (!isNull(enclosing)) return enclosing.isInsideClass();
        return false;
    }

    void assign(SyntaxNode node, Object value) {
        if (values.containsKey(node.toString())) {
            values.put(node.toString(), value);
            return;
        }
        if (!isNull(enclosing)) {
            enclosing.assign(node, value);
            return;
        }
        throw new InterpreterException(node, "Undefined variable '" + node + "'.");

    }


    @Override
    public String toString() {
        String result = values.toString();
        if (enclosing != null) result += " -> " + enclosing;
        return result;
    }
}
