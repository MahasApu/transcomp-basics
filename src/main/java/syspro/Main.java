package syspro;

import syspro.languageServer.LanguageServer;
import syspro.parser.Parser;
import syspro.tm.Tasks;
import syspro.tm.WebServer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
//        Lexer lexer = new Lexer();
//        Tasks.Lexer.registerSolution(lexer, new TestMode().strict(true).forceLineTerminators(TestLineTerminators.Mixed));
//        WebServer.start();
//        Parser parser = new Parser();
//        Tasks.Parser.registerSolution(parser);
        LanguageServer server = new LanguageServer();
        Tasks.LanguageServer.registerSolution(server);
//        WebServer.waitForWebServerExit();
    }
}
