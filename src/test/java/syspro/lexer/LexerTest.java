package syspro.lexer;

import org.junit.jupiter.api.Test;
import syspro.tm.lexer.Token;

import java.util.Objects;


class LexerTest {

    @Test
    void supplementaryChar() {
        //Supplementary character
        String strTest = """
                a𐐀
                
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void moreOrEqualChar() {
        //Supplementary character
        String strTest = """
                !
                >=fg
                
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void number() {
        //Supplementary character
        String strTest = """
                462
                #
                9   6
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void identifier() {
        //Supplementary character
        String strTest = """
                Ident9
                
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize();
    }

    @Test
    void isMatched() {
        //Supplementary character
        String strTest = "Ident9";
        Lexer lexer = new Lexer(strTest);
        System.out.println(lexer.isMatched("Ident9"));
    }

    @Test
    void isIdentifier() {
        //Supplementary character
        String strTest = ">";
        Lexer lexer = new Lexer(strTest);
        Token token = lexer.tokenizeLexeme();
        if (Objects.nonNull(token))

            System.out.println(token);
        else System.out.println("nullable");
    }

    @Test
    void test1() {
        //Supplementary character
        String strTest = """
object PrimitiveIntrinsics<T>
    native def default(): T # Available for all primitive or nullable types
    # The following are valid only for numeric T
    native def one(): T
    native def add(left: T, right: T): T
    native def subtract(left: T, right: T): T
    native def multiply(left: T, right: T): T
    native def divide(left: T, right: T): T
    native def remainder(left: T, right: T): T
    native def less(left: T, right: T): T
    native def greater(left: T, right: T): T
    native def toString(num: T): String
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
            lexer.tokenize();
    }


}