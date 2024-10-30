package syspro.lexer.utils;

import syspro.tm.lexer.BuiltInType;
import syspro.tm.lexer.Keyword;
import syspro.tm.lexer.Symbol;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilMaps {
    static final public Map<String, Symbol> symbolMap = Stream.of(Symbol.values())
            .collect(Collectors.toMap(k -> k.text, Function.identity()));
    static final public Map<String, Keyword> keywordMap = Stream.of(Keyword.values())
            .collect(Collectors.toMap(k -> k.text, Function.identity()));
    static final public Map<String, BuiltInType> builtInTypeMap = Map.of(
            "i32", BuiltInType.INT32,
            "i64", BuiltInType.INT64,
            "u32", BuiltInType.UINT32,
            "u64", BuiltInType.UINT64,
            "true", BuiltInType.BOOLEAN,
            "false", BuiltInType.BOOLEAN
    );
}
