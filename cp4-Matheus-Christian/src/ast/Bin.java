package ast;

import visitor.Visitor;

/**
 * Operação binária. A associatividade à esquerda está na forma da árvore:
 * a - b - c vira Bin(-, Bin(-, a, b), c).
 */
public class Bin extends Expr {

    public final String operator;
    public final Expr left;
    public final Expr right;

    public Bin(int line, int column, String operator, Expr left, Expr right) {
        super(line, column);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBin(this);
    }
}
