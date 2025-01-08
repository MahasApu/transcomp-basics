package syspro.parser.ast;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ASTNode implements SyntaxNodeWithSymbols {

    private AnySyntaxKind kind;
    private List<SyntaxNode> slots;
    private Token token;
    private SemanticSymbol symbol = null;


    public ASTNode(AnySyntaxKind kind, Token token, SyntaxNode... slots) {
        this.kind = kind;
        this.slots = new ArrayList<>(Arrays.asList(slots));
        this.token = token;
    }

    public ASTNode(AnySyntaxKind kind, Token token, List<SyntaxNode> slots) {
        this.kind = kind;
        this.slots = slots;
        this.token = token;
    }

    public void updateSymbol(SemanticSymbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public SemanticSymbol symbol() {
        return symbol;
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
