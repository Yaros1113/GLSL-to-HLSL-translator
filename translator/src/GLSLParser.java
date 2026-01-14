import java.util.*;

//нисходящий рекурсивный спуск, Абстрактное Синтаксическое Дерево

public class GLSLParser {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errors = new ArrayList<>();

    public GLSLParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<String> getErrors() {
        return errors;
    }

    // Базовый класс для всех узлов AST
    public static abstract class ASTNode {
        public abstract <R> R accept(ASTVisitor<R> visitor);
    }

    // Интерфейс посетителя для AST
    public interface ASTVisitor<R> {
        R visit(Program program);
        R visit(VariableDeclaration decl);
        R visit(FunctionDeclaration decl);
        R visit(StructDeclaration decl);
        R visit(Parameter param);
        R visit(BlockStatement block);
        R visit(VariableStatement stmt);
        R visit(ExpressionStatement stmt);
        R visit(ReturnStatement stmt);
        R visit(IfStatement stmt);
        R visit(ForStatement stmt);
        R visit(WhileStatement stmt);
        R visit(BinaryExpression expr);
        R visit(UnaryExpression expr);
        R visit(CallExpression expr);
        R visit(MemberExpression expr);
        R visit(TernaryExpression expr);
        R visit(Identifier identifier);
        R visit(Literal literal);
    }

    // Основные узлы AST
    public static class Program extends ASTNode {
        public final List<ASTNode> declarations = new ArrayList<>();

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class VariableDeclaration extends ASTNode {
        public String type;
        public String name;
        public ASTNode initializer;
        public String semantic;
        public boolean isUniform;
        public boolean isAttribute;
        public boolean isVarying;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FunctionDeclaration extends ASTNode {
        public String returnType;
        public String name;
        public List<Parameter> parameters = new ArrayList<>();
        public BlockStatement body;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class StructDeclaration extends ASTNode {
        public String name;
        public List<VariableDeclaration> fields = new ArrayList<>();

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Parameter extends ASTNode {
        public String type;
        public String name;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BlockStatement extends ASTNode {
        public List<ASTNode> statements = new ArrayList<>();

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    // Операторы (statements)
    public static class VariableStatement extends ASTNode {
        public VariableDeclaration declaration;

        public VariableStatement(VariableDeclaration declaration) {
            this.declaration = declaration;
        }

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ExpressionStatement extends ASTNode {
        public ASTNode expression;

        public ExpressionStatement(ASTNode expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }


    public static class ReturnStatement extends ASTNode {
        public ASTNode argument;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class IfStatement extends ASTNode {
        public ASTNode test;
        public ASTNode consequent;
        public ASTNode alternate;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ForStatement extends ASTNode {
        public ASTNode init;
        public ASTNode test;
        public ASTNode update;
        public ASTNode body;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class WhileStatement extends ASTNode {
        public ASTNode test;
        public ASTNode body;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class TernaryExpression extends ASTNode {
        public ASTNode test;
        public ASTNode consequent;
        public ASTNode alternate;

        public TernaryExpression(ASTNode test, ASTNode consequent, ASTNode alternate) {
            this.test = test;
            this.consequent = consequent;
            this.alternate = alternate;
        }

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            // можно позже добавить visit(TernaryExpression) в ASTVisitor,
            // а пока можно бросить исключение или вернуть null
            throw new UnsupportedOperationException("TernaryExpression visitor not implemented");
        }
    }

    // Выражения (expressions)

    public static class BinaryExpression extends ASTNode {
        public String operator;
        public ASTNode left;
        public ASTNode right;

        public BinaryExpression(String operator, ASTNode left, ASTNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    //public static class BinaryExpression extends ASTNode {
    //    public String operator;
    //    public ASTNode left;
    //    public ASTNode right;

    //    @Override
     //   public <R> R accept(ASTVisitor<R> visitor) {
     //       return visitor.visit(this);
     //   }
    //}

    public static class UnaryExpression extends ASTNode {
        public String operator;
        public ASTNode argument;

        public UnaryExpression(String operator, ASTNode argument) {
            this.operator = operator;
            this.argument = argument;
        }

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    //public static class UnaryExpression extends ASTNode {
    //    public String operator;
    //    public ASTNode argument;

    //    @Override
     //   public <R> R accept(ASTVisitor<R> visitor) {
    //        return visitor.visit(this);
    //    }
    //}

    public static class CallExpression extends ASTNode {
        public ASTNode callee;
        public List<ASTNode> arguments = new ArrayList<>();

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MemberExpression extends ASTNode {
        public ASTNode object;
        public ASTNode property;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Identifier extends ASTNode {
        public String name;

        public Identifier(String name) {
            this.name = name;
        }

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Literal extends ASTNode {
        public Object value;
        public String raw;

        public Literal(Object value) {
            this.value = value;
            this.raw = value != null ? value.toString() : "null";
        }

        public Literal(Object value, String raw) {
            this.value = value;
            this.raw = raw;
        }

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    // Правила парсинга ========================================================
    
    public Program parseProgram() {
        Program program = new Program();
        
        while (!isAtEnd()) {
            try {
                if (match(TokenType1.PREPROCESSOR_DIRECTIVE)) {
                    continue; // игнорируем #version, #define, #include
                }
                else if (match(TokenType1.KEYWORD_STRUCT)) {
                    program.declarations.add(parseStructDeclaration());
                } else if (checkDeclarationStart()) {
                    program.declarations.add(parseDeclaration());
                } else {
                    error("Unexpected token: " + peek().value);
                    advance();
                }
            } catch (ParseError error) {
                synchronize();
            }
        }

        return program;
    }

    private boolean checkDeclarationStart() {
        return check(TokenType1.KEYWORD_UNIFORM) ||
                check(TokenType1.KEYWORD_ATTRIBUTE) ||
                check(TokenType1.KEYWORD_VARYING) ||
                check(TokenType1.KEYWORD_IN) ||
                check(TokenType1.KEYWORD_OUT) ||
                checkTypeToken() ||
                check(TokenType1.KEYWORD_VOID);

    }

    private boolean checkTypeToken() {
        if (peek().type != TokenType1.IDENTIFIER)
            return false;

        return isBuiltinType(peek().value);
    }

    private boolean isBuiltinType(String name) {
        return name.equals("float") ||
                name.equals("int")   ||
                name.equals("bool")  ||
                name.equals("vec2")  ||
                name.equals("vec3")  ||
                name.equals("vec4")  ||
                name.equals("mat2")  ||
                name.equals("mat3")  ||
                name.equals("mat4")  ||
                name.equals("sampler2D") ||
                name.equals("samplerCube");
    }

    /*private boolean checkTypeToken() {
        TokenType1 type = peek().type;
        return type.name().startsWith("KEYWORD_") &&
               (type.name().contains("FLOAT") ||
                type.name().contains("INT") ||
                type.name().contains("BOOL") ||
                type.name().contains("VEC") ||
                type.name().contains("MAT") ||
                type.name().contains("SAMPLER"));
    }*/

    private boolean checkFunctionDeclaration() {
        int save = current;

        // return type
        if (match(TokenType1.KEYWORD_VOID) || checkTypeToken()) {
            advance(); // имя функции
            boolean isFunc = check(TokenType1.LPAREN);
            current = save;
            return isFunc;
        }

        current = save;
        return false;
    }


    private ASTNode parseDeclaration() {
        if (checkFunctionDeclaration()) {
            return parseFunctionDeclaration();
        } else {
            return parseVariableDeclaration(false);
        }
    }

    private StructDeclaration parseStructDeclaration() {
        StructDeclaration struct = new StructDeclaration();
        struct.name = expect(TokenType1.IDENTIFIER, "Expected struct name").value;
        expect(TokenType1.LBRACE, "Expected '{' after struct name");

        while (!match(TokenType1.RBRACE) && !isAtEnd()) {
            if (checkTypeToken()) {
                struct.fields.add(parseVariableDeclaration(true));
            } else {
                error("Expected type in struct field");
                advance();
            }
        }

        expect(TokenType1.SEMICOLON, "Expected ';' after struct declaration");
        return struct;
    }

    private FunctionDeclaration parseFunctionDeclaration() {
        FunctionDeclaration func = new FunctionDeclaration();

        // Тип возврата
        if (match(TokenType1.KEYWORD_VOID)) {
            func.returnType = "void";
        } else if (checkTypeToken()) {
            func.returnType = advance().value;
        } else {
            error("Expected return type for function");
        }

        // Имя функции
        func.name = expect(TokenType1.IDENTIFIER, "Expected function name").value;

        // Параметры
        expect(TokenType1.LPAREN, "Expected '(' after function name");
        if (!check(TokenType1.RPAREN)) {
            do {
                func.parameters.add(parseParameter());
            } while (match(TokenType1.COMMA));
        }
        expect(TokenType1.RPAREN, "Expected ')' after parameters");

        // Тело функции
        func.body = parseBlockStatement();
        return func;
    }

    private Parameter parseParameter() {
        Parameter param = new Parameter();

        if (checkTypeToken()) {
            param.type = advance().value;
        } else {
            error("Expected parameter type");
        }

        param.name = expect(TokenType1.IDENTIFIER, "Expected parameter name").value;
        return param;
    }

    private VariableDeclaration parseVariableDeclaration(boolean inStruct) {
        VariableDeclaration decl = new VariableDeclaration();

        // Модификаторы
        if (match(TokenType1.KEYWORD_UNIFORM)) {
            decl.isUniform = true;
        } else if (match(TokenType1.KEYWORD_ATTRIBUTE)) {
            decl.isAttribute = true;
        } else if (match(TokenType1.KEYWORD_VARYING)) {
            decl.isVarying = true;
        } else if (match(TokenType1.KEYWORD_IN, TokenType1.KEYWORD_OUT)) {
            // Обработка in/out модификаторов
        }

        // Тип переменной
        if (checkTypeToken()) {
            decl.type = advance().value;
        } else {
            error("Expected variable type");
        }

        // Имя переменной
        decl.name = expect(TokenType1.IDENTIFIER, "Expected variable name").value;

        // Семантика (если есть)
        if (match(TokenType1.COLON)) {
            decl.semantic = expect(TokenType1.IDENTIFIER, "Expected semantic").value;
        }

        // Инициализатор
        if (match(TokenType1.OP_ASSIGN)) {
            decl.initializer = parseExpression();
        }

        if (!inStruct) {
            expect(TokenType1.SEMICOLON, "Expected ';' after variable declaration");
        }

        return decl;
    }

    private BlockStatement parseBlockStatement() {
        BlockStatement block = new BlockStatement();
        expect(TokenType1.LBRACE, "Expected '{' before block");

        while (!check(TokenType1.RBRACE) && !isAtEnd()) {
            block.statements.add(parseStatement());
        }

        expect(TokenType1.RBRACE, "Expected '}' after block");
        return block;
    }

    private ASTNode parseStatement() {
        if (match(TokenType1.KEYWORD_RETURN)) return parseReturnStatement();
        if (match(TokenType1.KEYWORD_IF))     return parseIfStatement();
        if (match(TokenType1.KEYWORD_FOR))    return parseForStatement();
        if (match(TokenType1.KEYWORD_WHILE))  return parseWhileStatement();
        if (check(TokenType1.LBRACE))         return parseBlockStatement();
        if (checkDeclarationStart())         return new VariableStatement(parseVariableDeclaration(false));

        return new ExpressionStatement(parseExpressionStatement());
    }

    private ReturnStatement parseReturnStatement() {
        ReturnStatement stmt = new ReturnStatement();
        if (!check(TokenType1.SEMICOLON)) {
            stmt.argument = parseExpression();
        }
        expect(TokenType1.SEMICOLON, "Expected ';' after return");
        return stmt;
    }

    private IfStatement parseIfStatement() {
        IfStatement stmt = new IfStatement();
        expect(TokenType1.LPAREN, "Expected '(' after 'if'");
        stmt.test = parseExpression();
        expect(TokenType1.RPAREN, "Expected ')' after if condition");

        stmt.consequent = parseStatement();

        if (match(TokenType1.KEYWORD_ELSE)) {
            stmt.alternate = parseStatement();
        }

        return stmt;
    }

    private ForStatement parseForStatement() {
        ForStatement stmt = new ForStatement();
        expect(TokenType1.LPAREN, "Expected '(' after 'for'");

        // init: либо декларация, либо выражение, либо пусто
        if (!check(TokenType1.SEMICOLON)) {
            if (checkDeclarationStart()) {
                stmt.init = parseVariableDeclaration(false);
            } else {
                stmt.init = parseExpressionStatement();
            }
        } else {
            advance(); // ';'
        }

        // условие
        if (!check(TokenType1.SEMICOLON)) {
            stmt.test = parseExpression();
        }
        expect(TokenType1.SEMICOLON, "Expected ';' after loop condition");

        // update
        if (!check(TokenType1.RPAREN)) {
            stmt.update = parseExpression();
        }
        expect(TokenType1.RPAREN, "Expected ')' after for clauses");

        stmt.body = parseStatement();
        return stmt;
    }

    private WhileStatement parseWhileStatement() {
        WhileStatement stmt = new WhileStatement();
        expect(TokenType1.LPAREN, "Expected '(' after 'while'");
        stmt.test = parseExpression();
        expect(TokenType1.RPAREN, "Expected ')' after condition");
        stmt.body = parseStatement();
        return stmt;
    }

    private ASTNode parseExpressionStatement() {
        ASTNode expr = parseExpression();
        expect(TokenType1.SEMICOLON, "Expected ';' after expression");
        return expr;
    }


    // Парсинг выражений с приоритетом
    private ASTNode parseExpression() {
        return parseAssignment();
    }

    private ASTNode parseAssignment() {
        ASTNode expr = parseTernary();

        if (match(
                TokenType1.OP_ASSIGN,
                TokenType1.OP_PLUS_ASSIGN,
                TokenType1.OP_MINUS_ASSIGN,
                TokenType1.OP_MULT_ASSIGN,
                TokenType1.OP_DIV_ASSIGN
        )) {
            Token operator = previous();
            ASTNode value = parseAssignment();
            return new BinaryExpression(operator.value, expr, value);
        }

        return expr;
    }

    private ASTNode parseTernary() {
        ASTNode expr = parseLogicalOr();

        if (match(TokenType1.QUESTION)) {
            ASTNode consequent = parseExpression();
            expect(TokenType1.COLON, "Expected ':' in ternary operator");
            ASTNode alternate = parseTernary();
            return new TernaryExpression(expr, consequent, alternate);
        }

        return expr;
    }

    private ASTNode parseLogicalOr() {
        ASTNode expr = parseLogicalAnd();

        while (match(TokenType1.OP_OR)) {
            Token operator = previous();
            ASTNode right = parseLogicalAnd();
            expr = new BinaryExpression(operator.value, expr, right);
        }

        return expr;
    }

    // Остальные уровни (AND, equality, comparison, term, factor) аналогично:
    private ASTNode parseLogicalAnd() {
        ASTNode expr = parseEquality();
        while (match(TokenType1.OP_AND)) {
            Token operator = previous();
            ASTNode right = parseEquality();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        return expr;
    }

    private ASTNode parseEquality() {
        ASTNode expr = parseComparison();
        while (match(TokenType1.OP_EQ, TokenType1.OP_NE)) {
            Token operator = previous();
            ASTNode right = parseComparison();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        return expr;
    }

    private ASTNode parseComparison() {
        ASTNode expr = parseTerm();
        while (match(TokenType1.OP_LT, TokenType1.OP_GT, TokenType1.OP_LE, TokenType1.OP_GE)) {
            Token operator = previous();
            ASTNode right = parseTerm();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        return expr;
    }

    private ASTNode parseTerm() {
        ASTNode expr = parseFactor();
        while (match(TokenType1.OP_PLUS, TokenType1.OP_MINUS)) {
            Token operator = previous();
            ASTNode right = parseFactor();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        return expr;
    }

    private ASTNode parseFactor() {
        ASTNode expr = parseUnary();
        while (match(TokenType1.OP_MULT, TokenType1.OP_DIV)) {
            Token operator = previous();
            ASTNode right = parseUnary();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        return expr;
    }

    private ASTNode parseUnary() {
        if (match(TokenType1.OP_MINUS, TokenType1.OP_NOT, TokenType1.OP_INC, TokenType1.OP_DEC)) {
            Token operator = previous();
            ASTNode right = parseUnary();
            return new UnaryExpression(operator.value, right);
        }
        return parsePrimary();
    }

    /*private ASTNode parsePrimary() {
        if (match(TokenType1.BOOL_LITERAL, TokenType1.INT_LITERAL, TokenType1.FLOAT_LITERAL)) {
            return new Literal(previous().value);
        }

        if (match(TokenType1.STRING_LITERAL)) {
            return new Literal(previous().value.substring(1, previous().value.length() - 1));
        }

        if (match(
                TokenType1.KEYWORD_VEC2, TokenType1.KEYWORD_VEC3, TokenType1.KEYWORD_VEC4,
                TokenType1.KEYWORD_MAT2, TokenType1.KEYWORD_MAT3, TokenType1.KEYWORD_MAT4
        )) {
            String name = previous().value;

            if (match(TokenType1.LPAREN)) {
                CallExpression call = new CallExpression();
                call.callee = new Identifier(name);

                if (!check(TokenType1.RPAREN)) {
                    do {
                        call.arguments.add(parseExpression());
                    } while (match(TokenType1.COMMA));
                }

                expect(TokenType1.RPAREN, "Expected ')' after constructor args");
                return call;
            }

            return new Identifier(name);
        }

        if (match(TokenType1.LPAREN)) {
            ASTNode expr = parseExpression();
            expect(TokenType1.RPAREN, "Expected ')' after expression");
            return expr;
        }

        throw error(peek(), "Expected expression");
    }*/

    private ASTNode parsePrimary() {

        // Литералы
        if (match(TokenType1.BOOL_LITERAL, TokenType1.INT_LITERAL, TokenType1.FLOAT_LITERAL)) {
            return new Literal(previous().value);
        }

        if (match(TokenType1.STRING_LITERAL)) {
            return new Literal(previous().value.substring(1, previous().value.length() - 1));
        }

        // Идентификатор (включая vec4, mat4 и т.п.)
        if (match(TokenType1.IDENTIFIER)) {
            String name = previous().value;

            // Вызов функции или конструктора
            if (match(TokenType1.LPAREN)) {
                CallExpression call = new CallExpression();
                call.callee = new Identifier(name);

                if (!check(TokenType1.RPAREN)) {
                    do {
                        call.arguments.add(parseExpression());
                    } while (match(TokenType1.COMMA));
                }

                expect(TokenType1.RPAREN, "Expected ')' after arguments");
                return call;
            }

            // Обращение к члену: a.b
            if (match(TokenType1.DOT)) {
                MemberExpression member = new MemberExpression();
                member.object = new Identifier(name);
                member.property = parsePrimary();
                return member;
            }

            return new Identifier(name);
        }

        // Скобки
        if (match(TokenType1.LPAREN)) {
            ASTNode expr = parseExpression();
            expect(TokenType1.RPAREN, "Expected ')' after expression");
            return expr;
        }

        throw error(peek(), "Expected expression");
    }


    /*private ASTNode parsePrimary() {
        // GLSL constructors: vec2(...), vec3(...), mat4(...)
        if (match(TokenType1.IDENTIFIER)) {
            String name = previous().value;

            // constructor call
            if (match(TokenType1.LPAREN)) {
                CallExpression call = new CallExpression();
                //GLSLParser.CallExpression call = new GLSLParser.CallExpression();
                call.callee = new GLSLParser.Identifier(name);

                if (!check(TokenType1.RPAREN)) {
                    do {
                        call.arguments.add(parseExpression());
                    } while (match(TokenType1.COMMA));
                }

                expect(TokenType1.RPAREN, "Expected ')' after constructor arguments");
                return call;
            }

            return new GLSLParser.Identifier(name);
        }


        if (match(TokenType1.LPAREN)) {
            ASTNode expr = parseExpression();
            expect(TokenType1.RPAREN, "Expected ')' after expression");
            return expr;
        }

        throw error(peek(), "Expected expression");
    }*/

   /* private ASTNode parsePrimary() {
        if (match(TokenType1.BOOL_LITERAL, TokenType1.INT_LITERAL, TokenType1.FLOAT_LITERAL)) {
            return new Literal(previous().value);
        }

        if (match(TokenType1.STRING_LITERAL)) {
            return new Literal(previous().value.substring(1, previous().value.length() - 1));
        }

        if (match(TokenType1.IDENTIFIER)) {
            String name = previous().value;

                // Вызов функции
            if (match(TokenType1.LPAREN)) {
                CallExpression call = new CallExpression();
                call.callee = new Identifier(name);

                if (!check(TokenType1.RPAREN)) {
                    do {
                        call.arguments.add(parseExpression());
                    } while (match(TokenType1.COMMA));
                }

                expect(TokenType1.RPAREN, "Expected ')' after arguments");
                return call;
            }

            // Обращение к члену структуры (a.b)
            if (match(TokenType1.DOT)) {
                MemberExpression member = new MemberExpression();
                member.object = new Identifier(name);
                member.property = parsePrimary();
                return member;
            }

            return new Identifier(name);
        }

        if (match(TokenType1.LPAREN)) {
            ASTNode expr = parseExpression();
            expect(TokenType1.RPAREN, "Expected ')' after expression");
            return expr;
        }

        throw error(peek(), "Expected expression");
    }*/

    // Вспомогательные методы ==================================================

    private boolean match(TokenType1... types) {
        for (TokenType1 type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType1 type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType1.EOF;
    }

    private Token expect(TokenType1 type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        String errorMsg = String.format("[Line %d, Col %d] %s",
            token.line, token.column, message);
        errors.add(errorMsg);
        return new ParseError(errorMsg);
    }

    private void error(String message) {
        Token token = peek();
        error(token, message);
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType1.SEMICOLON) return;
            
            switch (peek().type) {
                case KEYWORD_STRUCT:
                case KEYWORD_UNIFORM:
                case KEYWORD_VOID:
                case KEYWORD_FLOAT:
                case KEYWORD_SAMPLER2D:
                    return;
                default://???
            }
            
            advance();
        }
    }
    
    private static class ParseError extends RuntimeException {
        public ParseError(String message) {
            super(message);
        }
    }

    // Пример использования
    /*public static void main(String[] args) {
        String glslCode = 
            "struct Light {\n" +
            "   vec3 position;\n" +
            "   vec4 color;\n" +
            "};\n\n" +
            "uniform Light lights[2];\n" +
            "varying vec2 texCoord;\n\n" +
            "vec4 calculateLight(vec3 position, vec3 normal) {\n" +
            "   vec4 result = vec4(0.0);\n" +
            "   for (int i = 0; i < 2; i++) {\n" +
            "       vec3 lightDir = normalize(lights[i].position - position);\n" +
            "       float diff = max(dot(normal, lightDir), 0.0);\n" +
            "       result += lights[i].color * diff;\n" +
            "   }\n" +
            "   return result;\n" +
            "}\n\n" +
            "void main() {\n" +
            "   vec3 normal = normalize(vNormal);\n" +
            "   vec4 lightColor = calculateLight(vPosition, normal);\n" +
            "   gl_FragColor = texture2D(uTexture, texCoord) * lightColor;\n" +
            "}";
        
        // Лексический анализ
        GLSLLexer lexer = new GLSLLexer(glslCode);
        List<Token> tokens = lexer.tokenize();
        
        // Синтаксический анализ
        GLSLParser parser = new GLSLParser(tokens);
        Program program = parser.parseProgram();
        
        // Вывод ошибок
        if (!parser.getErrors().isEmpty()) {
            System.out.println("Errors during parsing:");
            for (String error : parser.getErrors()) {
                System.out.println(error);
            }
        } else {
            System.out.println("Parsing completed successfully");
            // Здесь можно добавить обход AST или трансляцию в HLSL
        }
    }*/
}