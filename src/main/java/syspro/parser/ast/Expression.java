package syspro.parser.ast;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;

public class Expression implements SyntaxNode {

    AnySyntaxKind kind;

    public Expression(AnySyntaxKind kind) {
        this.kind = kind;
    }

    @Override
    public AnySyntaxKind kind() {
        return null;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public SyntaxNode slot(int index) {
        return null;
    }

    @Override
    public Token token() {
        return null;
    }
}
