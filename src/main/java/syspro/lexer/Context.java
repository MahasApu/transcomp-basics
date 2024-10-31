package syspro.lexer;

import syspro.tm.lexer.Token;

import java.util.ArrayList;
import java.util.Objects;

import static syspro.lexer.State.ObservedState.DEFAULT;
import static java.util.Objects.nonNull;
import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodePattern.*;
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

    public void resetContext() {
        this.curState = DEFAULT;
        symbolBuffer = new StringBuilder();
        tokens = new ArrayList<Token>();
    }

    public void initLexer(String source) {
        countTrailingTrivia = countLeadingTrivia = 0;
        this.symbolBuffer = new StringBuilder();
        this.tokens = new ArrayList<>();
        this.codePoints = getUnicodePoints(source);
        this.curState = DEFAULT;
        this.nextPos = -1;
    }

    public void resetBuffer() {
        symbolBuffer = new StringBuilder();
    }

    public void addNext() {
        symbolBuffer.append(getNextSymbol());
    }

    public boolean hasLexeme() {
        return !symbolBuffer.isEmpty();
    }

    public String getSymbol(int pos) {
        assert pos < codePoints.length;
        return codePointToString(codePoints[pos]);
    }

    public String getNextSymbol() {
        assert nextPos < codePoints.length;
        return codePointToString(codePoints[nextPos]);
    }


    public String nextCodePoint(int pos) {
        if (pos + 1 == codePoints.length) return null;
        return codePointToString(codePoints[pos + 1]);
    }

    public boolean nextIs(String s) {
        return Objects.equals(nextCodePoint(nextPos), s);
    }

    void addToken(Token token) {
        if (Objects.isNull(token)) {System.out.println("KJJJJJJJJJJJJJJ");}
        if (nonNull(token)) {
            tokens.add(token);
            symbolBuffer = new StringBuilder();
            start = end = nextPos;
        }
        if (curState.equals(INDENTATION)) return;
        countTrailingTrivia = countLeadingTrivia = 0;
    }


}
