package syspro.languageServer.semantic;

import syspro.tm.parser.Diagnostic;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.parser.TextSpan;
import syspro.tm.symbols.TypeSymbol;

import java.util.Collection;
import java.util.List;

public class SemanticModel implements syspro.tm.symbols.SemanticModel {
    @Override
    public SyntaxNode root() {
        return null;
    }

    @Override
    public Collection<TextSpan> invalidRanges() {
        return List.of();
    }

    @Override
    public Collection<Diagnostic> diagnostics() {
        return List.of();
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
