package syspro.lexer;


import syspro.tm.lexer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodePattern.*;
import static syspro.lexer.utils.UnicodeReader.codePointToString;
import static syspro.lexer.utils.UnicodeReader.getUnicodePoints;
import static syspro.lexer.utils.UtilMaps.*;


class Lexer implements syspro.tm.lexer.Lexer {


    private int countLeadingTrivia;
    private int countTrailingTrivia;

    private int curIndentationLevel;
    private int prevIndentationLevel;
    private int indentationLength;
    private int nextPos = -1;
    private int start;
    private int end;

    private int[] codePoints;
    private StringBuilder symbolBuffer;
    private ArrayList<Token> tokens;

    private State.ObservedState curState;

    Lexer() {
        this.curState = DEFAULT;
        this.symbolBuffer = new StringBuilder();
        this.tokens = new ArrayList<>();
    }

    private void initLexer(String source) {
        countTrailingTrivia = countLeadingTrivia = 0;
        this.symbolBuffer = new StringBuilder();
        this.tokens = new ArrayList<>();
        this.codePoints = getUnicodePoints(source);
        this.curState = DEFAULT;
        this.nextPos = -1;
    }

    private void resetBuffer() {
        symbolBuffer = new StringBuilder();
    }

    private void addNext() {
        symbolBuffer.append(getNextSymbol());
    }

    private boolean hasLexeme() {
        return !symbolBuffer.isEmpty();
    }

    private String getSymbol(int pos) {
        assert pos < codePoints.length;
        return codePointToString(codePoints[pos]);
    }

    private String getNextSymbol() {
        assert nextPos < codePoints.length;
        return codePointToString(codePoints[nextPos]);
    }


    private String nextCodePoint(int pos) {
        if (pos + 1 == codePoints.length) return null;
        return codePointToString(codePoints[pos + 1]);
    }

    private boolean nextIs(String s) {
        return Objects.equals(nextCodePoint(nextPos), s);
    }

    private void addToken(Token token) {
        if (nonNull(token)) {
            tokens.add(token);
            symbolBuffer = new StringBuilder();
            start = end = nextPos;
        }
        if (curState.equals(INDENTATION)) return;
        countTrailingTrivia = countLeadingTrivia = 0;
    }


    private SymbolToken tokenizeSymbol() {
        curState = SYMBOL;
        String lexeme = symbolBuffer.toString();
        end = nextPos + 1;
        start = nextPos;
        switch (symbolBuffer.toString()) {
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
        return new SymbolToken(start - countLeadingTrivia, end + countTrailingTrivia,
                countLeadingTrivia, countTrailingTrivia, symbolMap.get(lexeme));

    }


    public Token getLiteralToken() {
        end = nextPos - 1;
        start = nextPos - symbolBuffer.codePoints().toArray().length;

        String lexeme = symbolBuffer.toString();
        Token token = null;

        if (isIdentifierStart(lexeme)) {
            curState = IDENTIFIER;
            switch (lexeme) {
                case "this", "super", "is", "else", "for", "in", "while", "def", "var", "val", "return", "break",
                     "continue", "abstract", "virtual", "override", "native" -> {
                    token = new KeywordToken(start - countLeadingTrivia, end + countTrailingTrivia,
                            countLeadingTrivia, countTrailingTrivia, keywordMap.get(lexeme));
                }
                case "class", "object", "interface", "null" -> {
                    token = new IdentifierToken(start - countLeadingTrivia, end + countTrailingTrivia,
                            countLeadingTrivia, countTrailingTrivia, lexeme, keywordMap.get(lexeme));
                }
                case "true", "false" -> {
                    token = new BooleanLiteralToken(start - countLeadingTrivia, end + countTrailingTrivia,
                            countLeadingTrivia, countTrailingTrivia, Boolean.parseBoolean(lexeme));
                }
                default -> {
                    if (isIdentifier(lexeme))
                        token = new IdentifierToken(start - countLeadingTrivia, end + countTrailingTrivia,
                                countLeadingTrivia, countTrailingTrivia, lexeme, null);

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
            token = new IntegerLiteralToken(start - countLeadingTrivia, end + countTrailingTrivia,
                    countLeadingTrivia, countTrailingTrivia, type, hasSuffix, value);
        }
        return token;
    }


    private int countIndentationLength(int pos) {
        int count = 0;
        while (pos < codePoints.length - 1 && (codePoints[pos] == ' ' || codePoints[pos] == '\t')) {
            count += codePoints[pos] == '\t' ? 2 : 1;
            pos++;
        }
        return count;
    }

    private void updateIndentationLevel(int level) {
        prevIndentationLevel = curIndentationLevel;
        curIndentationLevel = level;
    }

    private void incrementIndentationLevel() {
        prevIndentationLevel = curIndentationLevel;
        curIndentationLevel++;
    }


    private void calculateIndentation() {
        if (nextPos + 1 >= codePoints.length) {
            updateIndentationLevel(0);
            resetIndentation();
            return;
        }
        if (codePoints[nextPos + 1] == '\n') return;
        int count = countIndentationLength(nextPos + 1);
        if (count == 0 || nextPos + 1 + count + 1 >= codePoints.length) {
            updateIndentationLevel(0);
            resetIndentation();
            return;
        }
        if (count % 2 != 0) return;

        if (curIndentationLevel == 0) {
            incrementIndentationLevel();
            indentationLength = count;
            resetIndentation();
            return;
        }

        assert indentationLength != 0;
        if (count % indentationLength != 0) return;

        updateIndentationLevel(count / indentationLength);
        resetIndentation();
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
            updateIndentationLevel(0);
            resetIndentation();
        }
    }

    private Token getStringLiteralToken() {
        end = nextPos;
        start = nextPos - (int) symbolBuffer.codePoints().count() - 1;
        return new StringLiteralToken(start - countLeadingTrivia, end + countTrailingTrivia,
                countLeadingTrivia, countTrailingTrivia, symbolBuffer.toString());
    }


    private Token getRuneLiteralToken() {
        end = nextPos;
        start = nextPos - (int) symbolBuffer.codePoints().count() - 1;
        String rune = symbolBuffer.toString();
        if (isRune(rune))
            return new RuneLiteralToken(start - countLeadingTrivia, end + countTrailingTrivia,
                    countLeadingTrivia, countTrailingTrivia, rune.codePointAt(0));
        return null;
    }

    public List<Token> tokenize() {
        while (++nextPos < codePoints.length) {
            String nextSymbol = getNextSymbol();
            Token token = null;
            switch (nextSymbol) {
                case "#" -> {
                    countLeadingTrivia++;
                    curState = COMMENTARY;
                }
                case "\n" -> {
                    if (curState.equals(RUNE) || curState.equals(STRING)) {
                        addNext();
                        break;
                    }
                    if (curState.equals(COMMENTARY)) {
                        countLeadingTrivia += symbolBuffer.length();
                        resetBuffer();
                    }
                    if (hasLexeme()) {
                        countTrailingTrivia++;
                        addToken(getLiteralToken());
                    }
                    else countLeadingTrivia++;
                    curState = INDENTATION;
                    calculateIndentation();
                }
                case " ", "\t", "\r" -> {
                    if (curState.equals(COMMENTARY) || curState.equals(STRING) || curState.equals(RUNE)) {
                        addNext();
                        break;
                    }
                    if (hasLexeme()) {
                        countTrailingTrivia++;
                        addToken(getLiteralToken());
                    }
                    else countLeadingTrivia++;
                }
                case "=", "<", ">", ".", ",", ":", "-", "+", "*", "/", "%", "!", "~", "&", "|", "^", "[", "]", "(", ")",
                     "?" -> {
                    if (curState.equals(COMMENTARY) || curState.equals(STRING) || curState.equals(RUNE)) {
                        addNext();
                        break;
                    }
                    if (hasLexeme()) addToken(getLiteralToken());
                    addNext();
                    addToken(tokenizeSymbol());
                }
                case "'" -> {
                    if (curState.equals(COMMENTARY)) {
                        addNext();
                        break;
                    }
                    if (curState.equals(RUNE)) {
                        addToken(getRuneLiteralToken());
                        curState = DEFAULT;
                        break;
                    }
                    curState = RUNE;
                }
                case "\"" -> {
                    if (curState.equals(COMMENTARY)) {
                        addNext();
                        break;
                    }
                    if (curState.equals(STRING)) {
                        addToken(getStringLiteralToken());
                        curState = DEFAULT;
                        break;
                    }
                    curState = STRING;
                }
                default -> addNext();
            }
        }
        if (hasLexeme()) addToken(getLiteralToken());
        resetIndentationAtTheEnd();
        return tokens;
    }


    @Override
    public List<Token> lex(String s) {
        initLexer(s);
        return tokenize();
    }
}



