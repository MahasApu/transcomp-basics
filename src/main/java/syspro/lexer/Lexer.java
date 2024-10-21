package syspro.lexer;


import syspro.tm.lexer.*;

import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodeReader.*;
import static syspro.lexer.Pattern.UnicodeSymbols.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class Lexer implements syspro.tm.lexer.Lexer {

    private final String source;
    private final int[] codePoints;
    private int start;
    private int end;
    private int curPos;
    private int leadingTriviaLength;
    private int trailingTriviaLength;
    private State.ObservedState oldState;
    private State.ObservedState newState;
    final private Map<String, Symbol> symbolMap;
    final private Map<String, Keyword> keywordMap;
    final private Map<String, BuiltInType> builtInTypeMap;

    Lexer(String source) {
        this.curPos = -1;
        this.source = source;
        this.codePoints = getUnicodePoints(source);
        this.symbolMap = Stream.of(Symbol.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        this.keywordMap = Stream.of(Keyword.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        this.builtInTypeMap = Stream.of(BuiltInType.values()).collect(Collectors.toMap(k -> k.name().toLowerCase(Locale.ROOT)
                , Function.identity()));
        this.oldState = DEFAULT;
        this.newState = DEFAULT;
    }

    private boolean isDigit(String s) {
//        if (newState.equals(NUMBER)) return true;
//        if (newState.equals(IDENTIFIER)) return true;
        return Pattern.compile(NUMBER_LITERAL.text).matcher(s).matches();
    }

    private boolean isLetterCharacter(String s) {
        return Pattern.compile(LETTER_CHARACTER.text).matcher(s).matches();
    }

    private boolean isIdentifier() {
        if (newState.equals(NUMBER)) return false;
        return true;
    }

    private Token readIdentifier() {
        if (newState.equals(NUMBER)) return new BadToken(start, end, leadingTriviaLength, trailingTriviaLength);
        return null;
    }

    private IntegerLiteralToken readNumber(int pos) {
        return null;
    }

    public boolean isMatched(String expected) {
        if (curPos == codePoints.length || curPos == -1) return false;
        if (expected.isEmpty() || expected.charAt(0) != codePoints[curPos]) return false;
        int startPos = curPos;

        for (int i = 0; i < codePoints.length; i++) {
            char a = expected.charAt(i);
            String codePoint = codePointToString(codePoints[curPos + i]);
            if (codePoint.charAt(0) != a) {
                curPos = startPos;
                return false;
            }
        }
        return true;
    }


    private boolean match(String expected) {
        if (curPos == codePoints.length || curPos == -1) return false;
        if (!codePointToString(codePoints[curPos]).equals(expected)) return false;

        curPos++;
        return true;
    }

    private String nextCodePoint(int pos) {
        if (pos + 1 == codePoints.length) return null; // todo: make it safe
        return codePointToString(codePoints[pos + 1]);
    }

    private SymbolToken readSymbol(int pos, String s) {
        newState = State.ObservedState.SYMBOL;
        String symbol = s;
        start = pos;
        end = pos + 1;
        switch (s) {
            case ">" -> {
                if (match("")) symbol = ">=";
                else if (nextCodePoint(pos).equals(">")) symbol = ">>";
            }
            case "<" -> {
                if (nextCodePoint(pos).equals("=")) symbol = "<=";
                else if (nextCodePoint(pos).equals("<")) symbol = "<<";
                else if (nextCodePoint(pos).equals(":")) symbol = "<:";
            }
            case "=" -> {
                if (nextCodePoint(pos).equals("=")) symbol = "==";
                else if (nextCodePoint(pos).equals("!")) symbol = "!=";
            }
            case "&" -> {
                if (nextCodePoint(pos).equals("&")) symbol = "&";
            }
            default -> end--;
        }
        return new SymbolToken(start, end, leadingTriviaLength, trailingTriviaLength, symbolMap.get(symbol));

    }

    public List<Token> tokenize() {
        int nextPos = curPos;
        ArrayList<Token> tokens = new ArrayList<>();
        while (++curPos < source.codePointCount(0, source.length())) {
            String unicodePoint = codePointToString(codePoints[curPos]);
            Token token = null;
            switch (unicodePoint) {
                case " " -> System.out.println("whitespace");
                case "\t" -> System.out.println("\\t");
                case "\n" -> System.out.println("\\n");
                case "\r" -> System.out.println("\\r");
                case "=", "<", ">", ".", ":", "-", "*", "/", "%", "!", "~", "&", "|", "^", "[", "]", "(", ")", "?" ->
                        token = readSymbol(curPos, unicodePoint);
                case String s when isDigit(s) -> System.out.println("is number");
                case String s when isLetterCharacter(s) -> System.out.println("is letter");
                default -> {
                    System.out.printf("Something else %s", unicodePoint);
                }
            }
            if (Objects.nonNull(token)) tokens.add(token);
        }
        return tokens;
    }


    @Override
    public List<Token> lex(String s) {
        return new ArrayList<Token>();
    }
}



