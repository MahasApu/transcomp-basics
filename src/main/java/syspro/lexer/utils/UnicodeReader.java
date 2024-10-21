package syspro.lexer.utils;

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
}
