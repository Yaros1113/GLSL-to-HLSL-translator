import java.util.*;

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

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ExpressionStatement extends ASTNode {
        public ASTNode expression;

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

    // Выражения (expressions)
    public static class BinaryExpression extends ASTNode {
        public String operator;
        public ASTNode left;
        public ASTNode right;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class UnaryExpression extends ASTNode {
        public String operator;
        public ASTNode argument;

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

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

        @Override
        public <R> R accept(ASTVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Literal extends ASTNode {
        public Object value;
        public String raw;

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
                if (match(TokenType.KEYWORD_STRUCT)) {
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
        return check(TokenType.KEYWORD_UNIFORM) ||
               check(TokenType.KEYWORD_ATTRIBUTE) ||
               check(TokenType.KEYWORD_VARYING) ||
               check(TokenType.KEYWORD_IN) ||
               check(TokenType.KEYWORD_OUT) ||
               checkTypeToken() ||
               check(TokenType.KEYWORD_VOID);
    }

    private boolean checkTypeToken() {
        TokenType type = peek().type;
        return type.name().startsWith("KEYWORD_") && 
               (type.name().contains("FLOAT") || 
                type.name().contains("INT") || 
                type.name().contains("BOOL") || 
                type.name().contains("VEC") || 
                type.name().contains("MAT") || 
                type.name().contains("SAMPLER"));
    }

    private ASTNode parseDeclaration() {
        if (checkFunctionDeclaration()) {
            return parseFunctionDeclaration();
        } else {
            return parseVariableDeclaration();
        }
    }

    private StructDeclaration parseStructDeclaration() {
        StructDeclaration struct = new StructDeclaration();
        struct.name = expect(TokenType.IDENTIFIER, "Expected struct name").value;
        expect(TokenType.LBRACE, "Expected '{' after struct name");
        
        while (!match(TokenType.RBRACE) && !isAtEnd()) {
            if (checkTypeToken()) {
                struct.fields.add(parseVariableDeclaration(true));
            } else {
                error("Expected type in struct field");
                advance();
            }
        }
        
        expect(TokenType.SEMICOLON, "Expected ';' after struct declaration");
        return struct;
    }

    private FunctionDeclaration parseFunctionDeclaration() {
        FunctionDeclaration func = new FunctionDeclaration();
        
        // Тип возврата
        if (match(TokenType.KEYWORD_VOID)) {
            func.returnType = "void";
        } else if (checkTypeToken()) {
            func.returnType = advance().value;
        } else {
            error("Expected return type for function");
        }
        
        // Имя функции
        func.name = expect(TokenType.IDENTIFIER, "Expected function name").value;
        
        // Параметры
        expect(TokenType.LPAREN, "Expected '(' after function name");
        if (!check(TokenType.RPAREN)) {
            do {
                func.parameters.add(parseParameter());
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RPAREN, "Expected ')' after parameters");
        
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
        
        param.name = expect(TokenType.IDENTIFIER, "Expected parameter name").value;
        return param;
    }

    private VariableDeclaration parseVariableDeclaration(boolean inStruct) {
        VariableDeclaration decl = new VariableDeclaration();
        
        // Модификаторы
        if (match(TokenType.KEYWORD_UNIFORM)) {
            decl.isUniform = true;
        } else if (match(TokenType.KEYWORD_ATTRIBUTE)) {
            decl.isAttribute = true;
        } else if (match(TokenType.KEYWORD_VARYING)) {
            decl.isVarying = true;
        } else if (match(TokenType.KEYWORD_IN, TokenType.KEYWORD_OUT)) {
            // Обработка in/out модификаторов
        }
        
        // Тип переменной
        if (checkTypeToken()) {
            decl.type = advance().value;
        } else {
            error("Expected variable type");
        }
        
        // Имя переменной
        decl.name = expect(TokenType.IDENTIFIER, "Expected variable name").value;
        
        // Семантика (если есть)
        if (match(TokenType.COLON)) {
            decl.semantic = expect(TokenType.IDENTIFIER, "Expected semantic").value;
        }
        
        // Инициализатор
        if (match(TokenType.OP_ASSIGN)) {
            decl.initializer = parseExpression();
        }
        
        if (!inStruct) {
            expect(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        }
        
        return decl;
    }

    private BlockStatement parseBlockStatement() {
        BlockStatement block = new BlockStatement();
        expect(TokenType.LBRACE, "Expected '{' before block");
        
        while (!match(TokenType.RBRACE) && !isAtEnd()) {
            block.statements.add(parseStatement());
        }
        
        return block;
    }

    private ASTNode parseStatement() {
        if (match(TokenType.KEYWORD_RETURN)) return parseReturnStatement();
        if (match(TokenType.KEYWORD_IF)) return parseIfStatement();
        if (match(TokenType.KEYWORD_FOR)) return parseForStatement();
        if (match(TokenType.KEYWORD_WHILE)) return parseWhileStatement();
        if (match(TokenType.LBRACE)) return parseBlockStatement();
        if (checkDeclarationStart()) return new VariableStatement(parseVariableDeclaration(false));
        
        return new ExpressionStatement(parseExpressionStatement());
    }

    private ReturnStatement parseReturnStatement() {
        ReturnStatement stmt = new ReturnStatement();
        if (!check(TokenType.SEMICOLON)) {
            stmt.argument = parseExpression();
        }
        expect(TokenType.SEMICOLON, "Expected ';' after return");
        return stmt;
    }

    private IfStatement parseIfStatement() {
        IfStatement stmt = new IfStatement();
        expect(TokenType.LPAREN, "Expected '(' after 'if'");
        stmt.test = parseExpression();
        expect(TokenType.RPAREN, "Expected ')' after if condition");
        
        stmt.consequent = parseStatement();
        
        if (match(TokenType.KEYWORD_ELSE)) {
            stmt.alternate = parseStatement();
        }
        
        return stmt;
    }

    // Парсинг выражений с приоритетом
    private ASTNode parseExpression() {
        return parseAssignment();
    }

    private ASTNode parseAssignment() {
        ASTNode expr = parseTernary();
        
        if (match(
            TokenType.OP_ASSIGN,
            TokenType.OP_PLUS_ASSIGN,
            TokenType.OP_MINUS_ASSIGN,
            TokenType.OP_MULT_ASSIGN,
            TokenType.OP_DIV_ASSIGN
        )) {
            Token operator = previous();
            ASTNode value = parseAssignment();
            return new BinaryExpression(operator.value, expr, value);
        }
        
        return expr;
    }

    private ASTNode parseTernary() {
        ASTNode expr = parseLogicalOr();
        
        if (match(TokenType.QUESTION)) {
            ASTNode consequent = parseExpression();
            expect(TokenType.COLON, "Expected ':' in ternary operator");
            ASTNode alternate = parseTernary();
            return new TernaryExpression(expr, consequent, alternate);
        }
        
        return expr;
    }

    private ASTNode parseLogicalOr() {
        ASTNode expr = parseLogicalAnd();
        
        while (match(TokenType.OP_OR)) {
            Token operator = previous();
            ASTNode right = parseLogicalAnd();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        
        return expr;
    }

    private ASTNode parseLogicalAnd() {
        ASTNode expr = parseEquality();
        
        while (match(TokenType.OP_AND)) {
            Token operator = previous();
            ASTNode right = parseEquality();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        
        return expr;
    }

    private ASTNode parseEquality() {
        ASTNode expr = parseComparison();
        
        while (match(TokenType.OP_EQ, TokenType.OP_NE)) {
            Token operator = previous();
            ASTNode right = parseComparison();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        
        return expr;
    }

    private ASTNode parseComparison() {
        ASTNode expr = parseTerm();
        
        while (match(
            TokenType.OP_LT, 
            TokenType.OP_GT, 
            TokenType.OP_LE, 
            TokenType.OP_GE
        )) {
            Token operator = previous();
            ASTNode right = parseTerm();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        
        return expr;
    }

    private ASTNode parseTerm() {
        ASTNode expr = parseFactor();
        
        while (match(TokenType.OP_PLUS, TokenType.OP_MINUS)) {
            Token operator = previous();
            ASTNode right = parseFactor();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        
        return expr;
    }

    private ASTNode parseFactor() {
        ASTNode expr = parseUnary();
        
        while (match(TokenType.OP_MULT, TokenType.OP_DIV)) {
            Token operator = previous();
            ASTNode right = parseUnary();
            expr = new BinaryExpression(operator.value, expr, right);
        }
        
        return expr;
    }

    private ASTNode parseUnary() {
        if (match(
            TokenType.OP_MINUS, 
            TokenType.OP_NOT, 
            TokenType.OP_INC, 
            TokenType.OP_DEC
        )) {
            Token operator = previous();
            ASTNode right = parseUnary();
            return new UnaryExpression(operator.value, right);
        }
        
        return parsePrimary();
    }

    private ASTNode parsePrimary() {
        if (match(TokenType.BOOL_LITERAL, TokenType.INT_LITERAL, TokenType.FLOAT_LITERAL)) {
            return new Literal(previous().value);
        }
        
        if (match(TokenType.STRING_LITERAL)) {
            return new Literal(previous().value.substring(1, previous().value.length() - 1));
        }
        
        if (match(TokenType.IDENTIFIER)) {
            String name = previous().value;
            
            // Вызов функции
            if (match(TokenType.LPAREN)) {
                CallExpression call = new CallExpression();
                call.callee = new Identifier(name);
                
                if (!check(TokenType.RPAREN)) {
                    do {
                        call.arguments.add(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                
                expect(TokenType.RPAREN, "Expected ')' after arguments");
                return call;
            }
            
            // Обращение к члену структуры (a.b)
            if (match(TokenType.DOT)) {
                MemberExpression member = new MemberExpression();
                member.object = new Identifier(name);
                member.property = parsePrimary();
                return member;
            }
            
            return new Identifier(name);
        }
        
        if (match(TokenType.LPAREN)) {
            ASTNode expr = parseExpression();
            expect(TokenType.RPAREN, "Expected ')' after expression");
            return expr;
        }
        
        throw error(peek(), "Expected expression");
    }

    // Вспомогательные методы ==================================================
    
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    private boolean check(TokenType type) {
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
        return peek().type == TokenType.EOF;
    }
    
    private Token expect(TokenType type, String message) {
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
            if (previous().type == TokenType.SEMICOLON) return;
            
            switch (peek().type) {
                case KEYWORD_STRUCT:
                case KEYWORD_UNIFORM:
                case KEYWORD_VOID:
                case KEYWORD_FLOAT:
                case KEYWORD_VEC2:
                case KEYWORD_VEC3:
                case KEYWORD_VEC4:
                case KEYWORD_MAT2:
                case KEYWORD_MAT3:
                case KEYWORD_MAT4:
                case KEYWORD_SAMPLER2D:
                    return;
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
    public static void main(String[] args) {
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
    }
}