package ast;

import visitor.Visitor;

/** Operação unária prefixa: '!' ou '-'. Podem repetir ("!!x", "- -x"). */
public class Unary extends Expr {

    public final String operator;
    public final Expr operand;

    public Unary(int line, int column, String operator, Expr operand) {
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitUnary(this);
    }
}
