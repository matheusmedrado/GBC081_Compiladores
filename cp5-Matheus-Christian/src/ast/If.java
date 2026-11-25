package ast;

import visitor.Visitor;

/**
 * IfStmt → 'if' '(' Expr ')' Block ElsePart.
 * elseBranch é null quando não há else. No "else if", elseBranch é outro If.
 */
public class If extends Stmt {

    public final Expr condition;
    public final Block thenBranch;
    public final StmtNode elseBranch;

    public If(int line, int column, Expr condition, Block thenBranch, StmtNode elseBranch) {
        super(line, column);
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitIf(this);
    }
}
