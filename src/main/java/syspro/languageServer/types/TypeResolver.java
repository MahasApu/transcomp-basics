package syspro.languageServer.types;

public class TypeResolver {
    enum DefaultValues {
        Int32(0),
        Int64(0),
        UInt32(0),
        UInt64(0),
        Boolean(false),
        Rune("U+0000");

        private final Object value;

        DefaultValues(Object value) {
            this.value = value;
        }
    }
}
