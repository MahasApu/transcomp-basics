package syspro.parser;

import syspro.lexer.Lexer;
import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.ParseResult;
import syspro.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.lexer.Symbol.*;
import static syspro.tm.parser.SyntaxKind.*;

public class Parser implements syspro.tm.parser.Parser {

    public List<ASTNode> parse(ParserContext ctx) {
        List<ASTNode> statements = new ArrayList<>();

        while (++ctx.pos < ctx.tokens.size()) {
            ctx.logger.info(ctx.kind(), String.format("start parsing - %s", ctx.get().toString()));
            ASTNode statement = parseDefinition(ctx);
            if (Objects.isNull(statement)) ctx.logger.info(NULL, "final result - ");
            else ctx.logger.info(statement, "final result  - ");

            statements.add(statement);
        }
        return statements;
    }

    private ASTNode parseDefinition(ParserContext ctx) {
        if (ctx.is(CLASS, OBJECT, INTERFACE)) return parseClassDefinition(ctx);
        if (ctx.is(VAL, VAR)) return parseVarDefinition(ctx);
        if (ctx.is(ABSTRACT, VIRTUAL, OVERRIDE, NATIVE, DEF)) return parseDefDefinition(ctx);
        if (ctx.get().toString().equals("class") || ctx.get().toString().equals("object")
                || ctx.get().toString().equals("interface")) return parseClassDefinition(ctx);
        if (ctx.is(IDENTIFIER)) return parseClassDefinition(ctx);
        return parseStatement(ctx);
    }

    private ASTNode parseVarDefinition(ParserContext ctx) {
        ASTNode keyword = ctx.expected("Expected any of VAR|VAL in var decl.", VAR, VAL);
        ASTNode name = ctx.expected("Expected name in var decl.", IDENTIFIER);

        ASTNode typeExpr = null;
        ASTNode colon = null;
        if (ctx.is(COLON)) {
            colon = ctx.expected("Expected : in var decl.", COLON);
            typeExpr = parseNameExpression(ctx);
        }

        ASTNode valueExpr = null;
        ASTNode eq = null;
        if (ctx.is(EQUALS)) {
            eq = ctx.expected("Expected = in var decl.", EQUALS);
            valueExpr = parseExpression(ctx);
        }
        ASTNode node = new ASTNode(VARIABLE_DEFINITION_STATEMENT, null, keyword, name, colon, typeExpr, eq, valueExpr);
        ctx.logger.info(node, "Variable_def_stmt node -");
        return node;
    }

    private ASTNode parseDefDefinition(ParserContext ctx) {
        ASTNode terminalList = parseTerminalList(ctx, ABSTRACT, VIRTUAL, OVERRIDE, NATIVE);
        ASTNode def = ctx.expected("Expected DEF keyword in func def.", DEF);

        ASTNode functionName = ctx.expected("Expected function name in func def.", IDENTIFIER);

        ASTNode openParen = ctx.expected("Expected ( for func def.", OPEN_PAREN);
        ASTNode parameterList = parseSeparatedList(this::parseParameterDefinition, COMMA, ctx);
        ASTNode closeParen = ctx.expected("Expected ) for func def.", CLOSE_PAREN);

        ASTNode returnType = null;
        ASTNode colon = null;
        if (ctx.match(COLON)) {
            colon = new ASTNode(COLON, ctx.prev());
            returnType = parsePrimary(ctx);
        }

        ASTNode functionBody = null;
        ASTNode indent = null;
        ASTNode dedent = null;
        if (ctx.is(INDENT)) {
            indent = ctx.expected("Expected INDENT in func def.", INDENT);
            functionBody = new ASTNode(LIST, null, parseStatementList(ctx));
            dedent = ctx.expected("Expected DEDENT in func def.", DEDENT);
        }
        ASTNode node = new ASTNode(FUNCTION_DEFINITION, null, terminalList, def,
                functionName, openParen, parameterList, closeParen,
                colon, returnType, indent, functionBody, dedent);

        ctx.logger.info(node, "Function definition -");
        return node;
    }

    private ASTNode parseNameExpression(ParserContext ctx) {
        if (ctx.match(QUESTION)) {
            ASTNode question = new ASTNode(QUESTION, ctx.prev());
            ASTNode innerExpr = parseNameExpression(ctx);
            return new ASTNode(OPTION_NAME_EXPRESSION, ctx.get(), question, innerExpr);
        }

        ASTNode name = ctx.expected("Expected name in name expr.", IDENTIFIER);

        ASTNode typeArguments = null;
        ASTNode lessThan = null;
        ASTNode greaterThen = null;
        if (ctx.match(LESS_THAN)) {
            lessThan = new ASTNode(LESS_THAN, ctx.prev());
            typeArguments = parseSeparatedList(this::parseNameExpression, COMMA, ctx);
            greaterThen = ctx.expected("Expected > in name expr.", GREATER_THAN);
        }

        if (Objects.nonNull(typeArguments) && typeArguments.slotCount() != 0)
            return new ASTNode(GENERIC_NAME_EXPRESSION, null, name, lessThan, typeArguments, greaterThen);
        return new ASTNode(IDENTIFIER_NAME_EXPRESSION, null, name);
    }

    private List<ASTNode> parseParameterDefinitionList(ParserContext ctx) {
        List<ASTNode> parameterList = new ArrayList<>();
        while (true) {
            if (ctx.match(IDENTIFIER)) {
                parameterList.add(parseParameterDefinition(ctx));
            } else if (!ctx.match(COMMA)) break;
        }
        return parameterList;
    }

    private ASTNode parseParameterDefinition(ParserContext ctx) {
        if (ctx.is(CLOSE_PAREN)) return new ASTNode(PARAMETER_DEFINITION, null, null, null, null);
        ASTNode identifier = ctx.expected("Expected name in param def", IDENTIFIER);
        ASTNode colon = ctx.expected("Expected COLON in param def", COLON);
        ASTNode name = parseNameExpression(ctx);
        return new ASTNode(PARAMETER_DEFINITION, null, identifier, colon, name);
    }

    private ASTNode parseTerminalList(ParserContext ctx, AnySyntaxKind... terms) {
        List<ASTNode> terminalList = new ArrayList<>();
        while (ctx.match(terms)) {
            terminalList.add(new ASTNode(ctx.look().toSyntaxKind(), ctx.get()));
        }
        return new ASTNode(LIST, null, terminalList);
    }

    private ASTNode parseClassDefinition(ParserContext ctx) {
        AnySyntaxKind kind = switch (ctx.get().toString()) {
            case "class" -> CLASS;
            case "object" -> OBJECT;
            case "interface" -> INTERFACE;
            default -> NULL;
        };
        ASTNode keyword = new ASTNode(kind, ctx.get());
        ctx.match(IDENTIFIER);
        ASTNode name = ctx.expected("Expected name in class decl", IDENTIFIER);

        ASTNode lessThan = null;
        ASTNode greaterThan = null;
        ASTNode generics = null;
        if (ctx.match(LESS_THAN)) {
            lessThan = new ASTNode(LESS_THAN, ctx.prev());
            generics = parseSeparatedList(this::parseTypeParameterDefinition, COMMA, ctx);
            greaterThan = ctx.expected("Expected > ", GREATER_THAN);
        }

        ASTNode typeBoundsList = null;
        ASTNode bound = null;
        if (ctx.match(BOUND)) {
            bound = new ASTNode(BOUND, ctx.prev());
            typeBoundsList = parseSeparatedList(this::parseTypeParameterDefinition, COMMA, ctx);
        }

        ASTNode indent = null;
        ASTNode memberDef = null;
        ASTNode dedent = null;
        if (ctx.match(INDENT)) {
            indent = new ASTNode(INDENT, ctx.prev());
            memberDef = parseVarDefDefinitionList(ctx);
            ctx.logger.info(memberDef, "Memberblock of class - ");
            dedent = ctx.expected("Expected DEDENT", DEDENT);

        }
        ctx.pos--;
        return new ASTNode(TYPE_DEFINITION, null, keyword, name, lessThan,
                generics, greaterThan, bound, typeBoundsList, indent, memberDef, dedent);
    }

    private ASTNode parseVarDefDefinitionList(ParserContext ctx) {
        List<ASTNode> list = new ArrayList<>();
        while (!ctx.is(DEDENT)) {
            if (ctx.is(VAL, VAR)) list.add(parseVarDefinition(ctx));
            if (ctx.is(ABSTRACT, VIRTUAL, OVERRIDE, NATIVE, DEF)) list.add(parseDefDefinition(ctx));
            else break;
        }
        return new ASTNode(LIST, null, list);
    }


    private ASTNode parseTypeParameterDefinition(ParserContext ctx) {
        ASTNode identifier = ctx.expected("Expected identifier in type param.", IDENTIFIER);
        ASTNode typeBound = null;
        if (ctx.is(BOUND)) typeBound = parseTypeBound(ctx);
        return new ASTNode(TYPE_PARAMETER_DEFINITION, null, identifier, typeBound);
    }

    private ASTNode parseTypeBound(ParserContext ctx) {
        ASTNode bound = ctx.expected("Expected :> in type bound.", BOUND);
        ASTNode types = parseSeparatedList(this::parseNameExpression, AMPERSAND, ctx);
        return new ASTNode(TYPE_BOUND, null, bound, types);
    }

    @FunctionalInterface
    public interface ParserMethod<T> {
        T parse(ParserContext ctx);
    }

    private ASTNode parseSeparatedList(ParserMethod<ASTNode> parser, AnySyntaxKind separator, ParserContext ctx) {
        List<ASTNode> list = new ArrayList<>();
        do {
            list.add(parser.parse(ctx));
        } while (ctx.match(separator));
        return new ASTNode(SEPARATED_LIST, null, list);
    }


    private ASTNode parsePrimary(ParserContext ctx) {
//        List<ASTNode> extensions = new ArrayList<>();
        ASTNode primary = parseAtom(ctx);
        if (Objects.nonNull(primary)) return primary;
        while (true) {
            if (ctx.is(DOT)) {
                ASTNode dot = ctx.expected("Expected DOT in primary.", DOT);
                ASTNode name = ctx.expected("Expected Identifier in primary", IDENTIFIER);
                return new ASTNode(MEMBER_ACCESS_EXPRESSION, null, dot, name);

            } else if (ctx.is(OPEN_PAREN)) {
                ASTNode openParen = ctx.expected("Expected ( in primary", OPEN_PAREN);
                ASTNode args = parseSeparatedList(this::parseExpression, COMMA, ctx);
                ASTNode closeParen = ctx.expected("Expected ) in primary", CLOSE_PAREN);
                return new ASTNode(INVOCATION_EXPRESSION, null, openParen, args, closeParen);

            } else if (ctx.is(OPEN_BRACKET)) {
                ASTNode openBracket = ctx.expected("Expected { in primary", OPEN_BRACKET);
                ASTNode expr = null; // parseExpression(ctx);
                ASTNode closeBracket = ctx.expected("Expected } in primary", CLOSE_BRACKET);
                return new ASTNode(INDEX_EXPRESSION, null, openBracket, expr, closeBracket);

            } else break;
        }

        return null;

    }

    private ASTNode parseAtom(ParserContext ctx) {
        return switch (ctx.kind()) {
            case IDENTIFIER -> new ASTNode(IDENTIFIER_NAME_EXPRESSION, ctx.step());
            case THIS -> new ASTNode(THIS_EXPRESSION, ctx.step());
            case SUPER -> new ASTNode(SUPER_EXPRESSION, ctx.step());
            case NULL -> new ASTNode(NULL_LITERAL_EXPRESSION, ctx.step());
            case STRING -> new ASTNode(STRING_LITERAL_EXPRESSION, ctx.step());
            case RUNE -> new ASTNode(RUNE_LITERAL_EXPRESSION, ctx.step());
            case INTEGER -> new ASTNode(INTEGER_LITERAL_EXPRESSION, ctx.step());
            case BOOLEAN -> {
                if (ctx.get().toString().equals("true")) yield new ASTNode(TRUE_LITERAL_EXPRESSION, ctx.step());
                yield new ASTNode(FALSE_LITERAL_EXPRESSION, ctx.step());
            }
            case OPEN_PAREN -> {
                ASTNode openParen = ctx.expected("Expected ( in atom", OPEN_PAREN);
                ASTNode expr = null;
                ASTNode closeParen = ctx.expected("Expected ) in atom", CLOSE_PAREN);
                yield new ASTNode(PARENTHESIZED_EXPRESSION, null, openParen, expr, closeParen);
            }
            case INDENT -> new ASTNode(INDENT, ctx.step());
            case DEDENT -> new ASTNode(DEDENT, ctx.step());
            default -> {
                ctx.logger.log(Logger.LogLevel.ERROR, Logger.Stage.SYNTAX,
                        String.format("Unexpected token in primary: %s.", ctx.get().toString()));
                yield null;
            }

        };
    }


    private ASTNode parseStatement(ParserContext ctx) {
        return switch (ctx.kind()) {
            case VAR, VAL -> parseVarDefStatement(ctx);
            case IDENTIFIER -> {
                if (ctx.is(EQUALS)) yield parseAssignStatement(ctx);
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

    private ASTNode parseExpressionStatement(ParserContext ctx) {
        ASTNode expr = parseExpression(ctx);
        AnySyntaxKind kind = Objects.isNull(expr) ? NULL : expr.kind();
        return new ASTNode(kind, ctx.get(), expr);
    }

    private ASTNode parseExpression(ParserContext ctx) {
        return parsePrimary(ctx);
    }

    private ASTNode parseIfStatement(ParserContext ctx) {
        ASTNode ifToken = ctx.expected("Expected if in if stmt.", IF);
        ASTNode cond = parseExpression(ctx);

        ASTNode elseStatements = null;
        ASTNode indent = ctx.expected("Expected indent in if stmt.", INDENT);
        ASTNode trueStatements = parseStatementList(ctx);
        if (ctx.match(ELSE)) {
            ctx.match(INDENT);
            elseStatements = parseStatementList(ctx);
        }
        return new ASTNode(IF_STATEMENT, null, cond, trueStatements, elseStatements);
    }

    private ASTNode parseWhileStatement(ParserContext ctx) {
        ctx.match(WHILE);
        ASTNode cond = parseExpression(ctx);
        ctx.match(INDENT);
        ASTNode statements = parseStatementList(ctx);
        return new ASTNode(WHILE_STATEMENT, null, cond, statements);
    }

    private ASTNode parseVarDefStatement(ParserContext ctx) {
        return parseVarDefinition(ctx);
    }


    private ASTNode parseContinueStatement(ParserContext ctx) {
        Token token = ctx.get();
        ctx.expected("Expected continue in statement.", CONTINUE);
        return new ASTNode(CONTINUE_STATEMENT, token);
    }

    private ASTNode parseBreakStatement(ParserContext ctx) {
        Token token = ctx.get();
        ctx.expected("Expected break in statement.", BREAK);
        return new ASTNode(BREAK_STATEMENT, token);
    }

    private ASTNode parseAssignStatement(ParserContext ctx) {
        Token token = ctx.get();
        ASTNode primary = parsePrimary(ctx);
        ctx.expected("Expected = in assignment.", EQUALS);
        ASTNode expr = parseExpression(ctx);
        return new ASTNode(ASSIGNMENT_STATEMENT, token, primary, expr);
    }

    private ASTNode parseForStatement(ParserContext ctx) {
        ctx.match(FOR);
        ASTNode primary = parsePrimary(ctx);
        ctx.expected("Expected IN in assignment.", IN);
        ASTNode expr = parseExpression(ctx);
        ASTNode statements = null;
        if (ctx.match(INDENT)) {
            statements = parseStatementList(ctx);
            ctx.expected("Expected DEDENT in assignment.", DEDENT);
        }
        return new ASTNode(FOR_STATEMENT, null, primary, expr, statements);

    }

    private ASTNode parseStatementList(ParserContext ctx) {
        List<ASTNode> list = new ArrayList<>();
        while (!ctx.is(DEDENT)) {
            list.add(parseStatement(ctx));
        }
        return new ASTNode(LIST, null, list);
    }

    private ASTNode parseReturnStatement(ParserContext ctx) {
        ASTNode ret = ctx.expected("Expected return in statement.", RETURN);
        ASTNode expr = null;
        if (!ctx.is(DEDENT)) {
            expr = parseExpression(ctx);
        }
        return new ASTNode(RETURN_STATEMENT, null, ret, expr);
    }


    private ASTNode parseUnaryExpression(ParserContext ctx) {
        Token token = ctx.get();
        return switch (ctx.kind()) {
            case EXCLAMATION -> {
                ctx.match(EXCLAMATION);
                ASTNode expr = parseUnaryExpression(ctx);
                yield new ASTNode(LOGICAL_NOT_EXPRESSION, token, expr);
            }
            case PLUS -> {
                ctx.match(PLUS);
                ASTNode expr = parseUnaryExpression(ctx);
                yield new ASTNode(UNARY_PLUS_EXPRESSION, token, expr);
            }
            case MINUS -> {
                ctx.match(MINUS);
                ASTNode expr = parseUnaryExpression(ctx);
                yield new ASTNode(UNARY_MINUS_EXPRESSION, token, expr);
            }
            case TILDE -> {
                ctx.match(TILDE);
                ASTNode expr = parseUnaryExpression(ctx);
                yield new ASTNode(BITWISE_NOT_EXPRESSION, token, expr);
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

        List<ASTNode> statements = parse(ctx);
        ctx.logger.close();
        return new SysproParseResult(new ASTNode(SOURCE_TEXT, ctx.tokens.get(0), new ASTNode(LIST, null, statements)), null, null);
    }
}
