package syspro.parser;

import syspro.parser.ast.ASTNode;
import syspro.parser.diagnostics.IndentationError;
import syspro.parser.diagnostics.SyntaxError;
import syspro.tm.lexer.IdentifierToken;
import syspro.tm.lexer.Keyword;
import syspro.tm.lexer.KeywordToken;
import syspro.tm.lexer.Token;
import syspro.tm.parser.*;
import syspro.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.parser.SyntaxKind.*;

public class ParserContext {

    public Logger logger;

    public List<Token> tokens;
    public int pos = -1;
    private List<TextSpan> invalidRanges = new ArrayList<>();
    private List<Diagnostic> diagnostics = new ArrayList<>();


    // variables for diagnostics
    private int lineInText = 1;
    private int lastTextPos = -1;
    private final String inputText;

    public boolean isEOF() {
        return pos >= tokens.size();
    }

    ParserContext(List<Token> tokens, Logger logger, String inputText) {
        this.logger = logger;
        this.tokens = tokens;
        this.inputText = inputText;
    }

    public boolean is(AnySyntaxKind... kind) {
        if (isEOF()) return false;
        for (AnySyntaxKind k : kind) {
            if (kind().equals(k)) return true;
        }
        return false;
    }

    public AnySyntaxKind kind() {
        if (isEOF()) return null;
        return get().toSyntaxKind();
    }

    public boolean match(AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (is(kind)) {
                step();
                return true;
            }
        }
        return false;
    }

    public Token get() {
        if (isEOF()) return prev();
        return tokens.get(pos);
    }

    public void addInvalidRange() {
        ErrorCode error = getError();

        int start = get().start;
        int last = getInvalidTokenEnd();

        TextSpan textSpan = new TextSpan(start, last - start);

        invalidRanges.add(textSpan);
        diagnostics.add(new Diagnostic(
                new DiagnosticInfo(error, null),
                textSpan,
                null
        ));
    }


    public void addInvalidRange(int start, int end) {

        ErrorCode error = getError();
        TextSpan textSpan = new TextSpan(start, end - start);

        invalidRanges.add(textSpan);
        diagnostics.add(new Diagnostic(
                new DiagnosticInfo(error, null),
                textSpan,
                null
        ));
    }


    public void addInvalidRange(int start, int end, String msg) {

        TextSpan textSpan = new TextSpan(start, end - start);

        int lineNum = getLineNumber();
        ErrorCode error = () -> (msg + " in line " + lineNum + ".");

        invalidRanges.add(textSpan);
        diagnostics.add(new Diagnostic(
                new DiagnosticInfo(error, null),
                textSpan,
                null
        ));
    }

    public void addInvalidRange(String msg) {

        step();
        int lineNum = getLineNumber();
        pos--;
        ErrorCode error = () -> msg + lineNum + ".";

        int start = get().start;
        int last = getInvalidTokenEnd();

        TextSpan textSpan = new TextSpan(start, last - start);


        invalidRanges.add(textSpan);
        diagnostics.add(new Diagnostic(
                new DiagnosticInfo(error, null),
                textSpan,
                null
        ));
    }

    private ErrorCode getError() {
        return switch (get().toSyntaxKind()) {
            case DEF -> new IndentationError("incorrect indentation in line " + (getLineNumber() + 1) + ".");
            case INDENT, DEDENT -> new IndentationError("unexpected indentation in line " + (getLineNumber() + 1) + ".");
            case IDENTIFIER -> new SyntaxError("invalid name in line " + getLineNumber() + ".");
            default -> new SyntaxError("unexpected token: " + get().toString() + ". In line " + getLineNumber() + ".");
        };

    }

    public Token step() {
        if (!isEOF()) pos++;
        return prev();
    }

    public Token prev() {
        return tokens.get(pos - 1);
    }

    private int getLineNumber() {
        int end = get().start;
        while (++lastTextPos < end) {
            if (inputText.charAt(lastTextPos) == '\n')
                lineInText++;
        }
        return lineInText;

    }

    public ASTNode expected(String msg, AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (is(kind))
                return new ASTNode(kind, step());
        }

        int start = get().start;
        int last = get().end;

        String errorKind = switch (kinds[0]) {
            case INDENT, DEDENT -> "IndentationError: ";
            default -> "SyntaxError: ";
        };

        Token cur = get();
        int lineNum = getLineNumber();
        ErrorCode error = () -> errorKind + msg +
                " Found: " + cur.toString() + ". In line " + lineNum + ".";
        TextSpan textSpan = new TextSpan(start, last - start);

        invalidRanges.add(textSpan);
        diagnostics.add(new Diagnostic(
                new DiagnosticInfo(error, null),
                textSpan,
                null
        ));
        return null;
    }


    public void updateTokenKind(Keyword kind) {
        if (kind.equals(NULL)) return;
        Token newToken = switch (get()) {
            case IdentifierToken t ->
                    new KeywordToken(t.start, t.end, t.leadingTriviaLength, t.trailingTriviaLength, kind);
            default -> get();
        };
        tokens.set(pos, newToken);
    }

    public int getInvalidEnd() {
        int start = pos;
        AnySyntaxKind kind = switch (kind()) {
            case DEDENT -> INDENT;
            default -> DEDENT;
        };

        while (!isEOF() && !kind().equals(kind))
            step();
        if (isEOF()) pos--;

        return pos - start;
    }

    public int getInvalidTokenEnd() {
        getInvalidEnd();
        if (isEOF()) return prev().end;
        return get().end;
    }

    public List<TextSpan> getInvalidRanges() {
        return invalidRanges;
    }

    public boolean definitionStarts() {
        return switch (kind()) {
            case VAL, VAR, ABSTRACT, VIRTUAL, OVERRIDE, NATIVE, DEF -> true;
            default -> false;
        };
    }

    public boolean typeDefinitionStarts() {
        if (isEOF()) return false;
        return switch (get().toString()) {
            case "class", "object", "interface" -> true;
            default -> false;
        };
    }


    public boolean statementStarts() {
        if (isEOF()) return false;
        if (typeDefinitionStarts()) return false;
        return switch (kind()) {
            case THIS, SUPER, IDENTIFIER, VAL, VAR,
                 BREAK, RETURN, CONTINUE, IF, WHILE, FOR -> true;
            default -> false;
        };
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

}
