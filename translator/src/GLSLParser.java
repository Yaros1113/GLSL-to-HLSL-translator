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
        public String qualifier; // "in", "out", "inout" или null
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
        int save = current;
        boolean result = false;

        try {
            // Пропускаем модификаторы
            while (match(
                    TokenType1.KEYWORD_UNIFORM,
                    TokenType1.KEYWORD_ATTRIBUTE,
                    TokenType1.KEYWORD_VARYING,
                    TokenType1.KEYWORD_IN,
                    TokenType1.KEYWORD_OUT
            )) {
                // Просто пропускаем
            }

            // Проверяем тип или void
            if (check(TokenType1.KEYWORD_VOID) || checkTypeToken()) {
                result = true;
            }
        } finally {
            current = save;
        }

        return result;
    }

    private boolean checkTypeToken() {
        Token token = peek();

        // Проверяем ключевые слова типов
        switch (token.type) {
            case KEYWORD_FLOAT:
            case KEYWORD_INT:
            case KEYWORD_BOOL:
            case KEYWORD_VEC2:
            case KEYWORD_VEC3:
            case KEYWORD_VEC4:
            case KEYWORD_MAT2:
            case KEYWORD_MAT3:
            case KEYWORD_MAT4:
            case KEYWORD_SAMPLER2D:
            case KEYWORD_SAMPLERCUBE:
                return true;
            case IDENTIFIER:
                // Проверяем пользовательские типы (структуры)
                return isBuiltinType(token.value);
            default:
                return false;
        }
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


    private boolean checkFunctionDeclaration() {
        int save = current;

        // Проверяем тип возврата (без изменения current)
        if (!(check(TokenType1.KEYWORD_VOID) || checkTypeToken())) {
            return false;
        }

        // Пропускаем тип возврата
        advance(); // пропускаем тип (float, void и т.д.)

        // Проверяем наличие идентификатора
        if (!check(TokenType1.IDENTIFIER)) {
            current = save;
            return false;
        }

        advance(); // пропускаем имя функции

        // Проверяем наличие '('
        if (!check(TokenType1.LPAREN)) {
            current = save;
            return false;
        }

        // Восстанавливаем позицию
        current = save;
        return true;
    }


    private ASTNode parseDeclaration() {
        int save = current;

        try {
            // Пробуем определить, что это: функция или переменная
            // Читаем до первой скобки или точки с запятой

            // Пропускаем модификаторы
            while (match(
                    TokenType1.KEYWORD_UNIFORM,
                    TokenType1.KEYWORD_ATTRIBUTE,
                    TokenType1.KEYWORD_VARYING
            )) {
                // Просто пропускаем
            }

            // Читаем тип
            if (!checkTypeToken() && !check(TokenType1.KEYWORD_VOID)) {
                throw error(peek(), "Expected type for declaration");
            }
            advance(); // тип

            // Читаем имя
            if (!check(TokenType1.IDENTIFIER)) {
                throw error(peek(), "Expected identifier");
            }
            String name = advance().value;

            // Определяем, что дальше
            if (check(TokenType1.LPAREN)) {
                // Это функция
                current = save;
                return parseFunctionDeclaration();
            } else {
                // Это переменная
                current = save;
                return parseVariableDeclaration(false);
            }

        } catch (ParseError e) {
            current = save;
            throw e;
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

        // Пропускаем модификаторы
        while (match(
                TokenType1.KEYWORD_UNIFORM,
                TokenType1.KEYWORD_ATTRIBUTE,
                TokenType1.KEYWORD_VARYING
        )) {
            // Пропускаем
        }

        // Тип возврата
        if (match(TokenType1.KEYWORD_VOID)) {
            func.returnType = "void";
        } else if (checkTypeToken()) {
            func.returnType = advance().value;
        } else {
            error("Expected return type for function");
            func.returnType = "float"; // Значение по умолчанию
        }

        // Имя функции
        if (check(TokenType1.IDENTIFIER)) {
            func.name = advance().value;
        } else {
            error("Expected function name");
            func.name = "unknown";
        }

        // Параметры
        if (match(TokenType1.LPAREN)) {
            if (!check(TokenType1.RPAREN)) {
                do {
                    try {
                        func.parameters.add(parseParameter());
                    } catch (ParseError e) {
                        // Пропускаем некорректный параметр
                        error("Error parsing parameter: " + e.getMessage());
                        synchronize();
                        if (check(TokenType1.COMMA) || check(TokenType1.RPAREN)) {
                            continue;
                        }
                    }
                } while (match(TokenType1.COMMA));
            }
            expect(TokenType1.RPAREN, "Expected ')' after parameters");
        } else {
            error("Expected '(' after function name");
        }

        // Тело функции
        try {
            func.body = parseBlockStatement();
        } catch (ParseError e) {
            error("Error parsing function body: " + e.getMessage());
            func.body = new BlockStatement(); // Пустое тело
        }

        return func;
    }

    private Parameter parseParameter() {
        Parameter param = new Parameter();

        // Квалификаторы параметра
        if (match(TokenType1.KEYWORD_IN, TokenType1.KEYWORD_OUT, TokenType1.KEYWORD_INOUT)) {
            param.qualifier = previous().value;
        }

        // Тип параметра
        if (checkTypeToken()) {
            param.type = advance().value;
        } else {
            throw error(peek(), "Expected parameter type");
        }

        // Имя параметра
        if (check(TokenType1.IDENTIFIER)) {
            param.name = advance().value;
        } else {
            // Параметр может быть без имени
            param.name = null;
        }

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
            throw error(peek(), "Expected variable type");
        }

        // Имя переменной
        decl.name = expect(TokenType1.IDENTIFIER, "Expected variable name").value;

        // Семантика (если есть)
        if (match(TokenType1.COLON)) {
            decl.semantic = expect(TokenType1.IDENTIFIER, "Expected semantic").value;
        }

        // Инициализатор
        if (match(TokenType1.OP_ASSIGN)) {
            try {
                decl.initializer = parseExpression();
            } catch (ParseError e) {
                // Если не удалось разобрать выражение, записываем ошибку
                error("Error parsing initializer: " + e.getMessage());
            }
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
        // Пропускаем препроцессорные директивы в начале
        if (match(TokenType1.PREPROCESSOR_DIRECTIVE)) {
            // Создаем узел для препроцессорной директивы или просто пропускаем
            while (match(TokenType1.PREPROCESSOR_DIRECTIVE)) {
                // Пропускаем все подряд идущие директивы
            }
        }
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


    private ASTNode parsePrimary() {
        // Литералы
        if (match(TokenType1.BOOL_LITERAL, TokenType1.INT_LITERAL, TokenType1.FLOAT_LITERAL)) {
            return new Literal(previous().value);
        }

        if (match(TokenType1.STRING_LITERAL)) {
            return new Literal(previous().value.substring(1, previous().value.length() - 1));
        }

        // Сохраняем позицию для отката
        int start = current;

        // Попробуем распарсить как конструктор типа (vec2, vec3, vec4 и т.д.)
        if (checkTypeToken()) {
            String typeName = peek().value;

            // Пропускаем тип
            advance();

            // Проверяем, есть ли скобки для конструктора
            if (match(TokenType1.LPAREN)) {
                CallExpression call = new CallExpression();
                call.callee = new Identifier(typeName);

                if (!check(TokenType1.RPAREN)) {
                    do {
                        call.arguments.add(parseExpression());
                    } while (match(TokenType1.COMMA));
                }

                expect(TokenType1.RPAREN, "Expected ')' after constructor arguments");
                return call;
            } else {
                // Это не конструктор - откатываемся
                current = start;
            }
        }

        // Идентификатор
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
            // Если нашли точку с запятой, возвращаемся
            if (previous().type == TokenType1.SEMICOLON) return;

            // Если нашли начало нового объявления, возвращаемся
            switch (peek().type) {
                case KEYWORD_STRUCT:
                case KEYWORD_UNIFORM:
                case KEYWORD_ATTRIBUTE:
                case KEYWORD_VARYING:
                case KEYWORD_IN:
                case KEYWORD_OUT:
                case KEYWORD_VOID:
                case KEYWORD_FLOAT:
                case KEYWORD_INT:
                case KEYWORD_BOOL:
                case KEYWORD_VEC2:
                case KEYWORD_VEC3:
                case KEYWORD_VEC4:
                    return;
                default:
                    advance();
            }
        }
    }
    
    private static class ParseError extends RuntimeException {
        public ParseError(String message) {
            super(message);
        }
    }

}