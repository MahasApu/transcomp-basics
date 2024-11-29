package syspro.parser;

import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.IdentifierToken;
import syspro.tm.lexer.Keyword;
import syspro.tm.lexer.KeywordToken;
import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.TextSpan;
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

    public boolean isEOF() {
        return pos >= tokens.size();
    }

    ParserContext(List<Token> tokens, Logger logger) {
        this.logger = logger;
        this.tokens = tokens;
    }

    public boolean is(AnySyntaxKind... kind) {
        if (isEOF()) return false;
        for (AnySyntaxKind k : kind) {
            if (kind().equals(k)) return true;
        }
        return false;
    }

    public AnySyntaxKind kind() {
        assert !isEOF();
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
        if (isEOF()) return null;
        return tokens.get(pos);
    }

    public void addInvalidRange(TextSpan textSpan) {
        invalidRanges.add(textSpan);
    }

    public Token step() {
        if (!isEOF()) pos++;
        return prev();
    }

    public Token prev() {
        return tokens.get(pos - 1);
    }


    public ASTNode expected(String msg, AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (is(kind))
                return new ASTNode(kind, step());
        }
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

        return pos - start;
    }

    public int getInvalidTokenEnd() {
        getInvalidEnd();
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
        if (typeDefinitionStarts()) return false;

        return switch (kind()) {
            case THIS, SUPER, IDENTIFIER, VAL, VAR,
                 BREAK, RETURN, CONTINUE, IF, WHILE, FOR -> true;
            default -> false;
        };
    }
}
