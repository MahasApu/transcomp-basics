package syspro.parser.ast;

import syspro.tm.parser.AnySyntaxKind;

public enum SyntaxCategory implements AnySyntaxKind{
    ANY,
    TERMINAL,
    NON_TERMINAL,
    DEFINITION,
    STATEMENT,
    EXPRESSION,
    PRIMARY,
    NAME_EXPRESSION,
    INDENT,
    DEDENT;

    @Override
    public boolean isTerminal() {
        return false;
    }
}
