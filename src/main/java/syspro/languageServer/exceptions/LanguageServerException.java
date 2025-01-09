package syspro.languageServer.exceptions;

import syspro.tm.parser.SyntaxNode;

public class LanguageServerException extends RuntimeException {

    public LanguageServerException(SyntaxNode node, String message) {
        super(formatMessage(node, message));
    }

    public LanguageServerException(String message) {
        super(formatMessage(message));
    }

    private static String formatMessage(String message) {
        return String.format("Error - %s", message);
    }

    private static String formatMessage(SyntaxNode node, String message) {
        return String.format("Error - %s: %s", node.kind(), message);
    }
}
