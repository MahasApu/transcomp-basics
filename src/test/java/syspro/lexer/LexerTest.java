package syspro.lexer;

import org.junit.jupiter.api.Test;
import syspro.parser.Parser;
import syspro.tm.lexer.Token;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

import static javax.lang.model.SourceVersion.isIdentifier;
import static org.junit.jupiter.api.Assertions.*;
import static syspro.lexer.utils.UnicodeReader.codePointToString;


class LexerTest {

    @Test
    void moreOrEqualChar() {
        //Supplementary character
        String strTest = """
                !
                >=fg
                
                """;
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void number() {
        //Supplementary character
        String strTest = """
                462 9 9u u
                """;
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(t -> System.out.println(t.toString()));
    }


    @Test
    void test1() {
        //Supplementary character
        String s = """
                class Indent1
                    def notMultipleOf2(): Boolean
                      return true""";
        Lexer lexer = new Lexer();
        lexer.lex(s).forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void testForNumRegex() {
        String regex = "(^\\p{Nd}+(i64|i32|u32|u64)?)$";

        assertFalse(Pattern.compile(regex).matcher("4774i640").matches());
        assertFalse(Pattern.compile(regex).matcher("4774i").matches());
        assertTrue(Pattern.compile(regex).matcher("4774i64").matches());
        assertTrue(Pattern.compile(regex).matcher("4774").matches());
    }


    @Test
    void isNumber() {
        //Supplementary character
        String strTest = """
                355453 34743 3939
                """;
        Lexer lexer = new Lexer();
//        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
    }


    @Test
    void testForIdentifier() {
        String s = "_ddgdh";
        System.out.println(Pattern.compile("[\\p{L}\\p{Nl}|_]+[\\p{L}\\p{Nl}|\\p{Nd}\\p{Mn}\\p{Mc}\\p{Cf}]*").matcher(s).matches());
    }


    @Test
    void test2() {
        String s = """
                class Indent9	val x = 42
                """;
        Lexer lexer = new Lexer();
        lexer.lex(s).forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void test3() {
        String RUNE_CHAR = "'";
        String SHORT_ESCAPE = """
                \\[0abfnrtv'\\]""";
        String UNICODE_ESCAPE = """
                [\\\\U+]*[0-9A-F]{4,5}""";
        String ESCAPE = String.format("(%s|%s)", SHORT_ESCAPE, UNICODE_ESCAPE);
        String SIMPLE_RUNE_CHARACTER = """
                [^'\\\r\n]""";
        String RUNE_CHARACTER = String.format("(%s|%s)", SIMPLE_RUNE_CHARACTER, ESCAPE);
        String RUNE = String.format("%s%s%s", RUNE_CHAR, RUNE_CHARACTER, RUNE_CHAR);


        String g = "^[\\'\\r\\n]|[['\\0' | '\\a' | '\\b' | '\\r' | '\\n' | '\\t' | '\\v' | '\\'' | '\"' | '\\\\']|['\\\\U+'[0-9A-F]{4,5}]";
        String s = "'\\U+56789'";
        String d = "'\\U+56789\\U+6464";
        System.out.println(Pattern.compile(RUNE).matcher(d).matches());
    }

    @Test
    void test4() {
        String strTest = """
                class Indent1
                   def notMultipleOf2(): Boolean
                      return true""";

        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }


    @Test
    void test5() {
        String strTest = """
                \n
                  class Indent8
                val x = 42
                """;

        Lexer lexer = new Lexer();
        for (Token t : lexer.lex(strTest)) {
            System.out.printf("%s   %d %d\n", t.toString(), t.start, t.end);
        }
    }

    @Test
    void test6() {
        String strTest = """
                                class 𝚨𐍁
                                    def nameImplicit(): String
                                        return "𝚨­𐍁"
                                    def nameExͯplicit(): String
                                        return "\\U+1D6A8\\U+00AD\\U+10341"
                                    def letterImplicit(): Rune
                                        return '𝚨'
                                    def letterExͯplicit(): Rune
                                        return '\\U+1D6A8'
                                    def number﻿Value(): Int64
                                        return 90
                                    def numberImplicit(): Rune
                                        return '𐍁'
                                    def numberExͯplicit(): Rune
                                        return '\\U+10341'
                """;
//        System.out.println(is("𝚨𐍁"));
        System.out.println();
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }

    int nextPos = 3;
    String s = "aaa    # sss";
    int[] codePoints = s.codePoints().toArray();

    public int processTrailingTrivia(int nextPos) {
        int pos = nextPos - 1;
        int count = 0;
        if (pos + 1 >= codePoints.length) return count;
        if (codePoints[pos + 1] == '#') {
            while (pos++ < codePoints.length || codePoints[pos] != '\n') count++;
        }
        while (pos++ < codePoints.length && (codePoints[pos] == ' ' || codePoints[pos] == '\t' || codePoints[pos] == '\r' || codePoints[pos] == '\n')) {
            count++;
        }
        if (pos >= codePoints.length || codePoints[pos] != '#') return count;
        return count + processTrailingTrivia(pos);
    }


    @Test
    void test0() {
        System.out.println(processTrailingTrivia(3));
    }

    @Test
    void test7() {
        String strTest = """
                class Indent5
                  def memberIsAt2(): Boolean
                    return true
                      # Comment introduced identation level in the method body (EOF rule is not applicable here)""";
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }


    @Test
    void test23() {

        String strTest =
                """
                        class Indent1
                           def notMultipleOf2(): Boolean
                              return true
                        """;
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }

    @Test
    void test8() {

        String strTest = """
                class Indent5
                  def memberIsAt2(): Boolean
                    return true
                      # Comment introduced identation level in the method body (EOF rule is not applicable here)""";
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }

    @Test
    void test9() throws IOException {
        String strTest0 = "class Indent6\n" +
                "  def memberIsAt2(): Boolean\n" +
                "    return true | false\n" +
                "    # The spaces in the following line are ignored for identation purposes,\n" +
                "    # as per EOF rule\n";
        String strTest1 = "class Bad1\n    val x = €\n";
        String strTest2 = "class Bad1\n    val x = €      val";

        Parser parser = new Parser();
        parser.parse(strTest0);
    }

    @Test
    void continueTest() throws IOException {
        String strTest = "continue";

        Parser parser = new Parser();
        parser.parse(strTest);
    }



}