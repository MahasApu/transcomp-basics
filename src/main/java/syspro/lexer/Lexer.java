package syspro.lexer;


import syspro.tm.lexer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodePattern.UnicodeSymbols.LETTER_CHARACTER;
import static syspro.lexer.utils.UnicodePattern.UnicodeSymbols.NUMBER_LITERAL;
import static syspro.lexer.utils.UnicodeReader.codePointToString;
import static syspro.lexer.utils.UnicodeReader.getUnicodePoints;


class Lexer implements syspro.tm.lexer.Lexer {

    private final String source;
    private final int[] codePoints;

    private int start;
    private int end;
    private int curPos;
    private int leadingTriviaLength;
    private int trailingTriviaLength;
    private StringBuilder symbolBuffer;
    ArrayList<Token> tokens;

    private State.ObservedState oldState;
    private State.ObservedState curState;
    final private Map<String, Symbol> symbolMap;
    final private Map<String, Keyword> keywordMap;
    final private Map<String, BuiltInType> builtInTypeMap = Map.of(
            "i32", BuiltInType.INT32,
            "i64", BuiltInType.INT64,
            "u32", BuiltInType.UINT32,
            "u64", BuiltInType.UINT64,
            "true", BuiltInType.BOOLEAN,
            "false", BuiltInType.BOOLEAN
    );

    Lexer(String source) {
        this.curPos = -1;
        this.source = source;
        this.codePoints = getUnicodePoints(source);
        this.symbolMap = Stream.of(Symbol.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        this.keywordMap = Stream.of(Keyword.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        this.oldState = DEFAULT;
        this.curState = DEFAULT;
        symbolBuffer = new StringBuilder();
        tokens = new ArrayList<Token>();
    }

    private boolean isDigit(String s) {
        return Pattern.compile(NUMBER_LITERAL.text).matcher(s).matches();
    }

    private boolean isLetterCharacter(String s) {
        return Pattern.compile(LETTER_CHARACTER.text).matcher(s).matches();
    }

    private boolean isIdentifierStart(String s) {
        String firstSymbol = String.valueOf(s.charAt(0));
        return isLetterCharacter(firstSymbol) || firstSymbol.equals("_");
    }

    public boolean isIdentifier(String s) {
        String regex = "[\\p{L}\\p{Nl}|_]+[\\p{L}\\p{Nl}|\\p{Nd}\\p{Mn}\\p{Mc}\\p{Cf}]*";
        return Pattern.compile(regex).matcher(s).matches();
    }

    private Token readIdentifier() {
        if (curState.equals(NUMBER)) return new BadToken(start, end, leadingTriviaLength, trailingTriviaLength);
        return null;
    }


    private boolean isNumber(String s) {
        return Pattern.compile(NUMBER_LITERAL.text + "*").matcher(s).matches();
    }

    private long readNumber(String s) {
        return 10;
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

    private boolean nextIs(String s) {
        return Objects.equals(nextCodePoint(curPos), s);
    }

    private int skipTrailingWhitespaces(int pos) {
        if (pos >= codePoints.length) return pos; // redo: mb better to return -1
        String symbol = codePointToString(codePoints[pos]);
        while (pos < codePoints.length && codePointToString(codePoints[pos++]).equals(" ")) ;
        return pos;
    }

    private boolean nextLexemeIs(String s) {
        int nextPos = skipTrailingWhitespaces(curPos);
        StringBuilder nextLexeme = new StringBuilder();
        while (++nextPos < source.codePointCount(0, source.length())) {
            String nextSymbol = codePointToString(codePoints[nextPos]);
        }
        return true;
    }

    // todo: think about BadToken
    private void addToken(Token token) {
        if (nonNull(token)) {
            tokens.add(token);
            symbolBuffer = new StringBuilder();
        }
    }

    private SymbolToken readSymbol(String s) {
        curState = SYMBOL;
        String symbol = s;
        start = curPos;
        end = curPos + 1;
        switch (s) {
            case ">" -> {
                if (nextIs("=")) symbol = ">=";
                else if (nextIs(">")) symbol = ">>";
                else end--;
            }
            case "<" -> {
                if (nextIs("=")) symbol = "<=";
                else if (nextIs("<")) symbol = "<<";
                else if (nextIs(":")) symbol = "<:";
                else end--;
            }
            case "=" -> {
                if (nextIs("=")) symbol = "==";
                else if (nextIs("!")) symbol = "!=";
                else end--;
            }
            case "&" -> {
                if (nextIs("&")) symbol = "&";
                else end--;
            }
            default -> end--;
        }
        curPos = end;
        return new SymbolToken(start, end, leadingTriviaLength, trailingTriviaLength, symbolMap.get(symbol));

    }

    public Token tokenizeLexeme() {
        String lexeme = symbolBuffer.toString();
        Token token = null;
        if (isIdentifierStart(lexeme)) {
            curState = IDENTIFIER;
            switch (lexeme) {
                case "this", "super", "is", "else", "for", "in", "while", "def", "var", "val", "return", "break",
                     "continue", "abstract", "virtual", "override", "native" -> {
                    token = new KeywordToken(start, end, leadingTriviaLength, trailingTriviaLength, keywordMap.get(lexeme));
                }
                case "class", "object", "interface", "null" -> {
                    token = new IdentifierToken(start, end, leadingTriviaLength, trailingTriviaLength, lexeme, keywordMap.get(lexeme));
                }
                case "true", "false" -> {
                    token = new BooleanLiteralToken(start, end, leadingTriviaLength, trailingTriviaLength, Boolean.parseBoolean(lexeme));
                }
                default -> {
                    if (isIdentifier(lexeme))
                        token = new IdentifierToken(start, end, leadingTriviaLength, trailingTriviaLength, lexeme, null);

                }
            }
        }
//        if (isNumber(lexeme)) {
//
//        }
        return token;
    }


    public List<Token> tokenize() {
        while (++curPos < source.codePointCount(0, source.length())) {
            String nextSymbol = codePointToString(codePoints[curPos]);
            Token token = null;
            switch (nextSymbol) {
                case "#" -> curState = COMMENTARY;
                case "\n" -> {
                    curState = curState.equals(COMMENTARY) ? INDENTATION : curState;
                }
                case " ", "\t", "\r" -> {
                    if (curState.equals(COMMENTARY)) break;
                    if (!symbolBuffer.isEmpty()) {
                        end = curPos;
                        start = curPos - symbolBuffer.length();
                        token = tokenizeLexeme();
                    }
                }
                case "=", "<", ">", ".", ",", ":", "-", "*", "/", "%", "!", "~", "&", "|", "^", "[", "]", "(", ")",
                     "?" -> {
                    if (curState.equals(COMMENTARY)) break;
                    if (!symbolBuffer.isEmpty()) {
                        Token prevToken = tokenizeLexeme();
                        addToken(prevToken);
                    }
                    token = readSymbol(nextSymbol);
                }
                default -> {
                    if (curState.equals(COMMENTARY)) break;
                    symbolBuffer.append(nextSymbol);
                }
            }
            addToken(token);
        }
        return tokens;
    }


    @Override
    public List<Token> lex(String s) {
//        System.out.print(s);
        return new ArrayList<Token>();
    }
}



