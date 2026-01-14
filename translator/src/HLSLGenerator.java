import java.util.HashMap;
import java.util.Map;

public class HLSLGenerator implements GLSLParser.ASTVisitor<String> {

    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();

    static {
        TYPE_MAPPING.put("float", "float");
        TYPE_MAPPING.put("int", "int");
        TYPE_MAPPING.put("bool", "bool");

        TYPE_MAPPING.put("vec2", "float2");
        TYPE_MAPPING.put("vec3", "float3");
        TYPE_MAPPING.put("vec4", "float4");

        TYPE_MAPPING.put("mat2", "float2x2");
        TYPE_MAPPING.put("mat3", "float3x3");
        TYPE_MAPPING.put("mat4", "float4x4");

        TYPE_MAPPING.put("sampler2D", "Texture2D");
        TYPE_MAPPING.put("samplerCube", "TextureCube");
    }

    public String generate(GLSLParser.Program program) {
        sb.setLength(0);
        indent = 0;
        program.accept(this);
        return sb.toString();
    }

    // ===== utils =====

    private void writeIndent() {
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
    }

    private String mapType(String glslType) {
        return TYPE_MAPPING.getOrDefault(glslType, glslType);
    }

    private String genExpr(GLSLParser.ASTNode node) {
        return node != null ? node.accept(this) : "";
    }

    // ========================================================================
    // ASTVisitor implementation
    // ========================================================================

    @Override
    public String visit(GLSLParser.Program program) {
        for (GLSLParser.ASTNode decl : program.declarations) {
            decl.accept(this);
            sb.append("\n");
        }
        return null;
    }

    @Override
    public String visit(GLSLParser.VariableDeclaration decl) {
        // Этот visit используется, когда декларация стоит в Program.declarations (глобал)
        writeIndent();

        String type = mapType(decl.type);
        sb.append(type).append(" ").append(decl.name);

        if (decl.initializer != null) {
            sb.append(" = ").append(genExpr(decl.initializer));
        }

        sb.append(";\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.FunctionDeclaration func) {
        writeIndent();
        String retType = mapType(func.returnType);
        sb.append(retType).append(" ").append(func.name).append("(");

        for (int i = 0; i < func.parameters.size(); i++) {
            GLSLParser.Parameter p = func.parameters.get(i);
            if (i > 0) sb.append(", ");
            sb.append(mapType(p.type)).append(" ").append(p.name);
        }

        sb.append(")");
        sb.append(" {\n");
        indent++;
        func.body.accept(this);
        indent--;
        writeIndent();
        sb.append("}\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.StructDeclaration decl) {
        writeIndent();
        sb.append("struct ").append(decl.name).append(" {\n");
        indent++;
        for (GLSLParser.VariableDeclaration field : decl.fields) {
            writeIndent();
            sb.append(mapType(field.type)).append(" ").append(field.name).append(";\n");
        }
        indent--;
        writeIndent();
        sb.append("};\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.Parameter param) {
        // Не используется напрямую, параметры обрабатываются в visit(FunctionDeclaration)
        return null;
    }

    @Override
    public String visit(GLSLParser.BlockStatement block) {
        for (GLSLParser.ASTNode stmt : block.statements) {
            stmt.accept(this);
        }
        return null;
    }

    @Override
    public String visit(GLSLParser.VariableStatement stmt) {
        GLSLParser.VariableDeclaration decl = stmt.declaration;
        writeIndent();
        sb.append(mapType(decl.type)).append(" ").append(decl.name);
        if (decl.initializer != null) {
            sb.append(" = ").append(genExpr(decl.initializer));
        }
        sb.append(";\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.ExpressionStatement stmt) {
        writeIndent();
        sb.append(genExpr(stmt.expression)).append(";\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.ReturnStatement stmt) {
        writeIndent();
        sb.append("return");
        if (stmt.argument != null) {
            sb.append(" ").append(genExpr(stmt.argument));
        }
        sb.append(";\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.IfStatement stmt) {
        writeIndent();
        sb.append("if (").append(genExpr(stmt.test)).append(") {\n");
        indent++;
        stmt.consequent.accept(this);
        indent--;
        writeIndent();
        sb.append("}");
        if (stmt.alternate != null) {
            sb.append(" else {\n");
            indent++;
            stmt.alternate.accept(this);
            indent--;
            writeIndent();
            sb.append("}");
        }
        sb.append("\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.TernaryExpression expr) {
        String test = genExpr(expr.test);
        String cons = genExpr(expr.consequent);
        String alt  = genExpr(expr.alternate);

        return "(" + test + " ? " + cons + " : " + alt + ")";
    }

    @Override
    public String visit(GLSLParser.ForStatement stmt) {
        writeIndent();
        sb.append("for (");

        // init
        if (stmt.init instanceof GLSLParser.VariableDeclaration vd) {
            sb.append(mapType(vd.type)).append(" ").append(vd.name);
            if (vd.initializer != null) {
                sb.append(" = ").append(genExpr(vd.initializer));
            }
        } else if (stmt.init instanceof GLSLParser.ExpressionStatement es) {
            sb.append(genExpr(es.expression));
        }
        sb.append("; ");

        // test
        if (stmt.test != null) {
            sb.append(genExpr(stmt.test));
        }
        sb.append("; ");

        // update
        if (stmt.update != null) {
            sb.append(genExpr(stmt.update));
        }
        sb.append(") {\n");

        indent++;
        stmt.body.accept(this);
        indent--;
        writeIndent();
        sb.append("}\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.WhileStatement stmt) {
        writeIndent();
        sb.append("while (").append(genExpr(stmt.test)).append(") {\n");
        indent++;
        stmt.body.accept(this);
        indent--;
        writeIndent();
        sb.append("}\n");
        return null;
    }

    @Override
    public String visit(GLSLParser.BinaryExpression expr) {
        String op = expr.operator;

        // Простейший маппинг текстурных функций можно делать выше по AST,
        // тут пока просто бинарный оператор как есть.
        return "(" + genExpr(expr.left) + " " + op + " " + genExpr(expr.right) + ")";
    }

    @Override
    public String visit(GLSLParser.UnaryExpression expr) {
        // предполагаем префиксную форму
        return "(" + expr.operator + genExpr(expr.argument) + ")";
    }

    @Override
    public String visit(GLSLParser.CallExpression expr) {
        String calleeName = "";
        if (expr.callee instanceof GLSLParser.Identifier id) {
            calleeName = id.name;
        } else {
            calleeName = expr.callee.accept(this);
        }

        // Примитивный маппинг некоторых GLSL-функций
        if (calleeName.equals("texture") || calleeName.equals("texture2D")) {
            calleeName = "tex2D";
        } else if (calleeName.equals("textureCube")) {
            calleeName = "texCUBE";
        } else if (calleeName.equals("mix")) {
            calleeName = "lerp";
        }

        StringBuilder loc = new StringBuilder();
        loc.append(calleeName).append("(");
        for (int i = 0; i < expr.arguments.size(); i++) {
            if (i > 0) loc.append(", ");
            loc.append(genExpr(expr.arguments.get(i)));
        }
        loc.append(")");
        return loc.toString();
    }

    @Override
    public String visit(GLSLParser.MemberExpression expr) {
        return genExpr(expr.object) + "." + genExpr(expr.property);
    }

    @Override
    public String visit(GLSLParser.Identifier identifier) {
        // Простая замена встроенных GLSL-переменных на HLSL
        if ("gl_Position".equals(identifier.name)) {
            return "output.position";
        }
        if ("gl_FragColor".equals(identifier.name)) {
            return "output.color";
        }
        return identifier.name;
    }

    @Override
    public String visit(GLSLParser.Literal literal) {
        // raw можно использовать, если ты его заполняешь в парсере
        return literal.raw != null ? literal.raw : String.valueOf(literal.value);
    }
}
