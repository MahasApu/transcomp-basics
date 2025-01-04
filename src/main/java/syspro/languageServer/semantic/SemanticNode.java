package syspro.languageServer.semantic;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SemanticNode implements SyntaxNodeWithSymbols {
    private final SemanticSymbol symbol;
    private final AnySyntaxKind kind;
    private final List<SyntaxNode> slots;
    private final Token token;


    public SemanticNode(SemanticSymbol symbol, SyntaxNode node) {
        this.symbol = symbol;
        this.kind = node.kind();
        this.token = node.token();
        this.slots = IntStream.range(0, node.slotCount())
                .mapToObj(node::slot)
                .collect(Collectors.toList());
    }

    @Override
    public SemanticSymbol symbol() { return symbol;}

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
