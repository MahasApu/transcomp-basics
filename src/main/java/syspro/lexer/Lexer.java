package syspro.lexer;


import syspro.tm.lexer.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class Lexer implements syspro.tm.lexer.Lexer {

    private final String inputLine;
    private int start;
    private int end;
    private State oldState;
    private State newState;
    private int curPos = -1;
    private Map<String, Symbol> symbolMap;
    private Map<String, Keyword> keywordMap;
    private Map<String, BuiltInType> builtInTypeMap;

    Lexer(String inputLine) {
        this.inputLine = inputLine;
        symbolMap = Stream.of(Symbol.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        keywordMap = Stream.of(Keyword.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        builtInTypeMap = Stream.of(BuiltInType.values()).collect(Collectors.toMap(k -> k.name().toLowerCase(Locale.ROOT)
                , Function.identity()));
    }


    private String tokenizer() {
        int nextPos = curPos;
        StringBuilder token = new StringBuilder();

        while (++nextPos < inputLine.length()) {
            char nextSymbol = inputLine.charAt(nextPos);
            if (nextSymbol != ' ') {
                token.append(nextSymbol);
            }
            if (!token.isEmpty()) return token.toString();
        }
        return token.toString();
    }


    @Override
    public List<Token> lex(String s) {
        return new ArrayList<Token>();
    }
}



