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
import syspro.tm.parser.ErrorCode;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.symbols.SemanticModel;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SymbolKind;
import syspro.tm.symbols.TypeLikeSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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


    // FOR_STATEMENT - forNode, primary, in, expr, indent, statements, dedent
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

    // WHILE_STATEMENT - whileNode, cond, indent, statements, dedent
    private void analyzeWhileStatement(ASTNode node, Environment env) {
        ASTNode condition = (ASTNode) node.slot(0);
        ASTNode body = (ASTNode) node.slot(1);

        visit(condition, env);
        visit(body, env);

    }

    // IF_STATEMENT - ifNode, cond, indentTrue, statementsTrue, dedentTrue,
    //                  elseNode, indentFalse, statementsFalse, dedentFalse
    private void analyzeIfStatement(ASTNode node, Environment env) {
        ASTNode condition = (ASTNode) node.slot(1);
        ASTNode thenBlock = (ASTNode) node.slot(3);
        ASTNode elseBlock = (ASTNode) node.slot(7);

        visit(condition, env);
        if (!isNull(elseBlock)) visit(thenBlock, env);
        if (!isNull(elseBlock)) visit(elseBlock, env);
    }

    private void analyzeIdentifierNameExpression(ASTNode node, Environment env) {
        String name = node.slot(0).token().toString();
        ASTNode identifier = (ASTNode) node.slot(0);

        VariableSymbol symbol = new VariableSymbol(name, null, env.get().getSymbol(), SymbolKind.LOCAL, identifier);
        node.updateSymbol(symbol);
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

    private void analyzeExpressionStatement(ASTNode node, Environment env) {
        ASTNode expression = (ASTNode) node.slot(0);
        visit(expression, env);
    }


    // Analyzes a list of type arguments or parameters in a separated list.
    private <T extends TypeLikeSymbol> void analyzeSeparatedList(
            ASTNode separatedList,
            List<? super T> symbols,
            Function<String, ErrorCode> errorConstructor,
            Environment env) {

        for (int i = 0; i < separatedList.slotCount(); i += 2) {
            ASTNode node = (ASTNode) separatedList.slot(i);
            String nodeName = node.slot(0).token().toString();
            SemanticSymbol symbol = env.lookup(nodeName);

            // Check if the type has been defined.
            if (!(symbol instanceof TypeLikeSymbol)) {
                env.addInvalidRange(node.span(), errorConstructor.apply("Undefined type: " + nodeName));
            } else {
                T typedSymbol = (T) construct((TypeLikeSymbol) symbol, node, env);
                node.updateSymbol(typedSymbol);
                symbols.add(typedSymbol);
            }
        }
    }

    // Only for type definition (in case of generics)
    // PARAMETER_DEFINITION - identifier, colon, name
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
            analyzeSeparatedList(boundList, ((TypeParameterSymbol) paramSymbol).bounds, TypeParameterError::new, env);
        }
    }

    // Only in function definition.
    // PARAMETER_DEFINITION - identifier, colon, name
    private void analyzeParameterDefinition(ASTNode node, Environment env) {
        String paramName = node.slot(0).token().toString();
        ASTNode typeNode = (ASTNode) node.slot(2);

        SemanticSymbol paramType = null;

        if (!isNull(typeNode)) {
            String typeName = typeNode.slot(0).token().toString();
            paramType = env.lookup(typeName);

            if (!(paramType instanceof TypeLikeSymbol))
                env.addInvalidRange(typeNode.span(), new DefinitionError("Undefined type in base types: " + paramName));
            else paramType = construct((TypeLikeSymbol) paramType, typeNode, env);
        }

        VariableSymbol paramSymbol = new VariableSymbol(paramName, (TypeLikeSymbol) paramType,
                env.get().getSymbol(),
                SymbolKind.PARAMETER, node);
        env.declare(paramName, paramSymbol, node);
        node.updateSymbol(paramSymbol);

    }

    // Constructs a specialized version of a TypeSymbol based on
    // the given node and environment (by calling construct function).
    private TypeLikeSymbol construct(TypeLikeSymbol symbol, ASTNode node, Environment env) {
        if (!isNull(node) && node.kind().equals(GENERIC_NAME_EXPRESSION)) {
            ASTNode list = (ASTNode) node.slot(2);
            List<TypeLikeSymbol> params = new ArrayList<>();

            for (int i = 0; i < list.slotCount(); i += 2) {
                String name = list.slot(i).slot(0).token().toString();
                params.add((TypeLikeSymbol) env.lookup(name));
            }
            symbol = ((TypeSymbol) symbol).construct(params);
        }
        return symbol;
    }


    //    TYPE_DEFINITION - keyword, name, lessThan, generics, greaterThan,
    //                      typeBoundsList, indent, memberDef, dedent
    private void analyzeTypeDefinition(ASTNode node, Environment env) {
        SyntaxNode name = node.slot(1);
        String typeName = name.token().toString();

        TypeSymbol typeSymbol = new TypeSymbol(typeName, node);
        env.declare(typeName, typeSymbol, node);

        env.push(new Scope(env.get(), typeName, typeSymbol));

        // Analyze type arguments of type.
        analyze((ASTNode) node.slot(3), env);
        typeSymbol.typeArguments = env.get().getAllTypeParameters();

        // Analyze base types of type.
        analyzeBaseTypes((ASTNode) node.slot(5), env);

        // Analyze members (such as functions, variables) of type.
        analyze((ASTNode) node.slot(7), env);
        typeSymbol.members = env.get().getAllMembers();

        env.pop();
        node.updateSymbol(typeSymbol);
        env.define(typeName, node);

    }

    // SEPARATED_LIST [NameExpression, & - separator]
    private void analyzeBaseTypes(ASTNode typeBounds, Environment env) {
        if (!isNull(typeBounds)) {
            ASTNode separatedList = (ASTNode) typeBounds.slot(1);
            List<TypeSymbol> baseTypes = ((TypeSymbol) env.get().getSymbol()).baseTypes;
            analyzeSeparatedList(separatedList, baseTypes, DefinitionError::new, env);
        }
    }

    //  VARIABLE_DEFINITION - keyword, name, colon, typeExpr, eq, valueExpr
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


    //    FUNCTION_DEFINITION - terminalList, def, functionName, openParen, parameterList, closeParen,
    //                          colon, returnType, indent, functionBody, dedent
    private void analyzeFunctionDefinition(ASTNode node, Environment env) {
        String name = node.slot(2).token().toString();
        boolean isConstructor = name.equals("this");

        TypeSymbol owner = (TypeSymbol) env.get().getSymbol();

        // Return modifiers (native, virtual, abstract, override) and the type of the function.
        List<Boolean> modifiers = getFuncTerminalsInfo((ASTNode) node.slot(0), owner.isAbstract());
        TypeLikeSymbol returnType = getReturnType((ASTNode) node.slot(7), isConstructor, env, owner.typeArguments);

        FunctionSymbol symbol = new FunctionSymbol(name, returnType,
                modifiers.get(0), modifiers.get(1), modifiers.get(2), modifiers.get(3),
                owner, node);

        SemanticSymbol existingSymbol = env.lookup(name);
        env.declare(name, symbol, node);
        env.push(new Scope(env.get(), name, symbol));

        ASTNode paramNode = (ASTNode) node.slot(4);
        analyze(paramNode, env);

        // Check whether there is a defined function with the same signature.
        if (existingSymbol instanceof FunctionSymbol existingFunc) {
            boolean hasClash = env.hasClashingSignature(existingFunc, env.get().getAllParameters());
            if (!existingFunc.isAbstract() && hasClash)
                env.addInvalidRange(node.span(), new FunctionError("Function overload with clashing signature: " + name));
        }

        // Analyze body of the function.
        analyze((ASTNode) node.slot(9), env);

        symbol.parameters = env.get().getAllParameters();
        symbol.locals = env.get().getAllLocals();

        env.pop();

        node.updateSymbol(symbol);

    }

    private TypeLikeSymbol getReturnType(ASTNode returnTypeNode, boolean isConstructor, Environment env, List<? extends TypeLikeSymbol> params) {
        if (isConstructor) return (TypeLikeSymbol) env.get().getSymbol();
        if (isNull(returnTypeNode)) return null;

        // Get the already defined (maybe not) type symbol by returning the name of the TypeNode.
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

        // Functions of interfaces are always virtual in SysPro language.
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
