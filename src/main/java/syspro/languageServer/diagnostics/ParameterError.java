package syspro.languageServer.diagnostics;

import syspro.tm.parser.ErrorCode;

public class ParameterError implements ErrorCode {
    String errorMessage;

    public ParameterError(String msg) {
        errorMessage = msg;
    }

    @Override
    public String name() {
        return "IndentationError: " + errorMessage;
    }
}
