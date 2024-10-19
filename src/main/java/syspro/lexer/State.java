package syspro.lexer;

import java.util.ArrayDeque;

class State {

    enum ObservedState {
        BOOLEAN,
        KEYWORD,
        IDENTIFIER,
        NUMBER_LETTER,
        RUNE,
        STRING_LETTER,
        INDENTATION;
    }

    private ArrayDeque<ObservedState> newState;
    private ArrayDeque<ObservedState> oldState;

}
