package syspro.lexer;
import syspro.tm.Tasks;
import syspro.tm.lexer.TestLineTerminators;
import syspro.tm.lexer.TestMode;

public class Main {

    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        Tasks.Lexer.registerSolution(lexer, new TestMode().forceLineTerminators(TestLineTerminators.Mixed));
    }
}
