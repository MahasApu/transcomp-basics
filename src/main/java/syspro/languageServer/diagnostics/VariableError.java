package syspro.languageServer.diagnostics;

import syspro.tm.parser.ErrorCode;

public class VariableError implements ErrorCode {
    String errorMessage;

    public VariableError(String msg) {
        errorMessage = msg;
    }

    @Override
    public String name() {
        return "IndentationError: " + errorMessage;
    }
}
