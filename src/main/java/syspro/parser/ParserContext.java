package syspro.parser;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxKind;

import java.util.List;

public class ParserContext {


    public SysproParseResult result;
    public List<Token> tokens;
    public int pos;


    public boolean isEOF() {
        return pos >= tokens.size();
    }

    ParserContext(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean is(SyntaxKind kind) {
        if(isEOF()) return false;
        return tokens.get(pos).toSyntaxKind().equals(kind);
    }

    public boolean match(SyntaxKind... kinds) {
        for (SyntaxKind kind : kinds) {
            if (is(kind)) {
                next();
                return true;
            }
        }
        return false;
    }

    public Token next() {
        if (!isEOF()) pos++;
        return prev();
    }

    public Token prev() {
        return tokens.get(pos - 1);
    }

    public Token get() {
        return tokens.get(pos);
    }

    public AnySyntaxKind kind() {
        assert !isEOF();
        return tokens.get(pos).toSyntaxKind();
    }
}
