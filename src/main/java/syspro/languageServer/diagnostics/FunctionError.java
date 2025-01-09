package syspro.languageServer.diagnostics;

import syspro.tm.parser.ErrorCode;

public class FunctionError implements ErrorCode {
    String errorMessage;

    public FunctionError(String msg) {
        errorMessage = msg;
    }

    @Override
    public String name() {
        return "IndentationError: " + errorMessage;
    }
}
