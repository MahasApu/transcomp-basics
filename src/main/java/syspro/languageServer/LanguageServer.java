package syspro.languageServer;

import syspro.languageServer.diagnostics.DefinitionError;
import syspro.languageServer.diagnostics.FunctionError;
import syspro.languageServer.diagnostics.TypeParameterError;
import syspro.languageServer.diagnostics.VariableError;
import syspro.languageServer.symbols.FunctionSymbol;
import syspro.languageServer.symbols.TypeParameterSymbol;
import syspro.languageServer.symbols.TypeSymbol;
import syspro.languageServer.symbols.VariableSymbol;
import syspro.parser.Parser;
import syspro.parser.ast.ASTNode;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;
import static syspro.tm.parser.SyntaxKind.*;

public class LanguageServer implements syspro.tm.symbols.LanguageServer {

    private void analyze(ASTNode tree, Environment env) {
        if (isNull(tree)) return;
        for (int i = 0; i < tree.slotCount(); i++) {
            visit((ASTNode) tree.slot(i), env);
        }
    }

    private void visit(ASTNode node, Environment env) {
        if (isNull(node)) return;
        switch (node.kind()) {
            case TYPE_DEFINITION -> analyzeTypeDefinition(node, env);
            case VARIABLE_DEFINITION -> analyzeVariableDefinition(node, env);
            case FUNCTION_DEFINITION -> analyzeFunctionDefinition(node, env);
            case PARAMETER_DEFINITION -> analyzeParameterDefinition(node, env);
            case TYPE_PARAMETER_DEFINITION -> analyzeTypeParameterDefinition(node, env);
            case EXPRESSION_STATEMENT -> analyzeExpressionStatement(node, env);
            case IF_STATEMENT -> analyzeIfStatement(node, env);
            case FOR_STATEMENT -> analyzeForStatement(node, env);
            case WHILE_STATEMENT -> analyzeWhileStatement(node, env);
            case SUPER_EXPRESSION -> analyzeSuperExpression(node, env);
            case THIS_EXPRESSION -> analyzeThisExpression(node, env);
            case IDENTIFIER_NAME_EXPRESSION -> analyzeIdentifierNameExpression(node, env);
            default -> analyze(node, env);
        }
    }

    private void analyzeIdentifierNameExpression(ASTNode node, Environment env) {
        String name = node.slot(0).token().toString();
        ASTNode identifier = (ASTNode) node.slot(0);


        VariableSymbol symbol = new VariableSymbol(name, null, env.get().getSymbol(), SymbolKind.LOCAL, identifier);
        node.updateSymbol(symbol);
    }


    // new ASTNode(FOR_STATEMENT, null, forNode, primary, in, expr, indent, statements, dedent)
    private void analyzeForStatement(ASTNode node, Environment env) {
        ASTNode primary = (ASTNode) node.slot(1);
        ASTNode iterable = (ASTNode) node.slot(3);
        ASTNode body = (ASTNode) node.slot(5);

        visit(primary, env);
        visit(iterable, env);
        visit(body, env);

        ((VariableSymbol) primary.symbol()).updateDefinition(node);
        node.updateSymbol(primary.symbol());
        env.declare(node.symbol().name(), node.symbol(), node);
        primary.updateSymbol(null);

    }

    private void analyzeThisExpression(ASTNode node, Environment env) {
        SemanticSymbol owner = env.getOwner();
        if (!(owner instanceof TypeSymbol)) {
            env.addInvalidRange(node.span(), new VariableError("'this' can only be used within a class or object."));
        }
    }


    private void analyzeSuperExpression(ASTNode node, Environment env) {
        SemanticSymbol owner = env.getOwner();
        if (!(owner instanceof TypeSymbol typeSymbol) || typeSymbol.baseTypes().isEmpty()) {
            env.addInvalidRange(node.span(), new VariableError("'super' can only be used within a class or object."));
        }
    }

    //   new ASTNode(WHILE_STATEMENT, null, whileNode, cond, indent, statements, dedent);
    private void analyzeWhileStatement(ASTNode node, Environment env) {
        ASTNode condition = (ASTNode) node.slot(0);
        ASTNode body = (ASTNode) node.slot(1);

        visit(condition, env);
        visit(body, env);

    }

    // ASTNode(IF_STATEMENT, null, ifNode, cond, indentTrue, statementsTrue, dedentTrue,
    //                             elseNode, indentFalse, statementsFalse, dedentFalse);
    private void analyzeIfStatement(ASTNode node, Environment env) {
        ASTNode condition = (ASTNode) node.slot(1);
        ASTNode thenBlock = (ASTNode) node.slot(3);
        ASTNode elseBlock = (ASTNode) node.slot(7);

        visit(condition, env);
        if (!isNull(elseBlock)) visit(thenBlock, env);
        if (!isNull(elseBlock)) visit(elseBlock, env);

    }

    private void analyzeExpressionStatement(ASTNode node, Environment env) {
        ASTNode expression = (ASTNode) node.slot(0);
        visit(expression, env);
    }


    // ASTNode(PARAMETER_DEFINITION, null, identifier, colon, name)
    // Only for type definition (in case of generics)
    private void analyzeTypeParameterDefinition(ASTNode node, Environment env) {
        String typeName = node.slot(0).token().toString();

        TypeLikeSymbol paramSymbol = (TypeLikeSymbol) env.lookup(typeName);
        if (isNull(paramSymbol)) {
            paramSymbol = new TypeParameterSymbol(typeName, env.get().getSymbol(), node);
            env.declare(typeName, paramSymbol, node);
        }
        paramSymbol = construct(paramSymbol, (ASTNode) paramSymbol.definition(), env);
        node.updateSymbol(paramSymbol);

        ASTNode boundsNode = (ASTNode) node.slot(1);
        if (!isNull(boundsNode)) {
            ASTNode boundList = (ASTNode) boundsNode.slot(1);

            for (int i = 0; i < boundList.slotCount(); i += 2) {

                ASTNode boundNode = (ASTNode) boundList.slot(i);
                String boundName = boundNode.slot(0).token().toString();
                SemanticSymbol boundSymbol = env.lookup(boundName);

                if (!(boundSymbol instanceof TypeLikeSymbol)) {
                    env.addInvalidRange(boundNode.span(), new TypeParameterError("Invalid type bound: " + boundName));
                } else {
                    boundSymbol = construct((TypeLikeSymbol) boundSymbol, boundNode, env);
                    boundNode.updateSymbol(boundSymbol);
                    ((TypeParameterSymbol) paramSymbol).bounds.add((TypeLikeSymbol) boundSymbol);
                }
            }
        }
    }


    // new ASTNode(PARAMETER_DEFINITION, null, identifier, colon, name);
    // Only in function definition
    private void analyzeParameterDefinition(ASTNode node, Environment env) {
        String paramName = node.slot(0).token().toString();
        ASTNode typeNode = (ASTNode) node.slot(2);

        TypeLikeSymbol paramType = null;
        if (!isNull(typeNode)) {
            String typeName = typeNode.slot(0).token().toString();
            paramType = (TypeLikeSymbol) env.lookup(typeName);
            if (isNull(paramType)) {
                paramType = new TypeParameterSymbol(typeName, env.getOwner(), typeNode);
                env.declare(paramName, paramType, typeNode);
            }
            paramType = construct(paramType, typeNode, env);
        }

        VariableSymbol paramSymbol = new VariableSymbol(paramName, paramType,
                env.get().getSymbol(),
                SymbolKind.PARAMETER, node);
        env.declare(paramName, paramSymbol, node);
        node.updateSymbol(paramSymbol);

    }

    private TypeLikeSymbol construct(TypeLikeSymbol symbol, ASTNode node, Environment env) {
        if (!isNull(node) && node.kind().equals(GENERIC_NAME_EXPRESSION)) {
            ASTNode list = (ASTNode) node.slot(2);
            List<TypeLikeSymbol> params = new ArrayList<>();
            for (int j = 0; j < list.slotCount(); j += 2)
                params.add((TypeLikeSymbol) env.lookup(list.slot(j).slot(0).token().toString()));
            symbol = ((TypeSymbol) symbol).construct(params);
        }
        return symbol;
    }

    //    return new ASTNode(TYPE_DEFINITION, token,
//    keyword, name, lessThan, generics, greaterThan, typeBoundsList, indent, memberDef, dedent);
    private void analyzeTypeDefinition(ASTNode node, Environment env) {
        SyntaxNode name = node.slot(1);
        String typeName = name.token().toString();

        TypeSymbol typeSymbol = new TypeSymbol(typeName, node);
        env.declare(typeName, typeSymbol, node);

        env.push(new Scope(env.get(), typeName, typeSymbol));

        analyze((ASTNode) node.slot(3), env);
        typeSymbol.typeArguments = env.get().getAllTypeParameters();

        typeSymbol.baseTypes = analyzeBaseTypes((ASTNode) node.slot(5), env);


        ASTNode members = (ASTNode) node.slot(7);
        analyze(members, env);

        typeSymbol.members = env.get().getAllMembers();

        env.pop();
        node.updateSymbol(typeSymbol);
        env.define(typeName, node);

    }


    private List<TypeSymbol> analyzeBaseTypes(ASTNode typeBounds, Environment env) {
        List<TypeSymbol> baseTypes = new ArrayList<>();
        if (!isNull(typeBounds)) {
            SyntaxNode separatedList = typeBounds.slot(1);
            for (int i = 0; i < separatedList.slotCount(); i += 2) {
                ASTNode baseNode = (ASTNode) separatedList.slot(i);
                String baseName = baseNode.slot(0).token().toString();

                TypeLikeSymbol baseType = (TypeLikeSymbol) env.lookup(baseName);

                if (isNull(baseType) || !(baseType instanceof TypeSymbol))
                    env.addInvalidRange(baseNode.span(), new DefinitionError("Undefined type in base types: " + baseName));

                baseType = construct(baseType, baseNode, env);
                baseTypes.add((TypeSymbol) baseType);
                baseNode.updateSymbol(baseType);
            }
        }
        return baseTypes;

    }

    //    new ASTNode(VARIABLE_DEFINITION, null, keyword, name, colon, typeExpr, eq, valueExpr);
    private void analyzeVariableDefinition(ASTNode node, Environment env) {
        String name = node.slot(1).token().toString();
        if (env.isDefined(name))
            env.addInvalidRange(node.span(), new DefinitionError("Variable '" + name + "' is already defined." + name));

        SyntaxNode type = node.slot(3);
        TypeLikeSymbol varTypeSymbol = null;
        if (!isNull(type)) {
            String typeName = type.slot(0).token().toString();
            varTypeSymbol = (TypeLikeSymbol) env.lookup(typeName);
            varTypeSymbol = construct(varTypeSymbol, (ASTNode) type, env);
        }

        SymbolKind kind = env.isInsideFunction() ? SymbolKind.LOCAL : SymbolKind.FIELD;

        VariableSymbol symbol = new VariableSymbol(name, varTypeSymbol, env.get().getSymbol(), kind, node);
        env.declare(name, symbol, node);
        node.updateSymbol(symbol);
    }


    //    ASTNode node = new ASTNode(FUNCTION_DEFINITION, null,
    //    terminalList, def, functionName, openParen, parameterList, closeParen,
    //            colon, returnType, indent, functionBody, dedent);
    private void analyzeFunctionDefinition(ASTNode node, Environment env) {
        String name = node.slot(2).token().toString();
        boolean isConstructor = name.equals("this");

        TypeSymbol owner = (TypeSymbol) env.get().getSymbol();
        List<Boolean> modifiers = getFuncTerminalsInfo((ASTNode) node.slot(0), owner.isAbstract());
        TypeLikeSymbol returnType = getReturnType((ASTNode) node.slot(7), isConstructor, env, (List<TypeLikeSymbol>) owner.typeArguments);

        FunctionSymbol symbol = new FunctionSymbol(name, returnType,
                modifiers.get(0), modifiers.get(1), modifiers.get(2), modifiers.get(3),
                owner, node);

        SemanticSymbol existingSymbol = env.lookup(name);
        env.declare(name, symbol, node);
        env.push(new Scope(env.get(), name, symbol));

        ASTNode paramNode = (ASTNode) node.slot(4);
        analyze(paramNode, env);

        if (existingSymbol instanceof FunctionSymbol existingFunc) {
            boolean hasClash = hasClashingSignature(existingFunc, env.get().getAllParameters());
            if (!existingFunc.isAbstract() && hasClash)
                env.addInvalidRange(node.span(), new FunctionError("Function overload with clashing signature: " + name));
        }

        ASTNode bodyNode = (ASTNode) node.slot(9);
        analyze(bodyNode, env);

        symbol.parameters = env.get().getAllParameters();
        symbol.locals = env.get().getAllLocals();

        env.pop();

        node.updateSymbol(symbol);

    }

    private boolean hasClashingSignature(FunctionSymbol existingFunc, List<VariableSymbol> actualParams) {

        List<VariableSymbol> existingParams = existingFunc.parameters;
        if (existingParams.size() != actualParams.size()) return false;
        for (int i = 0; i < actualParams.size(); i++)
            if (!Objects.equals(actualParams.get(i).type().name(), existingParams.get(i).type().name())) return false;
        return true;
    }

    private TypeLikeSymbol getReturnType(ASTNode returnTypeNode, boolean isConstructor, Environment env, List<TypeLikeSymbol> params) {
        if (isConstructor) return (TypeLikeSymbol) env.get().getSymbol();
        if (isNull(returnTypeNode)) return null;

        TypeLikeSymbol result = (TypeLikeSymbol) env.lookup(returnTypeNode.slot(0).token().toString());
        if (!isNull(result) && result instanceof TypeSymbol symbol) return symbol.construct(params);
        return result;
    }


    private List<Boolean> getFuncTerminalsInfo(ASTNode terminalList, boolean isAbstract) {
        boolean isNative = false;
        boolean isVirtual = isAbstract;
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

        if (isOverride || isAbstract) isVirtual = true;

        return List.of(isNative, isVirtual, isAbstract, isOverride);
    }


    @Override
    public SemanticModel buildModel(String code) {

        Parser parser = new Parser();
        ParseResult result = parser.parse(code);
        ASTNode tree = (ASTNode) result.root();
        Environment env = new Environment(tree);

        analyze((ASTNode) tree.slot(0), env);

        env.invalidRanges().addAll(result.invalidRanges());
        env.diagnostics().addAll(result.diagnostics());

        return new syspro.languageServer.semantic.SemanticModel(
                tree,
                env.invalidRanges(),
                env.diagnostics());
    }
}
