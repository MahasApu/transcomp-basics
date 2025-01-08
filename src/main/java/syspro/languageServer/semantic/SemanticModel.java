package syspro.languageServer.semantic;

import syspro.tm.parser.Diagnostic;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.parser.TextSpan;
import syspro.tm.symbols.SemanticSymbol;
import syspro.tm.symbols.SyntaxNodeWithSymbols;
import syspro.tm.symbols.TypeSymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class SemanticModel implements syspro.tm.symbols.SemanticModel {


    public SyntaxNode root;
    public Collection<TextSpan> invalidRanges;
    Collection<Diagnostic> diagnostics;
    private Collection<SemanticSymbol> symbols = new ArrayList<>();

    public SemanticModel(SyntaxNode root, Collection<TextSpan> invalidRanges, Collection<Diagnostic> diagnostics) {
        this.root = root;
        this.invalidRanges = invalidRanges;
        this.diagnostics = diagnostics;
        getAllSymbols();
    }

    private void getAllSymbols() {
        SyntaxNode listNode =  root.slot(0);
        for (int i = 0; i < listNode.slotCount(); i++) {
            SyntaxNodeWithSymbols node = (SyntaxNodeWithSymbols)listNode.slot(i);
            collectSymbolsRecursive(node);
        }
    }

    private void collectSymbolsRecursive(SyntaxNode node) {
        if (node instanceof SyntaxNodeWithSymbols syntaxNodeWithSymbols && syntaxNodeWithSymbols.symbol() != null) {
            symbols.add(syntaxNodeWithSymbols.symbol());

            if (syntaxNodeWithSymbols.symbol() instanceof TypeSymbol typeSymbol && !Objects.isNull(typeSymbol.members())) {

                for (var member : typeSymbol.members()) {
                    if (member instanceof SemanticSymbol memberSymbol) {
                        symbols.add(memberSymbol);
                    }
                }
            }
        }
        for (int i = 0; i < node.slotCount(); i++) {
            SyntaxNode child = node.slot(i);
            if (child != null) {
                collectSymbolsRecursive(child);
            }
        }
    }

    @Override
    public SyntaxNode root() {
        return root;
    }

    @Override
    public Collection<TextSpan> invalidRanges() {
        return invalidRanges;
    }

    @Override
    public Collection<Diagnostic> diagnostics() {
        return diagnostics;
    }

    @Override
    public List<? extends TypeSymbol> typeDefinitions() {
        List<TypeSymbol> typeSymbols = new ArrayList<>();
        for (SemanticSymbol symbol : symbols) {
            if (symbol instanceof TypeSymbol) {
                typeSymbols.add((TypeSymbol) symbol);
            }
        }
        return typeSymbols;
    }

    @Override
    public TypeSymbol lookupType(String name) {
        for (SemanticSymbol symbol : symbols) {
            if (symbol instanceof TypeSymbol typeSymbol && typeSymbol.name().equals(name)) {
                return typeSymbol;
            }
        }
        return null;
    }
}
