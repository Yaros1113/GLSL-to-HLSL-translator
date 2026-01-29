import java.util.*;

public class SymbolTable {
    private final Stack<Map<String, SymbolInfo>> scopes = new Stack<>();
    private final List<String> errors = new ArrayList<>();

    public SymbolTable() {
        enterScope(); // Глобальная область видимости
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    public void addSymbol(String name, SymbolInfo info) {
        if (scopes.peek().containsKey(name)) {
            errors.add("Duplicate symbol: " + name);
            return;
        }
        scopes.peek().put(name, info);
    }

    public SymbolInfo lookup(String name) {
        // Ищем от текущей области к глобальной
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        return null;
    }

    public SymbolInfo lookupCurrentScope(String name) {
        return scopes.peek().get(name);
    }

    public List<String> getErrors() {
        return errors;
    }

    public static class SymbolInfo {
        public final String name;
        public final String type;
        public final SymbolKind kind;
        public final Object extraInfo; // Дополнительная информация

        public SymbolInfo(String name, String type, SymbolKind kind, Object extraInfo) {
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.extraInfo = extraInfo;
        }

        @Override
        public String toString() {
            return kind + " " + name + ": " + type;
        }
    }

    public enum SymbolKind {
        VARIABLE,
        FUNCTION,
        PARAMETER,
        STRUCT,
        FIELD
    }
}