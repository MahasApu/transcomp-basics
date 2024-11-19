package syspro.parser;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.utils.Logger;

import java.util.List;

public class ParserContext {

    public Logger logger;

    public SysproParseResult result;
    public List<Token> tokens;
    public int pos;


    public boolean isEOF() {
        return pos >= tokens.size();
    }

    ParserContext(List<Token> tokens, Logger logger) {
        this.logger = logger;
        this.tokens = tokens;
    }

    public boolean is(AnySyntaxKind kind) {
        if (isEOF()) return false;
        return tokens.get(pos).toSyntaxKind().equals(kind);
    }

    public AnySyntaxKind kind() {
        assert !isEOF();
        return get().toSyntaxKind();
    }

    public boolean match(AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (is(kind)) {
                next();
                return true;
            }
        }
        return false;
    }


    public Token get() {
        return tokens.get(pos);
    }

    public Token look() {
        if (!isEOF()) return get();
        return null;
    }

    public void next() {
        if (!isEOF()) pos++;
        prev();
    }

    public Token prev() {
        return tokens.get(pos - 1);
    }


}
