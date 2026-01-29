package semantics;

import java.util.*;

public class SymbolTable {
    private final Stack<Map<String, SymbolInfo>> scopes = new Stack<>();
    private final List<String> errors = new ArrayList<>();
    private final Set<String> builtInSymbols = new HashSet<>();

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    public void addSymbol(String name, SymbolInfo info) {
        if (builtInSymbols.contains(name)) {
            return; // Игнорируем попытки переопределить встроенные символы
        }

        if (scopes.peek().containsKey(name)) {
            errors.add("Duplicate symbol: " + name);
            return;
        }
        scopes.peek().put(name, info);
    }

    public void addBuiltInSymbol(String name, SymbolInfo info) {
        builtInSymbols.add(name);
        scopes.get(0).put(name, info); // Добавляем в глобальную область видимости
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
        if (scopes.isEmpty()) return null;
        return scopes.peek().get(name);
    }

    public List<String> getErrors() {
        return errors;
    }

    // Вложенный класс для информации о символе
    public static class SymbolInfo {
        public final String name;
        public final String type;
        public final SymbolKind kind;
        public final Object extraInfo;

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

    // Вложенный enum для вида символа
    public enum SymbolKind {
        VARIABLE,
        FUNCTION,
        PARAMETER,
        STRUCT,
        FIELD,
        TYPE
    }
}