package syspro.lexer;

import java.util.ArrayDeque;

class State {

    enum ObservedState {
        DEFAULT,
        KEYWORD,
        SYMBOL,
        IDENTIFIER,
        BOOLEAN,
        NUMBER,
        STRING,
        RUNE,
        INDENTATION;
    }

    private ArrayDeque<ObservedState> newState;
    private ArrayDeque<ObservedState> oldState;

}
