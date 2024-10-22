package syspro.lexer;

import java.util.ArrayDeque;

class State {

    enum ObservedState {
        TRIVIA,
        DEFAULT,
        KEYWORD,
        SYMBOL,
        IDENTIFIER,
        BOOLEAN,
        NUMBER,
        STRING,
        RUNE,
        INDENTATION,
        COMMENTARY;
    }

    private ArrayDeque<ObservedState> newState;
    private ArrayDeque<ObservedState> oldState;

}
