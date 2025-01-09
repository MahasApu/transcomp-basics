package syspro.lexer.utils;

import java.util.regex.Pattern;

public class UnicodeReader {

    static public int[] getUnicodePoints(String inputLine) {
        return inputLine.codePoints().toArray();
    }

    static public String codePointToString(int codePoint) {
        if (Character.charCount(codePoint) == 1) {
            return String.valueOf((char) codePoint);
        } else {
            return new String(Character.toChars(codePoint));
        }
    }

    static public int runeToCodePoint(String rune) {
        if (rune.startsWith("\\U+")) {
            return Integer.parseInt(rune.substring(3), 16);
        }
        return rune.codePointAt(0);
    }

    static public String runeToString(String rune) {
        if (rune.startsWith("\\U+")) {
            return codePointToString(Integer.parseInt(rune.substring(3), 16));
        }
        return rune;
    }

    static public String substituteRune(String rune) {
        return Pattern.compile("[\\\\U+]+[A-F0-9]{4,5}")
                .matcher(rune)
                .replaceAll(match -> runeToString(match.group()));
    }
}
