package ast;

import visitor.Visitor;

/** Chamada usada como comando: "imprime(x);". */
public class CallStmt extends Stmt {

    public final Call call;

    public CallStmt(int line, int column, Call call) {
        super(line, column);
        this.call = call;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCallStmt(this);
    }
}
