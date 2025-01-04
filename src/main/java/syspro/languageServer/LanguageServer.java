package syspro.languageServer;

import syspro.languageServer.exceptions.InterpreterException;
import syspro.languageServer.semantic.SemanticNode;
import syspro.languageServer.symbols.FunctionSymbol;
import syspro.languageServer.symbols.TypeParameterSymbol;
import syspro.languageServer.symbols.TypeSymbol;
import syspro.languageServer.symbols.VariableSymbol;
import syspro.parser.Parser;
import syspro.parser.ast.ASTNode;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.MemberSymbol;
import syspro.tm.symbols.SemanticModel;
import syspro.tm.symbols.SyntaxNodeWithSymbols;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static syspro.tm.parser.SyntaxKind.*;

public class LanguageServer implements syspro.tm.symbols.LanguageServer {

    private SyntaxNodeWithSymbols analyze(SyntaxNode tree, Environment env) {

        SyntaxNode list = tree.slot(0);
        List<SyntaxNode> nodes = new ArrayList<>();

        for (int i = 0; i < list.slotCount(); i++) {
            nodes.add(visit(list.slot(i), env));
        }
        return new SemanticNode(null, new ASTNode(LIST, null, nodes));
    }


    private SyntaxNodeWithSymbols visit(SyntaxNode node, Environment env) {
        return switch (node.kind()) {
            case TYPE_DEFINITION -> analyzeTypeDef(node, env);
            case VARIABLE_DEFINITION -> analyzeVarDef(node, env);
            case FUNCTION_DEFINITION -> analyzeFuncDef(node, env);
            case EXPRESSION_STATEMENT -> analyzeExpressionStatement(node, env);
            case IF_STATEMENT -> analyzeIfStatement(node, env);
            case RETURN_STATEMENT -> analyzeReturnStatement(node, env);
            case WHILE_STATEMENT -> analyzeWhileStatement(node, env);
            case ASSIGNMENT_STATEMENT -> analyzeAssignStatement(node, env);
            case SUPER_EXPRESSION -> analyzeSuperExpression(node, env);
            case THIS_EXPRESSION -> analyzeThisExpression(node, env);
            default -> throw new InterpreterException(node, "Unexpected node kind: " + node.kind());
        };
    }


//    return new ASTNode(TYPE_DEFINITION, token,
//    keyword, name, lessThan, generics, greaterThan, typeBoundsList, indent, memberDef, dedent);

    private SyntaxNodeWithSymbols analyzeTypeDef(SyntaxNode node, Environment env) {
        SyntaxNode keyword = node.slot(0);
        SyntaxNode name = node.slot(1);
        String typeName = name.token().toString();

        if (env.hasDefinition(typeName)) {
            throw new InterpreterException(node, "Type '" + typeName + "' is already defined.");
        }

        SyntaxNode generics = node.slot(3);
        List<TypeParameterSymbol> typeParameters = new ArrayList<>();
        if (!isNull(generics)) {
            SyntaxNode list = generics.slot(0);
            for (int i = 0; i < list.slotCount(); i++) {
                SyntaxNode paramNode = list.slot(i);
                String paramName = paramNode.token().toString();
                TypeParameterSymbol paramSymbol = new TypeParameterSymbol(paramName, null);
                typeParameters.add(paramSymbol);
            }
        }

        SyntaxNode typeBounds = node.slot(5);
        List<TypeSymbol> baseTypes = new ArrayList<>();
        if (!isNull(typeBounds)) {
            for (int i = 0; i < typeBounds.slotCount(); i++) {
                SyntaxNode boundNode = typeBounds.slot(i);
                baseTypes.add(new TypeSymbol(boundNode.token().toString(),
                        null, null, null, boundNode));
            }
        }


        TypeSymbol typeSymbol = new TypeSymbol(typeName,
                null,
                typeParameters,
                baseTypes,
                node);
        TypeSymbol finalTypeSymbol = typeSymbol;
        typeParameters.replaceAll(typeParameterSymbol -> new TypeParameterSymbol(typeParameterSymbol.name(), finalTypeSymbol));
        env.pushToScope(typeSymbol);



        SyntaxNode members = node.slot(7);
        List<SyntaxNodeWithSymbols> nodes = new ArrayList<>();

        if (!isNull(members)) {
            for (int i = 0; i < members.slotCount(); i++) {
                nodes.add(visit(members.slot(i), env));
            }
        }


        List<MemberSymbol> m = nodes.stream().map(SyntaxNodeWithSymbols::symbol).map(symbol -> (MemberSymbol) symbol).toList();
        typeSymbol = new TypeSymbol(typeName,
                m,
                typeParameters,
                baseTypes,
                node);

        env.addDefinition(typeName, new SemanticNode(typeSymbol, node));

        env.popFromScope();

        return new SemanticNode(typeSymbol, node);
    }


//    new ASTNode(VARIABLE_DEFINITION, null, keyword, name, colon, typeExpr, eq, valueExpr);

    private SyntaxNodeWithSymbols analyzeVarDef(SyntaxNode node, Environment env) {
        String name = node.slot(1).token().toString();
        if (env.hasDefinition(name)) {
            throw new InterpreterException(node, "Variable '" + name + "' is already defined.");
        }

        SyntaxNode type = node.slot(3);

        TypeSymbol varTypeSymbol = null;
        if (type != null) {
            varTypeSymbol = new syspro.languageServer.symbols.TypeSymbol(type.token().toString(),
                    null, null, null, type);
        }

        VariableSymbol symbol = new VariableSymbol(name, varTypeSymbol, env.getTopScope(), null, node);

        env.addDefinition(name, new SemanticNode(symbol, node));
        return new SemanticNode(symbol, node);
    }

//    ASTNode node = new ASTNode(FUNCTION_DEFINITION, null,
//    terminalList, def, functionName, openParen, parameterList, closeParen,
//            colon, returnType, indent, functionBody, dedent);

    private SyntaxNodeWithSymbols analyzeFuncDef(SyntaxNode node, Environment env) {
        String name = node.slot(2).token().toString();
        if (env.hasDefinition(name)) {
            throw new InterpreterException(node, "Function '" + name + "' is already defined.");
        }

        SyntaxNode paramsNode = node.slot(4);
        FunctionSymbol symbol = new FunctionSymbol(name, null, null, null,
                false, false, false, false, env.getTopScope(), node);
        env.addDefinition(name, new SemanticNode(symbol, node));
        if (!isNull(paramsNode)) {
            for (int i = 0; i < paramsNode.slotCount(); i++) {
                visit(paramsNode.slot(i), env);
            }
        }

        return new SemanticNode(symbol, node);
    }


    private SyntaxNodeWithSymbols analyzeExpressionStatement(SyntaxNode node, Environment env) {
        SyntaxNode expression = node.slot(0);
        visit(expression, env);
        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeIfStatement(SyntaxNode node, Environment env) {
        SyntaxNode condition = node.slot(0);
        visit(condition, env);

        SyntaxNode thenBlock = node.slot(1);
        visit(thenBlock, env);

        SyntaxNode elseBlock = node.slot(2);
        if (!isNull(elseBlock)) {
            visit(elseBlock, env);
        }

        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeReturnStatement(SyntaxNode node, Environment env) {
        SyntaxNode value = node.slot(0);
        if (!isNull(value)) {
            visit(value, env);
        }
        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeWhileStatement(SyntaxNode node, Environment env) {
        SyntaxNode condition = node.slot(0);
        visit(condition, env);

        SyntaxNode body = node.slot(1);
        visit(body, env);

        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeAssignStatement(SyntaxNode node, Environment env) {
        SyntaxNode left = node.slot(0);
        visit(left, env);

        SyntaxNode right = node.slot(1);
        visit(right, env);

        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeSuperExpression(SyntaxNode node, Environment env) {
        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeThisExpression(SyntaxNode node, Environment env) {
        return new SemanticNode(null, node);
    }


    @Override
    public SemanticModel buildModel(String code) {

        Parser parser = new Parser();
        ParseResult result = parser.parse(code);
        SyntaxNode tree = result.root();
        Environment env = new Environment(tree);

        SyntaxNode semanticTree = analyze(tree, env);

        return new syspro.languageServer.semantic.SemanticModel(new SemanticNode(null, new ASTNode(SOURCE_TEXT, null, semanticTree)), List.of(), List.of());
    }
}
