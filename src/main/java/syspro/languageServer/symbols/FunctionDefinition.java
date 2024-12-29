package syspro.languageServer.symbols;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

public class FunctionDefinition implements SyntaxNodeWithSymbols {
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
