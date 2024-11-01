package syspro.lexer;

import syspro.lexer.utils.UtilMaps;
import syspro.tm.lexer.*;

import java.util.ArrayList;
import java.util.Objects;

import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodeReader.codePointToString;
import static syspro.lexer.utils.UnicodeReader.getUnicodePoints;

public class Context {

    public int countLeadingTrivia;
    public int countTrailingTrivia;

    public int curIndentationLevel;
    public int prevIndentationLevel;
    public int indentationLength;
    public int nextPos = -1;
    public int start;
    public int end;

    public int[] codePoints;
    public StringBuilder symbolBuffer;
    public ArrayList<Token> tokens;

    public State.ObservedState curState;


    public Context(String source) {
        this.curState = DEFAULT;
        this.symbolBuffer = new StringBuilder();
        this.tokens = new ArrayList<Token>();
        this.codePoints = getUnicodePoints(source);
    }

    void cancel() {
        nextPos--;
    }

    String getSuffix() {
        if (nextPos + 3 >= codePoints.length) return "";
        return symbol(nextPos) + symbol(nextPos + 1) + symbol(nextPos + 2);
    }

    boolean hasSuffix() {
        if (nextPos + 3 >= codePoints.length) return false;
        String suffix = getSuffix();
        return Objects.nonNull(UtilMaps.builtInTypeMap.get(suffix));
    }

    public void resetBuffer() {
        symbolBuffer = new StringBuilder();
    }

    public void putNext() {
        symbolBuffer.append(symbol());
    }

    public boolean hasLexeme() {
        return !symbolBuffer.isEmpty();
    }

    public String symbol() {
        assert nextPos < codePoints.length;
        return codePointToString(codePoints[nextPos]);
    }

    public String symbol(int pos) {
        assert pos < codePoints.length;
        return codePointToString(codePoints[pos]);
    }

    public boolean isSymbol(String string) {
        return Objects.equals(symbol(nextPos), string);
    }

    public boolean isNext(String string) {
        return Objects.equals(symbol(nextPos + 1), string);
    }


    void putToken(Token token) {
        if (Objects.isNull(token)) {
            tokens.add(new BadToken(nextPos - countLeadingTrivia, nextPos + countTrailingTrivia, countLeadingTrivia, countTrailingTrivia));
            curState = DEFAULT;
            resetBuffer();
        } else {
            tokens.add(token);
            resetBuffer();
        }
        start = end = nextPos;
        if (curState.equals(INDENTATION)) return;
        countTrailingTrivia = countLeadingTrivia = 0;
    }

    void putTrivia() {
        if (!hasLexeme()) {
            countLeadingTrivia++;
        } else {
            countTrailingTrivia++;
        }
    }


    int bufferLen() {
        return (int) symbolBuffer.codePoints().count();
    }

    int countNewlineLen() {
        if (nextPos == 0) return 0;
        return codePoints[nextPos - 1] == '\r' ? 1 : 0;
    }

    boolean isEOF(int pos) {
        return !(pos < codePoints.length);
    }

    boolean isState(State.ObservedState state) {
        return curState.equals(state);
    }

    boolean isNewline() {
        return isNewline(nextPos);
    }

    boolean isNewline(int pos) {
        assert pos < codePoints.length || pos + 1 < codePoints.length;
        return codePoints[pos] == '\n' || (codePoints[pos - 1] == '\r' && codePoints[pos] == '\n');

    }

    void updateToken() {
        int counter = tokens.size() - 1;
        while (counter - 1 >= 0 && tokens.get(counter) instanceof IndentationToken) {
            counter--;
        }
        Token token = tokens.get(counter);
        tokens.set(counter, updateToken(token));
    }

    private Token updateToken(Token tokenToUpdate) {
        return switch (tokenToUpdate) {
            case BooleanLiteralToken b ->
                    new BooleanLiteralToken(b.start, b.end + countLeadingTrivia, b.leadingTriviaLength, b.trailingTriviaLength + countLeadingTrivia, b.value);
            case IdentifierToken i ->
                    new IdentifierToken(i.start, i.end + countLeadingTrivia, i.leadingTriviaLength, i.trailingTriviaLength + countLeadingTrivia, i.value, i.contextualKeyword);
            case IntegerLiteralToken i ->
                    new IntegerLiteralToken(i.start, i.end + countLeadingTrivia, i.leadingTriviaLength, i.trailingTriviaLength + countLeadingTrivia, i.type, i.hasTypeSuffix, i.value);
            case KeywordToken k ->
                    new KeywordToken(k.start, k.end + countLeadingTrivia, k.leadingTriviaLength, k.trailingTriviaLength + countLeadingTrivia, k.keyword);
            case RuneLiteralToken r ->
                    new RuneLiteralToken(r.start, r.end + countLeadingTrivia, r.leadingTriviaLength, r.trailingTriviaLength + countLeadingTrivia, r.value);
            case StringLiteralToken s ->
                    new StringLiteralToken(s.start, s.end + countLeadingTrivia, s.leadingTriviaLength, s.trailingTriviaLength + countLeadingTrivia, s.value);
            case SymbolToken s ->
                    new SymbolToken(s.start, s.end + countLeadingTrivia, s.leadingTriviaLength, s.trailingTriviaLength + countLeadingTrivia, s.symbol);
            case BadToken b -> new BadToken(b.start, b.end + countLeadingTrivia, b.leadingTriviaLength, b.trailingTriviaLength + countLeadingTrivia);
            default -> throw new IllegalStateException("Unexpected value: " + tokenToUpdate);
        };
    }


}
