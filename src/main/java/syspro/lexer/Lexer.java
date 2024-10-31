package syspro.lexer;

import syspro.tm.lexer.*;

import java.util.List;

import static syspro.lexer.State.ObservedState.*;
import static syspro.lexer.utils.UnicodePattern.*;
import static syspro.lexer.utils.UtilMaps.*;


class Lexer implements syspro.tm.lexer.Lexer {

    private SymbolToken tokenizeSymbol(Context ctx) {
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
        ctx.start = ctx.nextPos - ctx.symbolBuffer.codePoints().toArray().length;

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


    private int countIndentationLength(Context ctx, int pos) {
        int count = 0;
        while (pos < ctx.codePoints.length - 1 && (ctx.codePoints[pos] == ' ' || ctx.codePoints[pos] == '\t')) {
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
        if (ctx.nextPos + 1 >= ctx.codePoints.length) {
            updateIndentationLevel(ctx, 0);
            resetIndentation(ctx);
            return;
        }
        if (ctx.codePoints[ctx.nextPos + 1] == '\n') return;
        int count = countIndentationLength(ctx, ctx.nextPos + 1);
        if (count == 0 || ctx.nextPos + 1 + count + 1 >= ctx.codePoints.length) {
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
        ctx.end = ctx.start = ctx.nextPos;
        int difference = ctx.curIndentationLevel - ctx.prevIndentationLevel;
        if (difference == 0) return;
        int sign = difference > 0 ? 1 : -1;
        difference *= sign;
        while (difference-- > 0) {
            ctx.addToken(new IndentationToken(ctx.end, ctx.end, 0, 0, sign));
        }
    }

    void resetIndentationAtTheEnd(Context ctx) {
        if (ctx.curIndentationLevel != 0) {
            updateIndentationLevel(ctx, 0);
            resetIndentation(ctx);
        }
    }

    private Token getStringLiteralToken(Context ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - (int) ctx.symbolBuffer.codePoints().count() - 1;
        return new StringLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                ctx.countLeadingTrivia, ctx.countTrailingTrivia, ctx.symbolBuffer.toString());
    }


    private Token getRuneLiteralToken(Context ctx) {
        ctx.end = ctx.nextPos;
        ctx.start = ctx.nextPos - (int) ctx.symbolBuffer.codePoints().count() - 1;
        String rune = ctx.symbolBuffer.toString();
        if (isRune(rune))
            return new RuneLiteralToken(ctx.start - ctx.countLeadingTrivia, ctx.end + ctx.countTrailingTrivia,
                    ctx.countLeadingTrivia, ctx.countTrailingTrivia, rune.codePointAt(0));
        return null;
    }

    private void addTrivia(Context ctx) {
        if (ctx.symbolBuffer.isEmpty()) {
            ctx.countLeadingTrivia++;
        } else {
            ctx.countTrailingTrivia++;
        }
    }

    private boolean nextIsNewline(Context ctx) {
        return ctx.codePoints[ctx.nextPos] == '\n' || (ctx.codePoints[ctx.nextPos] == '\r' && ctx.codePoints[ctx.nextPos + 1] == '\n');
    }

    public List<Token> tokenize(Context ctx) {
        while (++ctx.nextPos < ctx.codePoints.length) {
            String nextSymbol = ctx.getNextSymbol();

            if (ctx.curState.equals(COMMENTARY) && !nextIsNewline(ctx)) {
                addTrivia(ctx);
                continue;
            }
            switch (nextSymbol) {
                case "#" -> {
                    ctx.countLeadingTrivia++;
                    ctx.curState = COMMENTARY;
                }
                case "\n" -> {
                    if (ctx.curState.equals(RUNE) || ctx.curState.equals(STRING)) {
                        ctx.addNext();
                        break;
                    }
                    if (ctx.curState.equals(COMMENTARY)) {
                        ctx.countLeadingTrivia += ctx.symbolBuffer.length();
                        ctx.resetBuffer();
                    }
                    if (ctx.hasLexeme()) ctx.addToken(getLiteralToken(ctx));
                    addTrivia(ctx);
                    ctx.curState = INDENTATION;
                    calculateIndentation(ctx);
                }
                case " ", "\t", "\r" -> {
                    if (nextSymbol.equals("\r")) break;
                    if (ctx.curState.equals(STRING) || ctx.curState.equals(RUNE)) {
                        ctx.addNext();
                        break;
                    }
                    if (ctx.hasLexeme()) ctx.addToken(getLiteralToken(ctx));
                    addTrivia(ctx);
                }
                case "=", "<", ">", ".", ",", ":", "-", "+", "*", "/", "%", "!", "~", "&", "|", "^", "[", "]", "(",
                     ")", "?" -> {
                    if (ctx.curState.equals(STRING) || ctx.curState.equals(RUNE)) {
                        ctx.addNext();
                        break;
                    }
                    if (ctx.hasLexeme()) ctx.addToken(getLiteralToken(ctx));
                    ctx.addNext();
                    ctx.addToken(tokenizeSymbol(ctx));
                }
                case "'" -> {
                    if (ctx.curState.equals(RUNE)) {
                        ctx.addToken(getRuneLiteralToken(ctx));
                        ctx.curState = DEFAULT;
                        break;
                    }
                    ctx.curState = RUNE;
                }
                case "\"" -> {
                    if (ctx.curState.equals(STRING)) {
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
            if (ctx.curState.equals(COMMENTARY)) ctx.countLeadingTrivia += ctx.symbolBuffer.length();
            else ctx.addToken(getLiteralToken(ctx));
        }
        if (ctx.countLeadingTrivia != 0) {
            int counter = ctx.tokens.size() - 1;
            while (counter >= 0) {
                if (ctx.tokens.get(counter) instanceof IndentationToken) {
                    counter--;
                } else {
                    Token token = ctx.tokens.get(counter);
                    ctx.tokens.set(counter, awesomeFunction(ctx, token));
                    break;
                }
            }
        }

        resetIndentationAtTheEnd(ctx);
        return ctx.tokens;
    }


    private Token awesomeFunction(Context ctx, Token tokenToUpdate) {
        return switch (tokenToUpdate) {
            case BooleanLiteralToken b ->
                    new BooleanLiteralToken(b.start, b.end + ctx.countLeadingTrivia, b.leadingTriviaLength, b.trailingTriviaLength + ctx.countLeadingTrivia, b.value);
            case IdentifierToken i ->
                    new IdentifierToken(i.start, i.end + ctx.countLeadingTrivia, i.leadingTriviaLength, i.trailingTriviaLength + ctx.countLeadingTrivia, i.value, i.contextualKeyword);
            case IntegerLiteralToken i ->
                    new IntegerLiteralToken(i.start, i.end + ctx.countLeadingTrivia, i.leadingTriviaLength, i.trailingTriviaLength + ctx.countLeadingTrivia, i.type, i.hasTypeSuffix, i.value);
            case KeywordToken k ->
                    new KeywordToken(k.start, k.end + ctx.countLeadingTrivia, k.leadingTriviaLength, k.trailingTriviaLength + ctx.countLeadingTrivia, k.keyword);
            case RuneLiteralToken r ->
                    new RuneLiteralToken(r.start, r.end + ctx.countLeadingTrivia, r.leadingTriviaLength, r.trailingTriviaLength + ctx.countLeadingTrivia, r.value);
            case StringLiteralToken s ->
                    new StringLiteralToken(s.start, s.end + ctx.countLeadingTrivia, s.leadingTriviaLength, s.trailingTriviaLength + ctx.countLeadingTrivia, s.value);
            case SymbolToken s ->
                    new SymbolToken(s.start, s.end + ctx.countLeadingTrivia, s.leadingTriviaLength, s.trailingTriviaLength + ctx.countLeadingTrivia, s.symbol);
            default -> throw new IllegalStateException("Unexpected value: " + tokenToUpdate);
        };
    }


    @Override
    public List<Token> lex(String s) {
        Context ctx = new Context(s);
        return tokenize(ctx);
    }
}



