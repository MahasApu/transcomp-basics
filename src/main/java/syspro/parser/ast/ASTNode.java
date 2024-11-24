package syspro.parser.ast;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static syspro.tm.lexer.Keyword.NULL;

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
        List<ASTNode> list = new ArrayList<>();
        for (ASTNode node : slots) {
            if (Objects.isNull(node)) list.add(new ASTNode(NULL, null));
            else list.add(node);
        }
        return list;
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
