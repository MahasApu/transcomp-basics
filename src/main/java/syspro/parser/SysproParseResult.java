package syspro.parser;

import syspro.tm.parser.Diagnostic;
import syspro.tm.parser.ParseResult;
import syspro.tm.parser.SyntaxNode;
import syspro.tm.parser.TextSpan;

import java.util.Collection;
import java.util.List;

public class SysproParseResult implements ParseResult {

    public SyntaxNode root;
    public Collection<TextSpan> invalidRanges;
    Collection<Diagnostic> diagnostics;

    SysproParseResult(SyntaxNode root, Collection<TextSpan> invalidRanges, Collection<Diagnostic> diagnostics) {
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
        return List.of();
    }
}
