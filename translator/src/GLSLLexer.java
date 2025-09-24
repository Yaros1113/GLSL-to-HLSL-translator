import java.util.*;
import java.util.regex.*;

public class GLSLLexer {
    private final String input;
    private int pos;
    private final List<Token> tokens = new ArrayList<>();
    private static final Pattern TOKEN_PATTERNS;
    
    // Типы токенов
    public enum TokenType {
        // Ключевые слова
        KEYWORD_VOID, KEYWORD_FLOAT, KEYWORD_INT, KEYWORD_BOOL, 
        KEYWORD_VEC2, KEYWORD_VEC3, KEYWORD_VEC4,
        KEYWORD_MAT2, KEYWORD_MAT3, KEYWORD_MAT4,
        KEYWORD_SAMPLER2D, KEYWORD_SAMPLERCUBE,
        KEYWORD_IF, KEYWORD_ELSE, KEYWORD_FOR, KEYWORD_WHILE, 
        KEYWORD_DO, KEYWORD_RETURN, KEYWORD_BREAK, KEYWORD_CONTINUE,
        KEYWORD_UNIFORM, KEYWORD_ATTRIBUTE, KEYWORD_VARYING,
        KEYWORD_IN, KEYWORD_OUT, KEYWORD_INOUT,
        KEYWORD_STRUCT,
        
        // Операторы
        OP_PLUS, OP_MINUS, OP_MULT, OP_DIV, OP_ASSIGN, OP_EQ, OP_NE, 
        OP_LT, OP_GT, OP_LE, OP_GE, OP_AND, OP_OR, OP_NOT, 
        OP_INC, OP_DEC, OP_PLUS_ASSIGN, OP_MINUS_ASSIGN, OP_MULT_ASSIGN, OP_DIV_ASSIGN,
        
        // Разделители
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, 
        COMMA, SEMICOLON, COLON, DOT,
        
        // Литералы
        FLOAT_LITERAL, INT_LITERAL, BOOL_LITERAL, STRING_LITERAL,
        
        // Идентификаторы
        IDENTIFIER,
        
        // Прочее
        PREPROCESSOR_DIRECTIVE,
        
        // Конец файла
        EOF
    }
    
    // Класс токена
    public static class Token {
        public final TokenType type;
        public final String value;
        public final int line;
        public final int column;
        
        public Token(TokenType type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }
        
        @Override
        public String toString() {
            return String.format("Token[%s, '%s'] (line %d, col %d)", 
                    type, value, line, column);
        }
    }
    
    // Статическая инициализация регулярных выражений
    static {
        // Компилируем все шаблоны в один большой RegEx
        String patterns = 
            "(?<PREPROCESSOR>#.*)" + "|" +
            "(?<KEYWORD>\\b(void|float|int|bool|vec2|vec3|vec4|mat2|mat3|mat4|sampler2D|samplerCube|" +
            "if|else|for|while|do|return|break|continue|uniform|attribute|varying|in|out|inout|struct)\\b)" + "|" +
            "(?<BOOL>\\b(true|false)\\b)" + "|" +
            "(?<FLOAT>\\d+\\.\\d*([eE][-+]?\\d+)?|\\d*\\.\\d+([eE][-+]?\\d+)?|\\d+[eE][-+]?\\d+)" + "|" +
            "(?<INT>\\b\\d+\\b)" + "|" +
            "(?<STRING>\"([^\"\\\\]|\\\\.)*\")" + "|" +
            "(?<OP>\\+\\+|--|\\+=|-=|\\*=|/=|==|!=|<=|>=|&&|\\|\\||[+\\-*/=<>!&|])" + "|" +
            "(?<SEPARATOR>[\\(\\)\\{\\}\\[\\],;:\\.])" + "|" +
            "(?<IDENTIFIER>\\b[a-zA-Z_]\\w*\\b)" + "|" +
            "(?<COMMENT>//.*|/\\*[\\s\\S]*?\\*/)" + "|" +
            "(?<WHITESPACE>\\s+)" + "|" +
            "(?<UNKNOWN>.)";
        
        TOKEN_PATTERNS = Pattern.compile(patterns);
    }
    
    public GLSLLexer(String input) {
        this.input = input;
        this.pos = 0;
    }
    
    public List<Token> tokenize() {
        int line = 1;
        int lineStart = 0;
        
        Matcher matcher = TOKEN_PATTERNS.matcher(input);
        while (pos < input.length()) {
            if (!matcher.find(pos)) break;
            
            // Обновляем информацию о позиции
            int start = matcher.start();
            int end = matcher.end();
            pos = end;
            
            // Вычисляем позицию в строке
            int column = start - lineStart + 1;

            // Обновляем счетчики строк
            String tokenValue = matcher.group();
            for (char c : tokenValue.toCharArray()) {
                if (c == '\n') {
                    line++;
                    lineStart = start + tokenValue.indexOf('\n') + 1;
                }
            }
            
            // Обработка найденных групп
            if (matcher.group("PREPROCESSOR") != null) {
                tokens.add(new Token(TokenType.PREPROCESSOR_DIRECTIVE, 
                                    matcher.group(), line, column));
            }
            else if (matcher.group("KEYWORD") != null) {
                tokens.add(createKeywordToken(matcher.group(), line, column));
            }
            else if (matcher.group("BOOL") != null) {
                tokens.add(new Token(TokenType.BOOL_LITERAL, 
                                    matcher.group(), line, column));
            }
            else if (matcher.group("FLOAT") != null) {
                tokens.add(new Token(TokenType.FLOAT_LITERAL, 
                                    matcher.group(), line, column));
            }
            else if (matcher.group("INT") != null) {
                tokens.add(new Token(TokenType.INT_LITERAL, 
                                    matcher.group(), line, column));
            }
            else if (matcher.group("STRING") != null) {
                tokens.add(new Token(TokenType.STRING_LITERAL, 
                                    matcher.group(), line, column));
            }
            else if (matcher.group("OP") != null) {
                tokens.add(createOperatorToken(matcher.group(), line, column));
            }
            else if (matcher.group("SEPARATOR") != null) {
                tokens.add(createSeparatorToken(matcher.group(), line, column));
            }
            else if (matcher.group("IDENTIFIER") != null) {
                tokens.add(new Token(TokenType.IDENTIFIER, 
                                    matcher.group(), line, column));
            }
            else if (matcher.group("COMMENT") != null) {
                // Пропускаем комментарии
                updateLineInfo(matcher.group(), line, lineStart);
                continue;
            }
            else if (matcher.group("WHITESPACE") != null) {
                updateLineInfo(matcher.group(), line, lineStart);
                continue;
            }
            else if (matcher.group("UNKNOWN") != null) {
                throw new RuntimeException("Unexpected character at line " + 
                    line + ", column " + column + ": '" + matcher.group() + "'");
            }
        }
        
        tokens.add(new Token(TokenType.EOF, "", line, pos - lineStart + 1));
        return tokens;
    }
    
    private Token createKeywordToken(String value, int line, int column) {
        return new Token(
            TokenType.valueOf("KEYWORD_" + value.toUpperCase()), 
            value, line, column
        );
    }
    
    private Token createOperatorToken(String value, int line, int column) {
        TokenType type = switch (value) {
            case "++" -> TokenType.OP_INC;
            case "--" -> TokenType.OP_DEC;
            case "+=" -> TokenType.OP_PLUS_ASSIGN;
            case "-=" -> TokenType.OP_MINUS_ASSIGN;
            case "*=" -> TokenType.OP_MULT_ASSIGN;
            case "/=" -> TokenType.OP_DIV_ASSIGN;
            case "==" -> TokenType.OP_EQ;
            case "!=" -> TokenType.OP_NE;
            case "<=" -> TokenType.OP_LE;
            case ">=" -> TokenType.OP_GE;
            case "&&" -> TokenType.OP_AND;
            case "||" -> TokenType.OP_OR;
            case "+" -> TokenType.OP_PLUS;
            case "-" -> TokenType.OP_MINUS;
            case "*" -> TokenType.OP_MULT;
            case "/" -> TokenType.OP_DIV;
            case "=" -> TokenType.OP_ASSIGN;
            case "<" -> TokenType.OP_LT;
            case ">" -> TokenType.OP_GT;
            case "!" -> TokenType.OP_NOT;
            case "&" -> TokenType.OP_AND;
            case "|" -> TokenType.OP_OR;
            default -> throw new RuntimeException("Unknown operator: " + value);
        };
        return new Token(type, value, line, column);
    }
    
    private Token createSeparatorToken(String value, int line, int column) {
        TokenType type = switch (value) {
            case "(" -> TokenType.LPAREN;
            case ")" -> TokenType.RPAREN;
            case "{" -> TokenType.LBRACE;
            case "}" -> TokenType.RBRACE;
            case "[" -> TokenType.LBRACKET;
            case "]" -> TokenType.RBRACKET;
            case "," -> TokenType.COMMA;
            case ";" -> TokenType.SEMICOLON;
            case ":" -> TokenType.COLON;
            case "." -> TokenType.DOT;
            default -> throw new RuntimeException("Unknown separator: " + value);
        };
        return new Token(type, value, line, column);
    }
    
    private void updateLineInfo(String text, int line, int lineStart) {
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                line++;
                lineStart = pos;
            }
        }
    }
    
    public static void main(String[] args) {
        String glslCode = 
            "#version 330\n" +
            "uniform mat4 MVP;\n" +
            "\n" +
            "in vec3 position;\n" +
            "in vec2 texCoord;\n" +
            "out vec2 uv;\n" +
            "\n" +
            "void main() {\n" +
            "   gl_Position = MVP * vec4(position, 1.0);\n" +
            "   uv = texCoord;\n" +
            "   // This is a comment\n" +
            "   /* Multi-line \n" +
            "      comment */\n" +
            "}";
        
        GLSLLexer lexer = new GLSLLexer(glslCode);
        List<Token> tokens = lexer.tokenize();
        
        System.out.println("GLSL Lexer Output:");
        System.out.println("=".repeat(60));
        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}