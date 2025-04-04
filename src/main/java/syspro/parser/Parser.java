package syspro.parser;

import syspro.lexer.Lexer;
import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.Keyword;
import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;
import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.lexer.Symbol.*;
import static syspro.tm.parser.SyntaxKind.*;

public class Parser implements syspro.tm.parser.Parser {

    public List<SyntaxNode> parse(ParserContext ctx) {
        List<SyntaxNode> statements = new ArrayList<>();

        while (++ctx.pos < ctx.tokens.size()) {
            ctx.logger.info(ctx.kind(), String.format("start parsing - %s.", ctx.get().toString()));
            ASTNode statement = parseDefinition(ctx);
            if (isNull(statement)) ctx.logger.info(NULL, "final result - ");
            else ctx.logger.info(statement, "final result  - ");
            if (!isNull(statement)) statements.add(statement);
        }
        return statements;
    }

    private ASTNode parseDefinition(ParserContext ctx) {
        if (ctx.typeDefinitionStarts()) return parseTypeDefinition(ctx);
        return null;
    }

    private ASTNode parseVarDefinition(ParserContext ctx) {
        ASTNode keyword = ctx.expected("Expected any of VAR|VAL in var definition.", VAR, VAL);
        ASTNode name = ctx.expected("Expected name in var definition.", IDENTIFIER);

        ASTNode typeExpr = null;
        ASTNode colon = null;

        if (ctx.is(COLON)) {
            colon = ctx.expected("Expected : in var definition.", COLON);
            typeExpr = parseNameExpression(ctx);
        }

        ASTNode valueExpr = null;
        ASTNode eq = null;

        if (ctx.is(EQUALS)) {
            eq = ctx.expected("Expected = in var definition.", EQUALS);
            valueExpr = parseExpression(ctx);
        }
        ASTNode node = new ASTNode(VARIABLE_DEFINITION, null, keyword, name, colon, typeExpr, eq, valueExpr);
        ctx.logger.info(node, "Variable_def_statement node -");
        return node;
    }

    private ASTNode parseFuncDefinition(ParserContext ctx) {
        ASTNode terminalList = parseTerminalList(ctx, ABSTRACT, VIRTUAL, OVERRIDE, NATIVE);
        ASTNode def = ctx.expected("Expected DEF keyword in func definition.", DEF);

        ASTNode functionName = ctx.expected("Expected function name in func definition.", IDENTIFIER, THIS);
        // add panic mode
        if (isNull(functionName)) {
            functionName = new ASTNode(IDENTIFIER, ctx.get());
            ctx.step();
        }

        ASTNode openParen = ctx.expected("Expected ( for function definition.", OPEN_PAREN);
        ASTNode parameterList = parseSeparatedList(this::parseParameterDefinition, COMMA, ctx);
        ASTNode closeParen = ctx.expected("Expected ) for function definition.", CLOSE_PAREN);

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
            indent = ctx.expected("Expected INDENT in func definition.", INDENT);
            functionBody = parseStatementList(ctx, ctx::statementStarts);

            if (isNull(functionBody)) {
                indent = null;
                ctx.addInvalidRange();
            } else if (ctx.is(INDENT)) ctx.addInvalidRange();

            dedent = isNull(functionBody) ? null : ctx.expected("Expected DEDENT in func definition.", DEDENT);

        }

        if (ctx.statementStarts()) ctx.addInvalidRange("IndentationError: incorrect indentation in line ");

        ASTNode node = new ASTNode(FUNCTION_DEFINITION, null, terminalList, def,
                functionName, openParen, parameterList, closeParen,
                colon, returnType, indent, functionBody, dedent);

        ctx.logger.info(node, "Function definition -");
        return node;
    }


    private ASTNode parseParameterDefinition(ParserContext ctx) {
        if (ctx.is(CLOSE_PAREN)) return null;
        ASTNode identifier = ctx.expected("Expected name in parameters definition.", IDENTIFIER);
        ASTNode colon = ctx.expected("Expected : in parameters definition.", COLON);
        ASTNode name = parseNameExpression(ctx);
        return new ASTNode(PARAMETER_DEFINITION, null, identifier, colon, name);
    }

    private ASTNode parseTerminalList(ParserContext ctx, AnySyntaxKind... terms) {
        List<SyntaxNode> terminalList = new ArrayList<>();

        while (ctx.match(terms))
            terminalList.add(new ASTNode(ctx.prev().toSyntaxKind(), ctx.prev()));

        if (terminalList.isEmpty()) return null;
        return new ASTNode(LIST, null, terminalList);
    }

    private ASTNode parseTypeDefinition(ParserContext ctx) {
        Keyword kind = switch (ctx.get().toString()) {
            case "class" -> CLASS;
            case "object" -> OBJECT;
            case "interface" -> INTERFACE;
            default -> NULL;
        };
        ctx.updateTokenKind(kind);
        ASTNode keyword = new ASTNode(kind, ctx.get());

        ctx.match(kind);
        ASTNode name = ctx.expected("Expected name in type definition.", IDENTIFIER);

        ASTNode lessThan = null;
        ASTNode greaterThan = null;
        ASTNode generics = null;
        if (ctx.match(LESS_THAN)) {
            lessThan = new ASTNode(LESS_THAN, ctx.prev());
            generics = parseSeparatedList(this::parseTypeParameterDefinition, COMMA, ctx);
            greaterThan = ctx.expected("Expected > in type definition.", GREATER_THAN);
        }

        ASTNode typeBoundsList = null;
        if (ctx.match(BOUND))
            typeBoundsList = parseTypeBound(ctx);


        ASTNode indent = null;
        ASTNode memberDef = null;
        ASTNode dedent = null;

        if (ctx.match(INDENT)) {
            indent = new ASTNode(INDENT, ctx.prev());
            memberDef = parseVarDefDefinitionList(ctx);
            dedent = ctx.expected("Expected DEDENT in type definition.", DEDENT);
            if (memberDef.slotCount() == 0)
                ctx.addInvalidRange();

        } else if (ctx.statementStarts() || ctx.definitionStarts())
            ctx.addInvalidRange("IndentationError: incorrect indentation in line ");

        ctx.pos--;
        return new ASTNode(TYPE_DEFINITION, null, keyword, name, lessThan,
                generics, greaterThan, typeBoundsList, indent, memberDef, dedent);
    }

    private ASTNode parseVarDefDefinitionList(ParserContext ctx) {
        List<SyntaxNode> list = new ArrayList<>();
        while (!ctx.typeDefinitionStarts()) {
            if (ctx.is(VAL, VAR)) list.add(parseVarDefinition(ctx));
            else if (ctx.is(ABSTRACT, VIRTUAL, OVERRIDE, NATIVE, DEF)) list.add(parseFuncDefinition(ctx));
            else break;
        }
        return new ASTNode(LIST, null, list);
    }


    private ASTNode parseTypeParameterDefinition(ParserContext ctx) {
        ASTNode identifier = ctx.expected("Expected identifier in type parameters.", IDENTIFIER);
        ASTNode typeBound = null;
        if (ctx.match(BOUND)) typeBound = parseTypeBound(ctx);
        return new ASTNode(TYPE_PARAMETER_DEFINITION, null, identifier, typeBound);
    }

    private ASTNode parseTypeBound(ParserContext ctx) {
        ASTNode bound = new ASTNode(BOUND, ctx.prev());
        ASTNode types = parseSeparatedList(this::parseNameExpression, AMPERSAND, ctx);
        return new ASTNode(TYPE_BOUND, null, bound, types);
    }

    @FunctionalInterface
    public interface ParserMethod<T> {
        T parse(ParserContext ctx);
    }

    private ASTNode parseSeparatedList(ParserMethod<ASTNode> parser, AnySyntaxKind separator, ParserContext ctx) {
        if (ctx.is(CLOSE_PAREN)) return null;
        List<SyntaxNode> list = new ArrayList<>();
        do {
            ASTNode node = parser.parse(ctx);
            if (isNull(node)) break;
            list.add(node);
            if (ctx.is(separator)) list.add(new ASTNode(separator, ctx.get()));
        } while (ctx.match(separator));

        if (list.isEmpty()) return null;
        return new ASTNode(SEPARATED_LIST, null, list);
    }

    private ASTNode parseNameExpression(ParserContext ctx) {
        if (ctx.match(QUESTION)) {
            ASTNode question = new ASTNode(QUESTION, ctx.prev());
            ASTNode innerExpr = parseNameExpression(ctx);
            return new ASTNode(OPTION_NAME_EXPRESSION, ctx.get(), question, innerExpr);
        }

        ASTNode name = !ctx.is(INTEGER) ? ctx.expected("Expected name in name expression", IDENTIFIER) : null ;
        int resetPos = ctx.pos;

        ASTNode typeArguments = null;
        ASTNode lessThan = null;
        ASTNode greaterThan = null;

        if (ctx.match(LESS_THAN)) {
            lessThan = new ASTNode(LESS_THAN, ctx.prev());
            typeArguments = parseSeparatedList(this::parseNameExpression, COMMA, ctx);
            greaterThan = ctx.match(GREATER_THAN) ? new ASTNode(GREATER_THAN_EXPRESSION, ctx.prev()) : null;
        }

        if (isNull(lessThan) != isNull(greaterThan)) {
            ctx.pos = resetPos;
            return new ASTNode(IDENTIFIER_NAME_EXPRESSION, null, name);
        }

        if (Objects.nonNull(typeArguments) && typeArguments.slotCount() != 0)
            return new ASTNode(GENERIC_NAME_EXPRESSION, null, name, lessThan, typeArguments, greaterThan);
        return new ASTNode(IDENTIFIER_NAME_EXPRESSION, null, name);
    }

    private ASTNode parsePrimary(ParserContext ctx) {

        ASTNode primary = parseAtom(ctx);

        while (true) {
            if (ctx.is(DOT)) {
                ASTNode dot = ctx.expected("Expected . for member access.", DOT);
                ASTNode name = ctx.expected("Expected name in member access.", IDENTIFIER);
                primary = new ASTNode(MEMBER_ACCESS_EXPRESSION, null, primary, dot, name);

            } else if (ctx.is(OPEN_PAREN)) {
                ASTNode openParen = ctx.expected("Expected ( for invocation.", OPEN_PAREN);
                ASTNode args = parseSeparatedList(this::parseExpression, COMMA, ctx);
                ASTNode closeParen = ctx.expected("Expected ) for invocation.", CLOSE_PAREN);
                primary = new ASTNode(INVOCATION_EXPRESSION, null, primary, openParen, args, closeParen);

            } else if (ctx.is(OPEN_BRACKET)) {
                ASTNode openBracket = ctx.expected("Expected { before expression.", OPEN_BRACKET);
                ASTNode expr = parseExpression(ctx);
                ASTNode closeBracket = ctx.expected("Expected } in after expression.", CLOSE_BRACKET);
                primary = new ASTNode(INDEX_EXPRESSION, null, primary, openBracket, expr, closeBracket);

            } else break;
        }
        return primary;

    }

    private ASTNode parseAtom(ParserContext ctx) {

        if (ctx.kind().equals(IDENTIFIER)) return parseNameExpression(ctx);
        ASTNode value = new ASTNode(ctx.kind(), ctx.step());

        return switch (ctx.prev().toSyntaxKind()) {
            case BAD -> {
                Token t = ctx.prev();
                ctx.addInvalidRange(t.start + t.leadingTriviaLength, t.end + t.leadingTriviaLength, "SyntaxError: invalid token");
                yield value;
            }
            case THIS -> new ASTNode(THIS_EXPRESSION, null, value);
            case SUPER -> new ASTNode(SUPER_EXPRESSION, null, value);
            case NULL -> new ASTNode(NULL_LITERAL_EXPRESSION, null, value);
            case STRING -> new ASTNode(STRING_LITERAL_EXPRESSION, null, value);
            case RUNE -> new ASTNode(RUNE_LITERAL_EXPRESSION, null, value);
            case INTEGER -> new ASTNode(INTEGER_LITERAL_EXPRESSION, null, value);
            case BOOLEAN -> {
                if (ctx.prev().toString().equals("true"))
                    yield new ASTNode(TRUE_LITERAL_EXPRESSION, null, value);
                yield new ASTNode(FALSE_LITERAL_EXPRESSION, null, value);
            }
            case OPEN_PAREN -> {
                ctx.pos--;
                ASTNode openParen = ctx.expected("Expected ( before expression.", OPEN_PAREN);
                ASTNode expr = parseExpression(ctx);
                ASTNode closeParen = ctx.expected("Expected ) after expression.", CLOSE_PAREN);
                yield new ASTNode(PARENTHESIZED_EXPRESSION, null, openParen, expr, closeParen);
            }
            case INDENT, DEDENT -> {
                ctx.pos--;
                ctx.addInvalidRange(ctx.get().start, ctx.get().end);
                yield null;
            }
            default -> {
                ctx.logger.log(Logger.LogLevel.ERROR, Logger.Stage.SYNTAX,
                        String.format("Unexpected token in primary: %s.", value.kind()));
                ctx.pos--;
                ctx.addInvalidRange(ctx.get().start, ctx.get().end,
                        String.format("SyntaxError: Unexpected token: %s.", value.token().toString()));
                yield null;
            }

        };
    }


    private ASTNode parseStatement(ParserContext ctx) {

        return switch (ctx.kind()) {
            case VAR, VAL -> parseVarDefStatement(ctx);
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
        if (isNull(expr)) return null;
        return switch (expr.kind()) {
            case ASSIGNMENT_STATEMENT -> expr;
            default -> new ASTNode(EXPRESSION_STATEMENT, null, expr);
        };
    }

    private ASTNode parseExpression(ParserContext ctx) {
        return parseAssignStatement(ctx);
    }

    private ASTNode parseAssignStatement(ParserContext ctx) {

        ASTNode primary = parseLogicalOr(ctx);
        while (ctx.match(EQUALS)) {
            ASTNode eq = new ASTNode(EQUALS, ctx.prev());
            ASTNode expr = parseExpression(ctx);
            primary = new ASTNode(ASSIGNMENT_STATEMENT, null, primary, eq, expr);
        }
        return primary;
    }


    private ASTNode parseLogicalOr(ParserContext ctx) {

        ASTNode expr = parseLogicalAnd(ctx);
        while (ctx.match(BAR_BAR)) {
            ASTNode logicalOr = new ASTNode(BAR_BAR, ctx.prev());
            ASTNode right = parseLogicalAnd(ctx);
            expr = new ASTNode(LOGICAL_OR_EXPRESSION, null, expr, logicalOr, right);
        }
        return expr;
    }

    private ASTNode parseLogicalAnd(ParserContext ctx) {

        ASTNode expr = parseComparison(ctx);
        while (ctx.match(AMPERSAND_AMPERSAND)) {
            ASTNode logicalAnd = new ASTNode(AMPERSAND_AMPERSAND, ctx.prev());
            ASTNode right = parseComparison(ctx);
            expr = new ASTNode(LOGICAL_AND_EXPRESSION, null, expr, logicalAnd, right);
        }
        return expr;
    }

    private ASTNode parseComparison(ParserContext ctx) {

        ASTNode expr = parseBitwiseOr(ctx);
        while (ctx.match(EQUALS_EQUALS, EXCLAMATION_EQUALS, LESS_THAN,
                LESS_THAN_EQUALS, GREATER_THAN, GREATER_THAN_EQUALS, IS)) {
            ASTNode operator = new ASTNode(ctx.prev().toSyntaxKind(), ctx.prev());
            ASTNode right = parseBitwiseOr(ctx);

            if (isNull(right)) continue;

            AnySyntaxKind kind = switch (operator.kind()) {
                case EQUALS_EQUALS -> EQUALS_EXPRESSION;
                case EXCLAMATION_EQUALS -> NOT_EQUALS_EXPRESSION;
                case LESS_THAN -> LESS_THAN_EXPRESSION;
                case LESS_THAN_EQUALS -> LESS_THAN_OR_EQUAL_EXPRESSION;
                case GREATER_THAN -> GREATER_THAN_EXPRESSION;
                case GREATER_THAN_EQUALS -> GREATER_THAN_OR_EQUAL_EXPRESSION;
                default -> null;
            };
            if (ctx.is(IDENTIFIER) && Objects.equals(kind, IS_EXPRESSION))
                expr = new ASTNode(kind, null, expr, operator, right, new ASTNode(IDENTIFIER, ctx.get()));
            else expr = new ASTNode(kind, null, expr, operator, right);
        }
        return expr;
    }

    private ASTNode parseBitwiseOr(ParserContext ctx) {
        ASTNode expr = parseBitwiseXor(ctx);
        while (ctx.match(BAR)) {
            ASTNode operator = new ASTNode(BAR, ctx.prev());
            ASTNode right = parseBitwiseXor(ctx);
            expr = new ASTNode(BITWISE_OR_EXPRESSION, null, expr, operator, right);
        }
        return expr;
    }

    private ASTNode parseBitwiseXor(ParserContext ctx) {
        ASTNode expr = parseBitwiseAnd(ctx);
        while (ctx.match(CARET)) {
            ASTNode operator = new ASTNode(CARET, ctx.prev());
            ASTNode right = parseBitwiseAnd(ctx);
            expr = new ASTNode(BITWISE_EXCLUSIVE_OR_EXPRESSION, null, expr, operator, right);
        }
        return expr;
    }

    private ASTNode parseBitwiseAnd(ParserContext ctx) {
        ASTNode expr = parseBitwiseShift(ctx);
        while (ctx.match(AMPERSAND)) {
            ASTNode operator = new ASTNode(AMPERSAND, ctx.prev());
            ASTNode right = parseBitwiseShift(ctx);
            expr = new ASTNode(BITWISE_AND_EXPRESSION, null, expr, operator, right);
        }
        return expr;
    }

    private ASTNode parseBitwiseShift(ParserContext ctx) {

        ASTNode expr = parseBinaryExpression(ctx);
        while (ctx.match(LESS_THAN_LESS_THAN, GREATER_THAN_GREATER_THAN)) {
            ASTNode operator = new ASTNode(ctx.prev().toSyntaxKind(), ctx.prev());
            ASTNode right = parseBinaryExpression(ctx);

            AnySyntaxKind kind = switch (operator.kind()) {
                case LESS_THAN_LESS_THAN -> BITWISE_LEFT_SHIFT_EXPRESSION;
                case GREATER_THAN_GREATER_THAN -> BITWISE_RIGHT_SHIFT_EXPRESSION;
                default -> throw new IllegalStateException("Unexpected value: " + operator.kind());
            };

            expr = new ASTNode(kind, null, expr, operator, right);
        }
        return expr;
    }

    private ASTNode parseBinaryExpression(ParserContext ctx) {
        ASTNode expr = parseTerm(ctx);
        while (ctx.match(PLUS, MINUS)) {
            ASTNode operator = new ASTNode(ctx.prev().toSyntaxKind(), ctx.prev());
            ASTNode right = parseTerm(ctx);

            AnySyntaxKind kind = switch (operator.kind()) {
                case PLUS -> ADD_EXPRESSION;
                case MINUS -> SUBTRACT_EXPRESSION;
                default -> throw new IllegalStateException("Unexpected value: " + operator.kind());
            };

            expr = new ASTNode(kind, null, expr, operator, right);
        }
        return expr;
    }

    private ASTNode parseTerm(ParserContext ctx) {
        ASTNode expr = parseUnaryExpression(ctx);
        while (ctx.match(ASTERISK, SLASH, PERCENT)) {
            ASTNode operator = new ASTNode(ctx.prev().toSyntaxKind(), ctx.prev());
            ASTNode right = parseUnaryExpression(ctx);

            AnySyntaxKind kind = switch (operator.kind()) {
                case ASTERISK -> MULTIPLY_EXPRESSION;
                case SLASH -> DIVIDE_EXPRESSION;
                case PERCENT -> MODULO_EXPRESSION;
                default -> throw new IllegalStateException("Unexpected value: " + operator.kind());
            };

            expr = new ASTNode(kind, null, expr, operator, right);
        }
        return expr;
    }


    private ASTNode parseUnaryExpression(ParserContext ctx) {

        return switch (ctx.kind()) {
            case EXCLAMATION -> {
                ASTNode operator = ctx.expected("Expected EXCLAMATION.", EXCLAMATION);
                ASTNode expr = parseExpression(ctx);
                yield new ASTNode(LOGICAL_NOT_EXPRESSION, null, operator, expr);
            }
            case PLUS -> {
                ASTNode operator = ctx.expected("Expected PLUS.", PLUS);
                ASTNode expr = parseExpression(ctx);
                yield new ASTNode(UNARY_PLUS_EXPRESSION, null, operator, expr);
            }
            case MINUS -> {
                ASTNode operator = ctx.expected("Expected MINUS.", MINUS);
                ASTNode expr = parseExpression(ctx);
                yield new ASTNode(UNARY_MINUS_EXPRESSION, null, operator, expr);
            }
            case TILDE -> {
                ASTNode operator = ctx.expected("Expected TILDE.", TILDE);
                ASTNode expr = parseExpression(ctx);
                yield new ASTNode(BITWISE_NOT_EXPRESSION, null, operator, expr);
            }
            default -> parsePrimary(ctx);
        };
    }


    private ASTNode parseIfStatement(ParserContext ctx) {

        ASTNode ifNode = ctx.expected("Expected IF keyword in if statement.", IF);
        ASTNode cond = parseExpression(ctx);

        ASTNode indentTrue = null;
        ASTNode dedentTrue = null;
        ASTNode statementsTrue = null;

        if (ctx.is(INDENT)) {
            indentTrue = ctx.expected("Expected INDENT in if statement.", INDENT);
            statementsTrue = parseStatementList(ctx, ctx::statementStarts);
            if (isNull(statementsTrue)) {
                ctx.addInvalidRange();
                indentTrue = null;
            } else dedentTrue = ctx.expected("Expected DEDENT in if statement.", DEDENT);
        }

        ASTNode indentFalse = null;
        ASTNode dedentFalse = null;
        ASTNode statementsFalse = null;
        ASTNode elseNode = null;

        if (ctx.is(ELSE)) {
            elseNode = ctx.expected("Expected ELSE keyword in if statement.", ELSE);
            indentFalse = ctx.expected("Expected INDENT in if statement.", INDENT);
            statementsFalse = parseStatementList(ctx, ctx::statementStarts);
            if (isNull(statementsFalse))
                ctx.addInvalidRange();
            dedentFalse = ctx.expected("Expected DEDENT in if statement.", DEDENT);
        }
        return new ASTNode(IF_STATEMENT, null, ifNode, cond, indentTrue, statementsTrue, dedentTrue,
                elseNode, indentFalse, statementsFalse, dedentFalse);
    }

    private ASTNode parseWhileStatement(ParserContext ctx) {

        ASTNode whileNode = ctx.expected("Expected while in while statement.", WHILE);
        ASTNode cond = parseExpression(ctx);

        ASTNode indent = null;
        ASTNode dedent = null;
        ASTNode statements = null;
        if (ctx.is(INDENT)) {
            indent = ctx.expected("Expected INDENT in while statement.", INDENT);
            statements = parseStatementList(ctx, ctx::statementStarts);
            if (isNull(statements))
                ctx.addInvalidRange();
            dedent = ctx.expected("Expected DEDENT in while statement.", DEDENT);
        }
        return new ASTNode(WHILE_STATEMENT, null, whileNode, cond, indent, statements, dedent);
    }

    private ASTNode parseVarDefStatement(ParserContext ctx) {
        return new ASTNode(VARIABLE_DEFINITION_STATEMENT, null, parseVarDefinition(ctx));
    }

    private ASTNode parseContinueStatement(ParserContext ctx) {

        Token token = ctx.get();
        ASTNode continueNode = ctx.expected("Expected CONTINUE keyword in statement.", CONTINUE);
        return new ASTNode(CONTINUE_STATEMENT, token, continueNode);
    }

    private ASTNode parseBreakStatement(ParserContext ctx) {

        Token token = ctx.get();
        ASTNode breakNode = ctx.expected("Expected BREAK keyword in statement.", BREAK);
        return new ASTNode(BREAK_STATEMENT, token, breakNode);
    }


    private ASTNode parseForStatement(ParserContext ctx) {

        ASTNode forNode = ctx.expected("Expected FOR keyword in statement.", FOR);
        ASTNode primary = parsePrimary(ctx);
        ASTNode in = ctx.expected("Expected IN keyword in statement.", IN);
        ASTNode expr = parseExpression(ctx);

        ASTNode indent = null;
        ASTNode dedent = null;
        ASTNode statements = null;

        if (ctx.is(INDENT)) {
            indent = ctx.expected("Expected INDENT in for statement.", INDENT);
            statements = parseStatementList(ctx, ctx::statementStarts);
            if (isNull(statements))
                ctx.addInvalidRange();
            dedent = ctx.expected("Expected DEDENT in for statement.", DEDENT);
        }
        return new ASTNode(FOR_STATEMENT, null, forNode, primary, in, expr, indent, statements, dedent);

    }

    @FunctionalInterface
    public interface Checker {
        boolean check();
    }

    private ASTNode parseStatementList(ParserContext ctx, Checker checker) {
        List<SyntaxNode> list = new ArrayList<>();

        while (checker.check()) {
            ASTNode node = parseStatement(ctx);
            if (isNull(node))
                ctx.addInvalidRange();
            else list.add(node);
        }
        if (list.isEmpty()) return null;
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


    @Override
    public ParseResult parse(String s) {


        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.lex(s);

        Logger logger = new Logger(Logger.Stage.LEXICAL);
        ParserContext ctx = new ParserContext(tokens, logger, s);

        for (Token t : tokens) ctx.logger.info(t, "Token added - ");

        ctx.logger.updateStage(Logger.Stage.SYNTAX);

        List<SyntaxNode> statements = parse(ctx);

        ctx.logger.printTree(new ASTNode(SOURCE_TEXT, null, new ASTNode(LIST, null, statements)), true);

        ctx.logger.close();

        ctx.getDiagnostics().forEach(d -> System.out.println(d.info().errorCode().name()));

        return new SysproParseResult(new ASTNode(SOURCE_TEXT, null, new ASTNode(LIST, null, statements)),
                ctx.getInvalidRanges(), ctx.getDiagnostics());
    }
}
