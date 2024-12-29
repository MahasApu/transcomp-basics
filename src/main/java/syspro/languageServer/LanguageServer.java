package syspro.languageServer;

import syspro.parser.Parser;
import syspro.parser.ast.ASTNode;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticModel;

public class LanguageServer implements syspro.tm.symbols.LanguageServer {

    private SyntaxNode interpret(SyntaxNode tree) {
        for (int i = 0; i < tree.slotCount(); i++) {
            tree.slot(i);
        }
        return null;
    }


    int a = -1;


    @Override
    public SemanticModel buildModel(String code) {

        Parser parser = new Parser();
        ParseResult result = parser.parse(code);
        SyntaxNode tree = result.root();

        interpret(tree);


        return null;
    }
}
