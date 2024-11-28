package syspro.parser;

import syspro.parser.ast.ASTNode;
import syspro.parser.ast.SyntaxCategory;
import syspro.tm.lexer.*;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxKind;
import syspro.tm.parser.TextSpan;
import syspro.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static syspro.tm.lexer.Keyword.*;
import static syspro.tm.lexer.Symbol.*;
import static syspro.tm.parser.SyntaxKind.*;

public class ParserContext {

    public Logger logger;

    public SysproParseResult result;
    public List<Token> tokens;
    public int pos = -1;
    private boolean invalid = false;
    private List<TextSpan> invalidRanges = new ArrayList<>();

    private static Map<AnySyntaxKind, Integer> precedence = Stream.of(new Object[][]{
            {PLUS, 1},
            {MINUS, 1},
            {TILDE, 1},
            {ASTERISK, 2},
            {SLASH, 2},
            {PERCENT, 2},
            {LESS_THAN_LESS_THAN, 4},
            {GREATER_THAN_GREATER_THAN, 4},
            {AMPERSAND, 5},
            {CARET, 6},
            {BAR, 7},
            {EQUALS_EQUALS, 8},
            {EXCLAMATION_EQUALS, 8},
            {LESS_THAN, 8},
            {LESS_THAN_EQUALS, 8},
            {GREATER_THAN, 8},
            {GREATER_THAN_EQUALS, 8},
            {IS, 8},
            {EXCLAMATION, 9},
            {AMPERSAND_AMPERSAND, 10},
            {BAR_BAR, 11}
    }).collect(Collectors.toMap(data -> (AnySyntaxKind) data[0], data -> (Integer) data[1]));


    public boolean isEOF() {
        return pos >= tokens.size();
    }

    ParserContext(List<Token> tokens, Logger logger) {
        this.logger = logger;
        this.tokens = tokens;
    }

    public boolean is(AnySyntaxKind... kind) {
        if (isEOF()) return false;
        for (AnySyntaxKind k : kind) {
            if (tokens.get(pos).toSyntaxKind().equals(k)) return true;
        }
        return false;
    }

    public AnySyntaxKind kind() {
        assert !isEOF();
        return get().toSyntaxKind();
    }

    public boolean match(AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (is(kind) || (kind instanceof SyntaxCategory && isCategory((SyntaxCategory) kind))) {
                step();
                return true;
            }
        }
        return false;
    }


    public Token get() {
        if (isEOF()) return null;
        return tokens.get(pos);
    }

    public void addInvalidRange(TextSpan textSpan) {
        invalidRanges.add(textSpan);
    }

    public Token step() {
        if (!isEOF()) pos++;
        return prev();
    }

    public Token prev() {
        return tokens.get(pos - 1);
    }


    public ASTNode expected(String msg, AnySyntaxKind... kinds) {
        for (AnySyntaxKind kind : kinds) {
            if (kind instanceof SyntaxCategory && isCategory((SyntaxCategory) kind)) {
                return new ASTNode(kind, step());
            }
            if (is(kind)) {
                return new ASTNode(kind, step());
            }
        }
//        logger.log(Logger.LogLevel.ERROR, Logger.Stage.SYNTAX, msg + " Found " + kind());
        return null; //
    }

    public void setInvalid() {
        invalid = true;
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

    public void updateTokenKind(Keyword kind) {
        if (kind.equals(NULL)) return;
        Token newToken = switch (get()) {
            case IdentifierToken t ->
                    new KeywordToken(t.start, t.end, t.leadingTriviaLength, t.trailingTriviaLength, kind);
            default -> get();
        };
        tokens.set(pos, newToken);
    }

    public int getInvalidEnd() {
        int start = pos;
        int indentLevel = 0;

        while (!isEOF() && !kind().equals(DEDENT)) {
            if (kind().equals(INDENT)) indentLevel++;
            step();
        }

//        while (indentLevel != 0) {
//            if (kind().equals(DEDENT)) indentLevel--;
//            step();
//        }

        return pos - start; // FIXME
    }

    public List<TextSpan> getInvalidRanges() {
        return invalidRanges;
    }


    public boolean isTerm(Token token) {
        return switch (token.toSyntaxKind()) {
            case INDENT, DEDENT, VAL, VAR, LESS_THAN, GREATER_THAN, COLON, EQUALS, IDENTIFIER -> true;
            default -> false;
        };
    }

    public boolean definitionStarts() {
        return switch (kind()) {
            case VAL, VAR, ABSTRACT, VIRTUAL, OVERRIDE, NATIVE, DEF -> true;
            default -> false;
        };
    }

    public boolean statementStarts() {

        boolean isTypeDef = switch (get().toString()) {
            case "class", "object", "interface" -> true;
            default -> false;
        };
        if (isTypeDef) return false;

        return switch (kind()) {
            case THIS, SUPER, IDENTIFIER, VAL, VAR,
                 BREAK, RETURN, CONTINUE, IF, WHILE, FOR -> true;
            default -> false;
        };
    }
}
