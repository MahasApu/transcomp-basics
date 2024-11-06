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
}
