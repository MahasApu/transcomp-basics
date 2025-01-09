package syspro.languageServer.diagnostics;

import syspro.tm.parser.ErrorCode;

public class TypeParameterError implements ErrorCode {
    String errorMessage;

    public TypeParameterError(String msg) {
        errorMessage = msg;
    }

    @Override
    public String name() {
        return "IndentationError: " + errorMessage;
    }
}
