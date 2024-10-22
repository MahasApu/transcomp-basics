package syspro.lexer;
import syspro.tm.Tasks;

public class Main {

    public static void main(String[] args) {

//        for (String str: args) System.out.print(str);

//        for (String inputLine : args) {
        Lexer lexer = new Lexer("inputLine");
        Tasks.Lexer.registerSolution(lexer);

//            lexer.tokenize();

    }
}
