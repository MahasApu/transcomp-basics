package syspro.lexer;
import syspro.tm.Tasks;

public class Main {

    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        Tasks.Lexer.registerSolution(lexer);
    }
}
