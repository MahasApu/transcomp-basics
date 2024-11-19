package syspro.parser;

import syspro.lexer.Lexer;
import syspro.parser.ast.Expression;
import syspro.parser.ast.Statement;
import syspro.parser.ast.SyntaxCategory;
import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxKind;
import syspro.tm.parser.SyntaxNode;
import syspro.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.lexer.Symbol.*;
import static syspro.tm.parser.SyntaxKind.*;

public class Parser implements syspro.tm.parser.Parser {

    public List<SyntaxNode> parse(ParserContext ctx) {
        List<SyntaxNode> statements = new ArrayList<>();
        while (++ctx.pos < ctx.tokens.size()) {
            ctx.logger.info(ctx.kind(), "statement");

        }
        return null;
    }

    @FunctionalInterface
    public interface ParserMethod<T> {
        T parse();
    }

    private List<Expression> parseSeparatedList(ParserMethod<Expression> parser, AnySyntaxKind separator, ParserContext ctx) {
        List<Expression> list = new ArrayList<>();
        do {
            list.add(parser.parse());
        } while (ctx.match(separator));
        return list;
    }

    private Expression expression(ParserContext ctx) {
        return null;
    }

    private SyntaxNode parseStatement(ParserContext ctx) {
        return switch (ctx.kind()) {
            case IF_STATEMENT -> ifStatement(ctx);
            case ASSIGNMENT_STATEMENT -> assignStatement(ctx);
            case BREAK_STATEMENT -> breakStatement(ctx);
            case CONTINUE_STATEMENT -> continueStatement(ctx);
            case FOR_STATEMENT -> forStatement(ctx);
            case EXPRESSION_STATEMENT -> exprStatement(ctx);
            case RETURN_STATEMENT -> returnStatement(ctx);
            case VARIABLE_DEFINITION_STATEMENT -> varDefStatement(ctx);
            case WHILE_STATEMENT -> whileStatement(ctx);
            default -> parseExpressionStatement(ctx);
        };
    }

    private SyntaxNode parseExpressionStatement(ParserContext ctx) {
        SyntaxNode expr = parseExpression(ctx);
        assert expr != null;
        return new Expression(expr.kind());
    }

    private SyntaxNode parseExpression(ParserContext ctx) {
        return null;
    }

    private SyntaxNode ifStatement(ParserContext ctx) {
        SyntaxNode cond = parseExpression(ctx);
        ctx.match(INDENT);
        List<Statement> trueStatements = parseStatements(ctx);
        List<Statement> falseStatements = new ArrayList<>();
        if (ctx.match(ELSE)) {
            ctx.match(INDENT);
            falseStatements = parseStatements(ctx);
        }
        return new Statement(IF_STATEMENT);
    }

    private SyntaxNode whileStatement(ParserContext ctx) {
        SyntaxNode cond = parseExpression(ctx);
        ctx.match(INDENT);
        List<Statement> statements = parseStatements(ctx);
        return new Statement(WHILE_STATEMENT);
    }

    private SyntaxNode varDefStatement(ParserContext ctx) {
        String name = ctx.match(IDENTIFIER) ? ctx.prev().toString() : null;
        if (ctx.match(COLON)) {
            SyntaxNode expr = parseExpression(ctx);
            return new Statement(VARIABLE_DEFINITION_STATEMENT);
        }
        return null;
    }

    private SyntaxNode exprStatement(ParserContext ctx) {
        return null;
    }

    private SyntaxNode continueStatement(ParserContext ctx) {
        return ctx.match(CONTINUE) ? new Statement(CONTINUE_STATEMENT) : null;
    }

    private SyntaxNode breakStatement(ParserContext ctx) {
        return ctx.match(BREAK) ? new Statement(BREAK_STATEMENT) : null;
    }

    private SyntaxNode assignStatement(ParserContext ctx) {
        return null;
    }

    private SyntaxNode forStatement(ParserContext ctx) {
        SyntaxNode start = parseExpression(ctx);
        ctx.match(IN);
        SyntaxNode end = parseExpression(ctx);
        ctx.match(INDENT);
        List<Statement> statements = parseStatements(ctx);
        return new Statement(FOR_STATEMENT);

    }

    private List<Statement> parseStatements(ParserContext ctx) {
        return null;
    }

    private SyntaxNode returnStatement(ParserContext ctx) {
        SyntaxNode expr = ctx.match(SyntaxCategory.EXPRESSION) ? parseExpression(ctx) : null;
        assert expr != null;
        return new Statement(expr.kind());
    }


    @Override
    public ParseResult parse(String s) {

        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.lex(s);

        Logger logger = new Logger(Logger.Stage.LEXICAL);
        ParserContext ctx = new ParserContext(tokens, logger);

        for (Token t : tokens) {
            ctx.logger.info(t, "Token added - ");
        }

        ctx.logger.updateStage(Logger.Stage.SYNTAX);

        List<SyntaxNode> statements = parse(ctx);
        ctx.logger.close();
        return null;
    }
}
