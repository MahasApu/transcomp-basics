package syspro.lexer;


import syspro.tm.lexer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodePattern.*;
import static syspro.lexer.utils.UnicodeReader.codePointToString;
import static syspro.lexer.utils.UnicodeReader.getUnicodePoints;


class Lexer implements syspro.tm.lexer.Lexer {

    private int curIndentationLevel;
    private int prevIndentationLevel;
    private int indentationLength = 0;
    private int start;
    private int end;
    private int nextPos;

    private int[] codePoints;
    private StringBuilder symbolBuffer;
    private ArrayList<Token> tokens;

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
        tokens = new ArrayList<>();
        end = 0;
        start = 0;
    }

    private void initLexer(String source) {
        this.codePoints = getUnicodePoints(source);
        this.curState = DEFAULT;
        symbolBuffer = new StringBuilder();
        tokens = new ArrayList<>();
        this.nextPos = -1;
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
        start = nextPos - symbolBuffer.codePoints().toArray().length;
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
        return new SymbolToken(start - countLeadingTrivia(), end + countTrailingTrivia(),
                countLeadingTrivia(), countTrailingTrivia(), symbolMap.get(lexeme));

    }


    public Token tokenizeLexeme() {

        String lexeme = symbolBuffer.toString();
        Token token = null;

        if (isIdentifierStart(lexeme)) {
            curState = IDENTIFIER;
            switch (lexeme) {
                case "this", "super", "is", "else", "for", "in", "while", "def", "var", "val", "return", "break",
                     "continue", "abstract", "virtual", "override", "native" -> {
                    token = new KeywordToken(start - countLeadingTrivia(), end + countTrailingTrivia(),
                            countLeadingTrivia(), countTrailingTrivia(), keywordMap.get(lexeme));
                }
                case "class", "object", "interface", "null" -> {
                    token = new IdentifierToken(start - countLeadingTrivia(), end + countTrailingTrivia(),
                            countLeadingTrivia(), countTrailingTrivia(), lexeme, keywordMap.get(lexeme));
                }
                case "true", "false" -> {
                    token = new BooleanLiteralToken(start - countLeadingTrivia(), end + countTrailingTrivia(),
                            countLeadingTrivia(), countTrailingTrivia(), Boolean.parseBoolean(lexeme));
                }
                default -> {
                    if (isIdentifier(lexeme))
                        token = new IdentifierToken(start - countLeadingTrivia(), end + countTrailingTrivia(),
                                countLeadingTrivia(), countTrailingTrivia(), lexeme, null);

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
                String typeName = lexeme.substring(lexeme.length() - 3);
                type = builtInTypeMap.get(typeName);
                hasSuffix = true;
                value = Long.parseLong(lexeme.substring(0, lexeme.length() - 3));
            }
            token = new IntegerLiteralToken(start - countLeadingTrivia(), end + countTrailingTrivia(),
                    countLeadingTrivia(), countTrailingTrivia(), type, hasSuffix, value);
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

    public int countTrailingTrivia(int pos) {
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
        return curState.equals(INDENTATION) ? countTrailingTrivia(start) : 0;
    }


    private int countIndentationLength(int pos) {
        int count = 0;
        while (pos < codePoints.length - 1 && (codePoints[pos] == ' ' || codePoints[pos] == '\t')) {
            count += codePoints[pos] == '\t' ? 2 : 1;
            pos++;
        }
        return count;
    }


    private void calculateIndentation() {
        int prevInd = curIndentationLevel;
        assert codePoints[nextPos] == '\n';

        if (nextPos + 1 >= codePoints.length) {
            curIndentationLevel = 0;
            prevIndentationLevel = prevInd;
            resetIndentation();
            return;
        }
        if (codePoints[nextPos + 1] == '\n') return;
        int count = countIndentationLength(nextPos + 1);
        if (count == 0 || nextPos + 1 + count + 1 >= codePoints.length) {
            curIndentationLevel = 0;
            prevIndentationLevel = prevInd;
            resetIndentation();
            return;
        }
        if (count % 2 != 0) return;

        if (curIndentationLevel == 0) {
            curIndentationLevel++;
            prevIndentationLevel = prevInd;
            indentationLength = count;
            resetIndentation();
            return;
        }

        assert indentationLength != 0;
        if (count % indentationLength != 0) return;

        curIndentationLevel = count / indentationLength;
        prevIndentationLevel = prevInd;
        resetIndentation();
    }

    private Token getIndentationToken() {
        calculateIndentation();
        return null;
    }

    private void resetIndentation() {
        end = start = nextPos;
        int difference = curIndentationLevel - prevIndentationLevel;
        if (difference == 0) return;
        int sign = difference > 0 ? 1 : -1;
        difference *= sign;
        while (difference-- > 0) {
            addToken(new IndentationToken(end, end, 0, 0, sign));
        }
    }

    void resetIndentationAtTheEnd() {
        if (curIndentationLevel != 0) {
            prevIndentationLevel = curIndentationLevel;
            curIndentationLevel = 0;
            resetIndentation();
        }
    }

    private Token getStringLiteralToken() {
        end = nextPos;
        start = nextPos - (int) symbolBuffer.codePoints().count() - 1;
        return new StringLiteralToken(start - countLeadingTrivia(), end + countTrailingTrivia(end),
                countLeadingTrivia(), countTrailingTrivia(end), symbolBuffer.toString());
    }


    private Token getRuneLiteralToken() {
        end = nextPos;
        start = nextPos - (int) symbolBuffer.codePoints().count() - 1;
        String rune = symbolBuffer.toString();
        if (isRune(rune))
            return new RuneLiteralToken(start - countLeadingTrivia(), end + countTrailingTrivia(end),
                    countLeadingTrivia(), countTrailingTrivia(end), rune.codePointAt(0));
        return null;
    }

    public List<Token> tokenize() {
        while (++nextPos < codePoints.length) {
            String nextSymbol = codePointToString(codePoints[nextPos]);

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
                    }
                    if (!symbolBuffer.isEmpty()) {
                        addToken(getToken());
                    }
                    addToken(getIndentationToken());
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
                    addToken(readSymbol(nextSymbol));
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
        }
        if (!symbolBuffer.isEmpty()) addToken(getToken());
        resetIndentationAtTheEnd();
        return tokens;
    }


    @Override
    public List<Token> lex(String s) {
        initLexer(s);
        return tokenize();
    }
}



