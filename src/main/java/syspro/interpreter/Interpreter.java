//package syspro.interpreter;
//
//import syspro.languageServer.Environment;
//import syspro.languageServer.exceptions.LanguageServerException;
//import syspro.parser.Parser;
//import syspro.parser.ast.ASTNode;
//import syspro.tm.parser.AnySyntaxKind;
//import syspro.tm.parser.ParseResult;
//import syspro.tm.parser.SyntaxNode;
//import syspro.tm.symbols.SemanticModel;
//
//
//import static syspro.tm.lexer.Symbol.*;
//import static syspro.tm.parser.SyntaxKind.*;
//
//public class Interpreter implements syspro.tm.symbols.LanguageServer {
//
//    private void analyze(SyntaxNode tree, Environment ctx) {
//        SyntaxNode list = tree.slot(0);
//        for (int i = 0; i < list.slotCount(); i++) {
//            visit(list.slot(i), ctx);
//        }
//    }
//
//    private Object visit(SyntaxNode node, Environment ctx) {
//        switch (node.kind()) {
//            case TYPE_DEFINITION -> analyzeTypeDef(node, ctx);
//            case VARIABLE_DEFINITION -> analyzeVarDef(node, ctx);
//            case FUNCTION_DEFINITION -> analyzeFuncDef(node, ctx);
//            default -> throw new LanguageServerException(node, "Unexpected node kind: " + node.kind());
//        }
//        return null;
//    }
//
//
//    private void analyzeTypeDef(SyntaxNode node, Environment ctx) {
//        SyntaxNode keyword = node.slot(0);
//        // boolean isInterface = keyword.token().toString().equals("interface");
//
//        SyntaxNode name = node.slot(1);
//        String typeName = name.toString();
//
//        if (ctx.isDefined(typeName)) System.out.println("Type is already defined");
//        ASTNode genericsNode = (ASTNode) node.slot(3);
//        ASTNode typeBoundsNode = (ASTNode) node.slot(6);
//        ASTNode membersNode = (ASTNode) node.slot(8);
//
//        TypeDefinition typeDef = new TypeDefinition(null, keyword.kind(), null, typeName, genericsNode, typeBoundsNode, membersNode);
//        ctx.define(typeName, typeDef);
//
//        SyntaxNode members = node.slot(8);
//        if (members != null) {
//            for (int i = 0; i < members.slotCount(); i++) {
//                visit(members.slot(i), ctx);
//            }
//        }
//    }
//
//
//    private void analyzeVarDef(SyntaxNode node, Environment ctx) {
//        String name = node.slot(1).token().toString();
//        if (ctx.isDefined(name)) {
//            throw new LanguageServerException(node, "Variable '" + name + "' is already defined.");
//        }
//
//        SyntaxNode type = node.slot(2);
//        ctx.define(name, new VariableDefinition(null, type.kind(), null));
//    }
//
//
//    private void analyzeFuncDef(SyntaxNode node, Environment ctx) {
//        String name = node.slot(1).token().toString();
//        if (ctx.isDefined(name)) {
//            throw new LanguageServerException(node, "Function '" + name + "' is already defined.");
//        }
//
//        ASTNode params = (ASTNode) node.slot(2);
//        ctx.define(name, new FunctionDefinition(null, node.slot(1).kind(), null, params));
//    }
//
//
//    private void analyzeExpressionStatement(SyntaxNode node, Environment ctx) {
//        visit(node.slot(0), ctx);
//    }
//
//
//    private void analyzeIfStatement(SyntaxNode node, Environment ctx) {
//        Object cond = visit(node.slot(0), ctx);
//
//        if (!(cond instanceof Boolean)) {
//            throw new LanguageServerException(node, "Condition in if statement must be a boolean.");
//        }
//        if ((boolean) cond) visit(node.slot(1), ctx);
//        else if (node.slotCount() > 2) visit(node.slot(2), ctx);
//
//    }
//
//
//    private void analyzeReturnStatement(SyntaxNode node, Environment ctx) {
//        visit(node.slot(0), ctx);
//    }
//
//
//    private void analyzeWhileStatement(SyntaxNode node, Environment ctx) {
//        Object cond;
//        while (true) {
//            cond = visit(node.slot(0), ctx);
//            if (!(cond instanceof Boolean)) {
//                throw new LanguageServerException(node, "Condition in while statement must be a boolean.");
//            }
//            if (!(boolean) cond) break;
//            visit(node.slot(1), ctx);
//        }
//    }
//
//
//    private void analyzeAssignStatement(SyntaxNode node, Environment ctx) {
//        ASTNode left = (ASTNode) node.slot(0);
//        ASTNode right = (ASTNode) node.slot(1);
//
//        // hmm
//        Object rightValue = visit(right, ctx);
//
//        if (left.kind().isNameExpression()) {
//            String varName = left.slot(0).token().toString();
//            if (ctx.isDefined(varName)) {
//                ctx.define(varName, new VariableDefinition(null, node.kind(), null));
//            } else {
//                throw new LanguageServerException(node, "Variable '" + varName + "' is not defined.");
//            }
//        } else {
//            throw new LanguageServerException(node, "Invalid left-hand side for assignment.");
//        }
//    }
//
//
//    private Object analyzeLogicalExpression(SyntaxNode node, Environment ctx) {
//        Object left = visit(node.slot(0), ctx);
//
//        if (node.kind().equals(LOGICAL_OR_EXPRESSION)) {
//            if (ctx.getBoolean(node.slot(0))) return left;
//        } else if (ctx.getBoolean(node.slot(0))) return left;
//        return visit(node.slot(2), ctx);
//    }
//
//    private Object analyzeBinaryExpression(SyntaxNode node, Environment ctx) {
//        Object left = visit(node.slot(0), ctx);
//        Object right = visit(node.slot(2), ctx);
//        AnySyntaxKind operator = node.slot(1).token().toSyntaxKind();
//
//        switch (operator) {
//            case EXCLAMATION_EQUALS:
//                assert left != null;
//                return !left.equals(right);
//
//            case EQUALS_EQUALS:
//                assert left != null;
//                return left.equals(right);
//
//            case GREATER_THAN:
//                assert isNumeric(left, right);
//                return (double) left > (double) right;
//
//            case GREATER_THAN_EQUALS:
//                assert isNumeric(left, right);
//                return (double) left >= (double) right;
//
//            case LESS_THAN:
//                assert isNumeric(left, right);
//                return (double) left < (double) right;
//
//            case LESS_THAN_EQUALS:
//                assert isNumeric(left, right);
//                return (double) left <= (double) right;
//
//            case MINUS:
//                assert isNumeric(left, right);
//                return (double) left - (double) right;
//
//            case PLUS:
//                if (left instanceof Double && right instanceof Double) {
//                    return (double) left + (double) right;
//                }
//                if (left instanceof String && right instanceof String) {
//                    return (String) left + (String) right;
//                }
//                throw new LanguageServerException(node, "Operands must be two numbers or two strings.");
//
//            case SLASH:
//                assert isNumeric(left, right);
//                return (double) left / (double) right;
//
//            case ASTERISK:
//                assert isNumeric(left, right);
//                return (double) left * (double) right;
//
//            default:
//                throw new LanguageServerException(node, "Unsupported binary operator: " + operator);
//        }
//    }
//
//    private boolean isNumeric(Object left, Object right) {
//        return (left instanceof Number) && (right instanceof Number);
//    }
//
//    private boolean isNumeric(Object left) {
//        return (left instanceof Number);
//    }
//
//
//    private Object analyzeUnaryExpression(SyntaxNode node, Environment ctx) {
//        Object right = visit(node.slot(1), ctx);
//        AnySyntaxKind operator = node.slot(0).token().toSyntaxKind();
//
//        return switch (operator) {
//            case EXCLAMATION -> !ctx.getBoolean(node.slot(0));
//            case MINUS -> {
//                assert isNumeric(right);
//                yield -(double) right;
//            }
//            case PLUS -> {
//                assert isNumeric(right);
//                yield (double) right;
//            }
//            case TILDE -> {
//                assert isNumeric(right);
//                yield ~(int) right;
//            }
//            default -> throw new IllegalStateException("Unexpected value: " + operator);
//        };
//    }
//
//    private void analyzeNameExpression(SyntaxNode node, Environment ctx) {
//        String name = node.slot(0).token().toString();
//        if (node.slotCount() > 1 && node.slot(1).kind() == LESS_THAN) {
//            if (!ctx.isDefined(name)) {
//                throw new LanguageServerException(node, "Undefined generic type '" + name + "'");
//            }
//        }
//    }
//
//    private void analyzeSuperExpression(SyntaxNode node, Environment ctx) {
//        if (!ctx.isInsideClass()) {
//            throw new LanguageServerException(node, "'super' used outside of a class.");
//        }
//    }
//
//    private void analyzeThisExpression(SyntaxNode node, Environment ctx) {
//        if (!ctx.isInsideClass()) {
//            throw new LanguageServerException(node, "'this' used outside of a class.");
//        }
//    }
//
//
//    @Override
//    public SemanticModel buildModel(String code) {
//
//        Parser parser = new Parser();
//        ParseResult result = parser.parse(code);
//        SyntaxNode tree = result.root();
//        Environment ctx = new Environment(tree);
//
//        analyze(tree, ctx);
//
////        return new syspro.languageServer.semantic.SemanticModel(null, null, null);
//        return null;
//    }
//}
