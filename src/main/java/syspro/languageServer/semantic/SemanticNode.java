package syspro.languageServer.semantic;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

import java.util.List;

public class SemanticNode implements SyntaxNodeWithSymbols {
    private final AnySyntaxKind kind;
    private final List<SemanticNode> slots;
    private final Token token;


    public SemanticNode(AnySyntaxKind kind, List<SemanticNode> slots, Token token) {
        this.kind = kind;
        this.slots = slots;
        this.token = token;
    }

    @Override
    public AnySyntaxKind kind() {
        return kind;
    }

    @Override
    public int slotCount() {
        return slots.size();
    }

    @Override
    public SyntaxNode slot(int index) {
        return slots.get(index);
    }

    @Override
    public Token token() {
        return token;
    }
}
