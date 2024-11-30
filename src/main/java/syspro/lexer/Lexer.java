package syspro.lexer;

import syspro.lexer.utils.UnicodeReader;
import syspro.tm.lexer.*;

import java.util.List;

import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodePattern.*;
import static syspro.lexer.utils.UtilMaps.*;


public class Lexer implements syspro.tm.lexer.Lexer {


    private int countIndentationLength(LexerContext ctx, int pos) {
        int count = 0;
        while (!ctx.isEOF(pos) && (ctx.codePoints[pos] == ' ' || ctx.codePoints[pos] == '\t')) {
            count += ctx.codePoints[pos] == '\t' ? 2 : 1;
            pos++;
        }
        return count;
    }

    private void updateIndentationLevel(LexerContext ctx, int level) {
        ctx.prevIndentationLevel = ctx.curIndentationLevel;
        ctx.curIndentationLevel = level;
    }

    private void incrementIndentationLevel(LexerContext ctx) {
        ctx.prevIndentationLevel = ctx.curIndentationLevel;
        ctx.curIndentationLevel++;
    }

    private void calculateIndentation(LexerContext ctx) {
        if (ctx.isEOF(ctx.nextPos + 1)) {
            updateIndentationLevel(ctx, 0);
            resetIndentation(ctx);
            return;
        }
        if (ctx.isNewline(ctx.nextPos + 1)) return;
        int count = countIndentationLength(ctx, ctx.nextPos + 1);
        if (count == 0 || ctx.isEOF(ctx.nextPos + 1 + count)) {
            updateIndentationLevel(ctx, 0);
            resetIndentation(ctx);
            return;
        }
        if (count % 2 != 0) return;

        if (ctx.curIndentationLevel == 0) {
            incrementIndentationLevel(ctx);
            ctx.indentationLength = count;
            resetIndentation(ctx);
            return;
        }

        assert ctx.indentationLength != 0;
        if (count % ctx.indentationLength != 0) return;

        updateIndentationLevel(ctx, count / ctx.indentationLength);
        resetIndentation(ctx);
    }


    private void resetIndentation(LexerContext ctx) {
        int difference = ctx.curIndentationLevel - ctx.prevIndentationLevel;
        if (difference == 0) return;
        int sign = difference > 0 ? 1 : -1;
        difference *= sign;
        while (difference-- > 0) {
            ctx.putToken(new IndentationToken(ctx.nextPos - ctx.countNewlineLen(), ctx.nextPos, 0, 0, sign));
        }
    }

    void resetIndentationAtTheEnd(LexerContext ctx) {
        if (ctx.curIndentationLevel != 0) {
            updateIndentationLevel(ctx, 0);
            resetIndentation(ctx);
        }
    }

    private SymbolToken getSymbolToken(LexerContext ctx) {
        ctx.curState = SYMBOL;
        String lexeme = ctx.symbolBuffer.toString();
        ctx.start = ctx.end = ctx.nextPos;

        switch (ctx.symbolBuffer.toString()) {
            case ">" -> {
                if (ctx.lessThanCounter > 0) {
                    ctx.lessThanCounter--;
                    break;
                }
                if (ctx.isNext("=")) lexeme = ">=";
                else if (ctx.isNext(">")) lexeme = ">>";
            }
            case "<" -> {
                if (ctx.isNext("=")) lexeme = "<=";
                else if (ctx.isNext("<")) lexeme = "<<";
                else if (ctx.isNext(":")) lexeme = "<:";
                else if (!ctx.isNext(" ")) ctx.lessThanCounter++;
            }
            case "=" -> {
                if (ctx.isNext("=")) lexeme = "==";
                else if (ctx.isNext("!")) lexeme = "!=";
            }
            case "&" -> {
                if (ctx.isNext("&")) lexeme = "&";
            }
            default -> {
                if (!symbolMap.containsKey(lexeme)) return null;
            }
        }

        int shift = lexeme.length() == 2 ? 1 : 0;
        ctx.end = ctx.nextPos += shift;

        return new SymbolToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                ctx.countLeadingTrivia, ctx.countTrailingTrivia, symbolMap.get(lexeme));

    }

    void scanIdentifier(LexerContext ctx) {
        ctx.putNext();
        ctx.nextPos++;

        while (!ctx.isEOF(ctx.nextPos)) {
            boolean isIdentifierPart = isIdentifierContinue(ctx.symbol());
            if (!isIdentifierPart) {
                String lexeme = ctx.symbolBuffer.toString();
                if (isBoolean(lexeme)) {
                    ctx.curState = BOOLEAN;
                } else if (isKeyword(lexeme)) {
                    ctx.curState = KEYWORD;
                } else {
                    ctx.curState = IDENTIFIER;
                }
                break;

            } else {
                ctx.putNext();
                ctx.nextPos++;
            }
        }
        ctx.cancel();
    }


    public Token getLiteralToken(LexerContext ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - ctx.bufferLen() + 1;

        String lexeme = ctx.symbolBuffer.toString();
        Token token = null;

        if (isIdentifierStart(lexeme)) {
            switch (lexeme) {
                case "this", "super", "is", "if", "else", "for", "in", "while", "def", "var", "val", "return", "break",
                     "continue", "abstract", "virtual", "override", "native" -> {
                    token = new KeywordToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                            ctx.countLeadingTrivia, ctx.countTrailingTrivia, keywordMap.get(lexeme));
                }
                case "class", "object", "interface", "null" -> {
                    token = new IdentifierToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                            ctx.countLeadingTrivia, ctx.countTrailingTrivia, lexeme, keywordMap.get(lexeme));
                }
                case "true", "false" -> {
                    token = new BooleanLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                            ctx.countLeadingTrivia, ctx.countTrailingTrivia, Boolean.parseBoolean(lexeme));
                }
                default -> {
                    if (isIdentifier(lexeme))
                        token = new IdentifierToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                                ctx.countLeadingTrivia, ctx.countTrailingTrivia, lexeme, null);

                }
            }
        }
        return token;
    }


    void scanNumber(LexerContext ctx) {
        ctx.putNext();
        ctx.nextPos++;

        while (!ctx.isEOF(ctx.nextPos)) {
            boolean isDigit = isDigit(ctx.symbol());

            if (!isDigit) {
                String suffix = ctx.hasSuffix() ? ctx.getSuffix() : "";
                ctx.symbolBuffer.append(suffix);
                ctx.nextPos += suffix.length();
                ctx.curState = NUMBER;
                break;
            } else {
                ctx.putNext();
                ctx.nextPos++;
            }
        }
        ctx.cancel();
    }


    Token getIntegerToken(LexerContext ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - ctx.bufferLen() + 1;

        String lexeme = ctx.symbolBuffer.toString();
        Token token = null;

        if (isNumber(lexeme)) {
            ctx.curState = NUMBER;
            BuiltInType type = BuiltInType.INT64;
            boolean hasSuffix = false;
            long value;

            try {
                value = Long.parseLong(lexeme);
            } catch (NumberFormatException e) {
                int typeNameLen = 3;
                String typeName = lexeme.substring(lexeme.length() - typeNameLen);
                type = builtInTypeMap.get(typeName);
                hasSuffix = true;
                value = Long.parseLong(lexeme.substring(0, lexeme.length() - typeNameLen));
            }
            token = new IntegerLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                    ctx.countLeadingTrivia, ctx.countTrailingTrivia, type, hasSuffix, value);
        }
        return token;
    }


    private Token getStringLiteralToken(LexerContext ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - ctx.bufferLen() - 1;
        return new StringLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                ctx.countLeadingTrivia, ctx.countTrailingTrivia, UnicodeReader.substituteRune(ctx.symbolBuffer.toString()));
    }


    private Token getRuneLiteralToken(LexerContext ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - ctx.bufferLen() - 1;
        String rune = ctx.symbolBuffer.toString();

        if (isRune(rune)) {
            return new RuneLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                    ctx.countLeadingTrivia, ctx.countTrailingTrivia, UnicodeReader.runeToCodePoint(rune));
        }
        return null;
    }


    public List<Token> tokenize(LexerContext ctx) {
        while (!ctx.isEOF(++ctx.nextPos)) {
            String nextSymbol = ctx.symbol();

            if (ctx.isState(COMMENTARY) && !ctx.isNewline()) {
                ctx.putTrivia();
                continue;
            }
            if ((ctx.isState(RUNE) || ctx.isState(STRING)) && !ctx.isSymbol("'") && !ctx.isSymbol("\"")) {
                ctx.putNext();
                continue;
            }

            switch (nextSymbol) {
                case "#" -> {
                    ctx.putTrivia();
                    ctx.curState = COMMENTARY;
                }
                case "\n" -> {
                    ctx.putTrivia();
                    ctx.lessThanCounter = 0;
                    ctx.curState = INDENTATION;
                    calculateIndentation(ctx);
                }
                case " ", "\t", "\r" -> {
                    ctx.putTrivia();
                }
                case "=", "<", ">", ".", ",", ":", "-", "+", "*", "/", "%", "!", "~", "&", "|", "^", "[", "]", "(",
                     ")", "?" -> {
                    ctx.putNext();
                    ctx.putToken(getSymbolToken(ctx));
                }
                case "'" -> {
                    if (ctx.isState(RUNE)) {
                        ctx.putToken(getRuneLiteralToken(ctx));
                        ctx.curState = DEFAULT;
                        break;
                    }
                    ctx.curState = RUNE;
                }
                case "\"" -> {
                    if (ctx.isState(STRING)) {
                        ctx.putToken(getStringLiteralToken(ctx));
                        ctx.curState = DEFAULT;
                        break;
                    }
                    ctx.curState = STRING;
                }

                default -> {
                    if (isIdentifierStart(ctx.symbol())) {
                        scanIdentifier(ctx);
                        ctx.putToken(getLiteralToken(ctx));

                    } else if (isDigit(ctx.symbol())) {
                        scanNumber(ctx);
                        ctx.putToken(getIntegerToken(ctx));
                    } else ctx.putToken(null);
                }
            }
        }

        if (ctx.countLeadingTrivia != 0) {
            ctx.updateToken();
        }
        resetIndentationAtTheEnd(ctx);
        return ctx.tokens;
    }

    @Override
    public List<Token> lex(String s) {
        LexerContext ctx = new LexerContext(s);
        return tokenize(ctx);
    }
}



