package syspro.lexer;

import syspro.tm.lexer.*;

import java.util.List;

import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodePattern.*;
import static syspro.lexer.utils.UtilMaps.*;


class Lexer implements syspro.tm.lexer.Lexer {


    private int countIndentationLength(Context ctx, int pos) {
        int count = 0;
        while (!ctx.isEOF(pos) && (ctx.codePoints[pos] == ' ' || ctx.codePoints[pos] == '\t')) {
            count += ctx.codePoints[pos] == '\t' ? 2 : 1;
            pos++;
        }
        return count;
    }

    private void updateIndentationLevel(Context ctx, int level) {
        ctx.prevIndentationLevel = ctx.curIndentationLevel;
        ctx.curIndentationLevel = level;
    }

    private void incrementIndentationLevel(Context ctx) {
        ctx.prevIndentationLevel = ctx.curIndentationLevel;
        ctx.curIndentationLevel++;
    }

    private void calculateIndentation(Context ctx) {
        if (ctx.isEOF(ctx.nextPos + 1)) {
            updateIndentationLevel(ctx, 0);
            resetIndentation(ctx);
            return;
        }
        if (ctx.isNewline(ctx.nextPos + 1)) return;
        int count = countIndentationLength(ctx, ctx.nextPos + 1);
        if (count == 0 || ctx.isEOF(ctx.nextPos + 1 + count + 1)) {
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


    private void resetIndentation(Context ctx) {
        int difference = ctx.curIndentationLevel - ctx.prevIndentationLevel;
        if (difference == 0) return;
        int sign = difference > 0 ? 1 : -1;
        difference *= sign;
        while (difference-- > 0) {
            ctx.addToken(new IndentationToken(ctx.nextPos - ctx.countNewlineLen(),  ctx.nextPos, 0, 0, sign));
        }
    }

    void resetIndentationAtTheEnd(Context ctx) {
        if (ctx.curIndentationLevel != 0) {
            updateIndentationLevel(ctx, 0);
            resetIndentation(ctx);
        }
    }

    private SymbolToken getSymbolToken(Context ctx) {
        ctx.curState = SYMBOL;
        String lexeme = ctx.symbolBuffer.toString();
        ctx.end = ctx.nextPos + 1;
        ctx.start = ctx.nextPos;
        switch (ctx.symbolBuffer.toString()) {
            case ">" -> {
                if (ctx.nextIs("=")) lexeme = ">=";
                else if (ctx.nextIs(">")) lexeme = ">>";
                else ctx.end--;
            }
            case "<" -> {
                if (ctx.nextIs("=")) lexeme = "<=";
                else if (ctx.nextIs("<")) lexeme = "<<";
                else if (ctx.nextIs(":")) lexeme = "<:";
                else ctx.end--;
            }
            case "=" -> {
                if (ctx.nextIs("=")) lexeme = "==";
                else if (ctx.nextIs("!")) lexeme = "!=";
                else ctx.end--;
            }
            case "&" -> {
                if (ctx.nextIs("&")) lexeme = "&";
                else ctx.end--;
            }
            default -> ctx.end--;
        }
        ctx.nextPos = ctx.end;
        return new SymbolToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                ctx.countLeadingTrivia, ctx.countTrailingTrivia, symbolMap.get(lexeme));

    }


    public Token getLiteralToken(Context ctx) {
        ctx.end = ctx.nextPos - 1;
        ctx.start = ctx.nextPos - ctx.bufferLen();

        String lexeme = ctx.symbolBuffer.toString();
        Token token = null;

        if (isIdentifierStart(lexeme)) {
            ctx.curState = IDENTIFIER;
            switch (lexeme) {
                case "this", "super", "is", "else", "for", "in", "while", "def", "var", "val", "return", "break",
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
        } else if (isNumber(lexeme)) {
            ctx.curState = NUMBER;
            BuiltInType type = BuiltInType.INT64;
            boolean hasSuffix = false;
            long value;

            try {
                value = Long.parseLong(lexeme);
            } catch (NumberFormatException e) {
                String typeName = lexeme.substring(lexeme.length() - 3);
                type = builtInTypeMap.get(typeName);
                hasSuffix = true;
                value = Long.parseLong(lexeme.substring(0, lexeme.length() - 3));
            }
            token = new IntegerLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                    ctx.countLeadingTrivia, ctx.countTrailingTrivia, type, hasSuffix, value);
        }
        return token;
    }


    private Token getStringLiteralToken(Context ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - ctx.bufferLen() - 1;
        return new StringLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                ctx.countLeadingTrivia, ctx.countTrailingTrivia, ctx.symbolBuffer.toString());
    }


    private Token getRuneLiteralToken(Context ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - ctx.bufferLen() - 1;
        String rune = ctx.symbolBuffer.toString();
        if (isRune(rune))
            return new RuneLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                    ctx.countLeadingTrivia, ctx.countTrailingTrivia, rune.codePointAt(0));
        return null;
    }


    public List<Token> tokenize(Context ctx) {
        while (!ctx.isEOF(++ctx.nextPos)) {
            String nextSymbol = ctx.nextSymbol();

            if (ctx.isState(COMMENTARY) && !ctx.isNewline()) {
                ctx.addTrivia(ctx);
                continue;
            }
            switch (nextSymbol) {
                case "#" -> {
                    ctx.countLeadingTrivia++;
                    ctx.curState = COMMENTARY;
                }
                case "\n" -> {
                    if (ctx.isState(RUNE) || ctx.isState(STRING)) {
                        ctx.addNext();
                        break;
                    }
                    if (ctx.isState(COMMENTARY)) {
                        ctx.countLeadingTrivia += ctx.symbolBuffer.length();
                        ctx.resetBuffer();
                    }
                    if (ctx.hasLexeme()) ctx.addToken(getLiteralToken(ctx));
                    ctx.addTrivia(ctx);
                    ctx.curState = INDENTATION;
                    calculateIndentation(ctx);
                }
                case " ", "\t", "\r" -> {

                    if (ctx.isState(STRING) || ctx.isState(RUNE)) {
                        ctx.addNext();
                        break;
                    }
                    if (ctx.hasLexeme()) ctx.addToken(getLiteralToken(ctx));
                    ctx.addTrivia(ctx);
                }
                case "=", "<", ">", ".", ",", ":", "-", "+", "*", "/", "%", "!", "~", "&", "|", "^", "[", "]", "(",
                     ")", "?" -> {
                    if (ctx.isState(STRING) || ctx.isState(RUNE)) {
                        ctx.addNext();
                        break;
                    }
                    if (ctx.hasLexeme()) ctx.addToken(getLiteralToken(ctx));
                    ctx.addNext();
                    ctx.addToken(getSymbolToken(ctx));
                }
                case "'" -> {
                    if (ctx.isState(RUNE)) {
                        ctx.addToken(getRuneLiteralToken(ctx));
                        ctx.curState = DEFAULT;
                        break;
                    }
                    ctx.curState = RUNE;
                }
                case "\"" -> {
                    if (ctx.isState(STRING)) {
                        ctx.addToken(getStringLiteralToken(ctx));
                        ctx.curState = DEFAULT;
                        break;
                    }
                    ctx.curState = STRING;
                }
                default -> ctx.addNext();
            }
        }

        if (ctx.hasLexeme()) {
            if (ctx.isState(COMMENTARY)) ctx.countLeadingTrivia += ctx.symbolBuffer.length();
            else ctx.addToken(getLiteralToken(ctx));
        }
        if (ctx.countLeadingTrivia != 0) {
            ctx.updateToken();
        }
        resetIndentationAtTheEnd(ctx);
        return ctx.tokens;
    }


    @Override
    public List<Token> lex(String s) {
        Context ctx = new Context(s);
        return tokenize(ctx);
    }
}



