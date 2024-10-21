package syspro.lexer;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;


class LexerTest {

    @Test
    void supplementaryChar() {
        //Supplementary character
        String strTest = """
                a𐐀
                
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize();
    }

    @Test
    void moreOrEqualChar() {
        //Supplementary character
        String strTest = """
                >=
                
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void number() {
        //Supplementary character
        String strTest = """
                462
                
                9   6
                """;
        Lexer lexer = new Lexer(strTest);
        lexer.tokenize();
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
    void ismatched() {
        //Supplementary character
        String strTest = "Ident9";
        Lexer lexer = new Lexer(strTest);
        System.out.println(lexer.isMatched("Ident9"));
    }
}