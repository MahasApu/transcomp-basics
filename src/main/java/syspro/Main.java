package syspro;
import syspro.parser.Parser;
import syspro.tm.Tasks;

public class Main {

    public static void main(String[] args) {
//        Lexer lexer = new Lexer();
//        Tasks.Lexer.registerSolution(lexer, new TestMode().forceLineTerminators(TestLineTerminators.Mixed));
        Parser parser = new Parser();
        Tasks.Parser.registerSolution(parser);
    }
}
