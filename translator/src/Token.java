// Класс токена
public class Token {
    public final TokenType1 type;
    public final String value;
    public final int line;
    public final int column;
    
    public Token(TokenType1 type, String value, int line, int column) {
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