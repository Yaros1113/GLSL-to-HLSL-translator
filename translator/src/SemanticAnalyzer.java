import java.util.*;

public class SemanticAnalyzer implements GLSLParser.ASTVisitor<Void> {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errors = new ArrayList<>();
    private String currentFunctionReturnType = null;
    private boolean inFunction = false;

    // Таблица типов и их совместимости
    private final Map<String, Set<String>> typeCompatibility = new HashMap<>();

    public SemanticAnalyzer() {
        initializeTypeSystem();
    }

    private void initializeTypeSystem() {
        // Базовые типы
        addCompatibleTypes("float", "float");
        addCompatibleTypes("int", "int");
        addCompatibleTypes("bool", "bool");

        // Векторы
        addCompatibleTypes("vec2", "vec2");
        addCompatibleTypes("vec3", "vec3");
        addCompatibleTypes("vec4", "vec4");

        // Матрицы
        addCompatibleTypes("mat2", "mat2");
        addCompatibleTypes("mat3", "mat3");
        addCompatibleTypes("mat4", "mat4");

        // Преобразования
        addCompatibleTypes("float", "vec2"); // скаляр можно использовать с вектором
        addCompatibleTypes("float", "vec3");
        addCompatibleTypes("float", "vec4");
    }

    private void addCompatibleTypes(String type1, String type2) {
        typeCompatibility.computeIfAbsent(type1, k -> new HashSet<>()).add(type2);
        typeCompatibility.computeIfAbsent(type2, k -> new HashSet<>()).add(type1);
    }

    public List<String> analyze(GLSLParser.Program program) {
        // Первый проход: сбор деклараций (функций, структур, глобальных переменных)
        collectDeclarations(program);

        // Второй проход: проверка тел функций и выражений
        checkDeclarations(program);

        // Объединяем ошибки
        errors.addAll(symbolTable.getErrors());
        return errors;
    }

    private void collectDeclarations(GLSLParser.Program program) {
        for (GLSLParser.ASTNode decl : program.declarations) {
            if (decl instanceof GLSLParser.FunctionDeclaration) {
                collectFunctionDeclaration((GLSLParser.FunctionDeclaration) decl);
            } else if (decl instanceof GLSLParser.StructDeclaration) {
                collectStructDeclaration((GLSLParser.StructDeclaration) decl);
            } else if (decl instanceof GLSLParser.VariableDeclaration) {
                collectVariableDeclaration((GLSLParser.VariableDeclaration) decl);
            }
        }
    }

    private void collectFunctionDeclaration(GLSLParser.FunctionDeclaration func) {
        // Собираем информацию о параметрах
        List<SymbolTable.SymbolInfo> paramInfos = new ArrayList<>();
        for (GLSLParser.Parameter param : func.parameters) {
            paramInfos.add(new SymbolTable.SymbolInfo(
                    param.name,
                    param.type,
                    SymbolTable.SymbolKind.PARAMETER,
                    null
            ));
        }

        // Добавляем функцию в таблицу символов
        symbolTable.addSymbol(func.name, new SymbolTable.SymbolInfo(
                func.name,
                func.returnType,
                SymbolTable.SymbolKind.FUNCTION,
                paramInfos
        ));
    }

    private void collectStructDeclaration(GLSLParser.StructDeclaration struct) {
        // Собираем информацию о полях структуры
        List<SymbolTable.SymbolInfo> fieldInfos = new ArrayList<>();
        for (GLSLParser.VariableDeclaration field : struct.fields) {
            fieldInfos.add(new SymbolTable.SymbolInfo(
                    field.name,
                    field.type,
                    SymbolTable.SymbolKind.FIELD,
                    null
            ));
        }

        symbolTable.addSymbol(struct.name, new SymbolTable.SymbolInfo(
                struct.name,
                "struct",
                SymbolTable.SymbolKind.STRUCT,
                fieldInfos
        ));
    }

    private void collectVariableDeclaration(GLSLParser.VariableDeclaration decl) {
        symbolTable.addSymbol(decl.name, new SymbolTable.SymbolInfo(
                decl.name,
                decl.type,
                SymbolTable.SymbolKind.VARIABLE,
                null
        ));
    }

    private void checkDeclarations(GLSLParser.Program program) {
        for (GLSLParser.ASTNode decl : program.declarations) {
            decl.accept(this);
        }
    }

    // Реализация ASTVisitor =================================================

    @Override
    public Void visit(GLSLParser.Program program) {
        for (GLSLParser.ASTNode decl : program.declarations) {
            decl.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(GLSLParser.FunctionDeclaration func) {
        currentFunctionReturnType = func.returnType;
        inFunction = true;

        // Входим в новую область видимости (параметры функции)
        symbolTable.enterScope();

        // Добавляем параметры в текущую область видимости
        for (GLSLParser.Parameter param : func.parameters) {
            symbolTable.addSymbol(param.name, new SymbolTable.SymbolInfo(
                    param.name,
                    param.type,
                    SymbolTable.SymbolKind.PARAMETER,
                    null
            ));
        }

        // Проверяем тело функции
        if (func.body != null) {
            func.body.accept(this);
        }

        // Выходим из области видимости функции
        symbolTable.exitScope();
        inFunction = false;
        currentFunctionReturnType = null;
        return null;
    }

    @Override
    public Void visit(GLSLParser.VariableDeclaration decl) {
        // Проверяем инициализатор, если есть
        if (decl.initializer != null) {
            String initType = checkExpression(decl.initializer);
            if (initType != null && !areTypesCompatible(decl.type, initType)) {
                errors.add("Type mismatch in variable '" + decl.name + "': " +
                        decl.type + " vs " + initType);
            }
        }

        // Добавляем переменную в текущую область видимости
        symbolTable.addSymbol(decl.name, new SymbolTable.SymbolInfo(
                decl.name,
                decl.type,
                SymbolTable.SymbolKind.VARIABLE,
                null
        ));
        return null;
    }

    @Override
    public Void visit(GLSLParser.ReturnStatement stmt) {
        if (stmt.argument != null) {
            String returnType = checkExpression(stmt.argument);
            if (returnType != null && !areTypesCompatible(currentFunctionReturnType, returnType)) {
                errors.add("Return type mismatch: expected " +
                        currentFunctionReturnType + ", got " + returnType);
            }
        } else if (!"void".equals(currentFunctionReturnType)) {
            errors.add("Void return in non-void function");
        }
        return null;
    }

    @Override
    public Void visit(GLSLParser.BinaryExpression expr) {
        String leftType = checkExpression(expr.left);
        String rightType = checkExpression(expr.right);

        if (leftType != null && rightType != null) {
            // Проверка совместимости типов для оператора
            if (!areTypesCompatibleForOperator(expr.operator, leftType, rightType)) {
                errors.add("Incompatible types for operator '" + expr.operator +
                        "': " + leftType + " and " + rightType);
            }
        }
        return null;
    }

    @Override
    public Void visit(GLSLParser.CallExpression expr) {
        // Проверяем, что функция объявлена
        if (!(expr.callee instanceof GLSLParser.Identifier)) {
            errors.add("Invalid function call");
            return null;
        }

        String funcName = ((GLSLParser.Identifier) expr.callee).name;
        SymbolTable.SymbolInfo funcInfo = symbolTable.lookup(funcName);

        if (funcInfo == null || funcInfo.kind != SymbolTable.SymbolKind.FUNCTION) {
            errors.add("Undeclared function: " + funcName);
            return null;
        }

        // Проверяем количество аргументов
        @SuppressWarnings("unchecked")
        List<SymbolTable.SymbolInfo> params = (List<SymbolTable.SymbolInfo>) funcInfo.extraInfo;
        if (params.size() != expr.arguments.size()) {
            errors.add("Argument count mismatch for function '" + funcName +
                    "': expected " + params.size() + ", got " + expr.arguments.size());
            return null;
        }

        // Проверяем типы аргументов
        for (int i = 0; i < expr.arguments.size(); i++) {
            String argType = checkExpression(expr.arguments.get(i));
            if (argType != null && !areTypesCompatible(params.get(i).type, argType)) {
                errors.add("Argument " + (i + 1) + " type mismatch in call to '" +
                        funcName + "': expected " + params.get(i).type +
                        ", got " + argType);
            }
        }

        return null;
    }

    @Override
    public Void visit(GLSLParser.Identifier identifier) {
        // Проверяем, что идентификатор объявлен
        SymbolTable.SymbolInfo info = symbolTable.lookup(identifier.name);
        if (info == null) {
            errors.add("Undeclared identifier: " + identifier.name);
        }
        return null;
    }

    // Вспомогательные методы ================================================

    private String checkExpression(GLSLParser.ASTNode expr) {
        if (expr instanceof GLSLParser.Identifier) {
            SymbolTable.SymbolInfo info = symbolTable.lookup(((GLSLParser.Identifier) expr).name);
            return info != null ? info.type : null;
        } else if (expr instanceof GLSLParser.Literal) {
            Object value = ((GLSLParser.Literal) expr).value;
            if (value instanceof Float || value instanceof Double) {
                return "float";
            } else if (value instanceof Integer) {
                return "int";
            } else if (value instanceof Boolean) {
                return "bool";
            }
        } else if (expr instanceof GLSLParser.BinaryExpression) {
            // Для упрощения возвращаем тип первого операнда
            return checkExpression(((GLSLParser.BinaryExpression) expr).left);
        } else if (expr instanceof GLSLParser.CallExpression) {
            // Возвращаем тип функции
            if (((GLSLParser.CallExpression) expr).callee instanceof GLSLParser.Identifier) {
                String funcName = ((GLSLParser.Identifier) ((GLSLParser.CallExpression) expr).callee).name;
                SymbolTable.SymbolInfo info = symbolTable.lookup(funcName);
                return info != null ? info.type : null;
            }
        }
        return null;
    }

    private boolean areTypesCompatible(String type1, String type2) {
        if (type1 == null || type2 == null) return false;
        if (type1.equals(type2)) return true;

        Set<String> compatible = typeCompatibility.get(type1);
        return compatible != null && compatible.contains(type2);
    }

    private boolean areTypesCompatibleForOperator(String operator, String leftType, String rightType) {
        // Простая проверка - для арифметических операторов типы должны быть числовыми
        if (operator.equals("+") || operator.equals("-") ||
                operator.equals("*") || operator.equals("/")) {
            return isNumericType(leftType) && isNumericType(rightType);
        }
        // Для сравнений типы должны быть совместимы
        if (operator.equals("==") || operator.equals("!=") ||
                operator.equals("<") || operator.equals(">") ||
                operator.equals("<=") || operator.equals(">=")) {
            return areTypesCompatible(leftType, rightType);
        }
        return true;
    }

    private boolean isNumericType(String type) {
        return type.equals("float") || type.equals("int") ||
                type.equals("vec2") || type.equals("vec3") || type.equals("vec4");
    }

    // Остальные методы visit могут быть пустыми
    @Override public Void visit(GLSLParser.StructDeclaration decl) { return null; }
    @Override public Void visit(GLSLParser.Parameter param) { return null; }
    @Override public Void visit(GLSLParser.BlockStatement block) {
        symbolTable.enterScope();
        for (GLSLParser.ASTNode stmt : block.statements) {
            stmt.accept(this);
        }
        symbolTable.exitScope();
        return null;
    }
    @Override public Void visit(GLSLParser.VariableStatement stmt) {
        stmt.declaration.accept(this);
        return null;
    }
    @Override public Void visit(GLSLParser.ExpressionStatement stmt) {
        checkExpression(stmt.expression);
        return null;
    }
    @Override public Void visit(GLSLParser.IfStatement stmt) {
        checkExpression(stmt.test);
        stmt.consequent.accept(this);
        if (stmt.alternate != null) stmt.alternate.accept(this);
        return null;
    }
    @Override public Void visit(GLSLParser.ForStatement stmt) {
        symbolTable.enterScope();
        if (stmt.init != null) stmt.init.accept(this);
        if (stmt.test != null) checkExpression(stmt.test);
        if (stmt.update != null) checkExpression(stmt.update);
        if (stmt.body != null) stmt.body.accept(this);
        symbolTable.exitScope();
        return null;
    }
    @Override public Void visit(GLSLParser.WhileStatement stmt) {
        checkExpression(stmt.test);
        stmt.body.accept(this);
        return null;
    }
    @Override public Void visit(GLSLParser.TernaryExpression expr) { return null; }
    @Override public Void visit(GLSLParser.UnaryExpression expr) {
        checkExpression(expr.argument);
        return null;
    }
    @Override
    public Void visit(GLSLParser.MemberExpression expr) {
        // Проверяем объект
        expr.object.accept(this);

        // Для упрощения, просто проверяем, что свойство - это идентификатор
        if (expr.property instanceof GLSLParser.Identifier) {
            // В реальном анализаторе нужно проверять, что свойство существует в типе объекта
            return null;
        } else {
            errors.add("Invalid member access");
        }
        return null;
    }
    @Override public Void visit(GLSLParser.Literal literal) { return null; }
}