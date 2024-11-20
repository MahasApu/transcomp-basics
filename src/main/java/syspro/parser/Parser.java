package syspro.parser;

import syspro.lexer.Lexer;
import syspro.parser.ast.Expression;
import syspro.parser.ast.Statement;
import syspro.parser.ast.SyntaxCategory;
import syspro.parser.exceptions.ParserException;
import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.lexer.Symbol.*;
import static syspro.tm.parser.SyntaxKind.*;

public class Parser implements syspro.tm.parser.Parser {

    public List<SyntaxNode> parse(ParserContext ctx) {
        List<SyntaxNode> statements = new ArrayList<>();
        while (++ctx.pos < ctx.tokens.size()) {
            ctx.logger.info(ctx.kind(), "start parsing");
            statements.add(parseDeclaration(ctx));
        }
        return statements;
    }

    private SyntaxNode parseDeclaration(ParserContext ctx) {
        try {
            if (ctx.match(CLASS)) return parseClassDeclaration(ctx);
            if (ctx.match(DEF)) return parseDefDeclaration(ctx);
            if (ctx.match(VAL)) return parseVarDeclaration(ctx);
            if (ctx.match(VAR)) return parseVarDeclaration(ctx);
            return parseStatement(ctx);
        } catch (ParserException e) {
            // do smth
            return null;
        }
    }

    private SyntaxNode parseVarDeclaration(ParserContext ctx) {
        Token keyword = ctx.expected("Expected any of VAR|VAL in var decl.", VAR, VAL);
        Token name = ctx.expected("Expected name in var decl.", IDENTIFIER);

        SyntaxNode typeExpr = null;
        if (ctx.match(COLON)) typeExpr = parseNameExpression(ctx);

        SyntaxNode valueExpr = null;
        if (ctx.match(EQUALS)) valueExpr = parseExpression(ctx);
        return new Statement(VARIABLE_DEFINITION_STATEMENT);
    }

    private SyntaxNode parseDefDeclaration(ParserContext ctx) {
        List<SyntaxNode> terminalList = parseTerminalList(ctx);
        ctx.expected("Expected DEF keyword in func def.", DEF);

        Token functionName = ctx.expected("Expected function name in func def.", IDENTIFIER);

        ctx.expected("Expected ( for func def.", OPEN_PAREN);
        List<SyntaxNode> parameterList = parseParameterDefinitionList(ctx);
        ctx.expected("Expected ) for func def.", CLOSE_PAREN);

        SyntaxNode returnType = null;
        if (ctx.match(COLON)) returnType = parseNameExpression(ctx);

        if (ctx.match(INDENT)) {
            List<Statement> functionBody = parseStatementList(ctx);
            ctx.expected("Expected DEDENT in func def.", DEDENT);
        }

        return new Statement(FUNCTION_DEFINITION);
    }

    private SyntaxNode parseNameExpression(ParserContext ctx) {
        if (ctx.match(QUESTION)) {
            SyntaxNode innerExpr = parseNameExpression(ctx);
            return new Expression(OPTION_NAME_EXPRESSION);
        }

        Token name = ctx.expected("Expected name in name expr.", IDENTIFIER);

        List<SyntaxNode> typeArguments = null;
        if (ctx.match(LESS_THAN)) {
            typeArguments = parseSeparatedList(this::parseNameExpression, COMMA, ctx);
            ctx.expected("Expected > in name expr.", GREATER_THAN);
        }

        if (Objects.nonNull(typeArguments) && !typeArguments.isEmpty())
            return new Expression(GENERIC_NAME_EXPRESSION);
        return new Expression(IDENTIFIER_NAME_EXPRESSION);
    }

    private List<SyntaxNode> parseParameterDefinitionList(ParserContext ctx) {
        List<SyntaxNode> parameterList = new ArrayList<>();
        while (true) {
            if (ctx.match(IDENTIFIER)) {
                parameterList.add(parseParameterDefinition(ctx));
            } else if (!ctx.match(COMMA)) break;
        }
        return parameterList;
    }

    private SyntaxNode parseParameterDefinition(ParserContext ctx) {
        Token identifier = ctx.expected("Expected name in param def", IDENTIFIER);
        ctx.expected("Expected COLON in param def", COLON);
        SyntaxNode name = parseNameExpression(ctx);
        return new Statement(PARAMETER_DEFINITION);
    }

    private List<SyntaxNode> parseTerminalList(ParserContext ctx) {
        List<SyntaxNode> terminalList = new ArrayList<>();
        while (ctx.match(SyntaxCategory.TERMINAL)) {
            terminalList.add(new Statement(ctx.look().toSyntaxKind()));
        }
        return terminalList;
    }

    private SyntaxNode parseClassDeclaration(ParserContext ctx) {
        Token name = ctx.expected("Expect class name.", IDENTIFIER);
        ctx.expected("Expected <", LESS_THAN);

        if (ctx.match(LESS_THAN)) {
            List<SyntaxNode> generics = new ArrayList<>();
            while (!ctx.match(GREATER_THAN)) generics.add(new Expression(GENERIC_NAME_EXPRESSION));
        }
        ctx.expected("Expected > ", GREATER_THAN);

        return new Statement(TYPE_DEFINITION);
    }


    private SyntaxNode parseTypeDefinition(ParserContext ctx) {
        Token name = ctx.expected("Expected name in type def.", IDENTIFIER);

        List<SyntaxNode> typeParameters = null;
        if (ctx.match(LESS_THAN)) {
            typeParameters = parseSeparatedList(this::parseTypeParameterDefinition, COMMA, ctx);
            ctx.expected("Expected > in type def.", GREATER_THAN);
        }

        // type_bound := '<:' type_name ('&' type_name)*
        List<SyntaxNode> typeBoundsList = null;
        if (ctx.match(BOUND)) {
            typeBoundsList = parseTypeBound(ctx);
        }

        // TODO: inner definitions
        List<SyntaxNode> innerDefinitions = null;
        if (ctx.match(INDENT)) {
            // innerDefinitions = parseList(this::aaaa, ctx);
            ctx.expected("Expected DEDENT in type def", DEDENT);
        }
        return new Expression(TYPE_DEFINITION);


    }

    private SyntaxNode parseTypeParameterDefinition(ParserContext ctx) {
        return null;
    }

    private List<SyntaxNode> parseTypeBound(ParserContext ctx) {
        List<SyntaxNode> types = new ArrayList<>();
        types.add(parseNameExpression(ctx));
        return null;
    }


    private SyntaxNode assignment(ParserContext ctx) {
        return null;
    }

    @FunctionalInterface
    public interface ParserMethod<T> {
        T parse(ParserContext ctx);
    }

    private List<SyntaxNode> parseSeparatedList(ParserMethod<SyntaxNode> parser, AnySyntaxKind separator, ParserContext ctx) {
        List<SyntaxNode> list = new ArrayList<>();
        do {
            list.add(parser.parse(ctx));
        } while (ctx.match(separator));
        return list;
    }


    private SyntaxNode parseStatement(ParserContext ctx) {
        return switch (ctx.kind()) {
            case IF_STATEMENT -> parseIfStatement(ctx);
            case ASSIGNMENT_STATEMENT -> assignStatement(ctx);
            case BREAK_STATEMENT -> breakStatement(ctx);
            case CONTINUE_STATEMENT -> continueStatement(ctx);
            case FOR_STATEMENT -> forStatement(ctx);
            case EXPRESSION_STATEMENT -> exprStatement(ctx);
            case RETURN_STATEMENT -> returnStatement(ctx);
            case VARIABLE_DEFINITION_STATEMENT -> parseVarDefStatement(ctx);
            case WHILE_STATEMENT -> parseWhileStatement(ctx);
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

    private SyntaxNode parseIfStatement(ParserContext ctx) {
        SyntaxNode cond = parseExpression(ctx);
        ctx.match(INDENT);
        List<Statement> trueStatements = parseStatementList(ctx);
        List<Statement> falseStatements = new ArrayList<>();
        if (ctx.match(ELSE)) {
            ctx.match(INDENT);
            falseStatements = parseStatementList(ctx);
        }
        return new Statement(IF_STATEMENT);
    }

    private SyntaxNode parseWhileStatement(ParserContext ctx) {
        SyntaxNode cond = parseExpression(ctx);
        ctx.match(INDENT);
        List<Statement> statements = parseStatementList(ctx);
        return new Statement(WHILE_STATEMENT);
    }

    private SyntaxNode parseVarDefStatement(ParserContext ctx) {
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
        List<Statement> statements = parseStatementList(ctx);
        return new Statement(FOR_STATEMENT);

    }

    private List<Statement> parseStatementList(ParserContext ctx) {
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
