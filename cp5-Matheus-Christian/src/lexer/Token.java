package lexer;

public class Token {
    public final TokenType type;
    public final String lexeme;   // texto original, do jeito que está no fonte
    public final int line;        // linha e coluna onde o token começa (a partir de 1)
    public final int column;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("%d:%d %s '%s'", line, column, type, lexeme);
    }
}
