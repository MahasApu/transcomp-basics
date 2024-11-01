package syspro.lexer.utils;

import java.util.regex.Pattern;

import static syspro.lexer.utils.UnicodePattern.UnicodeSymbols.*;
import static syspro.lexer.utils.UnicodeReader.codePointToString;

public class UnicodePattern {

    public enum UnicodeSymbols {
        NUMBER_LITERAL("\\p{Nd}"),
        PUNCTUATION("\\p{Pc}"),
        NON_SPACING_MARK("\\p{Mn}"),
        SPACING_MARK("\\p{Mc}"),
        FORMAT("\\p{Cf}"),
        LETTER_CHARACTER("[\\p{L}\\p{Nl}]"),
        UNICODE_SCORE("_");

        public final String text;

        UnicodeSymbols(String text) {
            this.text = text;
        }
    }

    static private boolean isLetterCharacter(String s) {
        return Pattern.compile(LETTER_CHARACTER.text).matcher(s).matches();
    }

    static private boolean isPunctuation(String s) {
        return Pattern.compile(PUNCTUATION.text).matcher(s).matches();
    }

    static private boolean isNumberLiteral(String s) {
        return Pattern.compile(NUMBER_LITERAL.text).matcher(s).matches();
    }

    static private boolean isNoneSpacingMark(String s) {
        return Pattern.compile(NON_SPACING_MARK.text).matcher(s).matches();
    }

    static private boolean isSpacingMark(String s) {
        return Pattern.compile(SPACING_MARK.text).matcher(s).matches();
    }

    static private boolean isFormat(String s) {
        return Pattern.compile(FORMAT.text).matcher(s).matches();
    }

    static private boolean isUnicodeScore(String s) {
        return Pattern.compile(UNICODE_SCORE.text).matcher(s).matches();
    }

    static public boolean isIdentifierStart(String s) {
        if (s.isEmpty()) return false;
        String firstSymbol = codePointToString(s.codePoints().toArray()[0]);
        return isLetterCharacter(firstSymbol) || firstSymbol.equals("_");
    }

    static public boolean isIdentifierContinue(String s) {
        assert s.length() == 1;
        String regex = "[\\p{L}\\p{Nl}|\\p{Nd}\\p{Mn}\\p{Mc}\\p{Cf}]";
        return Pattern.compile(regex).matcher(s).matches();
    }


    static public boolean isKeyword(String s) {
        return UtilMaps.keywordMap.containsKey(s);
    }

    static public boolean isBoolean(String s) {
        return s.equals("true") || s.equals("false");
    }


    static public boolean isIdentifier(String s) {
        String regex = "[\\p{L}\\p{Nl}|_-]+[\\p{L}\\p{Nl}|\\p{Nd}\\p{Mn}\\p{Mc}\\p{Cf}]*";
        return Pattern.compile(regex).matcher(s).matches();
    }

    static public boolean isNumber(String s) {
        return Pattern.compile("(^\\p{Nd}+(i64|i32|u32|u64)?)$").matcher(s).matches();
    }

    static public boolean isDigit(String s) {
        return isNumberLiteral(s);
    }

    static public boolean isRune(String s) {
        String SHORT_ESCAPE = "\\[0abfnrtv'\\]";
        String UNICODE_ESCAPE = "[\\\\U+]*[0-9A-F]{4,5}";
        String ESCAPE = String.format("(%s|%s)", SHORT_ESCAPE, UNICODE_ESCAPE);
        String SIMPLE_RUNE_CHARACTER = "[^'\\\r\n]";
        String RUNE_CHARACTER = String.format("(%s|%s)", SIMPLE_RUNE_CHARACTER, ESCAPE);
        return Pattern.compile(RUNE_CHARACTER).matcher(s).matches();
    }


}
