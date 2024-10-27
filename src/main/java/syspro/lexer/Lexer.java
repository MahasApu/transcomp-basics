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
import static syspro.lexer.utils.UnicodeReader.codePointToString;
import static syspro.lexer.utils.UnicodeReader.getUnicodePoints;


class Lexer implements syspro.tm.lexer.Lexer {

    private int[] codePoints;


    private int curIndentationLevel;
    private int prevIndentationLevel;
    private int indentationLength = 0;
    private int start;
    private int end;
    private int nextPos;
    private StringBuilder symbolBuffer;
    ArrayList<Token> tokens;

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

    Lexer() {
        this.nextPos = -1;
        this.symbolMap = Stream.of(Symbol.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        this.keywordMap = Stream.of(Keyword.values()).collect(Collectors.toMap(k -> k.text, Function.identity()));
        this.curState = DEFAULT;
        symbolBuffer = new StringBuilder();
        tokens = new ArrayList<Token>();
        end = 0;
        start = 0;
    }

    private void initLexer(String source) {
        this.codePoints = getUnicodePoints(source);
        this.curState = DEFAULT;
        symbolBuffer = new StringBuilder();
        tokens = new ArrayList<Token>();
        this.nextPos = -1;
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

    private boolean isNumber(String s) {
        return Pattern.compile("(^\\p{Nd}+(i64|i32|u32|u64)?)$").matcher(s).matches();
    }

    public boolean isMatched(String expected) {
        if (nextPos == codePoints.length || nextPos == -1) return false;
        if (expected.isEmpty() || expected.charAt(0) != codePoints[nextPos]) return false;
        int startPos = nextPos;

        for (int i = 0; i < codePoints.length; i++) {
            char a = expected.charAt(i);
            String codePoint = codePointToString(codePoints[nextPos + i]);
            if (codePoint.charAt(0) != a) {
                nextPos = startPos;
                return false;
            }
        }
        return true;
    }

    private boolean match(String expected) {
        if (nextPos == codePoints.length || nextPos == -1) return false;
        if (!codePointToString(codePoints[nextPos]).equals(expected)) return false;

        nextPos++;
        return true;
    }

    private String nextCodePoint(int pos) {
        if (pos + 1 == codePoints.length) return null; // todo: make it safe
        return codePointToString(codePoints[pos + 1]);
    }

    private boolean nextIs(String s) {
        return Objects.equals(nextCodePoint(nextPos), s);
    }

    // todo: think about BadToken
    private void addToken(Token token) {
        if (nonNull(token)) {
            tokens.add(token);
            symbolBuffer = new StringBuilder();
            start = end = nextPos;
        }
    }

    private Token getToken() {
        end = nextPos - 1;
        start = nextPos - symbolBuffer.length();
        return tokenizeLexeme();
    }

    private SymbolToken readSymbol(String s) {
        curState = SYMBOL;
        String lexeme = s;
        end = nextPos + 1;
        start = nextPos;
        switch (s) {
            case ">" -> {
                if (nextIs("=")) lexeme = ">=";
                else if (nextIs(">")) lexeme = ">>";
                else end--;
            }
            case "<" -> {
                if (nextIs("=")) lexeme = "<=";
                else if (nextIs("<")) lexeme = "<<";
                else if (nextIs(":")) lexeme = "<:";
                else end--;
            }
            case "=" -> {
                if (nextIs("=")) lexeme = "==";
                else if (nextIs("!")) lexeme = "!=";
                else end--;
            }
            case "&" -> {
                if (nextIs("&")) lexeme = "&";
                else end--;
            }
            default -> end--;
        }
        nextPos = end;
        return new SymbolToken(start, end + countTrailingTrivia(), countLeadingTrivia(), countTrailingTrivia(), symbolMap.get(lexeme));

    }


    public Token tokenizeLexeme() {

        String lexeme = symbolBuffer.toString();
        Token token = null;

        if (isIdentifierStart(lexeme)) {
            curState = IDENTIFIER;
            switch (lexeme) {
                case "this", "super", "is", "else", "for", "in", "while", "def", "var", "val", "return", "break",
                     "continue", "abstract", "virtual", "override", "native" -> {
                    token = new KeywordToken(start, end + countTrailingTrivia(), countLeadingTrivia(), countTrailingTrivia(), keywordMap.get(lexeme));
                }
                case "class", "object", "interface", "null" -> {
                    token = new IdentifierToken(start, end + countTrailingTrivia(), countLeadingTrivia(), countTrailingTrivia(), lexeme, keywordMap.get(lexeme));
                }
                case "true", "false" -> {
                    token = new BooleanLiteralToken(start, end + countTrailingTrivia(), countLeadingTrivia(), countTrailingTrivia(), Boolean.parseBoolean(lexeme));
                }
                default -> {
                    if (isIdentifier(lexeme))
                        token = new IdentifierToken(start, end + countTrailingTrivia(), countLeadingTrivia(), countTrailingTrivia(), lexeme, null);

                }
            }
        } else if (isNumber(lexeme)) {
            curState = NUMBER;
            BuiltInType type = BuiltInType.INT64;
            boolean hasSuffix = false;
            long value;

            try {
                value = Long.parseLong(lexeme);
            } catch (NumberFormatException e) {
                type = builtInTypeMap.get(lexeme.substring(lexeme.length() - 3));
                hasSuffix = true;
                value = Long.parseLong(lexeme.substring(0, lexeme.length() - 3));
            }
            token = new IntegerLiteralToken(start, end + countTrailingTrivia(), countLeadingTrivia(), countTrailingTrivia(), type, hasSuffix, value);
        }
        return token;
    }

    public int countTrailingTrivia() {
        int pos = nextPos - 1;
        int count = 0;
        while (++pos < codePoints.length && (codePointToString(codePoints[pos]).equals(" ") ||
                codePointToString(codePoints[pos]).equals("\n") ||
                codePointToString(codePoints[pos]).equals("\t") ||
                codePointToString(codePoints[pos]).equals("\r"))) {
            count++;
        }
        if (pos == codePoints.length || !codePointToString(codePoints[pos]).equals("#")) return count;
        while (++pos < codePoints.length && !codePointToString(codePoints[pos]).equals("\n")) {
            count++;
        }
        return count;
    }

    public int countLeadingTrivia() {
        return 0;
    }


    private int countIndentationLength(int pos) {
        int count = 0;
        String nextSymbol = codePointToString(codePoints[++pos]);

        while (pos++ < codePoints.length - 1 && (nextSymbol.equals(" ") || nextSymbol.equals("\t"))) {
            count += nextSymbol.equals("\t") ? 2 : 1;
            nextSymbol = codePointToString(codePoints[pos]);
        }
        return count;

    }


    private void calculateIndentation() {
        int prevInd = curIndentationLevel;
        if (nextPos + 1 == codePoints.length) {
            curIndentationLevel = 0;
            return;
        }
        if (codePointToString(codePoints[nextPos + 1]).equals("\n")) return;
        else if (nextPos == codePoints.length) {
            curIndentationLevel = 0;
            return;
        }
        if (nextPos + indentationLength + 1 >= codePoints.length) {
            curIndentationLevel = 0;
            return;
        }
        if (codePointToString(codePoints[nextPos + indentationLength + 1]).equals("\n")) return;
        if (countIndentationLength(nextPos + indentationLength) == 0) {
            curIndentationLevel = 0;
            return;
        }
        if (countIndentationLength(nextPos + indentationLength) % 2 != 0) return;
        if (curIndentationLevel == 0) {
            indentationLength = countIndentationLength(nextPos + indentationLength);
            curIndentationLevel++;
            return;
        }
        if (countIndentationLength(nextPos + indentationLength) % indentationLength != 0) return;
        if (countIndentationLength(nextPos + indentationLength) == 0) {
            curIndentationLevel = 0;
            return;
        }

        curIndentationLevel = indentationLength != 0 ? countIndentationLength(nextPos + indentationLength) / indentationLength : 0;
        prevIndentationLevel = prevInd;
    }

    private int indentationDifference() {
        calculateIndentation();
        return curIndentationLevel - prevIndentationLevel;
    }

    private Token getIndentationToken() {
        int difference = indentationDifference();
        if (difference != 0)
            return new IndentationToken(start, end, countLeadingTrivia(), 0, difference);
        return null;
    }

    private void resetIndentation() {
        if (curIndentationLevel != 0) return;
        int difference = curIndentationLevel - prevIndentationLevel;
        while (difference-- > 0) {
            addToken(new IndentationToken(start, end, 0, 0, -1));
        }
    }

    private Token getStringLiteralToken() {
        end = nextPos;
        start = nextPos - symbolBuffer.length() - 1;
        return new StringLiteralToken(start, end, countLeadingTrivia(), countTrailingTrivia(), symbolBuffer.toString());
    }


    private int toCodePoint() {
        return 0;
    }


    private Token getRuneLiteralToken() {
        end = nextPos;
        start = nextPos - symbolBuffer.length() - 1;
        String rune = symbolBuffer.toString();
        if (isRune(rune))
            return new RuneLiteralToken(start, end + countTrailingTrivia(), countLeadingTrivia(), countTrailingTrivia(), toCodePoint());
        return null;
    }

    private boolean isRune(String s) {
        String RUNE_CHAR = "'";
        String SHORT_ESCAPE = "\\[0abfnrtv'\\]";
        String UNICODE_ESCAPE = "[\\\\U+]*[0-9A-F]{4,5}";
        String ESCAPE = String.format("(%s|%s)", SHORT_ESCAPE, UNICODE_ESCAPE);
        String SIMPLE_RUNE_CHARACTER = "[^'\\\r\n]";
        String RUNE_CHARACTER = String.format("(%s|%s)", SIMPLE_RUNE_CHARACTER, ESCAPE);
        String RUNE = String.format("%s%s%s", RUNE_CHAR, RUNE_CHARACTER, RUNE_CHAR);
        return Pattern.compile(RUNE_CHARACTER).matcher(s).matches();
    }

    private boolean isStateRuneOrString() {
        if (curState.equals(COMMENTARY) || curState.equals(STRING) || curState.equals(RUNE)) {
            symbolBuffer.append(codePointToString(codePoints[nextPos]));
            return true;
        }
        return false;
    }


    public List<Token> tokenize() {

        while (++nextPos < codePoints.length) {
            String nextSymbol = codePointToString(codePoints[nextPos]);
            Token token = null;

            switch (nextSymbol) {
                case "#" -> curState = COMMENTARY;
                case "\n" -> {
                    if (curState.equals(RUNE) || curState.equals(STRING)) {
                        symbolBuffer.append(nextSymbol);
                        break;
                    }
                    if (curState.equals(COMMENTARY)) {
                        symbolBuffer = new StringBuilder();
                        curState = INDENTATION;
                        break;
                    }
                    if (!symbolBuffer.isEmpty()) {
                        addToken(getToken());
                    }
                    curState = curState.equals(COMMENTARY) ? INDENTATION : curState;
//                    addToken(getIndentationToken());
//                    resetIndentation();
                }
                case " ", "\t", "\r" -> {
                    if (curState.equals(COMMENTARY) || curState.equals(STRING) || curState.equals(RUNE)) {
                        symbolBuffer.append(nextSymbol);
                        break;
                    }
                    if (!symbolBuffer.isEmpty()) {
                        addToken(getToken());
                    }
                }
                case "=", "<", ">", ".", ",", ":", "-", "+", "*", "/", "%", "!", "~", "&", "|", "^", "[", "]", "(", ")",
                     "?" -> {
                    if (curState.equals(COMMENTARY) || curState.equals(STRING) || curState.equals(RUNE)) {
                        symbolBuffer.append(nextSymbol);
                        break;
                    }
                    if (!symbolBuffer.isEmpty()) {
                        addToken(getToken());
                    }
                    token = readSymbol(nextSymbol);
                }
                case "'" -> {
                    if (curState.equals(RUNE)) {
                        addToken(getRuneLiteralToken());
                        curState = DEFAULT;
                        break;
                    }
                    curState = RUNE;
                }
                case "\"" -> {
                    if (curState.equals(STRING)) {
                        addToken(getStringLiteralToken());
                        curState = DEFAULT;
                        break;
                    }
                    curState = STRING;
                }
                default -> symbolBuffer.append(nextSymbol);
            }
            addToken(token);
        }
        if (!symbolBuffer.isEmpty()) addToken(getToken());
        return tokens;
    }


    @Override
    public List<Token> lex(String s) {
        initLexer(s);
        return tokenize();
    }
}



