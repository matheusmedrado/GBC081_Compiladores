package ast;

import visitor.Visitor;

/** Atribuição: "x = expr;". Atribuição é comando, não expressão. */
public class Assign extends Stmt {

    public final String name;
    public final Expr value;

    public Assign(int line, int column, String name, Expr value) {
        super(line, column);
        this.name = name;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitAssign(this);
    }
}
