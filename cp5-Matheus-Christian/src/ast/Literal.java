package ast;

import visitor.Visitor;

/**
 * Literal de um dos cinco tipos básicos. kind é o nome do tipo
 * (int, double, char, string, bool) e value é o lexema, do jeito que
 * está no fonte ("120", "0.5", "'x'", "\"ok\"", "true").
 */
public class Literal extends Expr {

    public final String kind;
    public final String value;

    public Literal(int line, int column, String kind, String value) {
        super(line, column);
        this.kind = kind;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitLiteral(this);
    }
}
