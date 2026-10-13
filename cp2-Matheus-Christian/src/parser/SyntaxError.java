package parser;

public class SyntaxError {
    public final String message;
    public final int line;
    public final int column;

    public SyntaxError(String message, int line, int column) {
        this.message = message;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("erro sintático em %d:%d: %s", line, column, message);
    }
}
