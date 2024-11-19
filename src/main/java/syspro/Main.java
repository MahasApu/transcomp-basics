package syspro;
import syspro.parser.Parser;
import syspro.tm.Tasks;

import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
//        Lexer lexer = new Lexer();
//        Tasks.Lexer.registerSolution(lexer, new TestMode().forceLineTerminators(TestLineTerminators.Mixed));
        Parser parser = new Parser();
        Tasks.Parser.registerSolution(parser);
    }
}
