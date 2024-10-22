package syspro.lexer.utils;

import static syspro.lexer.utils.UnicodePattern.UnicodeSymbols.*;

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


}
