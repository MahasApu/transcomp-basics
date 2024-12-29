package syspro.languageServer;

import syspro.languageServer.exceptions.InterpreterException;
import syspro.tm.parser.SyntaxNode;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

public class InterpreterContext {

    final InterpreterContext enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public InterpreterContext(InterpreterContext enclosing) {
        this.enclosing = enclosing;
    }

    Object get(SyntaxNode node) {
        String name = node.toString();
        if (values.containsKey(name)) return values.get(name);
        if (!isNull(enclosing)) return enclosing.get(node);
        return null;
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

    InterpreterContext ancestor(int distance) {
        InterpreterContext environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }

    @Override
    public String toString() {
        String result = values.toString();
        if (enclosing != null) result += " -> " + enclosing;
        return result;
    }
}
