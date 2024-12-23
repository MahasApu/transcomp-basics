package syspro.languageServer;

import syspro.parser.Parser;
import syspro.tm.parser.ParseResult;
import syspro.tm.symbols.SemanticModel;

public class LanguageServer implements syspro.tm.symbols.LanguageServer {



    int a = -1;
    @Override
    public SemanticModel buildModel(String code) {

        a++;
        if (a != 16) return null;

        Parser parser = new Parser();
        ParseResult result = parser.parse(code);


        return null;
    }
}
