package ast;

import visitor.Visitor;

/** ReturnStmt → 'return' ReturnValue ';'. value é null no "return;". */
public class Return extends Stmt {

    public final Expr value;

    public Return(int line, int column, Expr value) {
        super(line, column);
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitReturn(this);
    }
}
