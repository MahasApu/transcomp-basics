package syspro.languageServer.exceptions;

import syspro.tm.parser.SyntaxNode;

public class LanguageServerException extends RuntimeException {

    public LanguageServerException(SyntaxNode node, String message) {
        super(formatMessage(node, message));
    }

    private static String formatMessage(SyntaxNode node, String message) {
        return String.format("Error - %s: %s", node.kind(), message);
    }
}
