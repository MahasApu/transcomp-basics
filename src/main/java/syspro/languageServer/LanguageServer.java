package syspro.languageServer;

import syspro.languageServer.exceptions.InterpreterException;
import syspro.languageServer.symbols.FunctionDefinition;
import syspro.languageServer.symbols.TypeDefinition;
import syspro.languageServer.symbols.VariableDefinition;
import syspro.parser.Parser;
import syspro.parser.ast.ASTNode;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticModel;


import static syspro.tm.lexer.Symbol.*;
import static syspro.tm.parser.SyntaxKind.*;

public class LanguageServer implements syspro.tm.symbols.LanguageServer {

    private void interpret(SyntaxNode tree, InterpreterContext ctx) {
        SyntaxNode list = tree.slot(0);
        for (int i = 0; i < list.slotCount(); i++) {
            evaluate(list.slot(i), ctx);
        }
    }

    private Object evaluate(SyntaxNode node, InterpreterContext ctx) {
        switch (node.kind()) {
            case TYPE_DEFINITION -> evalTypeDef(node, ctx);
            case VARIABLE_DEFINITION -> evalVarDef(node, ctx);
            case FUNCTION_DEFINITION -> evalFuncDef(node, ctx);
            case EXPRESSION_STATEMENT -> evalExpressionStatement(node, ctx);
            case IF_STATEMENT -> evalIfStatement(node, ctx);
            case RETURN_STATEMENT -> evalReturnStatement(node, ctx);
            case WHILE_STATEMENT -> evalWhileStatement(node, ctx);
            case ASSIGNMENT_STATEMENT -> evalAssignStatement(node, ctx);
            case SUPER_EXPRESSION -> evalSuperExpression(node, ctx);
            case THIS_EXPRESSION -> evalThisExpression(node, ctx);
            default -> throw new InterpreterException(node, "Unexpected node kind: " + node.kind());
        }
        return null;
    }


    private void evalTypeDef(SyntaxNode node, InterpreterContext ctx) {
        SyntaxNode keyword = node.slot(0);
        // boolean isInterface = keyword.token().toString().equals("interface");

        SyntaxNode name = node.slot(1);
        String typeName = name.toString();

        if (ctx.hasDefinition(typeName)) System.out.println("Type is already defined");
        ASTNode genericsNode = (ASTNode) node.slot(3);
        ASTNode typeBoundsNode = (ASTNode) node.slot(6);
        ASTNode membersNode = (ASTNode) node.slot(8);

        TypeDefinition typeDef = new TypeDefinition(null, keyword.kind(), null, typeName, genericsNode, typeBoundsNode, membersNode);
        ctx.addDefinition(typeName, typeDef);

        SyntaxNode members = node.slot(8);
        if (members != null) {
            for (int i = 0; i < members.slotCount(); i++) {
                evaluate(members.slot(i), ctx);
            }
        }
    }


    private void evalVarDef(SyntaxNode node, InterpreterContext ctx) {
        String name = node.slot(1).token().toString();
        if (ctx.hasDefinition(name)) {
            throw new InterpreterException(node, "Variable '" + name + "' is already defined.");
        }

        SyntaxNode type = node.slot(2);
        ctx.addDefinition(name, new VariableDefinition(null, type.kind(), null));
    }


    private void evalFuncDef(SyntaxNode node, InterpreterContext ctx) {
        String name = node.slot(1).token().toString();
        if (ctx.hasDefinition(name)) {
            throw new InterpreterException(node, "Function '" + name + "' is already defined.");
        }

        ASTNode params = (ASTNode) node.slot(2);
        ctx.addDefinition(name, new FunctionDefinition(null, node.slot(1).kind(), null, params));
    }


    private void evalExpressionStatement(SyntaxNode node, InterpreterContext ctx) {
        evaluate(node.slot(0), ctx);
    }


    private void evalIfStatement(SyntaxNode node, InterpreterContext ctx) {
        Object cond = evaluate(node.slot(0), ctx);

        if (!(cond instanceof Boolean)) {
            throw new InterpreterException(node, "Condition in if statement must be a boolean.");
        }
        if ((boolean) cond) evaluate(node.slot(1), ctx);
        else if (node.slotCount() > 2) evaluate(node.slot(2), ctx);

    }


    private void evalReturnStatement(SyntaxNode node, InterpreterContext ctx) {
        evaluate(node.slot(0), ctx);
    }


    private void evalWhileStatement(SyntaxNode node, InterpreterContext ctx) {
        Object cond;
        while (true) {
            cond = evaluate(node.slot(0), ctx);
            if (!(cond instanceof Boolean)) {
                throw new InterpreterException(node, "Condition in while statement must be a boolean.");
            }
            if (!(boolean) cond) break;
            evaluate(node.slot(1), ctx);
        }
    }


    private void evalAssignStatement(SyntaxNode node, InterpreterContext ctx) {
        ASTNode left = (ASTNode) node.slot(0);
        ASTNode right = (ASTNode) node.slot(1);

        // hmm
        Object rightValue = evaluate(right, ctx);

        if (left.kind().isNameExpression()) {
            String varName = left.slot(0).token().toString();
            if (ctx.hasDefinition(varName)) {
                ctx.addDefinition(varName, new VariableDefinition(null, node.kind(), null));
            } else {
                throw new InterpreterException(node, "Variable '" + varName + "' is not defined.");
            }
        } else {
            throw new InterpreterException(node, "Invalid left-hand side for assignment.");
        }
    }


    private Object evalLogicalExpression(SyntaxNode node, InterpreterContext ctx) {
        Object left = evaluate(node.slot(0), ctx);

        if (node.kind().equals(LOGICAL_OR_EXPRESSION)) {
            if (ctx.getBoolean(node.slot(0))) return left;
        } else if (ctx.getBoolean(node.slot(0))) return left;
        return evaluate(node.slot(2), ctx);
    }

    private Object evalBinaryExpression(SyntaxNode node, InterpreterContext ctx) {
        Object left = evaluate(node.slot(0), ctx);
        Object right = evaluate(node.slot(2), ctx);
        AnySyntaxKind operator = node.slot(1).token().toSyntaxKind();

        switch (operator) {
            case EXCLAMATION_EQUALS:
                assert left != null;
                return !left.equals(right);

            case EQUALS_EQUALS:
                assert left != null;
                return left.equals(right);

            case GREATER_THAN:
                assert isNumeric(left, right);
                return (double) left > (double) right;

            case GREATER_THAN_EQUALS:
                assert isNumeric(left, right);
                return (double) left >= (double) right;

            case LESS_THAN:
                assert isNumeric(left, right);
                return (double) left < (double) right;

            case LESS_THAN_EQUALS:
                assert isNumeric(left, right);
                return (double) left <= (double) right;

            case MINUS:
                assert isNumeric(left, right);
                return (double) left - (double) right;

            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new InterpreterException(node, "Operands must be two numbers or two strings.");

            case SLASH:
                assert isNumeric(left, right);
                return (double) left / (double) right;

            case ASTERISK:
                assert isNumeric(left, right);
                return (double) left * (double) right;

            default:
                throw new InterpreterException(node, "Unsupported binary operator: " + operator);
        }
    }

    private boolean isNumeric(Object left, Object right) {
        return (left instanceof Number) && (right instanceof Number);
    }

    private boolean isNumeric(Object left) {
        return (left instanceof Number);
    }


    private Object evalUnaryExpression(SyntaxNode node, InterpreterContext ctx) {
        Object right = evaluate(node.slot(1), ctx);
        AnySyntaxKind operator = node.slot(0).token().toSyntaxKind();

        return switch (operator) {
            case EXCLAMATION -> !ctx.getBoolean(node.slot(0));
            case MINUS -> {
                assert isNumeric(right);
                yield -(double) right;
            }
            case PLUS -> {
                assert isNumeric(right);
                yield (double) right;
            }
            case TILDE -> {
                assert isNumeric(right);
                yield ~(int) right;
            }
            default -> throw new IllegalStateException("Unexpected value: " + operator);
        };
    }

    private void evalNameExpression(SyntaxNode node, InterpreterContext ctx) {
        String name = node.slot(0).token().toString();
        if (node.slotCount() > 1 && node.slot(1).kind() == LESS_THAN) {
            if (!ctx.hasDefinition(name)) {
                throw new InterpreterException(node, "Undefined generic type '" + name + "'");
            }
        }
    }

    private void evalSuperExpression(SyntaxNode node, InterpreterContext ctx) {
        if (!ctx.isInsideClass()) {
            throw new InterpreterException(node, "'super' used outside of a class.");
        }
    }

    private void evalThisExpression(SyntaxNode node, InterpreterContext ctx) {
        if (!ctx.isInsideClass()) {
            throw new InterpreterException(node, "'this' used outside of a class.");
        }
    }


    @Override
    public SemanticModel buildModel(String code) {

        Parser parser = new Parser();
        ParseResult result = parser.parse(code);
        SyntaxNode tree = result.root();
        InterpreterContext ctx = new InterpreterContext(tree);

        interpret(tree, ctx);

//        return new syspro.languageServer.semantic.SemanticModel(null, null, null);
        return null;
    }
}
