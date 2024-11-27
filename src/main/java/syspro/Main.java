package syspro;
import syspro.lexer.Lexer;
import syspro.parser.Parser;
import syspro.tm.Tasks;
import syspro.tm.WebServer;
import syspro.tm.lexer.TestLineTerminators;
import syspro.tm.lexer.TestMode;

import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
//        Lexer lexer = new Lexer();
//        Tasks.Lexer.registerSolution(lexer, new TestMode().strict(true).forceLineTerminators(TestLineTerminators.Mixed));
        WebServer.start();
        Parser parser = new Parser();
        Tasks.Parser.registerSolution(parser);
        WebServer.waitForWebServerExit();
    }
}
