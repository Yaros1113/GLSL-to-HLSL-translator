package semantics;

import parser.GLSLParser;
import java.util.*;

public class SemanticAnalyzer implements GLSLParser.ASTVisitor<Void> {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> errors = new ArrayList<>();
    private String currentFunctionReturnType = null;
    private boolean inFunction = false;
    private boolean inGlobalScope = true;
    private boolean inStructDeclaration = false;

    // Таблица типов и их совместимости
    private final Map<String, Set<String>> typeCompatibility = new HashMap<>();

    // Встроенные типы
    private final Set<String> builtInTypes = new HashSet<>();

    // Встроенные функции
    private final Map<String, BuiltInFunctionInfo> builtInFunctions = new HashMap<>();

    public SemanticAnalyzer() {
        initializeTypeSystem();
        initializeBuiltIns();
    }

    private void initializeTypeSystem() {
        // Базовые типы
        builtInTypes.add("float");
        builtInTypes.add("int");
        builtInTypes.add("bool");
        builtInTypes.add("void");

        // Векторы
        builtInTypes.add("vec2");
        builtInTypes.add("vec3");
        builtInTypes.add("vec4");

        // Матрицы
        builtInTypes.add("mat2");
        builtInTypes.add("mat3");
        builtInTypes.add("mat4");

        // Совместимость типов
        addCompatibleTypes("float", "float");
        addCompatibleTypes("int", "int");
        addCompatibleTypes("bool", "bool");

        addCompatibleTypes("vec2", "vec2");
        addCompatibleTypes("vec3", "vec3");
        addCompatibleTypes("vec4", "vec4");

        addCompatibleTypes("mat2", "mat2");
        addCompatibleTypes("mat3", "mat3");
        addCompatibleTypes("mat4", "mat4");

        // Скалярные преобразования
        addCompatibleTypes("float", "vec2");
        addCompatibleTypes("float", "vec3");
        addCompatibleTypes("float", "vec4");
        addCompatibleTypes("float", "mat2");
        addCompatibleTypes("float", "mat3");
        addCompatibleTypes("float", "mat4");

        // int к float
        addCompatibleTypes("int", "float");
        addCompatibleTypes("int", "vec2");
        addCompatibleTypes("int", "vec3");
        addCompatibleTypes("int", "vec4");
    }

    private void initializeBuiltIns() {
        // Встроенные функции-конструкторы
        addBuiltInFunction("vec2", "vec2", Arrays.asList("float", "float"));
        addBuiltInFunction("vec3", "vec3", Arrays.asList("float", "float", "float"));
        addBuiltInFunction("vec4", "vec4", Arrays.asList("float", "float", "float", "float"));

        // Математические функции
        addBuiltInFunction("length", "float", Arrays.asList("vec2"));
        addBuiltInFunction("length", "float", Arrays.asList("vec3"));
        addBuiltInFunction("length", "float", Arrays.asList("vec4"));

        // Добавляем встроенные типы в таблицу символов
        for (String type : builtInTypes) {
            symbolTable.addBuiltInSymbol(type, new SymbolTable.SymbolInfo(
                    type, type, SymbolTable.SymbolKind.TYPE, null));
        }

        // Добавляем встроенные функции
        for (Map.Entry<String, BuiltInFunctionInfo> entry : builtInFunctions.entrySet()) {
            BuiltInFunctionInfo info = entry.getValue();
            symbolTable.addBuiltInSymbol(entry.getKey(), new SymbolTable.SymbolInfo(
                    entry.getKey(), info.returnType, SymbolTable.SymbolKind.FUNCTION, info.paramTypes));
        }
    }

    private void addBuiltInFunction(String name, String returnType, List<String> paramTypes) {
        BuiltInFunctionInfo info = new BuiltInFunctionInfo(returnType, paramTypes);
        builtInFunctions.put(name, info);
    }

    private void addCompatibleTypes(String type1, String type2) {
        typeCompatibility.computeIfAbsent(type1, k -> new HashSet<>()).add(type2);
        typeCompatibility.computeIfAbsent(type2, k -> new HashSet<>()).add(type1);
    }

    public List<String> analyze(GLSLParser.Program program) {
        errors.clear();

        // Первый проход: сбор деклараций
        collectDeclarations(program);

        // Второй проход: проверка тел функций
        checkDeclarations(program);

        errors.addAll(symbolTable.getErrors());
        return errors;
    }

    private void collectDeclarations(GLSLParser.Program program) {
        // Входим в глобальную область видимости
        symbolTable.enterScope();
        inGlobalScope = true;

        for (GLSLParser.ASTNode decl : program.declarations) {
            if (decl instanceof GLSLParser.FunctionDeclaration) {
                collectFunctionDeclaration((GLSLParser.FunctionDeclaration) decl);
            } else if (decl instanceof GLSLParser.StructDeclaration) {
                collectStructDeclaration((GLSLParser.StructDeclaration) decl);
            } else if (decl instanceof GLSLParser.VariableDeclaration) {
                // Собираем глобальные переменные
                collectGlobalVariable((GLSLParser.VariableDeclaration) decl);
            }
        }

        inGlobalScope = false;
        symbolTable.exitScope();
    }

    private void collectGlobalVariable(GLSLParser.VariableDeclaration decl) {
        symbolTable.addSymbol(decl.name, new SymbolTable.SymbolInfo(
                decl.name,
                decl.type,
                SymbolTable.SymbolKind.VARIABLE,
                null
        ));
    }

    private void collectFunctionDeclaration(GLSLParser.FunctionDeclaration func) {
        List<SymbolTable.SymbolInfo> paramInfos = new ArrayList<>();
        for (GLSLParser.Parameter param : func.parameters) {
            String paramName = param.name != null ? param.name : "param_" + paramInfos.size();
            paramInfos.add(new SymbolTable.SymbolInfo(
                    paramName,
                    param.type,
                    SymbolTable.SymbolKind.PARAMETER,
                    null
            ));
        }

        symbolTable.addSymbol(func.name, new SymbolTable.SymbolInfo(
                func.name,
                func.returnType,
                SymbolTable.SymbolKind.FUNCTION,
                paramInfos
        ));
    }

    private void collectStructDeclaration(GLSLParser.StructDeclaration struct) {
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

    private void checkDeclarations(GLSLParser.Program program) {
        symbolTable.enterScope();
        inGlobalScope = true;

        for (GLSLParser.ASTNode decl : program.declarations) {
            decl.accept(this);
        }

        inGlobalScope = false;
        symbolTable.exitScope();
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
        boolean wasInGlobal = inGlobalScope;
        inGlobalScope = false;

        symbolTable.enterScope();

        for (GLSLParser.Parameter param : func.parameters) {
            if (param.name != null) {
                symbolTable.addSymbol(param.name, new SymbolTable.SymbolInfo(
                        param.name,
                        param.type,
                        SymbolTable.SymbolKind.PARAMETER,
                        null
                ));
            }
        }

        if (func.body != null) {
            func.body.accept(this);
        }

        // Проверяем, что не-void функция имеет return
        if (!"void".equals(currentFunctionReturnType)) {
            // Для простоты, считаем что если есть return - проверяем в visit(ReturnStatement)
            // Более сложная проверка потребовала бы анализа потока управления
        }

        symbolTable.exitScope();
        inFunction = false;
        inGlobalScope = wasInGlobal;
        currentFunctionReturnType = null;
        return null;
    }

    @Override
    public Void visit(GLSLParser.VariableDeclaration decl) {
        // Проверяем тип переменной
        if (!isTypeValid(decl.type)) {
            errors.add("Unknown type: " + decl.type + " for variable '" + decl.name + "'");
        }

        // Проверяем инициализатор
        if (decl.initializer != null) {
            String initType = checkExpression(decl.initializer);
            if (initType != null && !areTypesCompatible(decl.type, initType)) {
                errors.add("Type mismatch in variable '" + decl.name + "': expected " +
                        decl.type + ", got " + initType);
            }
        }

        // Добавляем переменную в таблицу символов
        // Не добавляем, если это поле структуры (мы их уже собрали в collectStructDeclaration)
        if (!inStructDeclaration) {
            symbolTable.addSymbol(decl.name, new SymbolTable.SymbolInfo(
                    decl.name,
                    decl.type,
                    SymbolTable.SymbolKind.VARIABLE,
                    null
            ));
        }
        return null;
    }

    @Override
    public Void visit(GLSLParser.StructDeclaration struct) {
        // Входим в контекст объявления структуры для полей
        boolean wasInStruct = inStructDeclaration;
        inStructDeclaration = true;

        // Проверяем поля структуры
        for (GLSLParser.VariableDeclaration field : struct.fields) {
            field.accept(this);
        }

        inStructDeclaration = wasInStruct;
        return null;
    }

    @Override
    public Void visit(GLSLParser.ReturnStatement stmt) {
        if (currentFunctionReturnType == null) {
            errors.add("Return statement outside of function");
            return null;
        }

        if (stmt.argument != null) {
            String returnType = checkExpression(stmt.argument);
            if (returnType != null && !areTypesCompatible(currentFunctionReturnType, returnType)) {
                errors.add("Return type mismatch: expected " +
                        currentFunctionReturnType + ", got " + returnType);
            }
        } else if (!"void".equals(currentFunctionReturnType)) {
            errors.add("Missing return value in non-void function");
        }
        return null;
    }

    @Override
    public Void visit(GLSLParser.BinaryExpression expr) {
        String leftType = checkExpression(expr.left);
        String rightType = checkExpression(expr.right);

        if (leftType != null && rightType != null) {
            if (!areTypesCompatibleForOperator(expr.operator, leftType, rightType)) {
                errors.add("Incompatible types for operator '" + expr.operator +
                        "': " + leftType + " and " + rightType);
            }
        }

        return null;
    }

    @Override
    public Void visit(GLSLParser.CallExpression expr) {
        String funcName = extractFunctionName(expr.callee);

        if (funcName == null) {
            errors.add("Invalid function call");
            return null;
        }

        // Проверяем встроенные функции
        if (builtInFunctions.containsKey(funcName)) {
            BuiltInFunctionInfo builtInInfo = builtInFunctions.get(funcName);

            // Проверяем количество аргументов
            if (builtInInfo.paramTypes.size() != expr.arguments.size()) {
                errors.add("Argument count mismatch for built-in function '" + funcName +
                        "': expected " + builtInInfo.paramTypes.size() + ", got " + expr.arguments.size());
                return null;
            }

            // Проверяем типы аргументов
            for (int i = 0; i < expr.arguments.size(); i++) {
                String argType = checkExpression(expr.arguments.get(i));
                String expectedType = builtInInfo.paramTypes.get(i);

                if (argType != null && !areTypesCompatible(expectedType, argType)) {
                    errors.add("Argument " + (i + 1) + " type mismatch in call to '" +
                            funcName + "': expected " + expectedType + ", got " + argType);
                }
            }

            return null;
        }

        // Проверяем пользовательские функции
        SymbolTable.SymbolInfo funcInfo = symbolTable.lookup(funcName);

        if (funcInfo == null || funcInfo.kind != SymbolTable.SymbolKind.FUNCTION) {
            errors.add("Undeclared function: " + funcName);
            return null;
        }

        @SuppressWarnings("unchecked")
        List<SymbolTable.SymbolInfo> params = (List<SymbolTable.SymbolInfo>) funcInfo.extraInfo;

        if (params.size() != expr.arguments.size()) {
            errors.add("Argument count mismatch for function '" + funcName +
                    "': expected " + params.size() + ", got " + expr.arguments.size());
            return null;
        }

        for (int i = 0; i < expr.arguments.size(); i++) {
            String argType = checkExpression(expr.arguments.get(i));
            String expectedType = params.get(i).type;

            if (argType != null && !areTypesCompatible(expectedType, argType)) {
                errors.add("Argument " + (i + 1) + " type mismatch in call to '" +
                        funcName + "': expected " + expectedType + ", got " + argType);
            }
        }

        return null;
    }

    private String extractFunctionName(GLSLParser.ASTNode callee) {
        if (callee instanceof GLSLParser.Identifier) {
            return ((GLSLParser.Identifier) callee).name;
        }
        return null;
    }

    @Override
    public Void visit(GLSLParser.Identifier identifier) {
        SymbolTable.SymbolInfo info = symbolTable.lookup(identifier.name);
        if (info == null) {
            // Проверяем, не является ли это встроенным типом
            if (!builtInTypes.contains(identifier.name)) {
                errors.add("Undeclared identifier: " + identifier.name);
            }
        }
        return null;
    }

    @Override
    public Void visit(GLSLParser.MemberExpression expr) {
        String objectType = checkExpression(expr.object);

        if (objectType != null && expr.property instanceof GLSLParser.Identifier) {
            String fieldName = ((GLSLParser.Identifier) expr.property).name;

            // Проверяем доступ к полям структуры
            SymbolTable.SymbolInfo structInfo = symbolTable.lookup(objectType);
            if (structInfo != null && structInfo.kind == SymbolTable.SymbolKind.STRUCT) {
                @SuppressWarnings("unchecked")
                List<SymbolTable.SymbolInfo> fields = (List<SymbolTable.SymbolInfo>) structInfo.extraInfo;
                boolean fieldFound = false;

                for (SymbolTable.SymbolInfo field : fields) {
                    if (field.name.equals(fieldName)) {
                        fieldFound = true;
                        break;
                    }
                }

                if (!fieldFound) {
                    errors.add("Unknown field '" + fieldName + "' in struct '" + objectType + "'");
                }
            } else {
                // Проверяем доступ к компонентам вектора (x, y, z, w, r, g, b, a)
                if (objectType != null && objectType.startsWith("vec")) {
                    Set<String> validComponents = new HashSet<>(Arrays.asList("x", "y", "z", "w", "r", "g", "b", "a"));
                    if (!validComponents.contains(fieldName)) {
                        errors.add("Invalid component '" + fieldName + "' for type '" + objectType + "'");
                    }
                }
            }
        }

        return null;
    }

    // Вспомогательные методы ================================================

    private String checkExpression(GLSLParser.ASTNode expr) {
        if (expr instanceof GLSLParser.Identifier) {
            SymbolTable.SymbolInfo info = symbolTable.lookup(((GLSLParser.Identifier) expr).name);
            return info != null ? info.type : null;
        } else if (expr instanceof GLSLParser.Literal) {
            GLSLParser.Literal literal = (GLSLParser.Literal) expr;
            Object value = literal.value;

            if (value instanceof Float || value instanceof Double) {
                return "float";
            } else if (value instanceof Integer) {
                return "int";
            } else if (value instanceof Boolean) {
                return "bool";
            } else if (value instanceof String) {
                String strValue = (String) value;
                try {
                    Float.parseFloat(strValue);
                    return "float";
                } catch (NumberFormatException e1) {
                    try {
                        Integer.parseInt(strValue);
                        return "int";
                    } catch (NumberFormatException e2) {
                        if ("true".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)) {
                            return "bool";
                        }
                    }
                }
                return null;
            }
        } else if (expr instanceof GLSLParser.BinaryExpression) {
            GLSLParser.BinaryExpression binExpr = (GLSLParser.BinaryExpression) expr;
            String leftType = checkExpression(binExpr.left);
            String rightType = checkExpression(binExpr.right);

            // Определяем тип результата операции
            if (leftType != null && rightType != null) {
                // Для арифметических операций
                if (isArithmeticOperator(binExpr.operator)) {
                    if (leftType.equals("float") && rightType.equals("float")) {
                        return "float";
                    } else if (leftType.equals("int") && rightType.equals("int")) {
                        return "int";
                    } else if (leftType.startsWith("vec") && rightType.equals("float")) {
                        return leftType;
                    } else if (rightType.startsWith("vec") && leftType.equals("float")) {
                        return rightType;
                    } else if (leftType.startsWith("vec") && rightType.equals(leftType)) {
                        return leftType;
                    }
                }

                // Для операторов сравнения возвращаем bool
                if (isComparisonOperator(binExpr.operator)) {
                    return "bool";
                }

                // Для логических операторов возвращаем bool
                if (isLogicalOperator(binExpr.operator)) {
                    return "bool";
                }
            }

            return leftType;
        } else if (expr instanceof GLSLParser.CallExpression) {
            String funcName = extractFunctionName(((GLSLParser.CallExpression) expr).callee);

            if (builtInFunctions.containsKey(funcName)) {
                return builtInFunctions.get(funcName).returnType;
            }

            SymbolTable.SymbolInfo info = symbolTable.lookup(funcName);
            return info != null ? info.type : null;
        } else if (expr instanceof GLSLParser.UnaryExpression) {
            return checkExpression(((GLSLParser.UnaryExpression) expr).argument);
        } else if (expr instanceof GLSLParser.MemberExpression) {
            // Для простоты возвращаем тип объекта
            return checkExpression(((GLSLParser.MemberExpression) expr).object);
        } else if (expr instanceof GLSLParser.TernaryExpression) {
            // Обработка тернарного оператора
            GLSLParser.TernaryExpression ternary = (GLSLParser.TernaryExpression) expr;

            // Проверяем условие - должно быть bool
            String conditionType = checkExpression(ternary.test);
            if (conditionType != null && !"bool".equals(conditionType)) {
                errors.add("Ternary condition must be boolean, got: " + conditionType);
            }

            // Проверяем оба выражения и возвращаем их общий тип
            String thenType = checkExpression(ternary.consequent);
            String elseType = checkExpression(ternary.alternate);

            if (thenType != null && elseType != null) {
                if (areTypesCompatible(thenType, elseType)) {
                    return thenType; // Возвращаем тип первого выражения
                } else {
                    errors.add("Incompatible types in ternary operator: " + thenType + " and " + elseType);
                }
            }

            return thenType;
        }

        return null;
    }

    private boolean isTypeValid(String type) {
        return builtInTypes.contains(type) || symbolTable.lookup(type) != null;
    }

    private boolean isArithmeticOperator(String operator) {
        return operator.equals("+") || operator.equals("-") ||
                operator.equals("*") || operator.equals("/");
    }

    private boolean isLogicalOperator(String operator) {
        return operator.equals("&&") || operator.equals("||");
    }

    private boolean isComparisonOperator(String operator) {
        return operator.equals("==") || operator.equals("!=") ||
                operator.equals("<") || operator.equals(">") ||
                operator.equals("<=") || operator.equals(">=");
    }

    private boolean areTypesCompatible(String type1, String type2) {
        if (type1 == null || type2 == null) return false;
        if (type1.equals(type2)) return true;
        if (type1.equals("float") && type2.equals("int")) return true;
        if (type1.equals("int") && type2.equals("float")) return true;

        Set<String> compatible = typeCompatibility.get(type1);
        return compatible != null && compatible.contains(type2);
    }

    private boolean areTypesCompatibleForOperator(String operator, String leftType, String rightType) {
        // Логические операторы требуют bool
        if (isLogicalOperator(operator)) {
            return "bool".equals(leftType) && "bool".equals(rightType);
        }

        // Операторы сравнения требуют совместимых типов
        if (isComparisonOperator(operator)) {
            return areTypesCompatible(leftType, rightType);
        }

        // Арифметические операторы требуют числовых типов
        if (isArithmeticOperator(operator)) {
            if ("bool".equals(leftType) || "bool".equals(rightType)) {
                return false;
            }
            return isNumericType(leftType) && isNumericType(rightType);
        }

        // Операторы присваивания требуют совместимости
        if (operator.equals("=") || operator.endsWith("=")) {
            return areTypesCompatible(leftType, rightType);
        }

        return true;
    }

    private boolean isNumericType(String type) {
        return type.equals("float") || type.equals("int") ||
                type.startsWith("vec") || type.startsWith("mat");
    }

    // Внутренний класс для информации о встроенных функциях
    private static class BuiltInFunctionInfo {
        final String returnType;
        final List<String> paramTypes;

        BuiltInFunctionInfo(String returnType, List<String> paramTypes) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }
    }

    // Остальные методы visit
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
        String testType = checkExpression(stmt.test);
        if (testType != null && !"bool".equals(testType)) {
            errors.add("If condition must be boolean, got: " + testType);
        }
        stmt.consequent.accept(this);
        if (stmt.alternate != null) stmt.alternate.accept(this);
        return null;
    }
    @Override public Void visit(GLSLParser.ForStatement stmt) {
        symbolTable.enterScope();
        if (stmt.init != null) stmt.init.accept(this);
        if (stmt.test != null) {
            String testType = checkExpression(stmt.test);
            if (testType != null && !"bool".equals(testType)) {
                errors.add("For condition must be boolean, got: " + testType);
            }
        }
        if (stmt.update != null) checkExpression(stmt.update);
        if (stmt.body != null) stmt.body.accept(this);
        symbolTable.exitScope();
        return null;
    }
    @Override public Void visit(GLSLParser.WhileStatement stmt) {
        String testType = checkExpression(stmt.test);
        if (testType != null && !"bool".equals(testType)) {
            errors.add("While condition must be boolean, got: " + testType);
        }
        stmt.body.accept(this);
        return null;
    }
    @Override
    public Void visit(GLSLParser.TernaryExpression expr) {
        // Проверяем условие - должно быть bool
        String conditionType = checkExpression(expr.test);
        if (conditionType != null && !"bool".equals(conditionType)) {
            errors.add("Ternary condition must be boolean, got: " + conditionType);
        }

        // Проверяем оба выражения
        checkExpression(expr.consequent);
        checkExpression(expr.alternate);
        return null;
    }
    @Override public Void visit(GLSLParser.UnaryExpression expr) {
        checkExpression(expr.argument);
        return null;
    }
    @Override public Void visit(GLSLParser.Literal literal) { return null; }
}