package syspro.parser.diagnostics;

import syspro.tm.parser.ErrorCode;

public class SyntaxError implements ErrorCode {

    String errorMessage;

    public SyntaxError(String msg) {
        errorMessage = msg;
    }

    @Override
    public String name() {
        return "SyntaxError: " + errorMessage;
    }
}
