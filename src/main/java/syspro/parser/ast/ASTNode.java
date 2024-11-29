package syspro.parser.ast;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ASTNode implements SyntaxNode {

    private final AnySyntaxKind kind;
    private final List<ASTNode> slots;
    private final Token token;


    public ASTNode(AnySyntaxKind kind, Token token, ASTNode... slots) {
        this.kind = kind;
        this.slots = reduceNulls(slots);
        this.token = token;
    }

    private List<ASTNode> reduceNulls(ASTNode[] slots) {
        return new ArrayList<>(Arrays.asList(slots));
    }

    public ASTNode(AnySyntaxKind kind, Token token, List<ASTNode> slots) {
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
