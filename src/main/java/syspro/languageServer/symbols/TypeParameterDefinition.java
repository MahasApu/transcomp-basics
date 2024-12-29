package syspro.languageServer.symbols;

import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeParameterDefinition implements SyntaxNodeWithSymbols {
    private final SemanticSymbol symbol;
    private final AnySyntaxKind kind;
    private final List<ASTNode> slots;
    private final Token token;


    public TypeParameterDefinition(SemanticSymbol symbol, AnySyntaxKind kind, Token token, ASTNode... slots) {
        this.symbol = symbol;
        this.kind = kind;
        this.slots = new ArrayList<>(Arrays.asList(slots));
        this.token = token;
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
