package generator;

import parser.GLSLParser;
import parser.GLSLParser.ASTVisitor;
import java.util.*;

public class HLSLGenerator implements GLSLParser.ASTVisitor<String> {

    private final Map<String, String> typeMapping = new HashMap<>();
    private final Map<String, String> functionMapping = new HashMap<>();
    private final List<String> uniforms = new ArrayList<>();
    private final Map<String, String> uniformTypes = new HashMap<>();
    private final Map<String, String> variableDeclarations = new HashMap<>();
    private boolean inMainFunction = false;
    private boolean inFunction = false;
    private int indentLevel = 0;
    private StringBuilder outputBuilder = new StringBuilder();
    private Set<String> definedFunctions = new HashSet<>();

    public HLSLGenerator() {
        initializeMappings();
    }

    private void initializeMappings() {
        // Типы данных
        typeMapping.put("vec2", "float2");
        typeMapping.put("vec3", "float3");
        typeMapping.put("vec4", "float4");
        typeMapping.put("mat2", "float2x2");
        typeMapping.put("mat3", "float3x3");
        typeMapping.put("mat4", "float4x4");
        typeMapping.put("sampler2D", "Texture2D");
        typeMapping.put("samplerCube", "TextureCube");
        typeMapping.put("float", "float");
        typeMapping.put("int", "int");
        typeMapping.put("bool", "bool");

        // Встроенные функции
        functionMapping.put("texture", "tex2D");
        functionMapping.put("texture2D", "tex2D");
        functionMapping.put("textureCube", "texCUBE");
        functionMapping.put("mix", "lerp");
        functionMapping.put("smoothstep", "smoothstep");
        functionMapping.put("length", "length");
        functionMapping.put("sin", "sin");
        functionMapping.put("cos", "cos");
        functionMapping.put("tan", "tan");
        functionMapping.put("fract", "frac");
        functionMapping.put("floor", "floor");
        functionMapping.put("ceil", "ceil");
        functionMapping.put("mod", "fmod");
        functionMapping.put("pow", "pow");
        functionMapping.put("exp", "exp");
        functionMapping.put("log", "log");
        functionMapping.put("sqrt", "sqrt");
        functionMapping.put("abs", "abs");
        functionMapping.put("min", "min");
        functionMapping.put("max", "max");
        functionMapping.put("clamp", "clamp");
        functionMapping.put("step", "step");
        functionMapping.put("dot", "dot");
    }

    public String generate(GLSLParser.Program program) {
        outputBuilder = new StringBuilder();

        // Сначала собираем информацию о программе
        collectInfo(program);

        // Генерируем uniform переменные
        generateUniforms();

        // Генерируем пользовательские функции (кроме main)
        generateUserFunctions(program);

        // Генерируем структуры для ввода/вывода
        outputBuilder.append("struct PSInput\n{\n");
        outputBuilder.append("    float4 position : SV_POSITION;\n");
        outputBuilder.append("};\n\n");

        outputBuilder.append("struct PSOutput\n{\n");
        outputBuilder.append("    float4 color : SV_TARGET;\n");
        outputBuilder.append("};\n\n");

        // Генерируем main функцию
        generateMainFunction(program);

        return outputBuilder.toString();
    }

    private void collectInfo(GLSLParser.Program program) {
        for (GLSLParser.ASTNode decl : program.declarations) {
            if (decl instanceof GLSLParser.VariableDeclaration) {
                GLSLParser.VariableDeclaration varDecl = (GLSLParser.VariableDeclaration) decl;
                if (varDecl.isUniform) {
                    uniforms.add(varDecl.name);
                    uniformTypes.put(varDecl.name, varDecl.type);
                }
            }
        }
    }

    private void generateUniforms() {
        if (uniforms.isEmpty()) return;

        outputBuilder.append("cbuffer Constants : register(b0)\n{\n");
        for (String uniform : uniforms) {
            String type = uniformTypes.get(uniform);
            String hlslType = mapType(type);
            outputBuilder.append("    ").append(hlslType).append(" ").append(uniform).append(";\n");
        }
        outputBuilder.append("};\n\n");
    }

    private void generateUserFunctions(GLSLParser.Program program) {
        for (GLSLParser.ASTNode decl : program.declarations) {
            if (decl instanceof GLSLParser.FunctionDeclaration) {
                GLSLParser.FunctionDeclaration func = (GLSLParser.FunctionDeclaration) decl;
                if (!func.name.equals("main")) {
                    definedFunctions.add(func.name);
                    outputBuilder.append(generateFunctionDeclaration(func));
                    outputBuilder.append("\n");
                }
            }
        }
    }

    private void generateMainFunction(GLSLParser.Program program) {
        for (GLSLParser.ASTNode decl : program.declarations) {
            if (decl instanceof GLSLParser.FunctionDeclaration) {
                GLSLParser.FunctionDeclaration func = (GLSLParser.FunctionDeclaration) decl;
                if (func.name.equals("main")) {
                    inMainFunction = true;
                    indentLevel = 0;

                    outputBuilder.append("PSOutput main(PSInput input)\n");
                    outputBuilder.append("{\n");
                    indentLevel++;

                    // Добавляем объявление output
                    outputBuilder.append(getIndent()).append("PSOutput output;\n");

                    // Генерируем тело функции
                    if (func.body != null) {
                        generateFunctionBody(func.body);
                    }

                    // Добавляем return если его нет
                    if (!outputBuilder.toString().contains("return output")) {
                        outputBuilder.append(getIndent()).append("return output;\n");
                    }

                    indentLevel--;
                    outputBuilder.append("}\n");

                    inMainFunction = false;
                    break;
                }
            }
        }
    }

    private String generateFunctionDeclaration(GLSLParser.FunctionDeclaration func) {
        StringBuilder sb = new StringBuilder();
        String returnType = mapType(func.returnType);

        sb.append(returnType).append(" ").append(func.name).append("(");

        // Параметры
        for (int i = 0; i < func.parameters.size(); i++) {
            GLSLParser.Parameter param = func.parameters.get(i);
            String paramType = mapType(param.type);
            sb.append(paramType).append(" ").append(param.name != null ? param.name : "param" + i);
            if (i < func.parameters.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(")\n");
        sb.append("{\n");
        indentLevel++;

        // Сохраняем текущее состояние
        boolean wasInFunction = inFunction;
        inFunction = true;

        // Генерируем тело функции
        if (func.body != null) {
            sb.append(generateBlockBody(func.body));
        }

        inFunction = wasInFunction;
        indentLevel--;
        sb.append("}\n");

        return sb.toString();
    }

    private void generateFunctionBody(GLSLParser.BlockStatement body) {
        for (GLSLParser.ASTNode stmt : body.statements) {
            String stmtCode = stmt.accept(this);
            if (!stmtCode.isEmpty()) {
                outputBuilder.append(getIndent()).append(stmtCode);
                if (!stmtCode.endsWith("}") && !stmtCode.endsWith("{")) {
                    outputBuilder.append(";");
                }
                outputBuilder.append("\n");
            }
        }
    }

    private String generateBlockBody(GLSLParser.BlockStatement body) {
        StringBuilder sb = new StringBuilder();
        for (GLSLParser.ASTNode stmt : body.statements) {
            String stmtCode = stmt.accept(this);
            if (!stmtCode.isEmpty()) {
                sb.append(getIndent()).append(stmtCode);
                if (!stmtCode.endsWith("}") && !stmtCode.endsWith("{")) {
                    sb.append(";");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String mapType(String glslType) {
        return typeMapping.getOrDefault(glslType, glslType);
    }

    private String getIndent() {
        return "    ".repeat(indentLevel);
    }

    // Visitor методы ========================================================

    @Override
    public String visit(GLSLParser.Program program) {
        return generate(program);
    }

    @Override
    public String visit(GLSLParser.FunctionDeclaration func) {
        // Уже обработано в generateUserFunctions
        return "";
    }

    @Override
    public String visit(GLSLParser.VariableDeclaration decl) {
        if (decl.isUniform) {
            return ""; // Uniform уже обработаны
        }

        StringBuilder sb = new StringBuilder();
        String mappedType = mapType(decl.type);

        sb.append(mappedType).append(" ").append(decl.name);

        if (decl.initializer != null) {
            sb.append(" = ").append(decl.initializer.accept(this));
        }

        variableDeclarations.put(decl.name, mappedType);
        return sb.toString();
    }

    @Override
    public String visit(GLSLParser.BlockStatement block) {
        // Для вложенных блоков увеличиваем отступ
        indentLevel++;
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        for (GLSLParser.ASTNode stmt : block.statements) {
            String stmtCode = stmt.accept(this);
            if (!stmtCode.isEmpty()) {
                sb.append(getIndent()).append(stmtCode);
                if (!stmtCode.endsWith("}") && !stmtCode.endsWith("{")) {
                    sb.append(";");
                }
                sb.append("\n");
            }
        }

        indentLevel--;
        sb.append(getIndent()).append("}");
        return sb.toString();
    }

    @Override
    public String visit(GLSLParser.ExpressionStatement stmt) {
        String expr = stmt.expression.accept(this);
        // Если это присваивание outColor в main функции, заменяем на output.color
        if (inMainFunction && expr.contains("outColor = ")) {
            expr = expr.replace("outColor = ", "output.color = ");
        }
        return expr;
    }

    @Override
    public String visit(GLSLParser.ReturnStatement stmt) {
        if (inMainFunction) {
            if (stmt.argument != null) {
                String returnExpr = stmt.argument.accept(this);
                // Если возвращаем outColor, заменяем на output.color
                if (returnExpr.equals("outColor")) {
                    returnExpr = "output.color";
                }
                return "output.color = " + returnExpr + "; return output";
            }
            return "return output";
        } else {
            return "return" + (stmt.argument != null ? " " + stmt.argument.accept(this) : "");
        }
    }

    @Override
    public String visit(GLSLParser.BinaryExpression expr) {
        String left = expr.left.accept(this);
        String right = expr.right.accept(this);

        // Специальная обработка для присваивания outColor
        if (inMainFunction && expr.operator.equals("=")) {
            if (expr.left instanceof GLSLParser.Identifier) {
                GLSLParser.Identifier id = (GLSLParser.Identifier) expr.left;
                if (id.name.equals("outColor")) {
                    return "output.color = " + right;
                }
            }
        }

        // Заменяем операторы
        String op = expr.operator;
        if (op.equals("&&")) op = "&&";
        if (op.equals("||")) op = "||";
        if (op.equals("==")) op = "==";
        if (op.equals("!=")) op = "!=";
        if (op.equals("<=")) op = "<=";
        if (op.equals(">=")) op = ">=";

        return "(" + left + " " + op + " " + right + ")";
    }

    @Override
    public String visit(GLSLParser.CallExpression expr) {
        StringBuilder sb = new StringBuilder();

        // Получаем имя функции
        String funcName = "";
        if (expr.callee instanceof GLSLParser.Identifier) {
            funcName = ((GLSLParser.Identifier) expr.callee).name;
        } else {
            funcName = expr.callee.accept(this);
        }

        // Заменяем имена встроенных функций
        String mappedFuncName = functionMapping.getOrDefault(funcName, funcName);

        // Проверяем, не является ли это конструктором типа
        if (typeMapping.containsKey(funcName)) {
            mappedFuncName = mapType(funcName);
        }

        sb.append(mappedFuncName).append("(");

        // Аргументы
        for (int i = 0; i < expr.arguments.size(); i++) {
            sb.append(expr.arguments.get(i).accept(this));
            if (i < expr.arguments.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    public String visit(GLSLParser.Identifier identifier) {
        String name = identifier.name;

        // Заменяем gl_FragCoord в main функции
        if (inMainFunction && name.equals("gl_FragCoord")) {
            return "input.position.xy";
        }

        // Заменяем outColor в main функции
        if (inMainFunction && name.equals("outColor")) {
            return "output.color";
        }

        return name;
    }

    @Override
    public String visit(GLSLParser.Literal literal) {
        String value = literal.raw;

        // Преобразуем vec конструкторы
        if (value.startsWith("vec")) {
            for (Map.Entry<String, String> entry : typeMapping.entrySet()) {
                if (value.contains(entry.getKey() + "(")) {
                    value = value.replace(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }

        // Добавляем 'f' к float литералам
        if (value.matches("\\d+\\.\\d*") || value.matches("\\.\\d+")) {
            if (!value.endsWith("f")) {
                value += "f";
            }
        }

        return value;
    }

    @Override
    public String visit(GLSLParser.UnaryExpression expr) {
        return expr.operator + expr.argument.accept(this);
    }

    @Override
    public String visit(GLSLParser.TernaryExpression expr) {
        return expr.test.accept(this) + " ? " +
                expr.consequent.accept(this) + " : " +
                expr.alternate.accept(this);
    }

    @Override
    public String visit(GLSLParser.MemberExpression expr) {
        String object = expr.object.accept(this);
        String property = expr.property.accept(this);

        // Специальная обработка для gl_FragCoord.xy
        if (object.equals("gl_FragCoord") && property.equals("xy") && inMainFunction) {
            return "input.position.xy";
        }

        return object + "." + property;
    }

    // Остальные методы visit (возвращают пустые строки)
    @Override public String visit(GLSLParser.StructDeclaration struct) { return ""; }
    @Override public String visit(GLSLParser.Parameter param) { return ""; }
    @Override public String visit(GLSLParser.VariableStatement stmt) {
        return stmt.declaration.accept(this);
    }

    @Override
    public String visit(GLSLParser.IfStatement stmt) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(stmt.test.accept(this)).append(") ");

        if (stmt.consequent instanceof GLSLParser.BlockStatement) {
            sb.append(stmt.consequent.accept(this));
        } else {
            sb.append("\n").append(getIndent()).append(stmt.consequent.accept(this));
        }

        if (stmt.alternate != null) {
            sb.append(" else ");
            if (stmt.alternate instanceof GLSLParser.BlockStatement) {
                sb.append(stmt.alternate.accept(this));
            } else {
                sb.append("\n").append(getIndent()).append(stmt.alternate.accept(this));
            }
        }

        return sb.toString();
    }

    @Override
    public String visit(GLSLParser.ForStatement stmt) {
        StringBuilder sb = new StringBuilder();
        sb.append("for (");

        if (stmt.init != null) {
            sb.append(stmt.init.accept(this));
        }
        sb.append("; ");

        if (stmt.test != null) {
            sb.append(stmt.test.accept(this));
        }
        sb.append("; ");

        if (stmt.update != null) {
            sb.append(stmt.update.accept(this));
        }

        sb.append(") ");

        if (stmt.body != null) {
            if (stmt.body instanceof GLSLParser.BlockStatement) {
                sb.append(stmt.body.accept(this));
            } else {
                sb.append("\n").append(getIndent()).append(stmt.body.accept(this));
            }
        }

        return sb.toString();
    }

    @Override
    public String visit(GLSLParser.WhileStatement stmt) {
        StringBuilder sb = new StringBuilder();
        sb.append("while (").append(stmt.test.accept(this)).append(") ");

        if (stmt.body != null) {
            if (stmt.body instanceof GLSLParser.BlockStatement) {
                sb.append(stmt.body.accept(this));
            } else {
                sb.append("\n").append(getIndent()).append(stmt.body.accept(this));
            }
        }

        return sb.toString();
    }
}
