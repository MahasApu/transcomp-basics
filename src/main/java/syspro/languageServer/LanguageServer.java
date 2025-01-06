package syspro.languageServer;

import syspro.languageServer.exceptions.LanguageServerException;
import syspro.languageServer.semantic.SemanticNode;
import syspro.languageServer.symbols.FunctionSymbol;
import syspro.languageServer.symbols.TypeParameterSymbol;
import syspro.languageServer.symbols.TypeSymbol;
import syspro.languageServer.symbols.VariableSymbol;
import syspro.parser.Parser;
import syspro.parser.ast.ASTNode;
import syspro.tm.lexer.Keyword;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static syspro.tm.parser.SyntaxKind.*;

public class LanguageServer implements syspro.tm.symbols.LanguageServer {

    private List<SyntaxNode> analyze(SyntaxNode tree, Environment env) {
        SyntaxNode list = tree.slot(0);
        List<SyntaxNode> nodes = new ArrayList<>();

        for (int i = 0; i < list.slotCount(); i++) {
            nodes.add(visit(list.slot(i), env));
        }
        return nodes;
    }

    private SyntaxNodeWithSymbols visit(SyntaxNode node, Environment env) {
        return switch (node.kind()) {
            case TYPE_DEFINITION -> analyzeTypeDefinition(node, env);
            case VARIABLE_DEFINITION -> analyzeVariableDefinition(node, env);
            case FUNCTION_DEFINITION -> analyzeFunctionDefinition(node, env);
            case PARAMETER_DEFINITION -> analyzeParameterDefinition(node, env);
            case TYPE_PARAMETER_DEFINITION -> analyzeTypeParameterDefinition(node, env);
            case EXPRESSION_STATEMENT -> analyzeExpressionStatement(node, env);
            case IF_STATEMENT -> analyzeIfStatement(node, env);
            case FOR_STATEMENT -> analyzeForStatement(node, env);
            case WHILE_STATEMENT -> analyzeWhileStatement(node, env);
            case ASSIGNMENT_STATEMENT -> analyzeAssignStatement(node, env);
            case SUPER_EXPRESSION -> analyzeSuperExpression(node, env);
            case THIS_EXPRESSION -> analyzeThisExpression(node, env);

            default -> new SemanticNode(null, node);
        };
    }

    private SyntaxNodeWithSymbols analyzeForStatement(SyntaxNode node, Environment env) {
        SyntaxNode initializer = node.slot(0);
        SyntaxNode condition = node.slot(1);
        SyntaxNode increment = node.slot(2);
        SyntaxNode body = node.slot(3);

        visit(initializer, env);
        visit(condition, env);
        visit(increment, env);
        visit(body, env);

        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeThisExpression(SyntaxNode node, Environment env) {
        // 'this' expression refers to the current class instance
        SemanticSymbol owner = env.getOwner();
        if (!(owner instanceof TypeSymbol)) {
            throw new LanguageServerException(node, "'this' can only be used within a class or object.");
        }
        return new SemanticNode(owner, node);
    }

    private SyntaxNodeWithSymbols analyzeSuperExpression(SyntaxNode node, Environment env) {
        // 'super' expression refers to the base class
        SemanticSymbol owner = env.getOwner();
        if (!(owner instanceof TypeSymbol typeSymbol) || typeSymbol.baseTypes().isEmpty()) {
            throw new LanguageServerException(node, "'super' can only be used within a class with a base type.");
        }
        return new SemanticNode(typeSymbol.baseTypes().getFirst(), node);
    }

    private SyntaxNodeWithSymbols analyzeAssignStatement(SyntaxNode node, Environment env) {
        SyntaxNode left = node.slot(0);
        SyntaxNode right = node.slot(2);

        SemanticNode leftSymbol = (SemanticNode) visit(left, env);
        SemanticNode rightSymbol = (SemanticNode) visit(right, env);

        if (!leftSymbol.symbol().name().equals(rightSymbol.symbol().name())) {
            throw new LanguageServerException(node, "Type mismatch in assignment statement.");
        }

        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeWhileStatement(SyntaxNode node, Environment env) {
        SyntaxNode condition = node.slot(0);
        SyntaxNode body = node.slot(1);

        visit(condition, env);
        visit(body, env);

        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeReturnStatement(SyntaxNode node, Environment env) {
        SyntaxNode value = node.slot(0);
        if (!isNull(value)) {
            visit(value, env);
        }
        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeIfStatement(SyntaxNode node, Environment env) {
        SyntaxNode condition = node.slot(0);
        SyntaxNode thenBlock = node.slot(1);
        SyntaxNode elseBlock = node.slot(2);

        visit(condition, env);
        visit(thenBlock, env);
        if (!isNull(elseBlock)) {
            visit(elseBlock, env);
        }
        return new SemanticNode(null, node);
    }

    private SyntaxNodeWithSymbols analyzeExpressionStatement(SyntaxNode node, Environment env) {
        SyntaxNode expression = node.slot(0);
        visit(expression, env);
        return new SemanticNode(null, node);
    }


    // Only for type definition (in case of generics)
    private SyntaxNodeWithSymbols analyzeTypeParameterDefinition(SyntaxNode node, Environment env) {
        String typeName = node.slot(0).token().toString();

        if (env.isDefined(typeName))
            throw new LanguageServerException(node, "Type parameter '" + typeName + "' is already defined.");

        SyntaxNode boundsNode = node.slot(1);
        List<TypeSymbol> bounds = new ArrayList<>();
        if (!isNull(boundsNode)) {
            SyntaxNode boundList = boundsNode.slot(1);
            for (int i = 0; i < boundList.slotCount(); i++) {
                SyntaxNode boundNode = boundList.slot(i);
                String boundName = boundNode.slot(0).token().toString();
                SemanticSymbol boundSymbol = env.lookup(boundName);
                if (isNull(boundSymbol)) boundSymbol = new TypeSymbol(boundName, null, null, null, boundNode);
                if (!(boundSymbol instanceof TypeSymbol))
                    throw new LanguageServerException(boundNode, "Invalid type bound: " + boundName);

                bounds.add((TypeSymbol) boundSymbol);
            }
        }

        TypeParameterSymbol paramSymbol = new TypeParameterSymbol(typeName, env.getOwner());

        env.declare(typeName, paramSymbol);
        return new SemanticNode(paramSymbol, node);
    }


    // new ASTNode(PARAMETER_DEFINITION, null, identifier, colon, name);

    // Only in function definition
    private SyntaxNodeWithSymbols analyzeParameterDefinition(SyntaxNode node, Environment env) {
        String paramName = node.slot(0).token().toString();
        SyntaxNode typeNode = node.slot(2);

        TypeLikeSymbol paramType = null;
        if (!isNull(typeNode)) {
            String typeName = typeNode.slot(0).token().toString();
            paramType = (TypeLikeSymbol) env.lookup(typeName);
            if (isNull(paramType)) paramType = new TypeParameterSymbol(typeName, env.getOwner());
        }

        VariableSymbol paramSymbol = new VariableSymbol(paramName, paramType, null, SymbolKind.PARAMETER, node);
        env.declare(paramName, paramSymbol);
        paramSymbol = new VariableSymbol(paramName, paramType, env.get().lookupSymbol(env.get().getName()), SymbolKind.PARAMETER, node);

        return new SemanticNode(paramSymbol, node);
    }

//    return new ASTNode(TYPE_DEFINITION, token,
//    keyword, name, lessThan, generics, greaterThan, typeBoundsList, indent, memberDef, dedent);

    private SyntaxNodeWithSymbols analyzeTypeDefinition(SyntaxNode node, Environment env) {
        SyntaxNode keyword = node.slot(0);
        SyntaxNode name = node.slot(1);
        String typeName = name.token().toString();

        if (env.isDefined(typeName)) {
            throw new LanguageServerException(node, "Type '" + typeName + "' is already defined.");
        }

        boolean isInterface = keyword.kind() == Keyword.INTERFACE;

        TypeSymbol ownerSymbol = new TypeSymbol(typeName, null, null, null, node);
        List<TypeParameterSymbol> typeParameters = analyzeTypeParameters(node.slot(3), ownerSymbol);
        List<TypeSymbol> baseTypes = analyzeBaseTypes(node.slot(5), env);


        TypeSymbol typeSymbol = new TypeSymbol(typeName, null, typeParameters, baseTypes, node);

        env.declare(typeName, typeSymbol);

        SyntaxNode members = node.slot(7);
        List<SyntaxNode> nodes = analyzeTypeMembers(members, env, typeSymbol);
        if (!nodes.isEmpty()) ((ASTNode) node).updateSlot(7, new ASTNode(members.kind(), members.token(), nodes));

        typeSymbol = new TypeSymbol(typeName,
                nodes.stream().map(s -> (SyntaxNodeWithSymbols) s).map(SyntaxNodeWithSymbols::symbol).map(s -> (MemberSymbol) s).toList(),
                typeParameters, baseTypes, node);

        env.define(typeName, new SemanticNode(typeSymbol, node));

        return new SemanticNode(typeSymbol, node);
    }

    private List<TypeParameterSymbol> analyzeTypeParameters(SyntaxNode generics, TypeSymbol ownerSymbol) {
        List<TypeParameterSymbol> typeParameters = new ArrayList<>();
        if (!isNull(generics)) {
            for (int i = 0; i < generics.slotCount(); i++) {
                SyntaxNode paramNode = generics.slot(i);
                String paramName = paramNode.slot(0).token().toString();
                TypeParameterSymbol paramSymbol = new TypeParameterSymbol(paramName, ownerSymbol);
                typeParameters.add(paramSymbol);
            }
        }
        return typeParameters;
    }

    private List<SyntaxNode> analyzeTypeMembers(SyntaxNode members, Environment env, TypeSymbol typeSymbol) {
        List<SyntaxNode> nodes = new ArrayList<>();
        if (!isNull(members)) {
            for (int i = 0; i < members.slotCount(); i++) {
                env.push(new Scope(env.get(), typeSymbol.name()));
                nodes.add(visit(members.slot(i), env));
                env.pop();
            }
        }
        return nodes;
    }

    private List<TypeSymbol> analyzeBaseTypes(SyntaxNode typeBounds, Environment env) {
        List<TypeSymbol> baseTypes = new ArrayList<>();
        if (!isNull(typeBounds)) {
            SyntaxNode separatedList = typeBounds.slot(1);
            for (int i = 0; i < separatedList.slotCount(); i++) {
                SyntaxNode boundNode = separatedList.slot(i);
                String boundName = boundNode.slot(0).token().toString();

                SemanticSymbol baseType = env.lookup(boundName);
                if (isNull(baseType)) baseType = new TypeSymbol(boundName, null, null, null, boundNode);
                if (!(baseType instanceof TypeSymbol)) throw new LanguageServerException(boundNode, boundName);
                baseTypes.add((TypeSymbol) baseType);
            }
        }
        return baseTypes;
    }

//    new ASTNode(VARIABLE_DEFINITION, null, keyword, name, colon, typeExpr, eq, valueExpr);

    private SyntaxNodeWithSymbols analyzeVariableDefinition(SyntaxNode node, Environment env) {
        String name = node.slot(1).token().toString();
        if (env.isDefined(name))
            throw new LanguageServerException(node, "Variable '" + name + "' is already defined.");


        SyntaxNode type = node.slot(3);
        TypeLikeSymbol varTypeSymbol = null;
        if (!isNull(type)) {
            String typeName = type.slot(0).token().toString();
            varTypeSymbol = (TypeLikeSymbol) env.lookup(typeName);
        }

        SymbolKind kind = env.isInsideFunction() ? SymbolKind.LOCAL : SymbolKind.FIELD;


        VariableSymbol symbol = new VariableSymbol(name, varTypeSymbol, null, kind, node);
        env.declare(name, symbol);
        symbol = new VariableSymbol(name, varTypeSymbol, env.get().lookupSymbol(env.get().getName()), kind, node);
        return new SemanticNode(symbol, node);
    }


    //    ASTNode node = new ASTNode(FUNCTION_DEFINITION, null,
    //    terminalList, def, functionName, openParen, parameterList, closeParen,
    //            colon, returnType, indent, functionBody, dedent);

    private SyntaxNodeWithSymbols analyzeFunctionDefinition(SyntaxNode node, Environment env) {
        String name = node.slot(2).token().toString();
        boolean isConstructor = name.equals("this");

        if (env.isDefined(name)) {
            throw new LanguageServerException(node, "Function '" + name + "' is already defined.");
        }

        List<Boolean> modifiers = getFuncTerminalsInfo(node.slot(0));
        TypeLikeSymbol returnType = getReturnType(node.slot(7), isConstructor, env);

        FunctionSymbol symbol = new FunctionSymbol(name, returnType, List.of(), List.of(),
                modifiers.get(0), modifiers.get(1), modifiers.get(2), modifiers.get(3), null, node);

        env.declare(name, symbol);
        env.push(new Scope(env.get(), name));

        SyntaxNode paramsNode = node.slot(4);
        List<SyntaxNode> nodes = analyzeFunctionParameters(paramsNode, env);
        if (!nodes.isEmpty()) ((ASTNode) node).updateSlot(4, new ASTNode(paramsNode.kind(), paramsNode.token(), nodes));


        SyntaxNode bodyNode = node.slot(9); // Slot 10 is the body
        List<SyntaxNode> bodyNodes = new ArrayList<>();
//        if (!isNull(bodyNode)) {
//            for (int i = 0; i < bodyNode.slotCount(); i++) {
//                bodyNodes.add(visit(bodyNode.slot(i), env)); // Analyze statements in the body
//            }
//            ((ASTNode) node).updateSlot(9, new ASTNode(bodyNode.kind(), bodyNode.token(), bodyNodes));
//        }

        SemanticSymbol owner = env.getOwner();
        symbol = new FunctionSymbol(
                name,
                returnType,
                nodes.stream().map(s -> (SyntaxNodeWithSymbols) s).map(SyntaxNodeWithSymbols::symbol).map(s -> (VariableSymbol) s).toList(),
                bodyNodes.stream().map(s -> ((SyntaxNodeWithSymbols) s).symbol()).map(s -> (VariableSymbol) s).toList(),
                modifiers.get(0), modifiers.get(1), modifiers.get(2), modifiers.get(3),
                owner,
                node);

        for (SyntaxNode n : nodes) {
            n = (SyntaxNodeWithSymbols) n;
        }


        env.pop();

        return new SemanticNode(symbol, node);
    }

    private TypeLikeSymbol getReturnType(SyntaxNode returnTypeNode, boolean isConstructor, Environment env) {
        if (isConstructor) return null;
        if (isNull(returnTypeNode)) return null;

        return (TypeLikeSymbol) env.lookup(returnTypeNode.slot(0).token().toString());
    }


    private List<Boolean> getFuncTerminalsInfo(SyntaxNode terminalList) {
        boolean isNative = false;
        boolean isVirtual = false;
        boolean isAbstract = false;
        boolean isOverride = false;

        if (!isNull(terminalList)) {
            for (int i = 0; i < terminalList.slotCount(); i++) {
                SyntaxNode terminal = terminalList.slot(i);
                switch (terminal.token().toString()) {
                    case "native" -> isNative = true;
                    case "virtual" -> isVirtual = true;
                    case "abstract" -> isAbstract = true;
                    case "override" -> isOverride = true;
                }
            }
        }

        return List.of(isNative, isVirtual, isAbstract, isOverride);
    }

    private List<SyntaxNode> analyzeFunctionParameters(SyntaxNode paramsNode, Environment env) {
        List<SyntaxNode> nodes = new ArrayList<>();
        if (!isNull(paramsNode)) {
            for (int i = 0; i < paramsNode.slotCount(); i++) {
                nodes.add(visit(paramsNode.slot(i), env));
            }
        }
        return nodes;
    }


    @Override
    public SemanticModel buildModel(String code) {

        Parser parser = new Parser();
        ParseResult result = parser.parse(code);
        SyntaxNode tree = result.root();
        Environment env = new Environment(tree);

        List<SyntaxNode> semanticNodes = analyze(tree, env);


        syspro.languageServer.semantic.SemanticModel model = new syspro.languageServer.semantic.SemanticModel(
                new ASTNode(SOURCE_TEXT, null, new ASTNode(LIST, null, semanticNodes)),
                List.of(), List.of());

        return model;
    }
}
