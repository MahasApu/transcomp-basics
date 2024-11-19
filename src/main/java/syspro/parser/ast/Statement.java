package syspro.parser.ast;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;

import java.util.List;

public class Statement implements SyntaxNode {

    final AnySyntaxKind kind;
    List<SyntaxNode> slot;

    public Statement(AnySyntaxKind kind) {
        this.kind = kind;
    }

    @Override
    public AnySyntaxKind kind() {
        return kind;
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
