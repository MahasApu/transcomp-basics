package syspro.parser.diagnostics;

import syspro.tm.parser.ErrorCode;

public class IndentationError implements ErrorCode {
    String errorMessage;

    public IndentationError(String msg) {
        errorMessage = msg;
    }

    @Override
    public String name() {
        return "IndentationError: " + errorMessage;
    }
}
