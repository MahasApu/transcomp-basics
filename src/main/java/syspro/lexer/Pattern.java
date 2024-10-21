package syspro.lexer;

import static syspro.lexer.Pattern.UnicodeSymbols.*;

public class Pattern {
    enum UnicodeSymbols {
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

    abstract static class RegularExpression {
        abstract String getRegex();
    }

    static class IdentifierRegex extends RegularExpression {

        @Override
        String getRegex() {
            String idContinue = String.format("[%s]|[%s%s%s%s]", LETTER_CHARACTER, NUMBER_LITERAL, NON_SPACING_MARK, SPACING_MARK, FORMAT);
            String idStart = String.format("[%s]|%s", LETTER_CHARACTER, UNICODE_SCORE);
            return idStart + idContinue + "*";
        }
    }

    static class BooleanRegex extends RegularExpression {

        @Override
        String getRegex() {
            return "/\\btrue\\b|\\bfalse\\b/gm";
        }
    }

    static class TriviaRegex extends RegularExpression {

        @Override
        String getRegex() {
            return null;
        }
    }

    static class IntegerRegex extends RegularExpression {

        @Override
        String getRegex() {
            return null;
        }
    }

    static class RuneRegex extends RegularExpression {

        @Override
        String getRegex() {
            return null;
        }
    }

    static class StringLetterRegex extends RegularExpression {

        @Override
        String getRegex() {
            return null;
        }
    }
}
