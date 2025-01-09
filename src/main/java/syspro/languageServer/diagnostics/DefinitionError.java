package syspro.languageServer.diagnostics;

import syspro.tm.parser.ErrorCode;

public class DefinitionError implements ErrorCode {
    String errorMessage;

    public DefinitionError(String msg) {
        errorMessage = msg;
    }

    @Override
    public String name() {
        return "IndentationError: " + errorMessage;
    }
}
