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


    private SyntaxNode parsePrimary(ParserContext ctx) {
        List<SyntaxNode> extensions = new ArrayList<>();
        SyntaxNode primary = parseAtom(ctx);
        while (true) {
            if (ctx.is(DOT)) {
                Token name = ctx.expected("Expected Identifier", IDENTIFIER);
                extensions.add(new Statement(MEMBER_ACCESS_EXPRESSION));
            } else if (ctx.is(OPEN_PAREN)) {
                List<SyntaxNode> args = parseSeparatedList(this::parseExpression, COMMA, ctx);
                ctx.expected("Expected ) in primary", CLOSE_PAREN);
                extensions.add(new Statement(INVOCATION_EXPRESSION));
            } else if (ctx.is(OPEN_BRACKET)) {
                SyntaxNode expr = parseExpression(ctx);
                ctx.expected("Expected } in primary", CLOSE_BRACKET);
                extensions.add(new Statement(INDEX_EXPRESSION));
            }
        }

    }

    private SyntaxNode parseAtom(ParserContext ctx) {
        return switch (ctx.kind()) {
            case IDENTIFIER -> new Statement(IDENTIFIER_NAME_EXPRESSION);
            case THIS -> new Statement(THIS_EXPRESSION);
            case SUPER -> new Statement(SUPER_EXPRESSION);
            case NULL -> new Statement(NULL_LITERAL_EXPRESSION);
            case STRING -> new Statement(STRING_LITERAL_EXPRESSION);
            case RUNE -> new Statement(RUNE_LITERAL_EXPRESSION);
            case INTEGER -> new Statement(INTEGER_LITERAL_EXPRESSION);
            case BOOLEAN -> {
                if (ctx.get().toString().equals("true")) yield new Statement(TRUE_LITERAL_EXPRESSION);
                yield new Statement(FALSE_LITERAL_EXPRESSION);
            }
            case OPEN_PAREN -> {
                ctx.expected("Expected ( in atom", OPEN_PAREN);
                SyntaxNode expr = parseExpression(ctx);
                ctx.expected("Expected ) in atom", CLOSE_PAREN);
                yield new Statement(PARENTHESIZED_EXPRESSION);
            }
            default -> throw new IllegalStateException("Unexpected value: " + ctx.kind());
        };
    }


    private SyntaxNode parseStatement(ParserContext ctx) {
        return switch (ctx.kind()) {
            case VAR, VAL -> parseVarDefStatement(ctx);
            case IDENTIFIER -> {
                if (ctx.look().toSyntaxKind().equals(EQUALS)) yield parseAssignStatement(ctx);
                yield parseExpressionStatement(ctx);
            }
            case RETURN -> parseReturnStatement(ctx);
            case BREAK -> parseBreakStatement(ctx);
            case CONTINUE -> parseContinueStatement(ctx);
            case IF -> parseIfStatement(ctx);
            case WHILE -> parseWhileStatement(ctx);
            case FOR -> parseForStatement(ctx);
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
        ctx.match(IF);
        SyntaxNode cond = parseExpression(ctx);
        ctx.match(INDENT);
//        List<Statement> trueStatements = parseStatementList(ctx);
        List<Statement> elseStatements = new ArrayList<>();
        if (ctx.match(ELSE)) {
            ctx.match(INDENT);
            elseStatements = parseStatementList(ctx);
        }
        return new Statement(IF_STATEMENT);
    }

    private SyntaxNode parseWhileStatement(ParserContext ctx) {
        ctx.match(WHILE);
        SyntaxNode cond = parseExpression(ctx);
        ctx.match(INDENT);
        List<Statement> statements = parseStatementList(ctx);
        return new Statement(WHILE_STATEMENT);
    }

    private SyntaxNode parseVarDefStatement(ParserContext ctx) {
        return parseVarDeclaration(ctx);
    }


    private SyntaxNode parseContinueStatement(ParserContext ctx) {
        ctx.expected("Expected continue in statement.", CONTINUE);
        return new Statement(CONTINUE_STATEMENT);
    }

    private SyntaxNode parseBreakStatement(ParserContext ctx) {
        ctx.expected("Expected break in statement.", BREAK);
        return new Statement(BREAK_STATEMENT);
    }

    private SyntaxNode parseAssignStatement(ParserContext ctx) {
        SyntaxNode primary = parsePrimary(ctx);
        ctx.expected("Expected = in assignment.", EQUALS);
        SyntaxNode expr = parseExpression(ctx);
        return new Statement(ASSIGNMENT_STATEMENT);
    }

    private SyntaxNode parseForStatement(ParserContext ctx) {
        ctx.match(FOR);
        SyntaxNode primary = parsePrimary(ctx);
        ctx.expected("Expected IN in assignment.", IN);
        SyntaxNode expr = parseExpression(ctx);
        List<Statement> statements = null;
        if (ctx.match(INDENT)) {
            statements = parseStatementList(ctx);
            ctx.expected("Expected DEDENT in assignment.", DEDENT);
        }
        return new Statement(FOR_STATEMENT);

    }

    private List<Statement> parseStatementList(ParserContext ctx) {
        return null;
    }

    private SyntaxNode parseReturnStatement(ParserContext ctx) {
        ctx.expected("Expected return in statement.", RETURN);
        SyntaxNode expr = null;
        if (ctx.is(DEDENT)) {
            expr = parseExpression(ctx);
        }
        return new Statement(RETURN_STATEMENT);
    }


    private SyntaxNode parseUnaryExpression(ParserContext ctx) {
        return switch (ctx.kind()) {
            case EXCLAMATION -> {
                ctx.match(EXCLAMATION);
                SyntaxNode expr = parseUnaryExpression(ctx);
                yield new Expression(LOGICAL_NOT_EXPRESSION);
            }
            case PLUS -> {
                ctx.match(PLUS);
                SyntaxNode expr = parseUnaryExpression(ctx);
                yield new Expression(UNARY_PLUS_EXPRESSION);
            }
            case MINUS -> {
                ctx.match(MINUS);
                SyntaxNode expr = parseUnaryExpression(ctx);
                yield new Expression(UNARY_MINUS_EXPRESSION);
            }
            case TILDE -> {
                ctx.match(TILDE);
                SyntaxNode expr = parseUnaryExpression(ctx);
                yield new Expression(BITWISE_NOT_EXPRESSION);
            }
            default -> parsePrimary(ctx);
        };
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
