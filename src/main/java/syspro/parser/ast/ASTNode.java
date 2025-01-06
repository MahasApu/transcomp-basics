package syspro.parser.ast;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ASTNode implements SyntaxNode {

    private final AnySyntaxKind kind;
    private final List<SyntaxNode> slots;
    private final Token token;


    public ASTNode(AnySyntaxKind kind, Token token, SyntaxNode... slots) {
        this.kind = kind;
        this.slots = reduceNulls(slots);
        this.token = token;
    }

    private List<SyntaxNode> reduceNulls(SyntaxNode[] slots) {
        return new ArrayList<>(Arrays.asList(slots));
    }

    public ASTNode(AnySyntaxKind kind, Token token, List<SyntaxNode> slots) {
        this.kind = kind;
        this.slots = slots;
        this.token = token;
    }

    public void updateSlot(int index, SyntaxNode node) {
        slots.set(index, node);
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
