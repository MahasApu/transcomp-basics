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
            case PARAMETER_DEFINITION -> analyzeParamDef(node, env);
            case TYPE_PARAMETER_DEFINITION -> analyzeTypeParamDef(node, env);
            default -> throw new LanguageServerException(node, "Unexpected node kind: " + node.kind());
        };
    }


    // Only for type definition (in case of generics)
    private SyntaxNodeWithSymbols analyzeTypeParamDef(SyntaxNode node, Environment env) {
        SyntaxNode identifier = node.slot(0);
        String typeName = identifier.token().toString();

        if (env.isDefined(typeName)) {
            throw new LanguageServerException(node, "Type parameter '" + typeName + "' is already defined.");
        }

        SyntaxNode boundsNode = node.slot(1);
        List<TypeSymbol> bounds = new ArrayList<>();
        if (!isNull(boundsNode)) {
            SyntaxNode boundList = boundsNode.slot(1);
            for (int i = 0; i < boundList.slotCount(); i++) {
                SyntaxNode boundNode = boundList.slot(i);
                String boundName = boundNode.slot(0).token().toString();
                SemanticSymbol boundSymbol = env.lookup(boundName);
                if (isNull(boundSymbol)) {
                    boundSymbol = new TypeSymbol(boundName, null, null, null, boundNode);
                }
                if (!(boundSymbol instanceof TypeSymbol)) {
                    throw new LanguageServerException(boundNode, "Invalid type bound: " + boundName);
                }
                bounds.add((TypeSymbol) boundSymbol);
            }
        }

        TypeParameterSymbol paramSymbol = new TypeParameterSymbol(typeName, env.getOwner());

        env.declare(typeName, paramSymbol);
        return new SemanticNode(paramSymbol, node);
    }

    // new ASTNode(PARAMETER_DEFINITION, null, identifier, colon, name);


    // Only in function definition
    private SyntaxNodeWithSymbols analyzeParamDef(SyntaxNode node, Environment env) {
        SyntaxNode identifier = node.slot(0);
        String paramName = identifier.token().toString();
        SyntaxNode typeNode = node.slot(2);

        TypeLikeSymbol paramType = null;
        if (!isNull(typeNode)) {
            String typeName = typeNode.slot(0).token().toString();
            paramType = (TypeLikeSymbol) env.lookup(typeName);
            if (isNull(paramType)){
                paramType = new TypeParameterSymbol(typeName, env.getOwner());
            }
        }

        VariableSymbol paramSymbol = new VariableSymbol(paramName, paramType, env.getOwner(), SymbolKind.PARAMETER, node);

        env.declare(paramName, paramSymbol);

        return new SemanticNode(paramSymbol, node);
    }

//    return new ASTNode(TYPE_DEFINITION, token,
//    keyword, name, lessThan, generics, greaterThan, typeBoundsList, indent, memberDef, dedent);

    private SyntaxNodeWithSymbols analyzeTypeDef(SyntaxNode node, Environment env) {
        SyntaxNode keyword = node.slot(0);
        SyntaxNode name = node.slot(1);
        String typeName = name.token().toString();

        if (env.isDefined(typeName)) {
            throw new LanguageServerException(node, "Type '" + typeName + "' is already defined.");
        }

        boolean isInterface = keyword.kind() == Keyword.INTERFACE;

        SyntaxNode generics = node.slot(3);
        List<TypeParameterSymbol> typeParameters = new ArrayList<>();
        TypeSymbol ownerSymbol = new TypeSymbol(typeName, null, null, null, node);

        if (!isNull(generics)) {
            for (int i = 0; i < generics.slotCount(); i++) {
                SyntaxNode paramNode = generics.slot(i);
                String paramName = paramNode.slot(0).token().toString();
                TypeParameterSymbol paramSymbol = new TypeParameterSymbol(paramName, ownerSymbol);
                typeParameters.add(paramSymbol);
//                env.declare(paramName, paramSymbol);
            }
        }

        SyntaxNode typeBounds = node.slot(5);
        List<TypeSymbol> baseTypes = new ArrayList<>();
        if (!isNull(typeBounds)) {
            SyntaxNode separatedList = typeBounds.slot(1);
            for (int i = 0; i < separatedList.slotCount(); i++) {
                SyntaxNode boundNode = separatedList.slot(i);
                String boundName = boundNode.slot(0).token().toString();

                SemanticSymbol baseType = env.lookup(boundName);
                if (isNull(baseType)) baseType = new TypeSymbol(boundName, null, null, null, boundNode);
                if (!(baseType instanceof TypeSymbol)) throw new LanguageServerException(boundNode, boundName);
                if (isInterface && !((TypeSymbol) baseType).isAbstract())
                    throw new LanguageServerException(boundNode, "Interface '" + typeName + "' cannot inherit from a non-interface type.");

                baseTypes.add((TypeSymbol) baseType);
            }
        }

        TypeSymbol typeSymbol = new TypeSymbol(typeName, null, typeParameters, baseTypes, node);

        env.declare(typeName, typeSymbol);

        SyntaxNode members = node.slot(7);
        List<SyntaxNodeWithSymbols> nodes = new ArrayList<>();

        if (!isNull(members)) {
            for (int i = 0; i < members.slotCount(); i++) {
                env.push(new Scope(env.get(), typeName));
                nodes.add(visit(members.slot(i), env));

                typeSymbol = new TypeSymbol(typeName,
                        nodes.stream().map(SyntaxNodeWithSymbols::symbol).map(s -> (MemberSymbol) s).toList(),
                        typeParameters, baseTypes, node);

                env.pop();
            }
        }

        env.define(typeName, new SemanticNode(typeSymbol, node));

        return new SemanticNode(typeSymbol, node);
    }


//    new ASTNode(VARIABLE_DEFINITION, null, keyword, name, colon, typeExpr, eq, valueExpr);

    private SyntaxNodeWithSymbols analyzeVarDef(SyntaxNode node, Environment env) {
        String name = node.slot(1).token().toString();
        if (env.isDefined(name))
            throw new LanguageServerException(node, "Variable '" + name + "' is already defined.");


        SyntaxNode type = node.slot(3);
        TypeLikeSymbol varTypeSymbol = null;
        if (!isNull(type)) {
            String typeName = type.slot(0).token().toString();
            varTypeSymbol = (TypeLikeSymbol) env.lookup(typeName);
        }

        VariableSymbol symbol = new VariableSymbol(name, varTypeSymbol, null, SymbolKind.FIELD, node);
        env.declare(name, symbol);
        symbol = new VariableSymbol(name, varTypeSymbol, env.getOwner(), SymbolKind.FIELD, node);
        return new SemanticNode(symbol, node);
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

    //    ASTNode node = new ASTNode(FUNCTION_DEFINITION, null,
    //    terminalList, def, functionName, openParen, parameterList, closeParen,
    //            colon, returnType, indent, functionBody, dedent);

    private SyntaxNodeWithSymbols analyzeFuncDef(SyntaxNode node, Environment env) {
        String name = node.slot(2).token().toString();
        boolean isConstructor = name.equals("this");


        if (env.isDefined(name)) {
            throw new LanguageServerException(node, "Function '" + name + "' is already defined.");
        }

        SyntaxNode terminalList = node.slot(0);
        List<Boolean> modifiers = getFuncTerminalsInfo(terminalList);

        SyntaxNode returnTypeNode = node.slot(7);
        TypeLikeSymbol returnType = null;
        if (!isConstructor && !isNull(returnTypeNode)) {
            returnType = (TypeLikeSymbol) env.lookup(returnTypeNode.slot(0).token().toString());
            // add diagnostic if returnType is null
        }

        SyntaxNode paramsNode = node.slot(4);
        FunctionSymbol symbol = new FunctionSymbol(name, returnType, null, null,
                modifiers.get(0), modifiers.get(1), modifiers.get(2), modifiers.get(3), null, node);

        env.declare(name, symbol);
        env.push(new Scope(env.get(), name));

        List<SyntaxNodeWithSymbols> nodes = new ArrayList<>();
        if (!isNull(paramsNode)) {
            for (int i = 0; i < paramsNode.slotCount(); i += 2) {
                nodes.add(visit(paramsNode.slot(i), env));
            }
        }

        SemanticSymbol owner = env.getOwner();
        symbol = new FunctionSymbol(name, returnType,
                nodes.stream().map(SyntaxNodeWithSymbols::symbol).map(s -> (VariableSymbol) s).toList(),
                null, modifiers.get(0), modifiers.get(1), modifiers.get(2), modifiers.get(3), owner, node);

        env.pop();

        return new SemanticNode(symbol, node);
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
