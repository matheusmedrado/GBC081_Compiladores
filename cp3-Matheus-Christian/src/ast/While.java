package ast;

import visitor.Visitor;

/** WhileStmt → 'while' '(' Expr ')' Block */
public class While extends Stmt {

    public final Expr condition;
    public final Block body;

    public While(int line, int column, Expr condition, Block body) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitWhile(this);
    }
}
