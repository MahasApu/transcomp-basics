package syspro.parser;

import syspro.lexer.Lexer;
import syspro.parser.ast.Expression;
import syspro.parser.ast.Statement;
import syspro.tm.lexer.Token;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxKind;

import java.util.ArrayList;
import java.util.List;

public class Parser implements syspro.tm.parser.Parser {

    public List<Statement> parse(ParserContext ctx) {
        List<Statement> statements = new ArrayList<>();
        while (++ctx.pos < ctx.tokens.size()) {
//            statements.add()
        }
        return null;
    }


    private Expression expression(ParserContext ctx) {
//        return switch (ctx.kind()) {
////            case SyntaxKind.EXP
//        }
        return null;
    }

    private Statement statement(ParserContext ctx) {
        return switch (ctx.kind()) {
            case SyntaxKind.IF_STATEMENT -> ifStatement(ctx);
            case SyntaxKind.ASSIGNMENT_STATEMENT -> assignStatement(ctx);
            case SyntaxKind.BREAK_STATEMENT -> breakStatement(ctx);
            case SyntaxKind.CONTINUE_STATEMENT -> continueStatement(ctx);
            case SyntaxKind.FOR_STATEMENT -> forStatement(ctx);
            case SyntaxKind.EXPRESSION_STATEMENT -> exprStatement(ctx);
            case SyntaxKind.RETURN_STATEMENT -> returnStatement(ctx);
            case SyntaxKind.VARIABLE_DEFINITION_STATEMENT -> varDefStatement(ctx);
            case SyntaxKind.WHILE_STATEMENT -> whileStatement(ctx);
            default -> throw new IllegalStateException("Unexpected value: " + ctx.kind());
        };
    }

    private Statement whileStatement(ParserContext ctx) {
        return null;
    }

    private Statement varDefStatement(ParserContext ctx) {
        return null;
    }

    private Statement exprStatement(ParserContext ctx) {
        return null;
    }

    private Statement continueStatement(ParserContext ctx) {
        return null;
    }

    private Statement breakStatement(ParserContext ctx) {
        return null;
    }

    private Statement assignStatement(ParserContext ctx) {
        return null;
    }

    private Statement ifStatement(ParserContext ctx) {
        return null;
    }

    private Statement forStatement(ParserContext ctx) {
        return null;
    }

    private Statement returnStatement(ParserContext ctx) {
        return null;
    }


    @Override
    public ParseResult parse(String s) {
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.lex(s);
        ParserContext ctx = new ParserContext(tokens);
        List<Statement> statements = parse(ctx);
        return null;
    }
}
