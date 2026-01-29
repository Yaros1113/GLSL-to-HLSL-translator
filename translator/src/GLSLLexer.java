import java.util.*;
import java.util.regex.*;

public class GLSLLexer {
    private final String input;
    private int pos;
    private final List<Token> tokens = new ArrayList<>();
    private static final Pattern TOKEN_PATTERNS;
    
    // Статическая инициализация регулярных выражений
    static {
        // Компилируем все шаблоны в один большой RegEx
        String patterns = 
            "(?<PREPROCESSOR>#.*)" + "|" +
            "(?<KEYWORD>\\b(void|float|int|bool|sampler2D|samplerCube|vec2|vec3|vec4|mat2|mat3|mat4|" + //
            "if|else|for|while|do|return|break|continue|uniform|attribute|varying|in|out|inout|struct)\\b)" + "|" +
            "(?<BOOL>\\b(true|false)\\b)" + "|" +
            "(?<FLOAT>\\d+\\.\\d*([eE][-+]?\\d+)?|\\d*\\.\\d+([eE][-+]?\\d+)?|\\d+[eE][-+]?\\d+)" + "|" +
            "(?<INT>\\b\\d+\\b)" + "|" +
            "(?<STRING>\"([^\"\\\\]|\\\\.)*\")" + "|" +
            "(?<OP>\\+\\+|--|\\+=|-=|\\*=|/=|==|!=|<=|>=|&&|\\|\\||[+\\-*/=<>!&|])" + "|" +
            "(?<SEPARATOR>[\\(\\)\\{\\}\\[\\],;:\\.?])" + "|" +
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
                tokens.add(new Token(TokenType1.PREPROCESSOR_DIRECTIVE,
                                    matcher.group(), line, column));
            }
            else if (matcher.group("KEYWORD") != null) {
                tokens.add(createKeywordToken(matcher.group(), line, column));
            }
            else if (matcher.group("BOOL") != null) {
                tokens.add(new Token(TokenType1.BOOL_LITERAL,
                                    matcher.group(), line, column));
            }
            else if (matcher.group("FLOAT") != null) {
                tokens.add(new Token(TokenType1.FLOAT_LITERAL,
                                    matcher.group(), line, column));
            }
            else if (matcher.group("INT") != null) {
                tokens.add(new Token(TokenType1.INT_LITERAL,
                                    matcher.group(), line, column));
            }
            else if (matcher.group("STRING") != null) {
                tokens.add(new Token(TokenType1.STRING_LITERAL,
                                    matcher.group(), line, column));
            }
            else if (matcher.group("OP") != null) {
                tokens.add(createOperatorToken(matcher.group(), line, column));
            }
            else if (matcher.group("SEPARATOR") != null) {
                tokens.add(createSeparatorToken(matcher.group(), line, column));
            }
            else if (matcher.group("IDENTIFIER") != null) {
                tokens.add(new Token(TokenType1.IDENTIFIER,
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

        tokens.add(new Token(TokenType1.EOF, "", line, pos - lineStart + 1));
        return tokens;
    }

    private Token createKeywordToken(String value, int line, int column) {
        return new Token(
            TokenType1.valueOf("KEYWORD_" + value.toUpperCase()),
            value, line, column
        );
    }

    private Token createOperatorToken(String value, int line, int column) {
        TokenType1 type = switch (value) {
            case "++" -> TokenType1.OP_INC;
            case "--" -> TokenType1.OP_DEC;
            case "+=" -> TokenType1.OP_PLUS_ASSIGN;
            case "-=" -> TokenType1.OP_MINUS_ASSIGN;
            case "*=" -> TokenType1.OP_MULT_ASSIGN;
            case "/=" -> TokenType1.OP_DIV_ASSIGN;
            case "==" -> TokenType1.OP_EQ;
            case "!=" -> TokenType1.OP_NE;
            case "<=" -> TokenType1.OP_LE;
            case ">=" -> TokenType1.OP_GE;
            case "&&" -> TokenType1.OP_AND;
            case "||" -> TokenType1.OP_OR;
            case "+" -> TokenType1.OP_PLUS;
            case "-" -> TokenType1.OP_MINUS;
            case "*" -> TokenType1.OP_MULT;
            case "/" -> TokenType1.OP_DIV;
            case "=" -> TokenType1.OP_ASSIGN;
            case "<" -> TokenType1.OP_LT;
            case ">" -> TokenType1.OP_GT;
            case "!" -> TokenType1.OP_NOT;
            case "&" -> TokenType1.OP_AND;
            case "|" -> TokenType1.OP_OR;
            default -> throw new RuntimeException("Unknown operator: " + value);
        };
        return new Token(type, value, line, column);
    }

    private Token createSeparatorToken(String value, int line, int column) {
        TokenType1 type = switch (value) {
            case "(" -> TokenType1.LPAREN;
            case ")" -> TokenType1.RPAREN;
            case "{" -> TokenType1.LBRACE;
            case "}" -> TokenType1.RBRACE;
            case "[" -> TokenType1.LBRACKET;
            case "]" -> TokenType1.RBRACKET;
            case "," -> TokenType1.COMMA;
            case ";" -> TokenType1.SEMICOLON;
            case ":" -> TokenType1.COLON;
            case "." -> TokenType1.DOT;
            case "?" -> TokenType1.QUESTION;
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

}