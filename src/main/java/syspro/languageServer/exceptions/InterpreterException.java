package syspro.languageServer.exceptions;

import syspro.tm.parser.SyntaxNode;

public class InterpreterException extends RuntimeException {
    public InterpreterException(SyntaxNode node, String s) {
    }
}
