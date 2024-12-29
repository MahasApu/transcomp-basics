package syspro.languageServer.semantic;

import syspro.tm.parser.Diagnostic;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.parser.TextSpan;
import syspro.tm.symbols.TypeSymbol;

import java.util.Collection;
import java.util.List;

public class SemanticModel implements syspro.tm.symbols.SemanticModel {


    public SyntaxNode root;
    public Collection<TextSpan> invalidRanges;
    Collection<Diagnostic> diagnostics;

    public SemanticModel(SyntaxNode root, Collection<TextSpan> invalidRanges, Collection<Diagnostic> diagnostics) {
        this.root = root;
        this.invalidRanges = invalidRanges;
        this.diagnostics = diagnostics;
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
        return List.of();
    }

    @Override
    public TypeSymbol lookupType(String name) {
        return null;
    }
}
