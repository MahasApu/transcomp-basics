package syspro.lexer;

import org.junit.jupiter.api.Test;
import syspro.tm.lexer.Token;

import java.util.Objects;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;


class LexerTest {

    @Test
    void supplementaryChar() {
        //Supplementary character
        String strTest = """
                a𐐀
                
                """;
        Lexer lexer = new Lexer();
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
    }

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
    void identifier() {
        //Supplementary character
        String strTest = """
                Ident9
                
                """;
        Lexer lexer = new Lexer();
        lexer.tokenize();
    }

    @Test
    void isMatched() {
        //Supplementary character
        String strTest = "Ident9";
        Lexer lexer = new Lexer();
        System.out.println(lexer.isMatched("Ident9"));
    }

    @Test
    void isIdentifier() {
        //Supplementary character
        String strTest = ">";
        Lexer lexer = new Lexer();
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
        Lexer lexer = new Lexer();
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
        lexer.tokenize();
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
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
    }

    @Test
    void unicodeEscapes() {
        //Supplementary character
        String strTest = """
                \\a \\b
                """;

        System.out.println(strTest);
        Lexer lexer = new Lexer();
        lexer.tokenize().forEach(t -> System.out.println(t.toString()));
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
class Indent7
  def memberIsAt2(): Boolean
    return true
    if true
        # All identation levels are closed per EOF rule
                """;

        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }


    @Test
    void test5() {
        String strTest = """
  
    DD
  class Indent8
    val x = 42
""";

        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }

    @Test
    void test6() {
        String strTest = """
class 𝚨𐍁
    def nameImplicit(): String
        return "𝚨𐍁"
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
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }

    @Test
    void test7() {
        String strTest = """

    def next(): T
        if !hasNext()
            System.failFast("No next element is available in ArrayList<T>")
        val result = _list[_index] # Will failFast if necessary
        _index = _index + 1u64
        return result
        if _size == _data.length
            _data = _data.clone(_size * 2, PrimitiveIntrinsics<T>.default())
        _data[_size] = item
        _size = _size + 1u64
    def subscript(index: UInt64): T
        if index >= _size
            System.failFast("List element read out of bounds")
        return _data[index]
    def subscript(index: UInt64, value: T)
        if index >= _size
            System.failFast("List element write out of bounds")
        _data[index] = value
    def iterator(): Iterator<T>
        return ArrayListIterator<T>(this)
    def toArray(): Array<T>
        return _data.clone(_size, PrimitiveIntrinsics<T>.default())""";
        Lexer lexer = new Lexer();
        lexer.lex(strTest).forEach(System.out::println);
    }
}