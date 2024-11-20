package syspro.parser;

import syspro.parser.ast.SyntaxCategory;
import syspro.parser.exceptions.ParserException;
import syspro.tm.lexer.Keyword;
import syspro.tm.lexer.Symbol;
import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxKind;
import syspro.utils.Logger;

import java.util.List;

import static syspro.tm.parser.SyntaxKind.*;
import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.lexer.Symbol.*;

public class ParserContext {

    public Logger logger;

    public SysproParseResult result;
    public List<Token> tokens;
    public int pos = -1;


    public boolean isEOF() {
        return pos >= tokens.size();
    }

    ParserContext(List<Token> tokens, Logger logger) {
        this.logger = logger;
        this.tokens = tokens;
    }

    public boolean is(AnySyntaxKind kind) {
        if (isEOF()) return false;
        return tokens.get(pos).toSyntaxKind().equals(kind);
    }

    public AnySyntaxKind kind() {
        assert !isEOF();
        return get().toSyntaxKind();
    }

    public boolean match(AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (is(kind)) {
                step();
                return true;
            }
        }
        return false;
    }


    public Token get() {
        return tokens.get(pos);
    }

    public Token look() {
        if (!isEOF()) return get();
        return null;
    }

    public Token step() {
        if (!isEOF()) pos++;
        return prev();
    }

    public Token prev() {
        return tokens.get(pos - 1);
    }


    public Token expected(String msg, AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (kind instanceof SyntaxCategory && isCategory((SyntaxCategory) kind)) {
                return step();
            }
            if (is(kind)) {
                return step();
            }
        }
        logger.log(Logger.LogLevel.ERROR, Logger.Stage.SYNTAX, msg);
        throw new ParserException(); //
    }

    public boolean isCategory(SyntaxCategory category) {
        if (isEOF()) return false;
        return category().equals(category);
    }

    public SyntaxCategory category() {
        return category(kind());
    }

    public static SyntaxCategory category(AnySyntaxKind kind) {
        if (kind instanceof Symbol) return SyntaxCategory.TERMINAL;
        if (kind instanceof Keyword) return SyntaxCategory.TERMINAL;
        if (kind instanceof SyntaxKind syntaxKind) {
            return switch (syntaxKind) {
                // Terminals
                case IDENTIFIER, BOOLEAN, INTEGER, RUNE, STRING -> SyntaxCategory.TERMINAL;

                // Non-terminals
                case SOURCE_TEXT, TYPE_BOUND, LIST, SEPARATED_LIST -> SyntaxCategory.NON_TERMINAL;

                // Definitions
                case TYPE_DEFINITION, FUNCTION_DEFINITION, VARIABLE_DEFINITION, TYPE_PARAMETER_DEFINITION,
                     PARAMETER_DEFINITION -> SyntaxCategory.DEFINITION;

                // Statements
                case IF_STATEMENT, WHILE_STATEMENT, FOR_STATEMENT, RETURN_STATEMENT,
                     BREAK_STATEMENT, CONTINUE_STATEMENT, EXPRESSION_STATEMENT,
                     ASSIGNMENT_STATEMENT, VARIABLE_DEFINITION_STATEMENT -> SyntaxCategory.STATEMENT;

                // Expressions
                case LOGICAL_AND_EXPRESSION, LOGICAL_OR_EXPRESSION, LOGICAL_NOT_EXPRESSION,
                     EQUALS_EXPRESSION, NOT_EQUALS_EXPRESSION, LESS_THAN_EXPRESSION, LESS_THAN_OR_EQUAL_EXPRESSION,
                     GREATER_THAN_EXPRESSION, GREATER_THAN_OR_EQUAL_EXPRESSION,
                     IS_EXPRESSION,
                     BITWISE_AND_EXPRESSION, BITWISE_OR_EXPRESSION, BITWISE_EXCLUSIVE_OR_EXPRESSION,
                     BITWISE_LEFT_SHIFT_EXPRESSION, BITWISE_RIGHT_SHIFT_EXPRESSION, BITWISE_NOT_EXPRESSION,
                     UNARY_PLUS_EXPRESSION, UNARY_MINUS_EXPRESSION,
                     ADD_EXPRESSION, SUBTRACT_EXPRESSION, MULTIPLY_EXPRESSION, DIVIDE_EXPRESSION, MODULO_EXPRESSION ->
                        SyntaxCategory.EXPRESSION;

                // Primary expressions
                case MEMBER_ACCESS_EXPRESSION, INVOCATION_EXPRESSION, INDEX_EXPRESSION,
                     THIS_EXPRESSION, SUPER_EXPRESSION, NULL_LITERAL_EXPRESSION,
                     TRUE_LITERAL_EXPRESSION, FALSE_LITERAL_EXPRESSION, STRING_LITERAL_EXPRESSION,
                     RUNE_LITERAL_EXPRESSION, INTEGER_LITERAL_EXPRESSION, PARENTHESIZED_EXPRESSION ->
                        SyntaxCategory.PRIMARY;

                // Name expressions
                case IDENTIFIER_NAME_EXPRESSION, OPTION_NAME_EXPRESSION, GENERIC_NAME_EXPRESSION ->
                        SyntaxCategory.NAME_EXPRESSION;

                // Indentation and dedentation
                case INDENT -> SyntaxCategory.INDENT;
                case DEDENT -> SyntaxCategory.DEDENT;

                default -> throw new IllegalArgumentException("Unknown SyntaxKind: " + syntaxKind);
            };
        }
        throw new IllegalArgumentException("Invalid SyntaxKind type: " + kind);
    }


    public boolean isTerm(Token token) {
        return switch (token.toSyntaxKind()) {
            case INDENT, DEDENT, VAL, VAR, LESS_THAN, GREATER_THAN, COLON, EQUALS, IDENTIFIER -> true;
            default -> false;
        };
    }

}
