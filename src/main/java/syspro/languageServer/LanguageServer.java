package syspro.languageServer;

import syspro.languageServer.exceptions.LanguageServerException;
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
            case ASSIGNMENT_STATEMENT -> analyzeAssignStatement(node, env);
            case SUPER_EXPRESSION -> analyzeSuperExpression(node, env);
            case THIS_EXPRESSION -> analyzeThisExpression(node, env);
            case IDENTIFIER_NAME_EXPRESSION -> analyzeIdentifierNameExpression(node, env);
            default -> analyze(node, env);
        }
    }

    private void analyzeIdentifierNameExpression(ASTNode node, Environment env) {
        String name = node.slot(0).token().toString();
        ASTNode identifier = (ASTNode) node.slot(0);

        VariableSymbol symbol = new VariableSymbol(name, null, env.get().lookupSymbol(env.get().getName()), SymbolKind.LOCAL, identifier);
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
        env.declare(node.symbol().name(), node.symbol());
        primary.updateSymbol(null);

    }

    private void analyzeThisExpression(ASTNode node, Environment env) {
        SemanticSymbol owner = env.getOwner();
        if (!(owner instanceof TypeSymbol)) {
            throw new LanguageServerException(node, "'this' can only be used within a class or object.");
        }
    }


    private void analyzeSuperExpression(ASTNode node, Environment env) {
        SemanticSymbol owner = env.getOwner();
        if (!(owner instanceof TypeSymbol typeSymbol) || typeSymbol.baseTypes().isEmpty()) {
            throw new LanguageServerException(node, "'super' can only be used within a class with a base type.");
        }
    }

    // new ASTNode(ASSIGNMENT_STATEMENT, null, primary, eq, expr)
    private void analyzeAssignStatement(ASTNode node, Environment env) {
        ASTNode primary = (ASTNode) node.slot(0);
        ASTNode expression = (ASTNode) node.slot(2);

        visit(primary, env);
        visit(expression, env);
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

        if (env.isDefined(typeName))
            throw new LanguageServerException(node, "Type parameter '" + typeName + "' is already defined.");

        ASTNode boundsNode = (ASTNode) node.slot(1);
        List<TypeSymbol> bounds = new ArrayList<>();
        if (!isNull(boundsNode)) {
            ASTNode boundList = (ASTNode) boundsNode.slot(1);
            for (int i = 0; i < boundList.slotCount(); i++) {
                ASTNode boundNode = (ASTNode) boundList.slot(i);
                String boundName = boundNode.slot(0).token().toString();
                SemanticSymbol boundSymbol = env.lookup(boundName);
                if (isNull(boundSymbol)) boundSymbol = new TypeSymbol(boundName, boundNode);
                if (!(boundSymbol instanceof TypeSymbol))
                    throw new LanguageServerException(boundNode, "Invalid type bound: " + boundName);

                bounds.add((TypeSymbol) boundSymbol);
            }
        }

        TypeParameterSymbol paramSymbol = new TypeParameterSymbol(typeName, env.getOwner());

        env.declare(typeName, paramSymbol);
        node.updateSymbol(paramSymbol);
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
            if (isNull(paramType)) paramType = new TypeParameterSymbol(typeName, env.getOwner());
        }

        VariableSymbol paramSymbol = new VariableSymbol(paramName, paramType, env.get().lookupSymbol(env.get().getName()), SymbolKind.PARAMETER, node);
        env.declare(paramName, paramSymbol);
        node.updateSymbol(paramSymbol);

    }

    //    return new ASTNode(TYPE_DEFINITION, token,
//    keyword, name, lessThan, generics, greaterThan, typeBoundsList, indent, memberDef, dedent);
    private void analyzeTypeDefinition(ASTNode node, Environment env) {
        SyntaxNode keyword = node.slot(0);
        SyntaxNode name = node.slot(1);
        String typeName = name.token().toString();


        boolean isInterface = keyword.kind() == Keyword.INTERFACE;

        TypeSymbol typeSymbol = new TypeSymbol(typeName, node);
        env.declare(typeName, typeSymbol);

        env.push(new Scope(env.get(), typeName));

        typeSymbol.typeArguments = analyzeTypeParameters((ASTNode) node.slot(3), env);
        typeSymbol.baseTypes = analyzeBaseTypes((ASTNode) node.slot(5), env);


        ASTNode members = (ASTNode) node.slot(7);
        analyze(members, env);

        typeSymbol.members = env.get().getAllMembers();;

        env.pop();
        node.updateSymbol(typeSymbol);
        env.define(typeName, node);

    }

    private List<TypeLikeSymbol> analyzeTypeParameters(ASTNode generics, Environment env) {
        List<TypeLikeSymbol> typeParameters = new ArrayList<>();
        if (!isNull(generics)) {
            for (int i = 0; i < generics.slotCount(); i++) {
                SyntaxNode paramNode = generics.slot(i);
                String paramName = paramNode.slot(0).token().toString();



                TypeLikeSymbol paramSymbol = (TypeLikeSymbol) env.lookup(paramName);
                if (isNull(paramSymbol)) paramSymbol = new TypeParameterSymbol(paramName, env.get().lookupSymbol(env.get().getName()));
                typeParameters.add(paramSymbol);
            }
        }
        return typeParameters;
    }


    private List<TypeSymbol> analyzeBaseTypes(ASTNode typeBounds, Environment env) {
        List<TypeSymbol> baseTypes = new ArrayList<>();
        if (!isNull(typeBounds)) {
            SyntaxNode separatedList = typeBounds.slot(1);
            for (int i = 0; i < separatedList.slotCount(); i++) {
                SyntaxNode boundNode = separatedList.slot(i);
                String boundName = boundNode.slot(0).token().toString();

                SemanticSymbol baseType = env.lookup(boundName);
                if (isNull(baseType)) baseType = new TypeSymbol(boundName, boundNode);
                if (!(baseType instanceof TypeSymbol)) throw new LanguageServerException(boundNode, boundName);
                baseTypes.add((TypeSymbol) baseType);
            }
        }
        return baseTypes;

    }

    //    new ASTNode(VARIABLE_DEFINITION, null, keyword, name, colon, typeExpr, eq, valueExpr);
    private void analyzeVariableDefinition(ASTNode node, Environment env) {
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

        VariableSymbol symbol = new VariableSymbol(name, varTypeSymbol, env.get().lookupSymbol(env.get().getName()), kind, node);
        env.declare(name, symbol);
        node.updateSymbol(symbol);
    }


    //    ASTNode node = new ASTNode(FUNCTION_DEFINITION, null,
    //    terminalList, def, functionName, openParen, parameterList, closeParen,
    //            colon, returnType, indent, functionBody, dedent);
    private void analyzeFunctionDefinition(ASTNode node, Environment env) {
        String name = node.slot(2).token().toString();
        boolean isConstructor = name.equals("this");

        List<Boolean> modifiers = getFuncTerminalsInfo((ASTNode) node.slot(0));
        TypeLikeSymbol returnType = getReturnType((ASTNode) node.slot(7), isConstructor, env);

        FunctionSymbol symbol = new FunctionSymbol(name, returnType,
                modifiers.get(0), modifiers.get(1), modifiers.get(2), modifiers.get(3),
                env.get().lookupSymbol(env.get().getName()), node);

        env.declare(name, symbol);
        env.push(new Scope(env.get(), name));

        ASTNode paramNode = (ASTNode) node.slot(4);
        analyze(paramNode, env);

        ASTNode bodyNode = (ASTNode) node.slot(9);
        analyze(bodyNode, env);

        symbol.parameters = env.get().getAllParameters();
        symbol.locals = env.get().getAllLocals();

        env.pop();

        node.updateSymbol(symbol);
    }


    private TypeLikeSymbol getReturnType(ASTNode returnTypeNode, boolean isConstructor, Environment env) {
        if (isConstructor) return null;
        if (isNull(returnTypeNode)) return null;

        return (TypeLikeSymbol) env.lookup(returnTypeNode.slot(0).token().toString());
    }


    private List<Boolean> getFuncTerminalsInfo(ASTNode terminalList) {
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

    @Override
    public SemanticModel buildModel(String code) {

        Parser parser = new Parser();
        ParseResult result = parser.parse(code);
        ASTNode tree = (ASTNode) result.root();
        Environment env = new Environment(tree);

        analyze((ASTNode) tree.slot(0), env);


        return new syspro.languageServer.semantic.SemanticModel(
                tree,
                result.invalidRanges(), result.diagnostics());
    }
}
